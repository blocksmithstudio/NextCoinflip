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
        String hoverMessage = Common.color(NextCoinflip.DEFAULT_CONFIG.getString("item-wager.hover-item")
                .replace("{item_displayname}", itemDisplayName)
                .replace("{item_amount}", itemAmount + ""));

        // Create the hoverable component for the item
        TextComponent hoverableComponent = new TextComponent(TextComponent.fromLegacyText(hoverMessage));
        hoverableComponent.setHoverEvent(ItemHoverEventHelper.createFrom(stack));

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
        String clickText = Common.color(NextCoinflip.DEFAULT_CONFIG.getString("item-wager.accept-button"));
        TextComponent clickableComponent = new TextComponent(TextComponent.fromLegacyText(clickText));
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

    private static String toNbtString(ItemStack bukkitStack) {
        try {
            // org.bukkit.craftbukkit.<ver>.inventory.CraftItemStack
            String cbPackage = Bukkit.getServer().getClass().getPackage().getName(); // org.bukkit.craftbukkit.v1_20_R4
            String cbVersion = cbPackage.substring(cbPackage.lastIndexOf('.') + 1);
            Class<?> craftItemStackClass = Class.forName("org.bukkit.craftbukkit." + cbVersion + ".inventory.CraftItemStack");
            Object nmsStack = craftItemStackClass
                    .getMethod("asNMSCopy", ItemStack.class)
                    .invoke(null, bukkitStack);

            // Try modern class names first (1.17+), fallback to legacy
            Class<?> compoundTagClass;
            try {
                compoundTagClass = Class.forName("net.minecraft.nbt.CompoundTag"); // 1.17+
            } catch (ClassNotFoundException ignored) {
                try {
                    compoundTagClass = Class.forName("net.minecraft.server." + cbVersion + ".NBTTagCompound"); // 1.16-
                } catch (ClassNotFoundException e) {
                    compoundTagClass = Class.forName("net.minecraft.nbt.NBTTagCompound"); // some mappings
                }
            }

            Object tagInstance = compoundTagClass.getDeclaredConstructor().newInstance();

            // Try common "save" signatures across versions
            //  - 1.17–1.20: ItemStack#save(CompoundTag) -> CompoundTag
            //  - newer mappings may differ; also try getTag() as a fallback
            try {
                Object written = nmsStack.getClass().getMethod("save", compoundTagClass).invoke(nmsStack, tagInstance);
                return written.toString(); // SNBT
            } catch (NoSuchMethodException e) {
                // Fallback: getTag() if present
                try {
                    Object tag = nmsStack.getClass().getMethod("getTag").invoke(nmsStack);
                    return tag != null ? tag.toString() : "{}";
                } catch (NoSuchMethodException ex) {
                    // Last resort: toString() on our created tag (may be empty but valid)
                    return tagInstance.toString();
                }
            }
        } catch (Throwable t) {
            // If anything fails, return empty tag (still valid SNBT)
            return "{}";
        }
    }

}
