package com.muhammaddaffa.nextcoinflip.gui;

import com.muhammaddaffa.mdlib.fastinv.FastInv;
import com.muhammaddaffa.mdlib.task.handleTask.HandleTask;
import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.Executor;
import com.muhammaddaffa.mdlib.utils.ItemBuilder;
import com.muhammaddaffa.mdlib.utils.Placeholder;
import com.muhammaddaffa.nextcoinflip.NextCoinflip;
import com.muhammaddaffa.nextcoinflip.coinflip.CoinflipGame;
import com.muhammaddaffa.nextcoinflip.coinflip.repository.CoinflipRepository;
import com.muhammaddaffa.nextcoinflip.gui.helpers.Pagination;
import com.muhammaddaffa.nextcoinflip.users.User;
import com.muhammaddaffa.nextcoinflip.users.models.WonItem;
import com.muhammaddaffa.nextcoinflip.users.repository.UserRepository;
import com.muhammaddaffa.nextcoinflip.utils.SoundUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public class GameSelectorGUI extends FastInv {

    private final Player player;
    private Pagination pagination;

    public GameSelectorGUI(Player player) {
        super(NextCoinflip.GAME_SELECTOR_MENU_CONFIG.getInt("size"), Common.color(NextCoinflip.GAME_SELECTOR_MENU_CONFIG.getString("title")));
        this.player = player;

        this.setAllItems();
        // Refresh the gui every 5 tick
        HandleTask task = Executor.syncTimer(0L, 5L, this::setAllItems);
        // Cancel the task on close
        this.addCloseHandler(event -> task.cancel());
    }

    private void setAllItems() {
        FileConfiguration config = NextCoinflip.GAME_SELECTOR_MENU_CONFIG.getConfig();
        List<Integer> paginationSlots = config.getIntegerList("games-slots");

        // Clear the inventory
        this.clearItems();
        // Set additional items first
        this.setAdditionalItems();
        // Set won items
        this.setWonItems();

        int currentPage = this.pagination == null ? 1 : this.pagination.currentPage;
        this.pagination = new Pagination(getPaginatedItems(player), paginationSlots.size());
        // Set the page
        this.pagination.currentPage = currentPage;
        // Check if that page exists
        if (this.pagination.getItems(currentPage) == null || this.pagination.getItems(currentPage).isEmpty()) {
            this.pagination.currentPage = 1;
        }

        List<? extends Pagination.PaginatedItem> items = this.pagination.getItems(this.pagination.currentPage);

        // If the items is null or empty, do nothing
        if (items == null || items.isEmpty()) {
            return;
        }

        for (int i = 0; i < items.size(); i++) {
            Integer slot = paginationSlots.get(i);
            Pagination.PaginatedItem item = items.get(i);

            // Set the item on the gui
            this.setItem(slot, item.stack(), item.onClick());
        }
    }

    private void setAdditionalItems() {
        FileConfiguration config = NextCoinflip.GAME_SELECTOR_MENU_CONFIG.getConfig();
        // Get list of items
        for (String key : config.getConfigurationSection("items").getKeys(false)) {
            String type = config.getString("items." + key + ".type", "DUMMY");
            List<Integer> slots = config.getIntegerList("items." + key + ".slots");
            ItemStack stack = ItemBuilder.fromConfig(config, "items." + key).build();

            // Set the items
            this.setItems(slots, stack, event -> {
                if (!(event.getWhoClicked() instanceof Player player)) return;
                // Check for next page and previous page
                if (type.equalsIgnoreCase("next_page")) {
                    if (!this.pagination.hasNextPage()) return;
                    this.pagination.currentPage++;
                    this.setAllItems();
                    // Click sound
                    SoundUtil.click(player);
                }
                if (type.equalsIgnoreCase("previous_page")) {
                    if (!this.pagination.hasPreviousPage()) return;
                    this.pagination.currentPage--;
                    this.setAllItems();
                    // Click sound
                    SoundUtil.click(player);
                }
                if (type.equalsIgnoreCase("stats")) {
                    SoundUtil.click(player);
                    // Open the stats gui
                    new CoinflipStatisticGUI(player).open(player);
                }
            });
        }
    }

    private void setWonItems() {
        FileConfiguration config = NextCoinflip.GAME_SELECTOR_MENU_CONFIG.getConfig();
        List<Integer> slots = config.getIntegerList("won-item.slots");

        // Check if player has empty or filled won items
        User user = UserRepository.getUser(player);
        List<WonItem> wonItems = user.getWonItems();

        if (wonItems.isEmpty()) {
            // Set with empty items
            ItemStack stack = ItemBuilder.fromConfig(config, "won-item.empty").build();
            this.setItems(slots, stack);
        } else {
            // Set with filler items
            ItemStack stack = ItemBuilder.fromConfig(config, "won-item.filled")
                    .placeholder(new Placeholder()
                            .add("{total}", Common.digits(wonItems.size())))
                    .build();
            this.setItems(slots, stack, event -> {
                new WonItemGUI(player).open(player);
            });
        }
    }

    private List<Pagination.PaginatedItem> getPaginatedItems(Player player) {
        FileConfiguration config = NextCoinflip.GAME_SELECTOR_MENU_CONFIG.getConfig();
        List<Pagination.PaginatedItem> items = new ArrayList<>();
        for (CoinflipGame game : CoinflipRepository.getGames()) {
            if (!game.isAvailable()) continue;
            // Check for the correct footer
            String footer = game.getCreator().equals(player.getUniqueId()) ?
                    config.getString("footer-placeholder.self") :
                    config.getString("footer-placeholder.others");
            // Add another placeholders to the item
            ItemStack stack = new ItemBuilder(game.getItem())
                    .placeholder(new Placeholder()
                            .add("{footer}", footer))
                    .build();
            // Add the coinflip game into the menu
            items.add(new Pagination.PaginatedItem(stack, event -> {
                SoundUtil.click(player);
                // If it's different from creator, start it
                if (game.getCreator().equals(player.getUniqueId())) {
                    if (game.isStarting()) {
                        NextCoinflip.DEFAULT_CONFIG.sendMessage(player, "messages.coinflip-already-started");
                        SoundUtil.villagerNo(player);
                        return;
                    }
                    // Cancel the game
                    game.cancel(false);
                    // Remove the game from the repository
                    CoinflipRepository.removeGame(game);
                    // Refresh the page
                    this.setAllItems();
                    // Sends message
                    NextCoinflip.DEFAULT_CONFIG.sendMessage(player, "messages.coinflip-cancel");
                    return;
                }
                // Execute the on click function
                game.onClick(player);
            }));
        }
        return items;
    }

}
