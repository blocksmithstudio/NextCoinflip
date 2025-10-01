package com.muhammaddaffa.nextcoinflip.users.repository;

import com.muhammaddaffa.mdlib.utils.Config;
import com.muhammaddaffa.nextcoinflip.NextCoinflip;
import com.muhammaddaffa.nextcoinflip.users.User;
import com.muhammaddaffa.nextcoinflip.users.models.WonItem;
import com.muhammaddaffa.nextcoinflip.utils.Serializer;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UserRepository {

    private static final Map<UUID, User> userMap = new HashMap<>();

    @NotNull
    public static User getUser(OfflinePlayer player) {
        return getUser(player.getUniqueId());
    }

    @NotNull
    public static User getUser(UUID uuid) {
        return userMap.computeIfAbsent(uuid, k -> new User(uuid));
    }

    public static void save() {
        Config data = NextCoinflip.DATA_CONFIG;
        FileConfiguration config = data.getConfig();

        // Fetch all users data
        for (User user : userMap.values()) {
            // Set all statistics
            config.set("users." + user.getUniqueId().toString() + ".statistic.wins", user.getStatistic().getWins());
            config.set("users." + user.getUniqueId().toString() + ".statistic.losses", user.getStatistic().getLosses());
            config.set("users." + user.getUniqueId().toString() + ".statistic.lose-streak", user.getStatistic().getLoseStreak());
            // Set all won items data
            for (WonItem item : user.getWonItems()) {
                String path = "users." + user.getUniqueId().toString() + ".won-items." + item.getUniqueId().toString() + ".";
                config.set(path + "item", Serializer.serialize(item.getItemStack()));
                config.set(path + "timestamp", item.getTimestamp());
            }
        }
        // Save the data
        data.saveConfig();
    }

    public static void load() {
        FileConfiguration config = NextCoinflip.DATA_CONFIG.getConfig();
        if (!config.isConfigurationSection("users")) {
            return;
        }
        // Fetch all users data from the config
        for (String key : config.getConfigurationSection("users").getKeys(false)) {
            UUID uuid = UUID.fromString(key);
            User user = new User(uuid);
            // Insert all necessary data
            user.getStatistic().setWins(config.getInt("users." + key + ".statistic.wins"));
            user.getStatistic().setLosses(config.getInt("users." + key + ".statistic.losses"));
            user.getStatistic().setLoseStreak(config.getInt("users." + key + ".statistic.lose-streak"));
            if (config.isConfigurationSection("users." + key + ".won-items")) {
                for (String uuidString : config.getConfigurationSection("users." + key + ".won-items").getKeys(false)) {
                    UUID id = UUID.fromString(uuidString);
                    ItemStack stack = Serializer.deserialize(config.getString("users." + key + ".won-items." + uuidString + ".item"));
                    long timestamp = config.getLong("users." + key + ".won-items." + uuidString + ".timestamp");
                    // Create the WonItem object and store it on the user object
                    user.addWonItem(new WonItem(id, stack, timestamp));
                }
            }
            // Store the data into the map
            userMap.put(uuid, user);
        }
    }

}
