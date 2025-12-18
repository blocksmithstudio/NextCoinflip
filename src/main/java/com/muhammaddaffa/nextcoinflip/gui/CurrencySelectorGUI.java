package com.muhammaddaffa.nextcoinflip.gui;

import com.muhammaddaffa.mdlib.fastinv.FastInv;
import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.ItemBuilder;
import com.muhammaddaffa.mdlib.utils.Placeholder;
import com.muhammaddaffa.nextcoinflip.NextCoinflip;
import com.muhammaddaffa.nextcoinflip.coinflip.impl.CurrencyCoinflipGame;
import com.muhammaddaffa.nextcoinflip.coinflip.repository.CoinflipRepository;
import com.muhammaddaffa.nextcoinflip.currencies.Currency;
import com.muhammaddaffa.nextcoinflip.currencies.repository.CurrencyRepository;
import com.muhammaddaffa.nextcoinflip.utils.SoundUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class CurrencySelectorGUI extends FastInv {

    private final int wager;
    private final Map<String, Function<Player, CurrencyCoinflipGame>> gameFactories = new ConcurrentHashMap<>();

    public CurrencySelectorGUI(int wager) {
        super(NextCoinflip.CURRENCY_SELECTOR_MENU_CONFIG.getInt("size"),
                Common.color(NextCoinflip.CURRENCY_SELECTOR_MENU_CONFIG.getString("title")));
        this.wager = Math.abs(wager);

        for (Currency currency : CurrencyRepository.getCurrencies()) {
            if (!currency.enabled()) continue;
            this.gameFactories.put(currency.id(), player -> new CurrencyCoinflipGame(player.getUniqueId(), currency, wager));
        }

        this.setAllItems();
    }

    private void setAllItems() {
        FileConfiguration config = NextCoinflip.CURRENCY_SELECTOR_MENU_CONFIG.getConfig();

        for (String key : config.getConfigurationSection("items").getKeys(false)) {
            String type = config.getString("items." + key + ".type", "DUMMY");
            List<Integer> slots = config.getIntegerList("items." + key + ".slots");
            ItemStack stack = ItemBuilder.fromConfig(config, "items." + key).build();

            this.setItems(slots, stack, event -> {
                if (!(event.getWhoClicked() instanceof Player player)) return;
                handleCoinflipSelection(player, type);
            });
        }
    }

    private void handleCoinflipSelection(Player player, String type) {
        type = type.toLowerCase();

        if (!gameFactories.containsKey(type)) {
            NextCoinflip.DEFAULT_CONFIG.sendMessage(player, "messages.invalid-type");
            return;
        }

        CurrencyCoinflipGame game = gameFactories.get(type).apply(player);
        Currency currency = game.getCurrency();

        if (this.wager < Math.max(1, game.getMinimumWager())) {
            sendMessage(player, "messages.minimum-required", Math.max(1, game.getMinimumWager()), currency);
            playFailedSounds(player);
            // Close the inventory
            player.closeInventory();
            return;
        }

        if (game.getMaximumWager() > 0 && this.wager > game.getMaximumWager()) {
            sendMessage(player, "messages.maximum-reached", game.getMaximumWager(), currency);
            playFailedSounds(player);
            // Close the inventory
            player.closeInventory();
            return;
        }

        // Check if player has enough wager
        if (!game.hasEnough(player)) {
            NextCoinflip.DEFAULT_CONFIG.sendMessage(player, "messages.not-enough-balance", new Placeholder()
                    .add("{currency}", currency.identifier()));
            // Failed to create a coinflip game
            playFailedSounds(player);
            player.closeInventory();
            return;
        }

        if (!hasExistingGameWithSameType(player, type)) {
            CoinflipRepository.register(game);
            // On game registered, withdraw the amount of wager from player
            game.withdraw(player, game.getWager());

            sendBroadcast(player, this.wager, currency);
            player.closeInventory();
            playSuccessSounds(player);
        }
    }

    private void sendMessage(Player player, String messageKey, double wager, Currency currency) {
        NextCoinflip.DEFAULT_CONFIG.sendMessage(player, messageKey, new Placeholder()
                .add("{wager}", Common.format(wager))
                .add("{currency}", currency.identifier()));
    }

    private void sendBroadcast(Player player, double wager, Currency currency) {
        String message = NextCoinflip.DEFAULT_CONFIG.getString("messages.coinflip-create")
                .replace("{player}", player.getName())
                .replace("{display_name}", player.getDisplayName())
                .replace("{wager}", Common.format(wager))
                .replace("{currency}", currency.identifier());

        NextCoinflip.DEFAULT_CONFIG.broadcast(PlaceholderAPI.setPlaceholders(player, message));
    }

    private boolean hasExistingGameWithSameType(Player player, String coinflipType) {
        if (CoinflipRepository
                .getGames(player)
                .stream()
                .anyMatch(game -> game.getId().equalsIgnoreCase(coinflipType))) {
            player.closeInventory();
            NextCoinflip.DEFAULT_CONFIG.sendMessage(player, "messages.game-existed");
            playFailedSounds(player);
            return true;
        }
        return false;
    }

    private void playFailedSounds(Player player) {
        SoundUtil.click(player);
        SoundUtil.villagerNo(player);
    }

    private void playSuccessSounds(Player player) {
        SoundUtil.click(player);
        SoundUtil.pling(player);
    }

}
