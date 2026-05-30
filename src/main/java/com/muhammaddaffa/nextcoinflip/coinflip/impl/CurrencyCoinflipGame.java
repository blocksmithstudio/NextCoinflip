package com.muhammaddaffa.nextcoinflip.coinflip.impl;

import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.ItemBuilder;
import com.muhammaddaffa.mdlib.utils.Placeholder;
import com.muhammaddaffa.nextcoinflip.NextCoinflip;
import com.muhammaddaffa.nextcoinflip.coinflip.CoinflipGame;
import com.muhammaddaffa.nextcoinflip.coinflip.repository.CoinflipRepository;
import com.muhammaddaffa.nextcoinflip.currencies.Currency;
import com.muhammaddaffa.nextcoinflip.gui.ConfirmationCurrencyGUI;
import com.muhammaddaffa.nextcoinflip.users.User;
import com.muhammaddaffa.nextcoinflip.users.repository.UserRepository;
import com.muhammaddaffa.nextcoinflip.utils.SkullCreator;
import com.muhammaddaffa.nextcoinflip.utils.SoundUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class CurrencyCoinflipGame extends CoinflipGame {

    private final Currency currency;
    private final int wager;

    private boolean starting = false;

    public CurrencyCoinflipGame(UUID creator, Currency currency, int wager) {
        super(creator);
        this.currency = currency;
        this.wager = wager;
    }

    @Override
    public String getId() {
        return currency.id();
    }

    @Override
    public ItemStack getItem() {
        // Create the placeholders
        Placeholder placeholder = this.getDefaultPlaceholders()
                .add("{wager_digits}", Common.digits(getWager()))
                .add("{wager_format}", Common.format(getWager()))
                .add("{currency}", currency.identifier());

        // Get the config and create the item
        FileConfiguration config = NextCoinflip.GAME_SELECTOR_MENU_CONFIG.getConfig();
        ItemBuilder builder = SkullCreator.createItem(this.getCreator())
                .name(config.getString("wager-items.currency.display-name"))
                .lore(config.getStringList("wager-items.currency.lore"))
                .placeholder(placeholder);

        // Return the item
        return builder.build();
    }

    @Override
    public void cancel(boolean shutdown) {
        refund(this.getCreator(), shutdown);
        if (this.getChallenger() != null) {
            refund(this.getChallenger(), shutdown);
        }
    }

    @Override
    public void finish(UUID winner, UUID loser) {
        // Add statistic to winner and loser
        User winnerUser = UserRepository.getUser(winner);
        winnerUser.getStatistic().resetLoseStreak();
        winnerUser.getStatistic().addWins();
        winnerUser.getStatistic().addCurrencyEarnings(currency, this.getWager());

        User loserUser = UserRepository.getUser(loser);
        loserUser.getStatistic().addLosses();
        loserUser.getStatistic().addLoseStreak();
        loserUser.getStatistic().addCurrencyEarnings(currency, -this.getWager());

        // Double the amount of deposit on winning
        // Only deposit no withdraw because withdraw already happened before the game finish
        deposit(winner, this.getWager() * 2);

        // Save the user here
        winnerUser.save(true);
        loserUser.save(true);
    }

    public int getWager() {
        return wager;
    }

    public Currency getCurrency() {
        return currency;
    }

    public int getMinimumWager() {
        return currency.minWager();
    }

    public int getMaximumWager() {
        return currency.maxWager();
    }

    public boolean hasEnough(Player player) {
        return currency.hasEnoughBalance(player, this.wager);
    }

    public void withdraw(Player player, int amount) {
        currency.takeBalance(player, amount);
    }

    public void deposit(Player player, int amount) {
        currency.giveBalance(player, amount);
    }

    public void deposit(UUID uuid, int amount) {
        currency.giveBalance(uuid, amount);
    }

    private void refund(UUID uuid, boolean queueOnly) {
        if (queueOnly) {
            currency.queueGiveBalance(uuid, this.getWager());
        } else {
            currency.giveBalance(uuid, this.getWager());
        }
    }

    @Override
    public void onClick(Player clicker) {
        // Open the confirmation menu gui
        new ConfirmationCurrencyGUI(this)
                .onCancel(() -> {
                    // Close the inventory
                    clicker.closeInventory();
                    // Click sound
                    SoundUtil.click(clicker);
                })
                .onAccept(() -> {
                    if (starting || !CoinflipRepository.contains(this) || !this.canStart(clicker.getUniqueId())) {
                        NextCoinflip.DEFAULT_CONFIG.sendMessage(clicker, "messages.coinflip-already-started");
                        clicker.closeInventory();
                        SoundUtil.villagerNo(clicker);
                        return;
                    }

                    SoundUtil.click(clicker);
                    // Check if the guy has enough money
                    if (!this.hasEnough(clicker)) {
                        NextCoinflip.DEFAULT_CONFIG.sendMessage(clicker, "messages.balance-not-enough", new Placeholder()
                                .add("{currency}", currency.identifier()));

                        // Close the inventory
                        clicker.closeInventory();
                        // Play fail sounds
                        SoundUtil.villagerNo(clicker);
                        return;
                    }
                    // Mark this game as started
                    starting = true;
                    // Set the challenger
                    this.setChallenger(clicker.getUniqueId());
                    // Withdraw money from the challenger
                    withdraw(clicker, this.getWager());
                    // Start the game
                    if (!this.start()) {
                        deposit(clicker, this.getWager());
                        this.setChallenger(null);
                        starting = false;
                        clicker.closeInventory();
                        NextCoinflip.DEFAULT_CONFIG.sendMessage(clicker, "messages.coinflip-already-started");
                        SoundUtil.villagerNo(clicker);
                    }
                })
                .open(clicker);
    }

}
