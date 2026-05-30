package com.muhammaddaffa.nextcoinflip.coinflip.impl;

import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.ItemBuilder;
import com.muhammaddaffa.mdlib.utils.WordUtils;
import com.muhammaddaffa.nextcoinflip.NextCoinflip;
import com.muhammaddaffa.nextcoinflip.coinflip.CoinflipGame;
import com.muhammaddaffa.nextcoinflip.coinflip.repository.CoinflipRepository;
import com.muhammaddaffa.nextcoinflip.gui.ConfirmationItemCoinflipChallengerGUI;
import com.muhammaddaffa.nextcoinflip.users.User;
import com.muhammaddaffa.nextcoinflip.users.repository.UserRepository;
import com.muhammaddaffa.nextcoinflip.utils.HoverableMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ItemCoinflipGame extends CoinflipGame {

    private final ItemStack[] offers = new ItemStack[2];

    private final Map<String, ChallengerDetails> challengerMap = new ConcurrentHashMap<>();

    public ItemCoinflipGame(UUID creator, ItemStack stack) {
        super(creator);
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
        // Get the display name of the item
        ItemStack stack = offers[0];
        ItemMeta meta = stack.getItemMeta();
        String itemDisplayName = (meta.getDisplayName() == null || meta.getDisplayName().isEmpty()) ?
                WordUtils.getNiceMaterialName(stack.getType()) :
                meta.getDisplayName();

        List<String> lore = new ArrayList<>();
        // Step 1: Add header lore
        lore.addAll(config.getStringList("wager-items.item.header-lore"));
        // Step 2: Add lore from the item
        if (meta.getLore() != null) lore.addAll(meta.getLore());
        // Step 3: Add footer lore
        lore.addAll(config.getStringList("wager-items.item.footer-lore"));

        return new ItemBuilder(stack.clone())
                .name(config.getString("wager-items.item.display-name"))
                .lore(lore)
                .placeholder(getDefaultPlaceholders()
                        .add("{item_display_name}", itemDisplayName)
                        .add("{item_amount}", stack.getAmount()))
                .build();
    }

    @Override
    public void cancel(boolean shutdown) {
        // Add won item object for player
        this.refundItem(this.getCreator(), offers[0], !shutdown);
        // Get all challenger
        for (ChallengerDetails detail : new ArrayList<>(this.challengerMap.values())) {
            Player player = Bukkit.getPlayer(detail.uuid());
            // Cancel the challenge
            this.cancelChallenge(detail.uuid(), !shutdown);
            // Sends message
            if (player != null) {
                NextCoinflip.DEFAULT_CONFIG.sendMessage(player, "messages.offer-cancelled");
            }
        }
    }

    @Override
    public void finish(UUID winner, UUID loser) {
        // Add statistic to winner and loser
        User winnerUser = UserRepository.getUser(winner);
        winnerUser.getStatistic().resetLoseStreak();
        winnerUser.getStatistic().addWins();

        User loserUser = UserRepository.getUser(loser);
        loserUser.getStatistic().addLosses();
        loserUser.getStatistic().addLoseStreak();

        // Give player both items from the offers
        for (ItemStack stack : offers) {
            if (stack != null) {
                UserRepository.getUser(winner).addWonItem(stack);
            }
        }

        // Save the user here
        winnerUser.save(true);
        loserUser.save(true);
    }

    @Override
    public void onClick(Player clicker) {
        Player creator = Bukkit.getPlayer(getCreator());
        if (creator == null || !CoinflipRepository.contains(this) || this.isStarting()) {
            NextCoinflip.DEFAULT_CONFIG.sendMessage(clicker, "messages.not-valid");
            return;
        }
        // Check if player already has an offer
        if (getChallenger(clicker.getName()) != null) {
            NextCoinflip.DEFAULT_CONFIG.sendMessage(clicker, "messages.already-offered");
            return;
        }
        ItemStack stack = clicker.getInventory().getItemInMainHand();
        if (stack == null || stack.getType() == Material.AIR) {
            NextCoinflip.DEFAULT_CONFIG.sendMessage(clicker, "messages.invalid-item");
            return;
        }
        // Open the menu for the clicker
        ConfirmationItemCoinflipChallengerGUI menu = new ConfirmationItemCoinflipChallengerGUI(clicker, stack, creator, getOffers()[0]);
        menu.onCancel(() -> {
            clicker.closeInventory();
            // Play sounds
            Common.playSound(clicker, Sound.UI_BUTTON_CLICK);
            Common.playSound(clicker, Sound.ENTITY_VILLAGER_NO);
        });
        menu.onAccept(() -> {
            // Add challenger
            if (!this.addChallenger(clicker, stack)) {
                NextCoinflip.DEFAULT_CONFIG.sendMessage(clicker, "messages.not-valid");
                Common.playSound(clicker, Sound.ENTITY_VILLAGER_NO);
                clicker.closeInventory();
                return;
            }
            // Close inventory for the player
            clicker.closeInventory();
            // Play sounds
            Common.playSound(clicker, Sound.UI_BUTTON_CLICK);
            Common.playSound(clicker, Sound.BLOCK_NOTE_BLOCK_PLING);
        });
        // Open the menu for player
        menu.open(clicker);
    }

    public ChallengerDetails getChallenger(String name) {
        return challengerMap.get(name.toLowerCase());
    }

    public Set<String> getChallengersName() {
        return challengerMap.keySet();
    }

    public Map<String, ChallengerDetails> getChallengerMap() {
        return challengerMap;
    }

    public void cancelChallenge(Player player) {
        if (player == null) {
            return;
        }
        this.cancelChallenge(player.getUniqueId());
    }

    public void cancelChallenge(UUID uuid) {
        this.cancelChallenge(uuid, true);
    }

    private void cancelChallenge(UUID uuid, boolean asyncSave) {
        String key = this.challengerMap.entrySet()
                .stream()
                .filter(entry -> entry.getValue().uuid().equals(uuid))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
        if (key == null) {
            return;
        }
        ChallengerDetails details = this.challengerMap.remove(key);
        if (details != null) {
            this.refundItem(details.uuid(), details.itemStack(), asyncSave);
        }
    }

    public void cancelAllExcept(ChallengerDetails challenger) {
        new ArrayList<>(this.challengerMap.values()).forEach(detail -> {
            if (!detail.uuid().equals(challenger.uuid())) {
                Player other = Bukkit.getPlayer(detail.uuid());
                // Cancel the challenge
                this.cancelChallenge(detail.uuid());
                // Sends message
                if (other != null) {
                    NextCoinflip.DEFAULT_CONFIG.sendMessage(other, "messages.offer-cancelled");
                }
            }
        });
    }

    public boolean addChallenger(Player challenger, ItemStack stack) {
        if (this.isStarting() || !CoinflipRepository.contains(this) || stack == null || stack.getType() == Material.AIR || stack.getAmount() <= 0) {
            return false;
        }
        Player creator = Bukkit.getPlayer(this.getCreator());
        if (creator == null) {
            return false;
        }
        // First, make the item stack clone first
        ItemStack clone = stack.clone();
        // Remove the item stack, so the original is gone
        stack.setAmount(0);

        // Add the challenger to the map
        this.challengerMap.put(challenger.getName().toLowerCase(), new ChallengerDetails(challenger.getUniqueId(), challenger.getDisplayName(), clone, System.currentTimeMillis()));

        // Send item offer message
        NextCoinflip.DEFAULT_CONFIG.sendMessage(challenger, "messages.item-offer");

        // Send notification to the creator
        HoverableMessage.sendHoverableItemMessage(creator, clone, NextCoinflip.DEFAULT_CONFIG.getString("messages.item-challenge")
                .replace("{challenger_display_name}", challenger.getDisplayName())
                .replace("{challenger_name}", challenger.getName()));
        // Send clickable message
        HoverableMessage.sendClickableTextMessage(creator, NextCoinflip.DEFAULT_CONFIG.getString("messages.item-challenge-accept")
                .replace("{player_name}", challenger.getName()), "/cf accept " + challenger.getName());
        return true;
    }

    private void refundItem(UUID uuid, ItemStack stack, boolean asyncSave) {
        if (stack == null || stack.getType() == Material.AIR || stack.getAmount() <= 0) {
            return;
        }
        User user = UserRepository.getUser(uuid);
        user.addWonItem(stack);
        user.save(asyncSave);
    }

    public record ChallengerDetails(
            UUID uuid,
            String displayName,
            ItemStack itemStack,
            long timestamp
    ) {

    }

}
