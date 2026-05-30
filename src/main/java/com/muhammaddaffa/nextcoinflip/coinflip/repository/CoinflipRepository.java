package com.muhammaddaffa.nextcoinflip.coinflip.repository;

import com.muhammaddaffa.nextcoinflip.coinflip.CoinflipGame;
import com.muhammaddaffa.nextcoinflip.coinflip.impl.ItemCoinflipGame;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class CoinflipRepository {

    private static final Map<UUID, CoinflipGame> gameMap = new ConcurrentHashMap<>();

    @Nullable
    public static CoinflipGame getGame(UUID uuid) {
        return gameMap.get(uuid);
    }

    public static Collection<CoinflipGame> getGames(Player player) {
        return getGames(player.getUniqueId());
    }

    public static Collection<CoinflipGame> getGames(UUID creator) {
        return gameMap.values().stream()
                .filter(game -> game.getCreator().equals(creator))
                .collect(Collectors.toList());
    }

    public static Collection<CoinflipGame> getGames() {
        return gameMap.values();
    }

    public static int getItemCoinflipGameAmount() {
        return getGames().stream()
                .filter(game -> game instanceof ItemCoinflipGame)
                .toList()
                .size();
    }

    public static void removeGame(CoinflipGame game) {
        removeGame(game.getUniqueId());
    }

    public static void removeGame(UUID uuid) {
        gameMap.remove(uuid);
    }

    public static void register(CoinflipGame game) {
        gameMap.put(game.getUniqueId(), game);
    }

}
