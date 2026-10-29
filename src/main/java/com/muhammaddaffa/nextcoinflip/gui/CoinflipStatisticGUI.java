package com.muhammaddaffa.nextcoinflip.gui;

import com.muhammaddaffa.mdlib.fastinv.FastInv;
import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.ItemBuilder;
import com.muhammaddaffa.mdlib.utils.Placeholder;
import com.muhammaddaffa.nextcoinflip.NextCoinflip;
import com.muhammaddaffa.nextcoinflip.currencies.Currency;
import com.muhammaddaffa.nextcoinflip.currencies.repository.CurrencyRepository;
import com.muhammaddaffa.nextcoinflip.users.User;
import com.muhammaddaffa.nextcoinflip.users.models.CoinflipStatistic;
import com.muhammaddaffa.nextcoinflip.users.repository.UserRepository;
import com.muhammaddaffa.nextcoinflip.utils.SoundUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CoinflipStatisticGUI extends FastInv {

    private final Player player;
    private final User user;

    public CoinflipStatisticGUI(Player player) {
        super(NextCoinflip.STATISTIC_CONFIG.getInt("size"),
                Common.color(NextCoinflip.STATISTIC_CONFIG.getString("title")));
        this.player = player;
        this.user = UserRepository.getUser(player);

        this.setAllItems();
    }

    private void setAllItems() {
        this.setAdditionalItems();
        this.setEarningsLossItem();
    }

    private void setEarningsLossItem() {
        FileConfiguration config = NextCoinflip.STATISTIC_CONFIG.getConfig();
        CoinflipStatistic statistic = user.getStatistic();
        List<Integer> slots = config.getIntegerList("earnings-losses-item.slots");
        // Create the lore first
        List<String> lore = new ArrayList<>();
        // Get all currencies in the game
        for (Currency currency : CurrencyRepository.getCurrencies()) {
            if (!currency.enabled()) continue;
            // Add the earnings lore
            long earnings = statistic.getCurrencyEarnings(currency);
            // Check positive or negative
            if (earnings >= 0) {
                lore.addAll(new Placeholder()
                        .add("{amount}", Common.digits(Math.abs(earnings)))
                        .add("{currency}", currency.identifier())
                        .translate(config.getStringList("earnings-losses-item.earnings.positive")));
            } else {
                lore.addAll(new Placeholder()
                        .add("{amount}", Common.digits(Math.abs(earnings)))
                        .add("{currency}", currency.identifier())
                        .translate(config.getStringList("earnings-losses-item.earnings.negative")));
            }


        }

        // Create the item
        ItemStack stack = ItemBuilder.fromConfig(config, "earnings-losses-item")
                .lore(lore)
                .build();
        // Set the items on the menu
        this.setItems(slots, stack);
    }

    private void setAdditionalItems() {
        FileConfiguration config = NextCoinflip.STATISTIC_CONFIG.getConfig();
        CoinflipStatistic statistic = user.getStatistic();
        // Get list of items
        for (String key : config.getConfigurationSection("items").getKeys(false)) {
            String type = config.getString("items." + key + ".type", "DUMMY");
            List<Integer> slots = config.getIntegerList("items." + key + ".slots");
            ItemStack stack = ItemBuilder.fromConfig(config, "items." + key)
                    .placeholder(new Placeholder()
                            .add("{wins}", Common.digits(statistic.getWins()))
                            .add("{losses}", Common.digits(statistic.getLosses()))
                            .add("{winrate}", Common.digits(statistic.getWinRate()))
                            .add("{total_game}", Common.digits(statistic.getTotalGames()))
                            .add("{streak}", Common.digits(statistic.getLoseStreak())))
                    .build();

            // Set the items
            this.setItems(slots, stack, event -> {
                if (!(event.getWhoClicked() instanceof Player player)) return;
                // Check for next page and previous page
                if (type.equalsIgnoreCase("back")) {
                    new GameSelectorGUI(player).open(player);
                    SoundUtil.click(player);
                }
            });
        }
    }

}
