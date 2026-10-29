package com.muhammaddaffa.nextcoinflip.utils;

import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.ItemStack;

public class Utils {

    public static ItemStack takeWagerItem(Player player, ItemStack expected) {
        if (expected == null || expected.getType().isAir() || expected.getAmount() <= 0
                || !expected.equals(player.getInventory().getItemInMainHand())) {
            return null;
        }
        ItemStack wager = expected.clone();
        player.getInventory().setItemInMainHand(null);
        return wager;
    }

    public static boolean isInventoryFull(Player player) {
        PlayerInventory inventory = player.getInventory();
        return inventory.firstEmpty() == -1;
    }

}
