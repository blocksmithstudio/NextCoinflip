package com.muhammaddaffa.nextcoinflip.currencies;

import com.muhammaddaffa.nextcoinflip.offlinecommand.OfflineCommandManager;
import me.clip.placeholderapi.PlaceholderAPI;
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
        String command = this.giveCommand()
                .replace("{player}", player.getName())
                .replace("{amount}", amount + "");
        // Execute the command
        OfflineCommandManager.executeCommand(player.getUniqueId(), command);
    }

    public void takeBalance(Player player, int amount) {
        String command = this.takeCommand()
                .replace("{player}", player.getName())
                .replace("{amount}", amount + "");
        // Execute the command
        OfflineCommandManager.executeCommand(player.getUniqueId(), command);
    }

}
