package com.muhammaddaffa.nextcoinflip.currencies.repository;

import com.muhammaddaffa.mdlib.utils.Logger;
import com.muhammaddaffa.nextcoinflip.NextCoinflip;
import com.muhammaddaffa.nextcoinflip.currencies.Currency;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CurrencyRepository {

    private static final Map<String, Currency> currencyMap = new HashMap<>();

    public static Collection<Currency> getCurrencies() {
        return currencyMap.values();
    }

    public static void load() {
        // Clear the map
        currencyMap.clear();

        FileConfiguration config = NextCoinflip.CURRENCY_CONFIG.getConfig();
        Logger.info("Starting to load all currency...");
        if (!config.isConfigurationSection("currencies")) {
            Logger.info("There is no configured currency found!");
            return;
        }

        for (String id : config.getConfigurationSection("currencies").getKeys(false)) {
            boolean enabled = config.getBoolean("currencies." + id + ".enabled");
            int minWager = config.getInt("currencies." + id + ".minimum-wager");
            int maxWager = config.getInt("currencies." + id + ".maximum-wager");
            String identifier = config.getString("currencies." + id + ".identifier");
            String balancePlaceholder = config.getString("currencies." + id + ".balance-placeholder");
            String takeCommand = config.getString("currencies." + id + ".take-command");
            String giveCommand = config.getString("currencies." + id + ".give-command");
            // Create and store the currency
            currencyMap.put(id, new Currency(id, enabled, minWager, maxWager, identifier, balancePlaceholder, takeCommand, giveCommand));
            Logger.info("Loaded " + id + " currency!");
        }
        Logger.info("Successfully loaded " + currencyMap.size() + " currencies!");
    }

}
