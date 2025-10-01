package com.muhammaddaffa.nextcoinflip.utils;

import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.WordUtils;
import com.muhammaddaffa.nextcoinflip.NextCoinflip;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.chat.hover.content.Item;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class HoverableMessage {

    public static void broadcastHoverableItemMessage(ItemStack stack, String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendHoverableItemMessage(player, stack, message);
        }
    }

    public static void broadcastClickableTextMessage(String message, String command) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendClickableTextMessage(player, message, command);
        }
    }

    public static void sendHoverableItemMessage(Player player, ItemStack stack, String message) {
        // Parse the message color codes
        String[] parts = message.split("\\{wager}", 2);
        String beforeWager = Common.color(parts[0]);
        String afterWager = Common.color(parts.length > 1 ? parts[1] : "");

        // Get the item's display name and amount
        String itemDisplayName = stack.hasItemMeta() && stack.getItemMeta().hasDisplayName() ?
                stack.getItemMeta().getDisplayName() :
                WordUtils.getNiceMaterialName(stack.getType());
        int itemAmount = stack.getAmount();

        // Create the item message
        String hoverMessage = NextCoinflip.DEFAULT_CONFIG.getString("item-wager.hover-item")
                .replace("{item_displayname}", itemDisplayName)
                .replace("{item_amount}", itemAmount + "");

        // Create the hoverable component for the item
        TextComponent hoverableComponent = new TextComponent(Common.color(hoverMessage));
        hoverableComponent.setHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_ITEM,
                new Item(stack.getType().getKey().toString(),
                        stack.getAmount(),
                        ItemTag.ofNbt(stack.getItemMeta() != null ? stack.getItemMeta().getAsString() : ""))
        ));

        // Create the final message
        TextComponent finalMessage = new TextComponent(TextComponent.fromLegacyText(beforeWager));
        finalMessage.addExtra(hoverableComponent);
        finalMessage.addExtra(new TextComponent(TextComponent.fromLegacyText(afterWager)));

        // Send the message to the player
        player.spigot().sendMessage(finalMessage);
    }

    public static void sendClickableTextMessage(Player player, String message, String command) {
        // Parse the message color codes
        String[] parts = message.split("\\{click}", 2);
        String beforeClick = Common.color(parts[0]);
        String afterClick = Common.color(parts.length > 1 ? parts[1] : "");

        // Create the clickable component
        String clickText = NextCoinflip.DEFAULT_CONFIG.getString("item-wager.accept-button");
        TextComponent clickableComponent = new TextComponent(Common.color(clickText));
        clickableComponent.setClickEvent(new ClickEvent(
                ClickEvent.Action.RUN_COMMAND,
                command
        ));
        clickableComponent.setHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(command).color(ChatColor.GREEN).create()
        ));

        // Create the final message
        TextComponent finalMessage = new TextComponent(TextComponent.fromLegacyText(beforeClick));
        finalMessage.addExtra(clickableComponent);
        finalMessage.addExtra(new TextComponent(TextComponent.fromLegacyText(afterClick)));

        // Send the message to the player
        player.spigot().sendMessage(finalMessage);
    }

}
