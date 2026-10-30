package com.muhammaddaffa.nextcoinflip.currencies;

import com.muhammaddaffa.nextcoinflip.offlinecommand.OfflineCommandManager;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;

import java.util.UUID;

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
        String parsed = PlaceholderAPI.setPlaceholders(player, this.balancePlaceholder()).trim();
        if (parsed.matches("-?[\\d,.]+")) {
            try {
                String cleaned = parsed.replace(",", "");
                double value = Double.parseDouble(cleaned);
                return (int) Math.floor(value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    public boolean hasEnoughBalance(Player player, int amount) {
        return amount > 0 && getBalance(player) >= amount;
    }

    public void giveBalance(Player player, int amount) {
        giveBalance(player.getUniqueId(), amount);
    }

    public void giveBalance(UUID uuid, int amount) {
        OfflineCommandManager.addCommand(uuid, this.prepareGiveCommand(amount));
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            OfflineCommandManager.deliver(player);
        }
    }

    public void queueGiveBalance(UUID uuid, int amount) {
        OfflineCommandManager.addCommand(uuid, prepareGiveCommand(amount));
    }

    public boolean takeBalance(Player player, int amount) {
        return takeBalance(player.getUniqueId(), amount);
    }

    public boolean takeBalance(UUID uuid, int amount) {
        return OfflineCommandManager.executeCommand(uuid, prepareTakeCommand(amount));
    }

    private String prepareGiveCommand(int amount) {
        return prepareCommand(this.giveCommand(), amount);
    }

    private String prepareTakeCommand(int amount) {
        return prepareCommand(this.takeCommand(), amount);
    }

    private String prepareCommand(String template, int amount) {
        if (amount <= 0) throw new IllegalArgumentException("Currency amount must be positive");
        return template.replace("{amount}", amount + "");
    }

}
