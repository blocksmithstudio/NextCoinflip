package com.muhammaddaffa.nextcoinflip.gui;

import com.muhammaddaffa.mdlib.fastinv.FastInv;
import com.muhammaddaffa.mdlib.utils.*;
import com.muhammaddaffa.nextcoinflip.NextCoinflip;
import com.muhammaddaffa.nextcoinflip.coinflip.CoinflipGame;
import com.muhammaddaffa.nextcoinflip.coinflip.impl.CurrencyCoinflipGame;
import com.muhammaddaffa.nextcoinflip.coinflip.impl.ItemCoinflipGame;
import com.muhammaddaffa.nextcoinflip.users.User;
import com.muhammaddaffa.nextcoinflip.users.models.CoinflipStatistic;
import com.muhammaddaffa.nextcoinflip.users.repository.UserRepository;
import com.muhammaddaffa.nextcoinflip.utils.HoverableMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class CoinflipGUI extends FastInv {

    private final CoinflipGame game;
    private final Consumer<UUID> onWin;

    private boolean canClose = false;

    private CoinflipTask task;

    public CoinflipGUI(CoinflipGame game, Consumer<UUID> onWin) {
        super(27, "Coinflip Game");
        this.game = game;
        this.onWin = onWin;

        this.addCloseHandler(event -> {
            // Open back this inventory
            if (!canClose) {
                Executor.sync(() -> this.open((Player) event.getPlayer()));
            }
        });

        fill(new ItemBuilder(Material.BLUE_STAINED_GLASS_PANE).name(" "));
        this.startFlipAnimation();
    }

    private void startFlipAnimation() {
        // Set the coinflip game to starting
        game.setGameMenu(this);
        // Set the coinflip task
        task = new CoinflipTask(this.game, this);
        task.runTaskTimer(NextCoinflip.getInstance(), 0L, 1L);
    }

    private void playSound(Player player, Sound sound) {
        Common.playSound(player, sound);
    }

    private void playSound(Collection<Player> players, Sound sound) {
        players.forEach(player -> playSound(player, sound));
    }

    private void fill(ItemBuilder builder) {
        this.fill(builder.build());
    }

    private void fill(ItemStack stack) {
        for (int i = 0; i < this.getInventory().getSize(); i++) {
            this.setItem(i, stack);
        }
    }

    public void setCanClose(boolean canClose) {
        this.canClose = canClose;
    }

    public Consumer<UUID> getOnWin() {
        return onWin;
    }

    public CoinflipTask getTask() {
        return task;
    }

    public static class CoinflipTask extends BukkitRunnable {

        private int step = 0;
        private final int totalSteps = 20;
        private int tickCounter = 0;
        private double delay = 5.0;// Starting delay, increases over time
        private int collateral = 0;

        private final int creatorSlot = 10;
        private final int creatorItemSlot = 11;
        private final int[] creatorFillSlots = {0, 1, 2, 9, 11, 18, 19, 20};

        private final int challengerSlot = 16;
        private final int challengerItemSlot = 15;
        private final int[] challengerFillSlots = {6, 7, 8, 15, 17, 24, 25, 26};

        private final int[] animationSlots = {3, 4, 5, 12, 13, 14, 21, 22, 23};

        private final CoinflipGame game;
        private final Player creator, challenger;
        private final ItemStack creatorHead, challengerHead;
        private final CoinflipGUI menu;

        public CoinflipTask(CoinflipGame game, CoinflipGUI menu) {
            this.game = game;
            this.creator = Bukkit.getPlayer(game.getCreator());
            this.challenger = Bukkit.getPlayer(game.getChallenger());
            this.creatorHead = this.getHeadItem(creator);
            this.challengerHead = this.getHeadItem(challenger);
            this.menu = menu;
        }

        @Override
        public void run() {
            // Increment the tick counter
            tickCounter++;

            // Set the menu static slots
            menu.setItem(creatorSlot, creatorHead);
            menu.setItems(creatorFillSlots, new ItemBuilder(Material.RED_STAINED_GLASS_PANE).name(" ").build());

            menu.setItem(challengerSlot, challengerHead);
            menu.setItems(challengerFillSlots, new ItemBuilder(Material.BLUE_STAINED_GLASS_PANE).name(" ").build());

            if (game instanceof ItemCoinflipGame icg) {
                menu.setItem(creatorItemSlot, icg.getOffers()[0].clone());
                menu.setItem(challengerItemSlot, icg.getOffers()[1].clone());
            }

            if (tickCounter >= delay) {
                if (step >= totalSteps) {
                    finish();
                    return;
                }

                // Check if it's a creator turn
                boolean isCreator = step % 2 == 0;

                // Update the animation on the menu
                if (isCreator) {
                    menu.setItems(animationSlots, new ItemBuilder(Material.RED_STAINED_GLASS_PANE).name(" ").build());
                } else {
                    menu.setItems(animationSlots, new ItemBuilder(Material.BLUE_STAINED_GLASS_PANE).name(" ").build());
                }

                // Play sound for both player
                menu.playSound(List.of(creator, challenger), Sound.BLOCK_NOTE_BLOCK_PLING);

                // Reset the tick counter and adjust delay
                tickCounter = 0;
                // Increment collateral
                collateral++;
                // If collateral hit 5, we should increase the delay
                if (collateral >= 5) {
                    collateral = 0;
                    delay += 1.79;
                }

                // Increment step
                step++;
            }
        }

        public void finish() {
            // Determine winner
            UUID winner = ThreadLocalRandom.current().nextBoolean() ? game.getCreator() : game.getChallenger();
            UUID loser = game.getOpposite(winner);

            Player winnerPlayer = Bukkit.getPlayer(winner);
            Player loserPlayer = Bukkit.getPlayer(loser);

            // Fill it with lime glass pane
            if (winner.equals(game.getCreator())) {
                menu.setItems(animationSlots, new ItemBuilder(Material.RED_STAINED_GLASS_PANE).name(" ").build());
            } else {
                menu.setItems(animationSlots, new ItemBuilder(Material.BLUE_STAINED_GLASS_PANE).name(" ").build());
            }

            // Notify via consumer
            menu.getOnWin().accept(winner);

            // Sound for the winner
            menu.playSound(winnerPlayer, Sound.UI_TOAST_CHALLENGE_COMPLETE);
            menu.playSound(Bukkit.getPlayer(game.getOpposite(winner)), Sound.ENTITY_VILLAGER_NO);

            Placeholder placeholder = new Placeholder()
                    .add("{winner}", winnerPlayer.getDisplayName())
                    .add("{loser}", loserPlayer.getDisplayName());

            // Send message for both side
            if (game instanceof ItemCoinflipGame itemGame) {
                placeholder.add("{currency}", "");
                ItemStack stack = itemGame.getOffers()[game.getPlayers().indexOf(loserPlayer)];
                // Send to winner
                HoverableMessage.sendHoverableItemMessage(
                        winnerPlayer,
                        stack,
                        NextCoinflip.DEFAULT_CONFIG.getString("messages.coinflip-winner"),
                        placeholder
                );

                HoverableMessage.sendHoverableItemMessage(
                        loserPlayer,
                        stack,
                        NextCoinflip.DEFAULT_CONFIG.getString("messages.coinflip-loser"),
                        placeholder
                );

            } else if(game instanceof CurrencyCoinflipGame currencyGame) {
                // Add more placeholders
                placeholder.add("{wager}", Common.format(currencyGame.getWager()))
                        .add("{currency}", " " + currencyGame.getCurrency().identifier());
                // Send message to the winner
                NextCoinflip.DEFAULT_CONFIG.sendMessage(winnerPlayer, "messages.coinflip-winner", placeholder);
                // Send message to the loser
                NextCoinflip.DEFAULT_CONFIG.sendMessage(loserPlayer, "messages.coinflip-loser", placeholder);
            }

            // Make player can close this menu
            menu.setCanClose(true);
            // Auto close 3 seconds later
            Executor.syncLater(60L, () -> {
                // Close this menu for players
                if (creator != null && creator.isOnline()) creator.closeInventory();
                if (challenger != null && challenger.isOnline()) challenger.closeInventory();
            });

            // Stop the task
            this.cancel();
        }

        private ItemStack getHeadItem(Player player) {
            User user = UserRepository.getUser(player);
            CoinflipStatistic statistic = user.getStatistic();

            return new ItemBuilder(Material.PLAYER_HEAD)
                    .skull(player)
                    .name(NextCoinflip.DEFAULT_CONFIG.getString("coinflip-head-item.display-name"))
                    .lore(NextCoinflip.DEFAULT_CONFIG.getStringList("coinflip-head-item.lore"))
                    .placeholder(new Placeholder()
                            .add("{player_name}", player.getName())
                            .add("{player_displayname}", player.getDisplayName())
                            .add("{wins}", Common.digits(statistic.getWins()))
                            .add("{losses}", Common.digits(statistic.getLosses()))
                            .add("{winrate}", Common.digits(statistic.getWinRate()))
                            .add("{total_game}", Common.digits(statistic.getTotalGames()))
                            .add("{lose_streak}", Common.digits(statistic.getLoseStreak())))
                    .build();
        }

    }

}
