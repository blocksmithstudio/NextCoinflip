package com.muhammaddaffa.nextcoinflip.users;

import com.muhammaddaffa.mdlib.utils.Executor;
import com.muhammaddaffa.mdlib.utils.Logger;
import com.muhammaddaffa.nextcoinflip.NextCoinflip;
import com.muhammaddaffa.nextcoinflip.database.SQLDatabase;
import com.muhammaddaffa.nextcoinflip.users.models.CoinflipStatistic;
import com.muhammaddaffa.nextcoinflip.users.models.WonItem;
import com.muhammaddaffa.nextcoinflip.utils.Serializer;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

public class User {

    private final UUID uuid;

    private final CoinflipStatistic statistic = new CoinflipStatistic();
    private final List<WonItem> wonItems = Collections.synchronizedList(new ArrayList<>());

    public User(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUniqueId() {
        return uuid;
    }

    public CoinflipStatistic getStatistic() {
        return statistic;
    }

    public List<WonItem> getWonItems() {
        return wonItems;
    }

    public void addWonItems(Collection<WonItem> items) {
        wonItems.addAll(items);
    }

    public void addWonItem(WonItem item) {
        wonItems.add(item);
    }

    public void addWonItem(ItemStack stack) {
        wonItems.add(new WonItem(stack));
    }

    public void removeWonItem(WonItem item) {
        removeWonItem(item.getUniqueId());
    }

    public void removeWonItem(UUID itemId) {
        wonItems.removeIf(item -> item.getUniqueId().equals(itemId));
        // Remove it from the database too
        Executor.async(() -> {
            SQLDatabase database = NextCoinflip.getInstance().getDatabase();
            // Get the query
            // (user_uuid, item_uuid, item, timestamp)
            String query = "DELETE FROM " + SQLDatabase.ITEM_TABLE + " WHERE item_uuid = ?";
            try (Connection connection = database.getConnection();
                    PreparedStatement statement = connection.prepareStatement(query)) {
                // Set the statements
                statement.setString(1, itemId.toString());
                // Execute the update
                statement.executeUpdate();

            } catch(SQLException ex) {
                Logger.severe("Failed to remove won item " + itemId.toString() + " for user " + this.uuid.toString());
                ex.printStackTrace();
            }
        });
    }

    public void save(boolean async) {
        if (async) {
            Executor.async(this::save);
        } else {
            this.save();
        }
    }

    private void save() {
        saveStatistics();
        saveItems();
    }

    private void saveStatistics() {
        SQLDatabase database = NextCoinflip.getInstance().getDatabase();
        // Get the query
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(database.getStatisticInsertQuery())) {
            // Set the arguments
            // (uuid, wins, losses, lose_streak, earnings)
            statement.setString(1, this.uuid.toString());
            statement.setInt(2, statistic.getWins());
            statement.setInt(3, statistic.getLosses());
            statement.setInt(4, statistic.getLoseStreak());
            statement.setString(5, statistic.serializeEarnings());
            // Execute update
            statement.executeUpdate();

        } catch(SQLException ex) {
            Logger.severe("Failed to save " + this.uuid.toString() + " statistics data!");
            ex.printStackTrace();
        }
    }

    private void saveItems() {
        SQLDatabase database = NextCoinflip.getInstance().getDatabase();
        // Get the query
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(database.getItemInsertQuery())) {
            // (user_uuid, item_uuid, item, timestamp)
            for (WonItem item : wonItems) {
                // Set the arguments
                statement.setString(1, this.uuid.toString());
                statement.setString(2, item.getUniqueId().toString());
                statement.setString(3, Serializer.serialize(item.getItemStack()));
                statement.setLong(4, item.getTimestamp());

                statement.addBatch();
            }
            // Execute the batch
            statement.executeBatch();

        } catch(SQLException ex) {
            Logger.severe("Failed to save " + this.uuid.toString() + " items data!");
            ex.printStackTrace();
        }
    }

}
