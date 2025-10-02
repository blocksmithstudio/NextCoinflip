package com.muhammaddaffa.nextcoinflip.utils;

import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;

public class Utils {

    public static boolean isInventoryFull(Player player) {
        PlayerInventory inventory = player.getInventory();
        return inventory.firstEmpty() == -1;
    }

}
