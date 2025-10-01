package com.muhammaddaffa.nextcoinflip.gui;

import com.muhammaddaffa.mdlib.fastinv.FastInv;
import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.ItemBuilder;
import com.muhammaddaffa.nextcoinflip.NextCoinflip;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ConfirmationItemCoinflipCreationGUI extends FastInv {

    private ItemStack originalItem;
    private Runnable onAccept, onCancel;

    public ConfirmationItemCoinflipCreationGUI(ItemStack originalItem) {
        super(NextCoinflip.CONFIRM_ITEM_WAGER_CREATION_CONFIG.getInt("size"),
                Common.color(NextCoinflip.CONFIRM_ITEM_WAGER_CREATION_CONFIG.getString("title")));
        this.originalItem = originalItem;

        this.setAllItems();
    }

    public ConfirmationItemCoinflipCreationGUI onAccept(Runnable runnable) {
        onAccept = runnable;
        return this;
    }

    public ConfirmationItemCoinflipCreationGUI onCancel(Runnable runnable) {
        onCancel = runnable;
        return this;
    }

    private void setAllItems() {
        this.setAdditionalItems();
        this.setPreviewItem();
    }

    private void setPreviewItem() {
        FileConfiguration config = NextCoinflip.CONFIRM_ITEM_WAGER_CREATION_CONFIG.getConfig();
        List<Integer> slots = config.getIntegerList("preview-item.slots");

        // We should clone the original item and modify some things
        ItemStack stack = new ItemBuilder(this.originalItem.clone())
                .name(config.getString("preview-item.display-name"))
                .addLore(config.getStringList("preview-item.lore"))
                .build();

        // Set the items on the menu
        this.setItems(slots, stack);
    }

    private void setAdditionalItems() {
        FileConfiguration config = NextCoinflip.CONFIRM_ITEM_WAGER_CREATION_CONFIG.getConfig();
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
