package com.muhammaddaffa.nextcoinflip.gui;

import com.muhammaddaffa.mdlib.fastinv.FastInv;
import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.ItemBuilder;
import com.muhammaddaffa.mdlib.utils.Placeholder;
import com.muhammaddaffa.mdlib.utils.WordUtils;
import com.muhammaddaffa.nextcoinflip.NextCoinflip;
import com.muhammaddaffa.nextcoinflip.users.User;
import com.muhammaddaffa.nextcoinflip.users.models.WonItem;
import com.muhammaddaffa.nextcoinflip.users.repository.UserRepository;
import com.muhammaddaffa.nextcoinflip.utils.HoverableMessage;
import com.muhammaddaffa.nextcoinflip.utils.SoundUtil;
import com.muhammaddaffa.nextcoinflip.utils.Utils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class WonItemGUI extends FastInv {

    private final Player player;
    private final User user;

    public WonItemGUI(Player player) {
        super(NextCoinflip.WON_ITEM_CONFIG.getInt("size"), Common.color(NextCoinflip.WON_ITEM_CONFIG.getString("title")));
        this.player = player;
        this.user = UserRepository.getUser(player);

        this.setAllItems();
    }

    private void setAllItems() {
        this.setAdditionalItems();
        this.setWonItems();
    }

    private void setWonItems() {
        FileConfiguration config = NextCoinflip.WON_ITEM_CONFIG.getConfig();
        List<Integer> slots = config.getIntegerList("won-items-slots");
        List<WonItem> wonItems = user.getWonItems();

        for (int i = 0; i < wonItems.size(); i++) {
            // If the counter is larger than the slots, skip it
            if (i >= slots.size()) break;
            WonItem item = wonItems.get(i);
            // Try to get the display name of the item
            ItemStack original = item.getItemStack();
            String displayName = WordUtils.getNiceMaterialName(original.getType());
            // Check if the item has item meta
            if (original.hasItemMeta()) {
                ItemMeta meta = original.getItemMeta();
                String name = meta.getDisplayName();
                if (name != null && !name.isEmpty()) displayName = name;
            }

            // Try to create the item
            ItemStack stack = new ItemBuilder(original.clone())
                    .name(config.getString("won-items-options.display-name"))
                    .addLore(config.getStringList("won-items-options.lore"))
                    .placeholder(new Placeholder()
                            .add("{item_display_name}", displayName)
                            .add("{item_amount}", original.getAmount())
                            .add("{date}", item.getFormattedDate()))
                    .build();

            // Set the item
            this.setItem(slots.get(i), stack, event -> {
                // Check if player has full inventory
                if (Utils.isInventoryFull(player)) {
                    SoundUtil.villagerNo(player);
                    // Sends message
                    NextCoinflip.DEFAULT_CONFIG.sendMessage(player, "messages.inventory-full");
                    return;
                }
                // Remove the item from the list
                if (!user.removeWonItem(item)) {
                    NextCoinflip.DEFAULT_CONFIG.sendMessage(player, "messages.not-valid");
                    player.closeInventory();
                    return;
                }
                Common.addInventoryItem(player, original.clone());
                // Play sounds
                SoundUtil.click(player);
                SoundUtil.pling(player);
                // Close the inventory
                player.closeInventory();
                // Sends message
                HoverableMessage.sendHoverableItemMessage(player, original, NextCoinflip.DEFAULT_CONFIG.getString("messages.item-claim"));
            });
        }

    }

    private void setAdditionalItems() {
        FileConfiguration config = NextCoinflip.WON_ITEM_CONFIG.getConfig();
        // Get list of items
        for (String key : config.getConfigurationSection("items").getKeys(false)) {
            String type = config.getString("items." + key + ".type", "DUMMY");
            List<Integer> slots = config.getIntegerList("items." + key + ".slots");
            ItemStack stack = ItemBuilder.fromConfig(config, "items." + key).build();

            // Set the items
            this.setItems(slots, stack, event -> {
                if (!(event.getWhoClicked() instanceof Player player)) return;
                // Check for next page and previous page
                if (type.equalsIgnoreCase("back")) {
                    new GameSelectorGUI(player).open(player);
                    SoundUtil.click(player);
                }
            });
        }
    }

}
