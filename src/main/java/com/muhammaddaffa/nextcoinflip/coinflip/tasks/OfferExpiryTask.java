package com.muhammaddaffa.nextcoinflip.coinflip.tasks;

import com.muhammaddaffa.nextcoinflip.NextCoinflip;
import com.muhammaddaffa.nextcoinflip.coinflip.impl.ItemCoinflipGame;
import com.muhammaddaffa.nextcoinflip.coinflip.repository.CoinflipRepository;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class OfferExpiryTask extends BukkitRunnable {

    @Override
    public void run() {
        for (ItemCoinflipGame game : CoinflipRepository.getGames()
                .stream()
                .filter(game -> game instanceof ItemCoinflipGame g && !g.isStarting())
                .map(game -> (ItemCoinflipGame) game)
                .toList()) {

            // Check if one of the offer is expired
            Map<String, ItemCoinflipGame.ChallengerDetails> map = game.getChallengerMap();
            for (ItemCoinflipGame.ChallengerDetails detail : map.values()) {
                if (TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - detail.timestamp()) > 120) {
                    Player player = Bukkit.getPlayer(detail.uuid());
                    // Remove the challenge
                    game.cancelChallenge(detail.uuid());
                    // Sends message
                    if (player != null) {
                        NextCoinflip.DEFAULT_CONFIG.sendMessage(player, "messages.offer-expired");
                    }
                }
            }

        }
    }

}