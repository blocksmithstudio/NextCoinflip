package com.muhammaddaffa.nextcoinflip.currencies;

import com.muhammaddaffa.mdlib.utils.Common;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public record Currency(
        String id,
        boolean enabled,
        int minWager,
        int maxWager,
        String identifier,
        String balancePlaceholder,
        String takeCommand,
        String giveCommand
) {

    public int getBalance(Player player) {
        String parsed = PlaceholderAPI.setPlaceholders(player, this.balancePlaceholder());
        if (Common.isInt(parsed)) {
            return Integer.parseInt(parsed);
        }
        return 0;
    }

    public boolean hasEnoughBalance(Player player, int amount) {
        return getBalance(player) >= amount;
    }

    public void giveBalance(Player player, int amount) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), this.giveCommand()
                .replace("{player}", player.getName())
                .replace("{amount}", amount + ""));
    }

    public void takeBalance(Player player, int amount) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), this.takeCommand()
                .replace("{player}", player.getName())
                .replace("{amount}", amount + ""));
    }

}
