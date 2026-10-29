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

import java.util.List;

public class CurrencySelectorGUI extends FastInv {

    private final int wager;

    public CurrencySelectorGUI(int wager) {
        super(NextCoinflip.CURRENCY_SELECTOR_MENU_CONFIG.getInt("size"),
                Common.color(NextCoinflip.CURRENCY_SELECTOR_MENU_CONFIG.getString("title")));
        this.wager = wager;

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
        type = type.toLowerCase(java.util.Locale.ROOT);

        if (CurrencyRepository.getCurrency(type) == null) {
            NextCoinflip.DEFAULT_CONFIG.sendMessage(player, "messages.invalid-type");
            return;
        }

        Currency currency = CurrencyRepository.getCurrency(type);
        if (currency == null || !currency.enabled() || !CurrencyCoinflipGame.isValidWager(this.wager)) {
            NextCoinflip.DEFAULT_CONFIG.sendMessage(player, "messages.invalid-wager");
            playFailedSounds(player);
            player.closeInventory();
            return;
        }

        if (this.wager < Math.max(1, currency.minWager())) {
            sendMessage(player, "messages.minimum-required", Math.max(1, currency.minWager()), currency);
            playFailedSounds(player);
            // Close the inventory
            player.closeInventory();
            return;
        }

        if (currency.maxWager() > 0 && this.wager > currency.maxWager()) {
            sendMessage(player, "messages.maximum-reached", currency.maxWager(), currency);
            playFailedSounds(player);
            // Close the inventory
            player.closeInventory();
            return;
        }

        // Check if player has enough wager
        CurrencyCoinflipGame game = new CurrencyCoinflipGame(player.getUniqueId(), currency, this.wager);
        if (!game.hasEnough(player)) {
            NextCoinflip.DEFAULT_CONFIG.sendMessage(player, "messages.balance-not-enough", new Placeholder()
                    .add("{currency}", currency.identifier()));
            // Failed to create a coinflip game
            playFailedSounds(player);
            player.closeInventory();
            return;
        }

        if (!hasExistingGameWithSameType(player, type)) {
            // On game registered, withdraw the amount of wager from player
            if (!game.withdraw(player, game.getWager())) {
                playFailedSounds(player);
                player.closeInventory();
                return;
            }
            CoinflipRepository.register(game);

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

        Common.broadcast(PlaceholderAPI.setPlaceholders(player, message));
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
