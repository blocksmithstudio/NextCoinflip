package com.muhammaddaffa.nextcoinflip.offlinecommand;

import com.muhammaddaffa.mdlib.utils.Executor;
import com.muhammaddaffa.mdlib.utils.Logger;
import com.muhammaddaffa.nextcoinflip.NextCoinflip;
import com.muhammaddaffa.nextcoinflip.utils.AtomicFiles;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class OfflineCommandManager {
    private static final Map<UUID, List<QueuedCommand>> commandMap = new LinkedHashMap<>();
    private static boolean loadFailed;

    private enum State {
        PENDING, DISPATCHING, COMPLETE, UNCERTAIN
    }

    private static final class QueuedCommand {
        private final UUID id;
        private final String command;
        private State state;
        private boolean warned;

        private QueuedCommand(UUID id, String command, State state) {
            this.id = id;
            this.command = command;
            this.state = state;
        }
    }

    public static void addCommand(UUID uuid, String command) {
        QueuedCommand entry = new QueuedCommand(UUID.randomUUID(), command, State.PENDING);
        List<QueuedCommand> commands = commandMap.computeIfAbsent(uuid, ignored -> new ArrayList<>());
        commands.add(entry);
        try {
            save();
        } catch (RuntimeException ex) {
            commands.remove(entry);
            if (commands.isEmpty()) {
                commandMap.remove(uuid);
            }
            throw ex;
        }
    }

    public static boolean executeCommand(UUID uuid, String command) {
        Player player = Bukkit.getPlayer(uuid);
        return player != null && Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                command.replace("{player}", player.getName()));
    }

    public static void startTask() {
        Executor.syncTimer(0L, 20L, () -> Bukkit.getOnlinePlayers().forEach(OfflineCommandManager::deliver));
    }

    public static void deliver(Player player) {
        UUID uuid = player.getUniqueId();
        List<QueuedCommand> commands = commandMap.get(uuid);
        if (commands == null) {
            return;
        }
        for (QueuedCommand entry : new ArrayList<>(commands)) {
            try {
                if (entry.state == State.UNCERTAIN) {
                    continue;
                }
                if (entry.state == State.PENDING) {
                    entry.state = State.DISPATCHING;
                    try {
                        save();
                    } catch (RuntimeException ex) {
                        entry.state = State.PENDING;
                        throw ex;
                    }
                    boolean accepted;
                    try {
                        accepted = Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                                entry.command.replace("{player}", player.getName()));
                    } catch (RuntimeException ex) {
                        entry.state = State.UNCERTAIN;
                        save();
                        throw ex;
                    }
                    entry.state = accepted ? State.COMPLETE : State.PENDING;
                    save();
                    if (!accepted) {
                        warn(entry);
                        continue;
                    }
                }
                if (entry.state == State.COMPLETE) {
                    commands.remove(entry);
                    try {
                        save();
                    } catch (RuntimeException ex) {
                        commands.add(entry);
                        throw ex;
                    }
                }
            } catch (RuntimeException ex) {
                warn(entry);
            }
        }
        if (commands.isEmpty()) {
            commandMap.remove(uuid);
        }
    }

    private static void warn(QueuedCommand entry) {
        if (!entry.warned) {
            Logger.severe("Currency delivery " + entry.id + " remains " + entry.state
                    + "; inspect data.yml. UNCERTAIN deliveries require reconciliation before retry.");
            entry.warned = true;
        }
    }

    public static void save() {
        if (loadFailed) {
            throw new IllegalStateException("Refusing to overwrite a currency queue that did not load successfully");
        }
        FileConfiguration config = NextCoinflip.DATA_CONFIG.getConfig();
        config.set("commands", null);
        commandMap.forEach((uuid, commands) -> {
            for (QueuedCommand entry : commands) {
                String path = "commands." + uuid + ".entries." + entry.id;
                config.set(path + ".command", entry.command);
                config.set(path + ".state", entry.state.name());
            }
        });
        AtomicFiles.write(NextCoinflip.DATA_CONFIG.getFile().toPath(), config.saveToString());
    }

    public static void load() {
        loadFailed = true;
        Map<UUID, List<QueuedCommand>> loaded = new LinkedHashMap<>();
        ConfigurationSection section = NextCoinflip.DATA_CONFIG.getConfig().getConfigurationSection("commands");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                UUID uuid = UUID.fromString(key);
                List<QueuedCommand> commands = new ArrayList<>();
                for (String command : section.getStringList(key + ".commands")) {
                    commands.add(new QueuedCommand(UUID.randomUUID(), command, State.PENDING));
                }
                ConfigurationSection entries = section.getConfigurationSection(key + ".entries");
                if (entries != null) {
                    for (String id : entries.getKeys(false)) {
                        String command = entries.getString(id + ".command");
                        if (command == null || command.isBlank()) {
                            throw new IllegalStateException("Invalid pending currency delivery " + id);
                        }
                        State state = State.valueOf(entries.getString(id + ".state", "PENDING"));
                        if (state == State.DISPATCHING) {
                            state = State.UNCERTAIN;
                        }
                        QueuedCommand entry = new QueuedCommand(UUID.fromString(id), command, state);
                        if (state == State.UNCERTAIN) {
                            warn(entry);
                        }
                        commands.add(entry);
                    }
                }
                loaded.put(uuid, commands);
            }
        }
        commandMap.clear();
        commandMap.putAll(loaded);
        loadFailed = false;
        try {
            save();
        } catch (RuntimeException ex) {
            loadFailed = true;
            throw ex;
        }
    }
}
