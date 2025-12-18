package com.muhammaddaffa.nextcoinflip.gui;

import com.muhammaddaffa.mdlib.fastinv.FastInv;
import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.ItemBuilder;
import com.muhammaddaffa.mdlib.utils.Placeholder;
import com.muhammaddaffa.nextcoinflip.NextCoinflip;
import com.muhammaddaffa.nextcoinflip.coinflip.impl.CurrencyCoinflipGame;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ConfirmationCurrencyGUI extends FastInv {

    private final CurrencyCoinflipGame game;
    private Runnable onAccept, onCancel;

    public ConfirmationCurrencyGUI(CurrencyCoinflipGame game) {
        super(NextCoinflip.CONFIRM_CURRENCY_WAGER_MENU_CONFIG.getInt("size"),
                Common.color(NextCoinflip.CONFIRM_CURRENCY_WAGER_MENU_CONFIG.getString("title")));
        this.game = game;

        this.setAllItems();
    }

    public ConfirmationCurrencyGUI onAccept(Runnable runnable) {
        onAccept = runnable;
        return this;
    }

    public ConfirmationCurrencyGUI onCancel(Runnable runnable) {
        onCancel = runnable;
        return this;
    }

    private void setAllItems() {
        FileConfiguration config = NextCoinflip.CONFIRM_CURRENCY_WAGER_MENU_CONFIG.getConfig();
        // Get list of items
        for (String key : config.getConfigurationSection("items").getKeys(false)) {
            String type = config.getString("items." + key + ".type", "DUMMY");
            List<Integer> slots = config.getIntegerList("items." + key + ".slots");
            ItemStack stack = ItemBuilder.fromConfig(config, "items." + key)
                    .placeholder(new Placeholder()
                            .add("{wager_digits}", Common.digits(game.getWager()))
                            .add("{wager_format}", Common.format(game.getWager()))
                            .add("{currency}", game.getCurrency().identifier())
                            .add("{creator_name}", game.getCreatorName())
                            .add("{creator_display_name}", game.getCreatorDisplayName()))
                    .build();

            // Set the items
            this.setItems(slots, stack, event -> {
                if (!(event.getWhoClicked() instanceof Player player)) return;
                // Check for next page and previous page
                if (type.equalsIgnoreCase("confirm")) {
                    if (onAccept != null) onAccept.run();
                }
                if (type.equalsIgnoreCase("cancel")) {
                    if (onCancel != null) onCancel.run();
                }
            });
        }
    }

}
