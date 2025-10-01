package com.muhammaddaffa.nextcoinflip.utils;

import com.muhammaddaffa.mdlib.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class SkullCreator {

    public static ItemBuilder createItem(UUID uuid) {
        return createItem(Bukkit.getPlayer(uuid));
    }

    public static ItemBuilder createItem(@Nullable OfflinePlayer player) {
        ItemBuilder builder = new ItemBuilder(Material.PLAYER_HEAD);
        if (player == null) {
            return builder;
        }
        return builder.skull(player);
    }

}
