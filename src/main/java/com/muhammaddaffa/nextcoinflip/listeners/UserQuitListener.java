package com.muhammaddaffa.nextcoinflip.listeners;

import com.muhammaddaffa.nextcoinflip.coinflip.CoinflipGame;
import com.muhammaddaffa.nextcoinflip.coinflip.impl.ItemCoinflipGame;
import com.muhammaddaffa.nextcoinflip.coinflip.repository.CoinflipRepository;
import com.muhammaddaffa.nextcoinflip.gui.CoinflipGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class UserQuitListener implements Listener {

    @EventHandler
    private void selfFinish(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // Remove all things
        this.finishOwnGame(player);
        this.finishOtherGame(player);
        this.removeItemOffer(player);
    }

    private void removeItemOffer(Player player) {
        CoinflipRepository.getGames()
                .stream()
                .filter(game -> game instanceof ItemCoinflipGame)
                .map(game -> (ItemCoinflipGame) game)
                .forEach(game -> game.cancelChallenge(player));
    }

    private void finishOtherGame(Player player) {
        CoinflipRepository.getGames()
                .stream()
                .filter(game -> game.getChallenger() != null && game.getChallenger().equals(player.getUniqueId()) && game.isStarting())
                .forEach(this::tryFinish);
    }

    private void finishOwnGame(Player player) {
        CoinflipRepository.getGames(player)
                .stream()
                .filter(CoinflipGame::isStarting)
                .forEach(this::tryFinish);
    }

    private void tryFinish(CoinflipGame game) {
        CoinflipGUI menu = game.getGameMenu();
        if (menu == null) {
            return;
        }
        CoinflipGUI.CoinflipTask task = menu.getTask();
        if (task == null) {
            return;
        }
        // Finish the coinflip game
        task.finish();
    }

}
