package com.muhammaddaffa.nextcoinflip.hooks;

import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.nextcoinflip.currencies.Currency;
import com.muhammaddaffa.nextcoinflip.currencies.repository.CurrencyRepository;
import com.muhammaddaffa.nextcoinflip.users.User;
import com.muhammaddaffa.nextcoinflip.users.repository.UserRepository;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CoinflipExpansion extends PlaceholderExpansion {

    @Override
    public @NotNull String getAuthor() {
        return "aglerr";
    }

    @Override
    public @NotNull String getIdentifier() {
        return "nextcoinflip";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true; // This is required or else PlaceholderAPI will unregister the Expansion on reload
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        User user = UserRepository.getUser(player.getUniqueId());
        if (params.equalsIgnoreCase("wins")) {
            return user.getStatistic().getWins() + "";
        }

        if (params.equalsIgnoreCase("losses")) {
            return user.getStatistic().getLosses() + "";
        }

        if (params.equalsIgnoreCase("losestreak")) {
            return user.getStatistic().getLoseStreak() + "";
        }

        if (params.equalsIgnoreCase("winrate")) {
            return Common.digits(user.getStatistic().getWinRate());
        }

        if (params.equalsIgnoreCase("total_game")) {
            return Common.digits(user.getStatistic().getTotalGames());
        }

        if (params.startsWith("earnings")) {
            // %nextcoinflip_earnings_<currency>%
            String label = params.replace("earnings_", "");
            Currency currency = CurrencyRepository.getCurrency(label);
            if (currency == null || !currency.enabled()) {
                return null;
            }
            return Common.digits(user.getStatistic().getCurrencyEarnings(currency));
        }

        return null; // Placeholder is unknown by the Expansion
    }

}
