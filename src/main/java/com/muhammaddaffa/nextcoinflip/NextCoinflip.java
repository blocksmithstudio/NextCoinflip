package com.muhammaddaffa.nextcoinflip;

import com.muhammaddaffa.mdlib.MDLib;
import com.muhammaddaffa.mdlib.utils.Config;
import com.muhammaddaffa.mdlib.utils.ConfigBuilder;
import com.muhammaddaffa.mdlib.utils.Executor;
import com.muhammaddaffa.mdlib.utils.Logger;
import com.muhammaddaffa.nextcoinflip.coinflip.CoinflipGame;
import com.muhammaddaffa.nextcoinflip.coinflip.impl.ItemCoinflipGame;
import com.muhammaddaffa.nextcoinflip.coinflip.settlement.SettlementRecovery;
import com.muhammaddaffa.nextcoinflip.coinflip.repository.CoinflipRepository;
import com.muhammaddaffa.nextcoinflip.coinflip.tasks.OfferExpiryTask;
import com.muhammaddaffa.nextcoinflip.commands.CoinflipCommand;
import com.muhammaddaffa.nextcoinflip.currencies.repository.CurrencyRepository;
import com.muhammaddaffa.nextcoinflip.database.SQLDatabase;
import com.muhammaddaffa.nextcoinflip.hooks.CoinflipExpansion;
import com.muhammaddaffa.nextcoinflip.offlinecommand.OfflineCommandManager;
import com.muhammaddaffa.nextcoinflip.users.listeners.UserQuitListener;
import com.muhammaddaffa.nextcoinflip.users.listeners.UserListener;
import com.muhammaddaffa.nextcoinflip.users.repository.UserRepository;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;

public final class NextCoinflip extends JavaPlugin {

    private static final int PLUGIN_ID = 27532;

    private static NextCoinflip instance;

    public static Config DEFAULT_CONFIG, CURRENCY_CONFIG, DATA_CONFIG;
    public static Config GAME_SELECTOR_MENU_CONFIG, CURRENCY_SELECTOR_MENU_CONFIG, CONFIRM_CURRENCY_WAGER_MENU_CONFIG,
            CONFIRM_ITEM_WAGER_CREATION_CONFIG, WON_ITEM_CONFIG, STATISTIC_CONFIG, CONFIRM_ITEM_WAGER_CHALLENGER_CONFIG;

    private final SQLDatabase database = new SQLDatabase();
    @Override
    public void onLoad() {
        MDLib.inject(this);
    }

    @Override
    public void onEnable() {
        MDLib.onEnable(this);
        instance = this;

        SettlementRecovery.requireResolved(this.getDataFolder().toPath());

        configs();

        database.connect();
        database.createStatisticTable();
        database.createItemTable();

        CurrencyRepository.load();
        Bukkit.getOnlinePlayers().forEach(UserRepository::load);
        commands();
        listeners();
        tasks();
        metrics();

        new CoinflipExpansion().register();

        OfflineCommandManager.load();
        OfflineCommandManager.startTask();
    }

    @Override
    public void onDisable() {
        try {
            for (CoinflipGame game : new ArrayList<>(CoinflipRepository.getGames())) {
                this.shutdownStep("game settlement", () -> game.cancel(true));
            }
            if (DATA_CONFIG != null) {
                this.shutdownStep("pending currency deliveries", OfflineCommandManager::save);
            }
            this.shutdownStep("player data", UserRepository::unloadAll);
            this.shutdownStep("recovery snapshot", SettlementRecovery::save);
        } finally {
            try {
                this.shutdownStep("database pool", this.database::close);
            } finally {
                this.shutdownStep("library cleanup", MDLib::shutdown);
            }
        }
    }

    private void shutdownStep(String operation, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            Logger.severe("NextCoinflip could not complete " + operation + "; recovery is required.");
        }
    }

    private void configs() {
        DEFAULT_CONFIG = new ConfigBuilder("config.yml").setShouldAutoUpdate(true).build();
        CURRENCY_CONFIG = new ConfigBuilder("currencies.yml").build();
        GAME_SELECTOR_MENU_CONFIG = new ConfigBuilder("game_selector.yml").setDirectory("gui").build();
        CURRENCY_SELECTOR_MENU_CONFIG = new ConfigBuilder("currency_selector.yml").setDirectory("gui").build();
        CONFIRM_CURRENCY_WAGER_MENU_CONFIG = new ConfigBuilder("confirm_currency_wager.yml").setDirectory("gui").build();
        CONFIRM_ITEM_WAGER_CREATION_CONFIG = new ConfigBuilder("confirm_item_wager_creation.yml").setDirectory("gui").build();
        WON_ITEM_CONFIG = new ConfigBuilder("won_items.yml").setDirectory("gui").build();
        STATISTIC_CONFIG = new ConfigBuilder("statistic.yml").setDirectory("gui").build();
        CONFIRM_ITEM_WAGER_CHALLENGER_CONFIG = ConfigBuilder.create("confirm_item_wager_challenger.yml").setDirectory("gui").build();
        DATA_CONFIG = ConfigBuilder.create("data.yml").setShouldReload(false).build();

        Config.updateConfigs();
        Config.reload();
    }

    private void listeners() {
        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new UserQuitListener(), this);
        pm.registerEvents(new UserListener(), this);
    }

    private void commands() {
        MDLib.getCommandRegistry().register(new CoinflipCommand());
    }

    private void tasks() {
        new OfferExpiryTask().runTaskTimer(this, 0L, (10 * 200));
        Executor.syncTimer(20L, 20L, () -> {
            for (CoinflipGame game : new ArrayList<>(CoinflipRepository.getGames())) {
                try {
                    if (game.hasPendingSettlement()) {
                        game.retrySettlement();
                    }
                    if (game instanceof ItemCoinflipGame itemGame) {
                        itemGame.retryRefunds();
                    }
                } catch (RuntimeException ex) {
                    Logger.severe("Game " + game.getUniqueId() + " has pending settlement work; retrying.");
                }
            }
        });
    }

    private void metrics() {
        Metrics metrics = new Metrics(this, PLUGIN_ID);
    }

    public SQLDatabase getDatabase() {
        return database;
    }

    public static NextCoinflip getInstance() {
        return instance;
    }

}
