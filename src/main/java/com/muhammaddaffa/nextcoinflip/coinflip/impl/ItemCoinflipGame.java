package com.muhammaddaffa.nextcoinflip.coinflip.impl;

import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.ItemBuilder;
import com.muhammaddaffa.mdlib.utils.WordUtils;
import com.muhammaddaffa.mdlib.utils.Logger;
import com.muhammaddaffa.nextcoinflip.NextCoinflip;
import com.muhammaddaffa.nextcoinflip.coinflip.CoinflipGame;
import com.muhammaddaffa.nextcoinflip.coinflip.repository.CoinflipRepository;
import com.muhammaddaffa.nextcoinflip.gui.ConfirmationItemCoinflipChallengerGUI;
import com.muhammaddaffa.nextcoinflip.utils.HoverableMessage;
import com.muhammaddaffa.nextcoinflip.utils.Utils;
import com.muhammaddaffa.nextcoinflip.utils.Serializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ItemCoinflipGame extends CoinflipGame {

    private final ItemStack[] offers = new ItemStack[2];

    private final Map<String, ChallengerDetails> challengerMap = new ConcurrentHashMap<>();
    private final Map<UUID, Settlement> refunds = new HashMap<>();

    public ItemCoinflipGame(UUID creator, ItemStack stack) {
        super(creator);
        if (stack == null || stack.getType().isAir() || stack.getAmount() <= 0) {
            throw new IllegalArgumentException("An item wager must not be empty");
        }
        offers[0] = stack.clone();
    }

    public ItemStack[] getOffers() {
        return offers;
    }

    @Override
    public String getId() {
        return "item";
    }

    @Override
    public ItemStack getItem() {
        FileConfiguration config = NextCoinflip.GAME_SELECTOR_MENU_CONFIG.getConfig();
        ItemStack stack = offers[0];
        ItemMeta meta = stack.getItemMeta();
        String itemDisplayName = (meta.getDisplayName() == null || meta.getDisplayName().isEmpty()) ?
                WordUtils.getNiceMaterialName(stack.getType()) :
                meta.getDisplayName();

        List<String> lore = new ArrayList<>();
        lore.addAll(config.getStringList("wager-items.item.header-lore"));
        if (meta.getLore() != null) lore.addAll(meta.getLore());
        lore.addAll(config.getStringList("wager-items.item.footer-lore"));

        return new ItemBuilder(stack.clone())
                .name(config.getString("wager-items.item.display-name"))
                .lore(lore)
                .placeholder(getDefaultPlaceholders()
                        .add("{item_display_name}", "&f" + itemDisplayName)
                        .add("{item_amount}", stack.getAmount()))
                .build();
    }

    @Override
    public void cancel(boolean shutdown) {
        this.settle(null, () -> {
            List<Settlement> deliveries = new ArrayList<>();
            deliveries.add(this.updateUser(this.getCreator(), user -> user.addWonItem(this.offers[0])));
            if (this.getChallenger() != null && this.offers[1] != null) {
                deliveries.add(this.updateUser(this.getChallenger(), user -> user.addWonItem(this.offers[1])));
            }
            for (ChallengerDetails detail : new ArrayList<>(this.challengerMap.values())) {
                deliveries.add(new Settlement(detail.uuid(), "offer-refund", () -> {
                    this.cancelChallenge(detail.uuid());
                    Player player = Bukkit.getPlayer(detail.uuid());
                    if (player != null) {
                        NextCoinflip.DEFAULT_CONFIG.sendMessage(player, "messages.offer-cancelled");
                    }
                }));
            }
            return deliveries;
        });
    }

    @Override
    public void finish(UUID winner, UUID loser) {
        this.settle(winner, () -> {
            List<Settlement> deliveries = new ArrayList<>();
            deliveries.add(this.updateUser(winner, user -> {
                user.getStatistic().resetLoseStreak();
                user.getStatistic().addWins();
                for (ItemStack stack : this.offers) {
                    if (stack != null) {
                        user.addWonItem(stack);
                    }
                }
            }));
            deliveries.add(this.updateUser(loser, user -> {
                user.getStatistic().addLosses();
                user.getStatistic().addLoseStreak();
            }));
            for (ChallengerDetails detail : new ArrayList<>(this.challengerMap.values())) {
                deliveries.add(new Settlement(detail.uuid(), "offer-refund", () -> this.cancelChallenge(detail.uuid())));
            }
            return deliveries;
        });
    }

    @Override
    public void onClick(Player clicker) {
        Player creator = Bukkit.getPlayer(getCreator());
        if (creator == null || !CoinflipRepository.contains(this) || this.isStarting()) {
            NextCoinflip.DEFAULT_CONFIG.sendMessage(clicker, "messages.not-valid");
            return;
        }
        if (getChallenger(clicker.getName()) != null) {
            NextCoinflip.DEFAULT_CONFIG.sendMessage(clicker, "messages.already-offered");
            return;
        }
        ItemStack stack = clicker.getInventory().getItemInMainHand();
        if (stack == null || stack.getType() == Material.AIR) {
            NextCoinflip.DEFAULT_CONFIG.sendMessage(clicker, "messages.invalid-item");
            return;
        }
        stack = stack.clone();
        final ItemStack expected = stack;
        ConfirmationItemCoinflipChallengerGUI menu = new ConfirmationItemCoinflipChallengerGUI(clicker, stack, creator, getOffers()[0]);
        menu.onCancel(() -> {
            clicker.closeInventory();
            Common.playSound(clicker, Sound.UI_BUTTON_CLICK);
            Common.playSound(clicker, Sound.ENTITY_VILLAGER_NO);
        });
        menu.onAccept(() -> {
            if (!this.addChallenger(clicker, expected)) {
                NextCoinflip.DEFAULT_CONFIG.sendMessage(clicker, "messages.not-valid");
                Common.playSound(clicker, Sound.ENTITY_VILLAGER_NO);
                clicker.closeInventory();
                return;
            }
            clicker.closeInventory();
            Common.playSound(clicker, Sound.UI_BUTTON_CLICK);
            Common.playSound(clicker, Sound.BLOCK_NOTE_BLOCK_PLING);
        });
        menu.open(clicker);
    }

    public ChallengerDetails getChallenger(String name) {
        return challengerMap.get(name.toLowerCase(Locale.ROOT));
    }

    public Set<String> getChallengersName() {
        return challengerMap.keySet();
    }

    public Map<String, ChallengerDetails> getChallengerMap() {
        return challengerMap;
    }

    @Override
    public Map<String, Object> recoveryData() {
        Map<String, Object> data = super.recoveryData();
        data.put("creator-item", Serializer.serialize(this.offers[0]));
        data.put("challenger-item", this.offers[1] == null ? null : Serializer.serialize(this.offers[1]));
        List<Map<String, Object>> pending = new ArrayList<>();
        for (ChallengerDetails offer : this.challengerMap.values()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("player", offer.uuid().toString());
            entry.put("item", Serializer.serialize(offer.itemStack()));
            Settlement refund = this.refunds.get(offer.uuid());
            entry.put("remaining-steps", refund == null ? 3 : refund.remainingSteps());
            pending.add(entry);
        }
        data.put("pending-offers", pending);
        return data;
    }

    @Override
    public boolean involves(UUID uuid) {
        return super.involves(uuid) || this.challengerMap.values().stream().anyMatch(offer -> offer.uuid().equals(uuid));
    }

    public void retryRefunds() {
        RuntimeException failure = null;
        for (UUID uuid : new ArrayList<>(this.refunds.keySet())) {
            try {
                this.cancelChallenge(uuid);
            } catch (RuntimeException ex) {
                if (failure == null) {
                    failure = new IllegalStateException("Item refunds remain incomplete");
                }
                failure.addSuppressed(ex);
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    public void cancelChallenge(Player player) {
        if (player == null) {
            return;
        }
        this.cancelChallenge(player.getUniqueId());
    }

    public void cancelChallenge(UUID uuid) {
        String key = this.challengerMap.entrySet()
                .stream()
                .filter(entry -> entry.getValue().uuid().equals(uuid))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
        if (key == null) {
            return;
        }
        ChallengerDetails details = this.challengerMap.get(key);
        if (details != null) {
            Settlement refund = this.refunds.computeIfAbsent(uuid,
                    ignored -> this.updateUser(uuid, user -> user.addWonItem(details.itemStack())));
            refund.run();
            this.challengerMap.remove(key, details);
            this.refunds.remove(uuid);
        }
    }

    public void cancelAllExcept(ChallengerDetails challenger) {
        new ArrayList<>(this.challengerMap.values()).forEach(detail -> {
            if (!detail.uuid().equals(challenger.uuid())) {
                Player other = Bukkit.getPlayer(detail.uuid());
                try {
                    this.cancelChallenge(detail.uuid());
                } catch (RuntimeException ex) {
                    Logger.severe("An item offer refund remains pending for game " + this.getUniqueId());
                }
                if (other != null) {
                    NextCoinflip.DEFAULT_CONFIG.sendMessage(other, "messages.offer-cancelled");
                }
            }
        });
    }

    public boolean addChallenger(Player challenger, ItemStack stack) {
        if (this.isStarting() || !CoinflipRepository.contains(this)
                || getCreator().equals(challenger.getUniqueId()) || getChallenger(challenger.getName()) != null) {
            return false;
        }
        Player creator = Bukkit.getPlayer(this.getCreator());
        if (creator == null) {
            return false;
        }
        ItemStack clone = Utils.takeWagerItem(challenger, stack);
        if (clone == null) return false;

        this.challengerMap.put(challenger.getName().toLowerCase(Locale.ROOT), new ChallengerDetails(challenger.getUniqueId(), challenger.getDisplayName(), clone, System.currentTimeMillis()));

        NextCoinflip.DEFAULT_CONFIG.sendMessage(challenger, "messages.item-offer");

        HoverableMessage.sendHoverableItemMessage(creator, clone, NextCoinflip.DEFAULT_CONFIG.getString("messages.item-challenge")
                .replace("{challenger_display_name}", challenger.getDisplayName())
                .replace("{challenger_name}", challenger.getName()));
        HoverableMessage.sendClickableTextMessage(creator, NextCoinflip.DEFAULT_CONFIG.getString("messages.item-challenge-accept")
                .replace("{player_name}", challenger.getName()), "/cf accept " + challenger.getName());
        return true;
    }

    public boolean acceptChallenge(String name, ChallengerDetails expected) {
        if (getChallenger(name) != expected || this.refunds.containsKey(expected.uuid()) || !canStart(expected.uuid())) {
            return false;
        }
        setChallenger(expected.uuid());
        offers[1] = expected.itemStack();
        if (!start()) {
            setChallenger(null);
            offers[1] = null;
            return false;
        }
        challengerMap.remove(name.toLowerCase(Locale.ROOT));
        cancelAllExcept(expected);
        return true;
    }

    public record ChallengerDetails(
            UUID uuid,
            String displayName,
            ItemStack itemStack,
            long timestamp
    ) {

    }

}
