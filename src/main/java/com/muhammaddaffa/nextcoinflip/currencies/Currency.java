package com.muhammaddaffa.nextcoinflip.currencies;

import com.muhammaddaffa.nextcoinflip.offlinecommand.OfflineCommandManager;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

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
        // Allow commas and decimals, but ensure the string only contains digits, commas, dots, or a leading minus
        if (parsed.matches("-?[\\d,.]+")) {
            try {
                // Remove commas (thousand separators)
                String cleaned = parsed.replace(",", "");
                // Parse as double to handle decimals like 123.45
                double value = Double.parseDouble(cleaned);
                return (int) Math.floor(value); // or use Math.round(value) if you prefer rounding
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0; // invalid format or contains non-numeric characters
    }

    public boolean hasEnoughBalance(Player player, int amount) {
        return getBalance(player) >= amount;
    }

    public void giveBalance(Player player, int amount) {
        giveBalance(player.getUniqueId(), amount);
    }

    public void giveBalance(UUID uuid, int amount) {
        OfflineCommandManager.executeCommand(uuid, prepareGiveCommand(amount));
    }

    public void queueGiveBalance(UUID uuid, int amount) {
        OfflineCommandManager.addCommand(uuid, prepareGiveCommand(amount));
    }

    public void takeBalance(Player player, int amount) {
        takeBalance(player.getUniqueId(), amount);
    }

    public void takeBalance(UUID uuid, int amount) {
        OfflineCommandManager.executeCommand(uuid, prepareTakeCommand(amount));
    }

    private String prepareGiveCommand(int amount) {
        return prepareCommand(this.giveCommand(), amount);
    }

    private String prepareTakeCommand(int amount) {
        return prepareCommand(this.takeCommand(), amount);
    }

    private String prepareCommand(String template, int amount) {
        return template.replace("{amount}", amount + "");
    }

}