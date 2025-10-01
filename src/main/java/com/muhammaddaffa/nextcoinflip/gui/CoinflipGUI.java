package com.muhammaddaffa.nextcoinflip.gui;

import com.muhammaddaffa.mdlib.fastinv.FastInv;
import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.Executor;
import com.muhammaddaffa.mdlib.utils.ItemBuilder;
import com.muhammaddaffa.mdlib.utils.Placeholder;
import com.muhammaddaffa.nextcoinflip.NextCoinflip;
import com.muhammaddaffa.nextcoinflip.coinflip.CoinflipGame;
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
        private int animationSlot = 13;

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
            if (step >= totalSteps) {
                finish();
                return;
            }

            // Increment the tick counter
            tickCounter++;

            if (tickCounter >= delay) {
                // Check if it's a creator turn
                boolean isCreator = step % 2 == 0;

                // Update the animation on the menu
                if (isCreator) {
                    menu.fill(new ItemBuilder(Material.BLUE_STAINED_GLASS_PANE).name(" "));
                    menu.setItem(animationSlot, creatorHead);
                } else {
                    menu.fill(new ItemBuilder(Material.RED_STAINED_GLASS_PANE).name(" "));
                    menu.setItem(animationSlot, challengerHead);
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

            // Fill it with lime glass pane
            menu.fill(new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).name(" "));
            // Display the winner's head
            menu.setItem(animationSlot, winner.equals(game.getCreator()) ? creatorHead : challengerHead);

            // Notify via consumer
            menu.getOnWin().accept(winner);

            // Sound for the winner
            menu.playSound(Bukkit.getPlayer(winner), Sound.UI_TOAST_CHALLENGE_COMPLETE);
            menu.playSound(Bukkit.getPlayer(game.getOpposite(winner)), Sound.ENTITY_VILLAGER_NO);

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
            return new ItemBuilder(Material.PLAYER_HEAD)
                    .skull(player)
                    .name("{player_displayname}")
                    .placeholder(new Placeholder()
                            .add("{player_name}", player.getName())
                            .add("{player_displayname}", player.getDisplayName()))
                    .build();
        }

    }

}
