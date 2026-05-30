package com.muhammaddaffa.nextcoinflip.commands;

import com.muhammaddaffa.mdlib.commands.args.ArgSuggester;
import com.muhammaddaffa.mdlib.commands.args.Suggester;
import com.muhammaddaffa.mdlib.commands.args.builtin.IntArg;
import com.muhammaddaffa.mdlib.commands.args.builtin.StringArg;
import com.muhammaddaffa.mdlib.commands.commands.RoutedCommand;
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
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class CoinflipCommand extends RoutedCommand {

    public CoinflipCommand() {
        super("coinflip", "", "/coinflip <hand|accept> | /coinflip <amount>", null);

        // Set the aliases
        alias("cf", "nextcoinflip");

        // coinflip
        root()
                .argOptional("amount", new IntArg())
                .exec((sender, ctx) -> {
                    handleRoot(sender, ctx.get("amount", Integer.class));
                });

        // coinflip reload
        sub("reload")
                .perm("nextcoinflip.admin")
                .exec((sender, ctx) -> {
                    handleReload(sender);
                });

        // coinflip hand
        sub("hand")
                .alias("h")
                .exec((sender, ctx) -> {
                    handleHand(sender);
                });

        // coinflip accept <player>
        sub("accept")
                .arg("name", new StringArg(),
                        ArgSuggester.ofDynamic((sender, prefix) -> {
                            if (sender instanceof Player player) {
                                ItemCoinflipGame itemGame = CoinflipRepository.getGames(player)
                                        .stream()
                                        .filter(game -> game instanceof ItemCoinflipGame)
                                        .map(game -> (ItemCoinflipGame) game)
                                        .findAny()
                                        .orElse(null);
                                // If item game is not null, return the challenger name
                                if (itemGame != null) {
                                    return StringUtil.copyPartialMatches(prefix == null ? "" : prefix, itemGame.getChallengersName(), new ArrayList<>());
                                }
                            }
                            return List.of();
                        }))
                .exec((sender, ctx) -> {
                    handleAccept(sender, ctx.get("name", String.class));
                });
    }

    private void handleRoot(CommandSender sender, Integer amount) {
        if (sender instanceof Player player) {
            if (amount == null) {
                // Open the coinflip menu gui
                new GameSelectorGUI(player).open(player);
            } else {
                // Proceed to create a new coinflip game
                new CurrencySelectorGUI(amount).open(player);
            }
        }
    }

    private void handleReload(CommandSender sender) {
        // Reload the config and everything
        Config.reload();
        // Reload all data
        CurrencyRepository.load();
        // Sends message
        NextCoinflip.DEFAULT_CONFIG.sendMessage(sender, "messages.reload");
    }

    private void handleHand(CommandSender sender) {
        if (sender instanceof Player player) {
            // Check if the item is wager able
            ItemStack stack = player.getInventory().getItemInMainHand();
            if (stack.getType() == Material.AIR) {
                NextCoinflip.DEFAULT_CONFIG.sendMessage(player, "messages.invalid-item");
                return;
            }
            // We check if there are more than 3 item coinflip game at a time
            if (CoinflipRepository.getItemCoinflipGameAmount() >= 3) {
                NextCoinflip.DEFAULT_CONFIG.sendMessage(player, "messages.cannot-create-item-coinflip");
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
                        if (stack.getType() == Material.AIR || stack.getAmount() <= 0) {
                            NextCoinflip.DEFAULT_CONFIG.sendMessage(player, "messages.invalid-item");
                            player.closeInventory();
                            SoundUtil.villagerNo(player);
                            return;
                        }
                        if (CoinflipRepository.getItemCoinflipGameAmount() >= 3) {
                            NextCoinflip.DEFAULT_CONFIG.sendMessage(player, "messages.cannot-create-item-coinflip");
                            player.closeInventory();
                            SoundUtil.villagerNo(player);
                            return;
                        }
                        // Register an item coinflip
                        ItemCoinflipGame game = new ItemCoinflipGame(player.getUniqueId(), stack);
                        // Remove the item from player's inventory
                        stack.setAmount(0);
                        // Register it on the repository
                        CoinflipRepository.register(game);

                        // Send hoverable item message
                        String message = NextCoinflip.DEFAULT_CONFIG.getString("messages.coinflip-create")
                                .replace("{player}", player.getName())
                                .replace("{display_name}", player.getDisplayName())
                                .replace("{currency}", "");

                        HoverableMessage.broadcastHoverableItemMessage(clone, PlaceholderAPI.setPlaceholders(player, message));

                        // Close the inventory
                        player.closeInventory();
                        SoundUtil.click(player);
                        SoundUtil.pling(player);
                    })
                    .open(player);
        }
    }

    private void handleAccept(CommandSender sender, String target) {
        if (!(sender instanceof Player player)) {
            return;
        }

        if (target == null || target.isEmpty()) {
            Common.sendMessage(player, "&cUsage: /cf accept <player>");
            return;
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
            return;
        }

        // Now, we try to find the challenger
        ItemCoinflipGame.ChallengerDetails challenger = game.getChallenger(target);
        if (challenger == null) {
            NextCoinflip.DEFAULT_CONFIG.sendMessage(player, "messages.no-challenger", new Placeholder()
                    .add("{player}", target));
            return;
        }

        Player other = Bukkit.getPlayer(challenger.uuid());
        if (other == null || !game.canStart(challenger.uuid())) {
            game.cancelChallenge(challenger.uuid());
            NextCoinflip.DEFAULT_CONFIG.sendMessage(player, "messages.not-valid");
            return;
        }
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
            if (game.getChallenger(target) == null || !game.canStart(challenger.uuid())) {
                // Sends message
                NextCoinflip.DEFAULT_CONFIG.sendMessage(player, "messages.not-valid");
                // Close the inventory
                player.closeInventory();
                return;
            }
            // On accept, set the opponent, and start coinflip
            game.setChallenger(challenger.uuid());
            game.getOffers()[1] = challenger.itemStack();
            if (!game.start()) {
                game.setChallenger(null);
                game.getOffers()[1] = null;
                NextCoinflip.DEFAULT_CONFIG.sendMessage(player, "messages.not-valid");
                player.closeInventory();
                return;
            }
            // Cancel all offer from other player
            game.cancelAllExcept(challenger);
        });

        // Open the menu for the player
        menu.open(player);
    }

}
