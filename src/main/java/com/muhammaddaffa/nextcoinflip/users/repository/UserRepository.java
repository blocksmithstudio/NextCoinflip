package com.muhammaddaffa.nextcoinflip.users.repository;

import com.muhammaddaffa.mdlib.utils.Logger;
import com.muhammaddaffa.nextcoinflip.NextCoinflip;
import com.muhammaddaffa.nextcoinflip.database.SQLDatabase;
import com.muhammaddaffa.nextcoinflip.coinflip.repository.CoinflipRepository;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

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

    public static synchronized void unload(Player player) {
        getUser(player).save(false);
        if (CoinflipRepository.getGames().stream().noneMatch(game -> game.involves(player.getUniqueId()))) {
            userMap.remove(player.getUniqueId());
        }
    }

    public static synchronized void unloadAll() {
        RuntimeException failure = null;
        var iterator = userMap.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            try {
                entry.getValue().save(false);
                if (CoinflipRepository.getGames().stream().noneMatch(game -> game.involves(entry.getKey()))) {
                    iterator.remove();
                }
            } catch (RuntimeException ex) {
                if (failure == null) {
                    failure = new IllegalStateException("Some player saves remain incomplete");
                }
                failure.addSuppressed(ex);
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    public static void load(Player player) {
        getUser(player);
    }

    public static synchronized List<Map<String, Object>> recoveryData() {
        List<Map<String, Object>> users = new ArrayList<>();
        for (User user : userMap.values()) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("player", user.getUniqueId().toString());
            data.put("wins", user.getStatistic().getWins());
            data.put("losses", user.getStatistic().getLosses());
            data.put("lose-streak", user.getStatistic().getLoseStreak());
            data.put("earnings", user.getStatistic().serializeEarnings());
            List<Map<String, Object>> items = new ArrayList<>();
            for (WonItem item : user.getWonItems()) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("id", item.getUniqueId().toString());
                entry.put("item", Serializer.serialize(item.getItemStack()));
                entry.put("timestamp", item.getTimestamp());
                items.add(entry);
            }
            data.put("items", items);
            users.add(data);
        }
        return users;
    }

    private static void loadStatistics(User user) {
        SQLDatabase database = NextCoinflip.getInstance().getDatabase();
        String query = "SELECT * FROM " + SQLDatabase.STATISTIC_TABLE + " WHERE uuid = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, user.getUniqueId().toString());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    int wins = rs.getInt("wins");
                    int losses = rs.getInt("losses");
                    int loseStreak = rs.getInt("lose_streak");
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
        String query = "SELECT * FROM " + SQLDatabase.ITEM_TABLE + " WHERE user_uuid = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, user.getUniqueId().toString());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("item_uuid"));
                    ItemStack item = Serializer.deserialize(rs.getString("item"));
                    long timestamp = rs.getLong("timestamp");
                    if (item != null) {
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
