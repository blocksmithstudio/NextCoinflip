package com.muhammaddaffa.nextcoinflip.commands;

import com.muhammaddaffa.mdlib.commandapi.CommandAPICommand;
import com.muhammaddaffa.mdlib.commandapi.arguments.ArgumentSuggestions;
import com.muhammaddaffa.mdlib.commandapi.arguments.StringArgument;
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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class MainCommand {

    private final CommandAPICommand command;

    public MainCommand() {
        this.command = new CommandAPICommand("coinflip")
                .withAliases("cf")
                .withOptionalArguments(new StringArgument("argument"))
                .withOptionalArguments(new StringArgument("target")
                        .replaceSuggestions(ArgumentSuggestions.strings(info -> {
                            Optional<Object> optional = info.previousArgs().getOptional("argument");
                            if (info.sender() instanceof Player player && optional.isPresent() && ((String) optional.get()).equalsIgnoreCase("accept")) {
                                ItemCoinflipGame itemGame = CoinflipRepository.getGames(player)
                                        .stream()
                                        .filter(game -> game instanceof ItemCoinflipGame)
                                        .map(game -> (ItemCoinflipGame) game)
                                        .findAny()
                                        .orElse(null);
                                // If item game is not null, return the challenger name
                                if (itemGame != null) {
                                    return itemGame.getChallengersName().toArray(String[]::new);
                                }
                            }
                            return new String[0];
                        })))
                .executesPlayer((player, args) -> {
                    Optional<Object> optional = args.getOptional("argument");
                    // If the argument is non-existent, open the game selector menu
                    if (optional.isEmpty()) {
                        // It should open up the game selector menu
                        new GameSelectorGUI(player).open(player);
                        return;
                    }
                    String arg = (String) optional.get();

                    if (Common.isInt(arg)) {
                        // Open the currency selector menu
                        new CurrencySelectorGUI(Integer.parseInt(arg)).open(player);
                    } else {
                        // If the argument is reload, execute the reload sub-command
                        if (arg.equalsIgnoreCase("reload")) {
                            this.reload(player);
                            return;
                        }
                        // If the argument is hand, open the item coinflip menu
                        if (arg.equalsIgnoreCase("hand")) {
                            // Check if the item is wager able
                            ItemStack stack = player.getInventory().getItemInMainHand();
                            if (stack.getType() == Material.AIR) {
                                NextCoinflip.DEFAULT_CONFIG.sendMessage(player, "messages.invalid-item");
                                return;
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
                            Optional<Object> targetOptional = args.getOptional("target");
                            if (targetOptional.isEmpty()) {
                                Common.sendMessage(player, "&cUsage: /cf accept <player>");
                                return;
                            }
                            String targetName = (String) targetOptional.get();
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
                                return;
                            }
                            // Now, we try to find the challenger
                            ItemCoinflipGame.ChallengerDetails challenger = game.getChallenger(targetName);
                            if (challenger == null) {
                                NextCoinflip.DEFAULT_CONFIG.sendMessage(player, "messages.no-challenger", new Placeholder()
                                        .add("{player}", targetName));
                                return;
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
                                if (game.getChallenger(targetName) == null) {
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

                });
        // Register
        this.command.register(NextCoinflip.getInstance());
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
