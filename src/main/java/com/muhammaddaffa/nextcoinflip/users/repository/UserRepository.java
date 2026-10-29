package com.muhammaddaffa.nextcoinflip.users.repository;

import com.muhammaddaffa.mdlib.utils.Logger;
import com.muhammaddaffa.nextcoinflip.NextCoinflip;
import com.muhammaddaffa.nextcoinflip.database.SQLDatabase;
import com.muhammaddaffa.nextcoinflip.users.User;
import com.muhammaddaffa.nextcoinflip.users.models.CoinflipStatistic;
import com.muhammaddaffa.nextcoinflip.users.models.WonItem;
import com.muhammaddaffa.nextcoinflip.utils.Serializer;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
    public static synchronized User getUser(UUID uuid) {
        return userMap.computeIfAbsent(uuid, k -> {
            User user = new User(k);
            loadStatistics(user);
            loadItems(user);
            return user;
        });
    }

    // Used when player leave the server
    public static synchronized void unload(Player player) {
        getUser(player).save(false);
        userMap.remove(player.getUniqueId());
    }

    public static synchronized void unloadAll() {
        userMap.values().forEach(user -> user.save(false));
        userMap.clear();
    }

    public static void load(Player player) {
        getUser(player);
    }

    private static void loadStatistics(User user) {
        SQLDatabase database = NextCoinflip.getInstance().getDatabase();
        // Get the query
        String query = "SELECT * FROM " + SQLDatabase.STATISTIC_TABLE + " WHERE uuid = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            // (uuid, wins, losses, lose_streak, earnings)
            statement.setString(1, user.getUniqueId().toString());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    // Load the statistics data
                    int wins = rs.getInt("wins");
                    int losses = rs.getInt("losses");
                    int loseStreak = rs.getInt("lose_streak");
                    // Set the statistics
                    CoinflipStatistic statistic = user.getStatistic();
                    statistic.setWins(wins);
                    statistic.setLosses(losses);
                    statistic.setLoseStreak(loseStreak);
                    statistic.deserializeEarnings(rs.getString("earnings"));
                }
            }
        } catch (SQLException ex) {
            Logger.severe("Failed to load " + user.getUniqueId().toString() + " statistics data!");
            throw new IllegalStateException("Cannot load player statistics", ex);
        }
    }

    private static void loadItems(User user) {
        SQLDatabase database = NextCoinflip.getInstance().getDatabase();
        // Get the query
        String query = "SELECT * FROM " + SQLDatabase.ITEM_TABLE + " WHERE user_uuid = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            // (user_uuid, item_uuid, item, timestamp)
            statement.setString(1, user.getUniqueId().toString());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("item_uuid"));
                    ItemStack item = Serializer.deserialize(rs.getString("item"));
                    long timestamp = rs.getLong("timestamp");
                    if (item != null) {
                        // Insert the item to user won item
                        user.addWonItem(new WonItem(uuid, item, timestamp));
                    }
                }
            }
        } catch (SQLException ex) {
            Logger.severe("Failed to load " + user.getUniqueId().toString() + " statistics data!");
            throw new IllegalStateException("Cannot load player items", ex);
        }
    }

}
