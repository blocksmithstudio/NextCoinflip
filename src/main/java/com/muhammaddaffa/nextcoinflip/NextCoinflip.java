package com.muhammaddaffa.nextcoinflip;

import com.muhammaddaffa.mdlib.MDLib;
import com.muhammaddaffa.mdlib.utils.Config;
import com.muhammaddaffa.mdlib.utils.ConfigBuilder;
import com.muhammaddaffa.nextcoinflip.coinflip.CoinflipGame;
import com.muhammaddaffa.nextcoinflip.coinflip.repository.CoinflipRepository;
import com.muhammaddaffa.nextcoinflip.coinflip.tasks.OfferExpiryTask;
import com.muhammaddaffa.nextcoinflip.commands.MainCommand;
import com.muhammaddaffa.nextcoinflip.currencies.repository.CurrencyRepository;
import com.muhammaddaffa.nextcoinflip.listeners.UserQuitListener;
import com.muhammaddaffa.nextcoinflip.users.repository.UserRepository;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class NextCoinflip extends JavaPlugin {

    private static NextCoinflip instance;

    public static Config DEFAULT_CONFIG, CURRENCY_CONFIG, DATA_CONFIG;
    public static Config GAME_SELECTOR_MENU_CONFIG, CURRENCY_SELECTOR_MENU_CONFIG, CONFIRM_CURRENCY_WAGER_MENU_CONFIG,
            CONFIRM_ITEM_WAGER_CREATION_CONFIG, WON_ITEM_CONFIG, STATISTIC_CONFIG, CONFIRM_ITEM_WAGER_CHALLENGER_CONFIG;

    @Override
    public void onLoad() {
        MDLib.inject(this);
    }

    @Override
    public void onEnable() {
        MDLib.onEnable(this);
        instance = this;

        // Register configs
        configs();

        // Register commands & listeners
        commands();
        listeners();

        // Register tasks
        tasks();

        // Load all data
        UserRepository.load();
        CurrencyRepository.load();
    }

    @Override
    public void onDisable() {
        MDLib.shutdown();
        // On server stop, we should cancel all coinflip
        CoinflipRepository.getGames().forEach(CoinflipGame::cancel);
        // Save all users data
        UserRepository.save();
    }

    private void configs() {
        // config.yml
        DEFAULT_CONFIG = new ConfigBuilder("config.yml").setShouldAutoUpdate(true).build();
        // currencies.yml
        CURRENCY_CONFIG = new ConfigBuilder("currencies.yml").build();
        // gui/game_selector.yml
        GAME_SELECTOR_MENU_CONFIG = new ConfigBuilder("game_selector.yml").setDirectory("gui").build();
        // gui/currency_selector.yml
        CURRENCY_SELECTOR_MENU_CONFIG = new ConfigBuilder("currency_selector.yml").setDirectory("gui").build();
        // gui/confirm_currency_wager.yml
        CONFIRM_CURRENCY_WAGER_MENU_CONFIG = new ConfigBuilder("confirm_currency_wager.yml").setDirectory("gui").build();
        // gui/confirm_item_wager_creation.yml
        CONFIRM_ITEM_WAGER_CREATION_CONFIG = new ConfigBuilder("confirm_item_wager_creation.yml").setDirectory("gui").build();
        // gui/won_items.yml
        WON_ITEM_CONFIG = new ConfigBuilder("won_items.yml").setDirectory("gui").build();
        // gui/statistic.yml
        STATISTIC_CONFIG = new ConfigBuilder("statistic.yml").setDirectory("gui").build();
        // gui/confirm_item_wager_challenger.yml
        CONFIRM_ITEM_WAGER_CHALLENGER_CONFIG = ConfigBuilder.create("confirm_item_wager_challenger.yml").setDirectory("gui").build();
        // data.yml
        DATA_CONFIG = ConfigBuilder.create("data.yml").setShouldReload(false).build();

        Config.updateConfigs();
        Config.reload();
    }

    private void listeners() {
        PluginManager pm = Bukkit.getPluginManager();
        // Register events
        pm.registerEvents(new UserQuitListener(), this);
    }

    private void commands() {
        new MainCommand();
    }

    private void tasks() {
        // Offer expiry task
        new OfferExpiryTask().runTaskTimer(this, 0L, (10 * 200));
    }

    public static NextCoinflip getInstance() {
        return instance;
    }

}
