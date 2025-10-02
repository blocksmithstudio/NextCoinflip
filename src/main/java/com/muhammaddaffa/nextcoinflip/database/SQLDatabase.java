package com.muhammaddaffa.nextcoinflip.database;

import com.muhammaddaffa.mdlib.MDLib;
import com.muhammaddaffa.mdlib.utils.Logger;
import com.muhammaddaffa.nextcoinflip.NextCoinflip;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Consumer;

public class SQLDatabase {

    public static final String STATISTIC_TABLE = "nextcoinflip_statistics";
    public static final String ITEM_TABLE = "nextcoinflip_items";

    private HikariDataSource dataSource;
    private boolean mysql;

    public void connect() {
        // get all variables we want
        FileConfiguration config = NextCoinflip.DEFAULT_CONFIG.getConfig();
        String path = "plugins/NextCoinflip/database.db";
        // create the hikari config
        HikariConfig hikari = new HikariConfig();
        hikari.setConnectionTestQuery("SELECT 1");
        hikari.setPoolName("NextCoinflip Database Pool");
        hikari.setMaxLifetime(1000000);
        hikari.setConnectionTimeout(600000);
        hikari.setIdleTimeout(600000);
        hikari.setLeakDetectionThreshold(360000);
        hikari.addDataSourceProperty("characterEncoding", "utf8");
        hikari.addDataSourceProperty("useUnicode", true);

        if (config.getBoolean("mysql.enabled")) {
            this.mysql = true;
            String host = config.getString("mysql.host");
            int port = config.getInt("mysql.port");
            String database = config.getString("mysql.database");
            String user = config.getString("mysql.user");
            String password = config.getString("mysql.password");
            boolean useSSL = config.getBoolean("mysql.useSSL");
            Logger.info("Trying to connect to the MySQL database...");

            hikari.setDriverClassName("com.mysql.jdbc.Driver");
            hikari.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s?useSSL=%b",
                    host, port, database, useSSL));
            hikari.setUsername(user);
            hikari.setPassword(password);
            hikari.setMinimumIdle(5);
            hikari.setMaximumPoolSize(50);

            this.dataSource = new HikariDataSource(hikari);
            Logger.info("Successfully established connection with MySQL database!");

        } else {
            this.mysql = false;
            hikari.setConnectionTestQuery("SELECT 1");
            hikari.setDriverClassName("org.sqlite.JDBC");
            hikari.setJdbcUrl("jdbc:sqlite:" + path);
            hikari.setMaximumPoolSize(10);

            Logger.info("Trying to connect to the SQLite database...");
            // create the file if it's not exist
            try {
                File file = new File(path);
                if (!file.exists()) {
                    file.createNewFile();
                }
                this.dataSource = new HikariDataSource(hikari);
                Logger.info("Successfully established connection with SQLite database!");
            } catch (IOException ex) {
                Logger.severe("Failed to create the database file, stopping the server!");
                Bukkit.getPluginManager().disablePlugin(MDLib.instance());
                throw new RuntimeException(ex);
            }
        }
    }

    public void createStatisticTable() {
        this.executeUpdate("CREATE TABLE IF NOT EXISTS " + STATISTIC_TABLE + " (" +
                "uuid VARCHAR(255) NOT NULL PRIMARY KEY," +
                "wins INTEGER NOT NULL DEFAULT 0," +
                "losses INTEGER NOT NULL DEFAULT 0," +
                "lose_streak INTEGER NOT NULL DEFAULT 0," +
                "earnings TEXT" +
                ");");
    }

    public void createItemTable() {
        this.executeUpdate("CREATE TABLE IF NOT EXISTS " + ITEM_TABLE + " (" +
                "user_uuid VARCHAR(255) NOT NULL," +
                "item_uuid VARCHAR(255) NOT NULL PRIMARY KEY," +
                "item TEXT," +
                "timestamp BIGINT NOT NULL" +
                ");");
    }

    public String getStatisticInsertQuery() {
        return this.mysql ?
                // This is the mysql query
                "INSERT INTO " + STATISTIC_TABLE +
                        " (uuid, wins, losses, lose_streak, earnings) " +
                        "VALUES (?,?,?,?,?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "uuid = VALUES(uuid), " +
                        "wins = VALUES(wins), " +
                        "losses = VALUES(losses), " +
                        "lose_streak = VALUES(lose_streak), " +
                        "earnings = VALUES(earnings)"
                :
                // This is the sqlite query
                "INSERT INTO " + STATISTIC_TABLE +
                        " (uuid, wins, losses, lose_streak, earnings) " +
                        "VALUES (?,?,?,?,?) " +
                        "ON CONFLICT(uuid) DO UPDATE SET " +
                        "uuid = excluded.uuid, " +
                        "wins = excluded.wins, " +
                        "losses = excluded.losses, " +
                        "lose_streak = excluded.lose_streak, " +
                        "earnings = excluded.earnings"
                ;
    }

    public String getItemInsertQuery() {
        return this.mysql ?
                // This is the mysql query
                "INSERT INTO " + ITEM_TABLE +
                        " (user_uuid, item_uuid, item, timestamp) " +
                        "VALUES(?,?,?,?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "user_uuid = VALUES(user_uuid), " +
                        "item_uuid = VALUES(item_uuid), " +
                        "item = VALUES(item), " +
                        "timestamp = VALUES(timestamp)"
                :
                // This is the sqlite query
                "INSERT INTO " + ITEM_TABLE +
                        " (user_uuid, item_uuid, item, timestamp) " +
                        "VALUES(?,?,?,?) " +
                        "ON CONFLICT(item_uuid) DO UPDATE SET " +
                        "user_uuid = excluded.user_uuid, " +
                        "item_uuid = excluded.item_uuid, " +
                        "item = excluded.item, " +
                        "timestamp = excluded.timestamp"
                ;
    }

    public void executeUpdate(String statement) {
        this.executeUpdate(statement, error -> {
            Logger.severe("An error occurred while running statement: " + statement);
            error.printStackTrace();
        });
    }

    public void executeUpdate(String statement, Consumer<SQLException> onFailure) {
        try (Connection connection = this.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(statement)) {

            preparedStatement.executeUpdate();
        } catch (SQLException ex) {
            onFailure.accept(ex);
        }
    }

    public void executeQuery(String statement, Consumer<ResultSet> callback) {
        this.executeQuery(statement, callback, error -> {
            Logger.severe("An error occurred while running query: " + statement);
            error.printStackTrace();
        });
    }

    public void executeQuery(String statement, Consumer<ResultSet> callback, Consumer<SQLException> onFailure) {
        try (Connection connection = this.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(statement);
             ResultSet result = preparedStatement.executeQuery()) {

            callback.accept(result);
        } catch (SQLException ex) {
            onFailure.accept(ex);
        }
    }

    public void buildStatement(String query, Consumer<PreparedStatement> consumer) {
        this.buildStatement(query, consumer, error -> {
            Logger.severe("An error occurred while building statement: " + query);
            error.printStackTrace();
        });
    }

    public void buildStatement(String query, Consumer<PreparedStatement> consumer, Consumer<SQLException> onFailure) {
        try (Connection connection = this.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            consumer.accept(preparedStatement);
        } catch (SQLException ex) {
            onFailure.accept(ex);
        }
    }

    public Connection getConnection() throws SQLException {
        return this.dataSource.getConnection();
    }

    public boolean isMysql() {
        return mysql;
    }

    public void close() {
        this.dataSource.close();
    }

}
