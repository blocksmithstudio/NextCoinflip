package com.muhammaddaffa.nextcoinflip.coinflip.impl;

import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.ItemBuilder;
import com.muhammaddaffa.mdlib.utils.Placeholder;
import com.muhammaddaffa.nextcoinflip.NextCoinflip;
import com.muhammaddaffa.nextcoinflip.coinflip.CoinflipGame;
import com.muhammaddaffa.nextcoinflip.coinflip.repository.CoinflipRepository;
import com.muhammaddaffa.nextcoinflip.currencies.Currency;
import com.muhammaddaffa.nextcoinflip.gui.ConfirmationCurrencyGUI;
import com.muhammaddaffa.nextcoinflip.utils.SkullCreator;
import com.muhammaddaffa.nextcoinflip.utils.SoundUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CurrencyCoinflipGame extends CoinflipGame {

    private final Currency currency;
    private final int wager;

    private boolean starting = false;

    public CurrencyCoinflipGame(UUID creator, Currency currency, int wager) {
        super(creator);
        if (!isValidWager(wager) || wager < Math.max(1, currency.minWager())
                || (currency.maxWager() > 0 && wager > currency.maxWager())) {
            throw new IllegalArgumentException("Wager is outside the supported currency limits");
        }
        this.currency = currency;
        this.wager = wager;
    }

    public static boolean isValidWager(int wager) {
        return wager > 0 && wager <= Integer.MAX_VALUE / 2;
    }

    @Override
    public String getId() {
        return currency.id();
    }

    @Override
    public ItemStack getItem() {
        Placeholder placeholder = this.getDefaultPlaceholders()
                .add("{wager_digits}", Common.digits(getWager()))
                .add("{wager_format}", Common.format(getWager()))
                .add("{currency}", currency.identifier());

        FileConfiguration config = NextCoinflip.GAME_SELECTOR_MENU_CONFIG.getConfig();
        ItemBuilder builder = SkullCreator.createItem(this.getCreator())
                .name(config.getString("wager-items.currency.display-name"))
                .lore(config.getStringList("wager-items.currency.lore"))
                .placeholder(placeholder);

        return builder.build();
    }

    @Override
    public void cancel(boolean shutdown) {
        this.settle(null, () -> {
            List<Settlement> deliveries = new ArrayList<>();
            deliveries.add(new Settlement(this.getCreator(), "currency", () -> this.refund(this.getCreator(), shutdown)));
            if (this.getChallenger() != null) {
                deliveries.add(new Settlement(this.getChallenger(), "currency", () -> this.refund(this.getChallenger(), shutdown)));
            }
            return deliveries;
        });
    }

    @Override
    public void finish(UUID winner, UUID loser) {
        this.settle(winner, () -> List.of(
                new Settlement(winner, "currency", () -> this.deposit(winner, this.getWager() * 2)),
                this.updateUser(winner, user -> {
                    user.getStatistic().resetLoseStreak();
                    user.getStatistic().addWins();
                    user.getStatistic().addCurrencyEarnings(this.currency, this.wager);
                }),
                this.updateUser(loser, user -> {
                    user.getStatistic().addLosses();
                    user.getStatistic().addLoseStreak();
                    user.getStatistic().addCurrencyEarnings(this.currency, -this.wager);
                })));
    }

    public int getWager() {
        return wager;
    }

    @Override
    public Map<String, Object> recoveryData() {
        Map<String, Object> data = super.recoveryData();
        data.put("wager", this.wager);
        data.put("give-command", this.currency.giveCommand());
        return data;
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

    public boolean withdraw(Player player, int amount) {
        return currency.takeBalance(player, amount);
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
        new ConfirmationCurrencyGUI(this)
                .onCancel(() -> {
                    clicker.closeInventory();
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
                    if (!this.hasEnough(clicker)) {
                        NextCoinflip.DEFAULT_CONFIG.sendMessage(clicker, "messages.balance-not-enough", new Placeholder()
                                .add("{currency}", currency.identifier()));

                        clicker.closeInventory();
                        SoundUtil.villagerNo(clicker);
                        return;
                    }
                    starting = true;
                    this.setChallenger(clicker.getUniqueId());
                    if (!withdraw(clicker, this.getWager())) {
                        this.setChallenger(null);
                        starting = false;
                        clicker.closeInventory();
                        SoundUtil.villagerNo(clicker);
                        return;
                    }
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
