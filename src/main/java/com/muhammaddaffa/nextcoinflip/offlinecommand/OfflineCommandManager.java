package com.muhammaddaffa.nextcoinflip.offlinecommand;

import com.muhammaddaffa.mdlib.utils.Config;
import com.muhammaddaffa.mdlib.utils.Executor;
import com.muhammaddaffa.nextcoinflip.NextCoinflip;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OfflineCommandManager implements Listener {

    private static final Map<UUID, List<String>> commandMap = new ConcurrentHashMap<>();

    public static void addCommand(UUID uuid, String command) {
        commandMap.computeIfAbsent(uuid, k -> new ArrayList<>())
                .add(command);
    }

    public static void executeCommand(UUID uuid, String command) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            commandMap.computeIfAbsent(uuid, k -> new ArrayList<>())
                    .add(command);
            return;
        }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command
                .replace("{player}", player.getName()));
    }

    public static void startTask() {
        Executor.syncTimer(0, 20, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                List<String> commands = commandMap.remove(player.getUniqueId());
                if (commands == null || commands.isEmpty()) {
                    continue;
                }
                // Execute the command
                for (String command : commands) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command
                            .replace("{player}", player.getName()));
                }
            }
        });
    }

    public static void save() {
        Config data = NextCoinflip.DATA_CONFIG;
        FileConfiguration config = data.getConfig();

        // Clear the data first
        config.set("commands", null);
        // Save all commands
        commandMap.forEach((uuid, commands) -> {
            config.set("commands." + uuid + ".commands", commands);
        });

        // Save the data
        data.saveConfig();
    }

    public static void load() {
        FileConfiguration config = NextCoinflip.DATA_CONFIG.getConfig();
        if (config.isConfigurationSection("commands")) {
            for (String key : config.getConfigurationSection("commands").getKeys(false)) {
                UUID uuid = UUID.fromString(key);
                List<String> commands = config.getStringList("commands." + key + ".commands");
                // Store it on the map
                commandMap.put(uuid, commands);
            }
        }
    }

}
