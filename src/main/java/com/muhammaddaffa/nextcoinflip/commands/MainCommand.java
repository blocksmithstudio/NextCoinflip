package com.muhammaddaffa.nextcoinflip.commands;

import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.Config;
import com.muhammaddaffa.mdlib.utils.Placeholder;
import com.muhammaddaffa.nextcoinflip.NextCoinflip;
import com.muhammaddaffa.nextcoinflip.coinflip.impl.ItemCoinflipGame;
import com.muhammaddaffa.nextcoinflip.coinflip.repository.CoinflipRepository;
import com.muhammaddaffa.nextcoinflip.currencies.repository.CurrencyRepository;
import com.muhammaddaffa.nextcoinflip.gui.ConfirmationItemCoinflipChallengerGUI;
import com.muhammaddaffa.nextcoinflip.gui.ConfirmationItemCoinflipCreationGUI;
import com.muhammaddaffa.nextcoinflip.gui.CurrencySelectorGUI;
import com.muhammaddaffa.nextcoinflip.gui.GameSelectorGUI;
import com.muhammaddaffa.nextcoinflip.utils.HoverableMessage;
import com.muhammaddaffa.nextcoinflip.utils.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MainCommand implements TabExecutor {

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String @NotNull [] args) {

        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], List.of("accept", "hand", "<amount>"), new ArrayList<>());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("accept") && sender instanceof Player player) {
            ItemCoinflipGame itemGame = CoinflipRepository.getGames(player)
                    .stream()
                    .filter(game -> game instanceof ItemCoinflipGame)
                    .map(game -> (ItemCoinflipGame) game)
                    .findAny()
                    .orElse(null);
            // If item game is not null, return the challenger name
            if (itemGame != null) {
                return StringUtil.copyPartialMatches(args[1], itemGame.getChallengersName(), new ArrayList<>());
            }
        }
        return List.of();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String @NotNull [] args) {

        if (args.length == 0 && sender instanceof Player player) {
            // It should open up the game selector menu
            new GameSelectorGUI(player).open(player);
            return true;
        }

        String arg = args[0];

        if (arg.equalsIgnoreCase("reload") && sender.hasPermission("nextcoinflip.admin")) {
            this.reload(sender);
            return true;
        }

        if (sender instanceof Player player) {
            if (Common.isInt(arg)) {
                // Open the currency selector menu
                new CurrencySelectorGUI(Integer.parseInt(arg)).open(player);
            } else {
                // If the argument is hand, open the item coinflip menu
                if (arg.equalsIgnoreCase("hand")) {
                    // Check if the item is wager able
                    ItemStack stack = player.getInventory().getItemInMainHand();
                    if (stack.getType() == Material.AIR) {
                        NextCoinflip.DEFAULT_CONFIG.sendMessage(player, "messages.invalid-item");
                        return true;
                    }
                    // Otherwise, we should open the confirmation menu
                    ItemStack clone = stack.clone();
                    new ConfirmationItemCoinflipCreationGUI(stack)
                            .onCancel(() -> {
                                // Close the inventory
                                player.closeInventory();
                                // Click sound
                                SoundUtil.click(player);
                            })
                            .onAccept(() -> {
                                // Register an item coinflip
                                ItemCoinflipGame game = new ItemCoinflipGame(player.getUniqueId(), stack);
                                // Remove the item from player's inventory
                                stack.setAmount(0);
                                // Register it on the repository
                                CoinflipRepository.register(game);

                                // Send hoverable item message
                                HoverableMessage.broadcastHoverableItemMessage(clone, NextCoinflip.DEFAULT_CONFIG.getString("messages.coinflip-create")
                                        .replace("{display_name}", player.getDisplayName())
                                        .replace("{currency}", ""));

                                // Close the inventory
                                player.closeInventory();
                                SoundUtil.click(player);
                                SoundUtil.pling(player);
                            })
                            .open(player);
                }
                // If the argument is "accept"
                if (arg.equalsIgnoreCase("accept")) {
                    String target = args[1];
                    if (target == null || target.isEmpty()) {
                        Common.sendMessage(player, "&cUsage: /cf accept <player>");
                        return true;
                    }
                    // First we need to find an ItemCoinflipGame from player
                    ItemCoinflipGame game = CoinflipRepository.getGames(player)
                            .stream()
                            .filter(g -> g instanceof ItemCoinflipGame)
                            .map(g -> (ItemCoinflipGame) g)
                            .findAny()
                            .orElse(null);
                    // Check if it's exists or not
                    if (game == null) {
                        NextCoinflip.DEFAULT_CONFIG.sendMessage(player, "messages.no-item-game");
                        return true;
                    }
                    // Now, we try to find the challenger
                    ItemCoinflipGame.ChallengerDetails challenger = game.getChallenger(target);
                    if (challenger == null) {
                        NextCoinflip.DEFAULT_CONFIG.sendMessage(player, "messages.no-challenger", new Placeholder()
                                .add("{player}", target));
                        return true;
                    }
                    Player other = Bukkit.getPlayer(challenger.uuid());
                    ConfirmationItemCoinflipChallengerGUI menu = new ConfirmationItemCoinflipChallengerGUI(player, game.getOffers()[0], other, challenger.itemStack());
                    // On cancel, cancel the coinflip
                    menu.onCancel(() -> {
                        // Remove player as a challenger
                        game.cancelChallenge(other);
                        // Send message
                        NextCoinflip.DEFAULT_CONFIG.sendMessage(other, "messages.decline-offer", new Placeholder()
                                .add("{player}", player.getDisplayName()));
                        // Close the inventory for the player
                        player.closeInventory();
                        // Click sound
                        Common.playSound(player, Sound.UI_BUTTON_CLICK);
                    });
                    menu.onAccept(() -> {
                        SoundUtil.click(player);
                        // Check if the offer is still valid
                        if (game.getChallenger(target) == null) {
                            // Sends message
                            NextCoinflip.DEFAULT_CONFIG.sendMessage(player, "messages.not-valid");
                            // Close the inventory
                            player.closeInventory();
                            return;
                        }
                        // On accept, set the opponent, and start coinflip
                        game.setChallenger(challenger.uuid());
                        game.getOffers()[1] = challenger.itemStack();
                        game.start();
                        // Cancel all offer from other player
                        game.cancelAllExcept(challenger);
                    });
                    // Open the menu for the player
                    menu.open(player);
                }
            }
        }
        return true;
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("nextcoinflip.admin")) {
            NextCoinflip.DEFAULT_CONFIG.sendMessage(sender, "messages.no-permission");
            return;
        }
        // Reload the config and everything
        Config.reload();
        // Reload all data
        CurrencyRepository.load();
        // Sends message
        NextCoinflip.DEFAULT_CONFIG.sendMessage(sender, "messages.reload");
    }

}
