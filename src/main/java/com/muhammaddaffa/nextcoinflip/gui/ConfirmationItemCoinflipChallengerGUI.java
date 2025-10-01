package com.muhammaddaffa.nextcoinflip.gui;

import com.muhammaddaffa.mdlib.fastinv.FastInv;
import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.ItemBuilder;
import com.muhammaddaffa.mdlib.utils.Placeholder;
import com.muhammaddaffa.nextcoinflip.NextCoinflip;
import com.muhammaddaffa.nextcoinflip.utils.SkullCreator;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ConfirmationItemCoinflipChallengerGUI extends FastInv {

    private final Player challenger, opponent;
    private final ItemStack challengerItem, opponentItem;
    private Runnable onAccept, onCancel;

    public ConfirmationItemCoinflipChallengerGUI(Player challenger, ItemStack challengerItem, Player opponent, ItemStack opponentItem) {
        super(NextCoinflip.CONFIRM_ITEM_WAGER_CHALLENGER_CONFIG.getInt("size"),
                Common.color(NextCoinflip.CONFIRM_ITEM_WAGER_CHALLENGER_CONFIG.getString("title")));
        this.challenger = challenger;
        this.challengerItem = challengerItem;
        this.opponent = opponent;
        this.opponentItem = opponentItem;

        this.setAllItems();
    }

    public ConfirmationItemCoinflipChallengerGUI onAccept(Runnable runnable) {
        onAccept = runnable;
        return this;
    }

    public ConfirmationItemCoinflipChallengerGUI onCancel(Runnable runnable) {
        onCancel = runnable;
        return this;
    }

    private void setAllItems() {
        this.setAdditionalItems();
        this.setPreviewItem();
    }

    private void setPreviewItem() {
        // Set challenger items
        this.setChallengerHead();
        this.setChallengerItem();
        // Set opponent items
        this.setCreatorHead();
        this.setCreatorItem();
    }

    private void setChallengerHead() {
        FileConfiguration config = NextCoinflip.CONFIRM_ITEM_WAGER_CHALLENGER_CONFIG.getConfig();
        List<Integer> slots = config.getIntegerList("challenger-head.slots");
        ItemStack stack = SkullCreator.createItem(challenger)
                .name(config.getString("challenger-head.display-name"))
                .placeholder(new Placeholder()
                        .add("{challenger_displayname}", challenger.getDisplayName())
                        .add("{opponent_displayname}", opponent.getDisplayName()))
                .build();
        // Set the items
        this.setItems(slots, stack);
    }

    private void setChallengerItem() {
        FileConfiguration config = NextCoinflip.CONFIRM_ITEM_WAGER_CHALLENGER_CONFIG.getConfig();
        List<Integer> slots = config.getIntegerList("preview-item.slots.challenger");
        String displayName = config.getString("preview-item.display-name.challenger");
        List<String> lore = config.getStringList("preview-item.lore");
        // Set the item on the menu
        this.setItems(slots, new ItemBuilder(this.challengerItem.clone())
                .name(displayName)
                .lore(lore)
                .build());
    }

    private void setCreatorHead() {
        FileConfiguration config = NextCoinflip.CONFIRM_ITEM_WAGER_CHALLENGER_CONFIG.getConfig();
        List<Integer> slots = config.getIntegerList("opponent-head.slots");
        ItemStack stack = SkullCreator.createItem(opponent)
                .name(config.getString("opponent-head.display-name"))
                .placeholder(new Placeholder()
                        .add("{opponent_displayname}", opponent.getDisplayName())
                        .add("{challenger_displayname}", challenger.getDisplayName()))
                .build();
        // Set the items
        this.setItems(slots, stack);
    }

    private void setCreatorItem() {
        FileConfiguration config = NextCoinflip.CONFIRM_ITEM_WAGER_CHALLENGER_CONFIG.getConfig();
        List<Integer> slots = config.getIntegerList("preview-item.slots.opponent");
        String displayName = config.getString("preview-item.display-name.opponent");
        List<String> lore = config.getStringList("preview-item.lore");
        // Set the item on the menu
        this.setItems(slots, new ItemBuilder(opponentItem.clone())
                .name(displayName)
                .lore(lore)
                .build());
    }

    private void setAdditionalItems() {
        FileConfiguration config = NextCoinflip.CONFIRM_ITEM_WAGER_CHALLENGER_CONFIG.getConfig();
        // Get list of items
        for (String key : config.getConfigurationSection("items").getKeys(false)) {
            String type = config.getString("items." + key + ".type", "DUMMY");
            List<Integer> slots = config.getIntegerList("items." + key + ".slots");
            ItemStack stack = ItemBuilder.fromConfig(config, "items." + key).build();

            // Set the items
            this.setItems(slots, stack, event -> {
                if (!(event.getWhoClicked() instanceof Player player)) return;
                // Check for next page and previous page
                if (type.equalsIgnoreCase("confirm")) {
                    if (onAccept != null) onAccept.run();
                }
                if (type.equalsIgnoreCase("cancel")) {
                    if (onCancel != null) onCancel.run();
                }
            });
        }
    }

}
