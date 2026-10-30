package com.muhammaddaffa.nextcoinflip.coinflip.settlement;

import com.muhammaddaffa.mdlib.utils.Logger;
import com.muhammaddaffa.nextcoinflip.NextCoinflip;
import com.muhammaddaffa.nextcoinflip.coinflip.CoinflipGame;
import com.muhammaddaffa.nextcoinflip.coinflip.repository.CoinflipRepository;
import com.muhammaddaffa.nextcoinflip.users.repository.UserRepository;
import com.muhammaddaffa.nextcoinflip.utils.AtomicFiles;
import org.bukkit.configuration.file.YamlConfiguration;

import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.util.UUID;

public final class SettlementRecovery {
    private SettlementRecovery() {
    }

    public static void requireResolved(Path directory) {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (var files = Files.list(directory)) {
            if (files.anyMatch(path -> path.getFileName().toString().startsWith("recovery-")
                    && path.getFileName().toString().endsWith(".yml"))) {
                throw new IllegalStateException("Unresolved NextCoinflip recovery snapshot; reconcile it before starting wagers");
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot inspect NextCoinflip recovery snapshots", ex);
        }
    }

    public static void save() {
        var games = CoinflipRepository.getGames().stream().map(CoinflipGame::recoveryData).toList();
        var users = UserRepository.recoveryData();
        if (games.isEmpty() && users.isEmpty()) {
            return;
        }
        YamlConfiguration config = new YamlConfiguration();
        config.set("format", 1);
        config.set("created", System.currentTimeMillis());
        config.set("automatic-replay", false);
        config.set("games", games);
        config.set("users", users);
        Path target = NextCoinflip.getInstance().getDataFolder().toPath()
                .resolve("recovery-" + UUID.randomUUID() + ".yml");
        AtomicFiles.write(target, config.saveToString());
        Logger.severe("Unfinished work was preserved in " + target.getFileName()
                + "; reconcile it before resuming wagers. Recovery snapshots are not replayed automatically.");
    }
}
