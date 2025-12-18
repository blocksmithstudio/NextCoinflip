package com.muhammaddaffa.nextcoinflip.coinflip.impl;

import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.ItemBuilder;
import com.muhammaddaffa.mdlib.utils.WordUtils;
import com.muhammaddaffa.nextcoinflip.NextCoinflip;
import com.muhammaddaffa.nextcoinflip.coinflip.CoinflipGame;
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
        UserRepository.getUser(this.getCreator()).addWonItem(offers[0]);
        // Get all challenger
        for (ChallengerDetails detail : this.challengerMap.values()) {
            Player player = Bukkit.getPlayer(detail.uuid());
            // Cancel the challenge
            this.cancelChallenge(player);
            // Sends message
            NextCoinflip.DEFAULT_CONFIG.sendMessage(player, "messages.offer-cancelled");
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
        for (ItemStack stack : List.of(offers)) {
            UserRepository.getUser(winner).addWonItem(stack);
        }

        // Save the user here
        winnerUser.save(true);
        loserUser.save(true);
    }

    @Override
    public void onClick(Player clicker) {
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
        ConfirmationItemCoinflipChallengerGUI menu = new ConfirmationItemCoinflipChallengerGUI(clicker, stack, Bukkit.getPlayer(getCreator()), getOffers()[0]);
        menu.onCancel(() -> {
            clicker.closeInventory();
            // Play sounds
            Common.playSound(clicker, Sound.UI_BUTTON_CLICK);
            Common.playSound(clicker, Sound.ENTITY_VILLAGER_NO);
        });
        menu.onAccept(() -> {
            // Add challenger
            this.addChallenger(clicker, stack);
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
        ChallengerDetails details = this.challengerMap.remove(player.getName().toLowerCase());
        if (details != null) {
            UserRepository.getUser(player).addWonItem(details.itemStack());
        }
    }

    public void cancelAllExcept(ChallengerDetails challenger) {
        this.challengerMap.forEach((name, detail) -> {
            if (!detail.uuid().equals(challenger.uuid())) {
                Player other = Bukkit.getPlayer(detail.uuid());
                // Cancel the challenge
                this.cancelChallenge(other);
                // Sends message
                NextCoinflip.DEFAULT_CONFIG.sendMessage(other, "messages.offer-cancelled");
            }
        });
    }

    public void addChallenger(Player challenger, ItemStack stack) {
        if (this.isStarting()) {
            return;
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
        Player creator = Bukkit.getPlayer(this.getCreator());
        HoverableMessage.sendHoverableItemMessage(creator, clone, NextCoinflip.DEFAULT_CONFIG.getString("messages.item-challenge")
                .replace("{challenger_display_name}", challenger.getDisplayName()));
        // Send clickable message
        HoverableMessage.sendClickableTextMessage(creator, NextCoinflip.DEFAULT_CONFIG.getString("messages.item-challenge-accept")
                .replace("{player_name}", challenger.getName()), "/cf accept " + challenger.getName());
    }

    public record ChallengerDetails(
            UUID uuid,
            String displayName,
            ItemStack itemStack,
            long timestamp
    ) {

    }

}
