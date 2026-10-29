package com.muhammaddaffa.nextcoinflip.utils;

import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.Placeholder;
import com.muhammaddaffa.mdlib.utils.WordUtils;
import com.muhammaddaffa.nextcoinflip.NextCoinflip;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class HoverableMessage {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

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
        sendHoverableItemMessage(player, stack, message, null);
    }

    public static void sendHoverableItemMessage(Player player, ItemStack stack, String message, @Nullable Placeholder placeholder) {
        // Parse the message color codes
        String[] parts = message.split("\\{wager}", 2);
        String beforeWager = Common.color(parts[0]);
        String afterWager = Common.color(parts.length > 1 ? parts[1] : "");

        if (placeholder != null) {
            beforeWager = placeholder.translate(beforeWager);
            afterWager = placeholder.translate(afterWager);
        }

        // Get the item's display name and amount
        String itemDisplayName = stack.hasItemMeta() && stack.getItemMeta().hasDisplayName() ?
                stack.getItemMeta().getDisplayName() :
                WordUtils.getNiceMaterialName(stack.getType());
        int itemAmount = stack.getAmount();

        // Create the item message
        String hoverMessage = Common.color(NextCoinflip.DEFAULT_CONFIG.getString("item-wager.hover-item")
                .replace("{item_displayname}", itemDisplayName)
                .replace("{item_amount}", itemAmount + ""));

        // Create the hoverable component for the item
        Component hoverableComponent = LEGACY.deserialize(hoverMessage).hoverEvent(stack.asHoverEvent());
        player.sendMessage(LEGACY.deserialize(beforeWager)
                .append(hoverableComponent)
                .append(LEGACY.deserialize(afterWager)));
    }

    public static void sendClickableTextMessage(Player player, String message, String command) {
        // Parse the message color codes
        String[] parts = message.split("\\{click}", 2);
        String beforeClick = Common.color(parts[0]);
        String afterClick = Common.color(parts.length > 1 ? parts[1] : "");

        // Create the clickable component
        String clickText = Common.color(NextCoinflip.DEFAULT_CONFIG.getString("item-wager.accept-button"));
        Component clickableComponent = LEGACY.deserialize(clickText)
                .clickEvent(ClickEvent.runCommand(command))
                .hoverEvent(Component.text(command, NamedTextColor.GREEN));
        player.sendMessage(LEGACY.deserialize(beforeClick)
                .append(clickableComponent)
                .append(LEGACY.deserialize(afterClick)));
    }

}
