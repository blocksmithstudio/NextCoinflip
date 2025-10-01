package com.muhammaddaffa.nextcoinflip.coinflip;

import com.muhammaddaffa.mdlib.utils.Placeholder;
import com.muhammaddaffa.mdlib.utils.TimeUtils;
import com.muhammaddaffa.nextcoinflip.coinflip.repository.CoinflipRepository;
import com.muhammaddaffa.nextcoinflip.gui.CoinflipGUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public abstract class CoinflipGame {

    private final UUID uuid, creator;
    private UUID challenger;

    private CoinflipGUI gameMenu;

    private final long timestamp;

    protected CoinflipGame(UUID creator) {
        this.uuid = UUID.randomUUID();
        this.creator = creator;
        this.timestamp = System.currentTimeMillis();
    }

    public UUID getUniqueId() {
        return uuid;
    }

    public UUID getCreator() {
        return creator;
    }

    @Nullable
    public String getCreatorDisplayName() {
        Player player = Bukkit.getPlayer(creator);
        if (player == null) {
            return "&cUnknown";
        }
        return player.getDisplayName();
    }

    public UUID getChallenger() {
        return challenger;
    }

    public void setChallenger(UUID challenger) {
        this.challenger = challenger;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getElapsedTimestamp() {
        return System.currentTimeMillis() - timestamp;
    }

    public String getFormattedElapsedTimestamp() {
        return TimeUtils.format(TimeUnit.MILLISECONDS.toSeconds(getElapsedTimestamp()));
    }

    public Placeholder getDefaultPlaceholders(Placeholder placeholder) {
        // Get the creator placeholder
        Player creator = Bukkit.getPlayer(this.creator);
        if (creator != null) {
            placeholder.add("{creator_uuid}", creator.getUniqueId().toString());
            placeholder.add("{creator_name}", creator.getName());
            placeholder.add("{creator_display_name}", creator.getDisplayName());
        }

        // Get the challenger placeholders
        if (challenger != null) {
            Player challenger = Bukkit.getPlayer(this.challenger);
            if (challenger != null) {
                placeholder.add("{challenger_uuid}", challenger.getUniqueId().toString());
                placeholder.add("{challenger_name}", challenger.getName());
                placeholder.add("{challenger_display_name}", challenger.getDisplayName());
            }
        }

        // Time elapsed placeholder
        placeholder.add("{time_elapsed}", getFormattedElapsedTimestamp());

        return placeholder;
    }

    public Placeholder getDefaultPlaceholders() {
        return getDefaultPlaceholders(new Placeholder());
    }

    public boolean isAvailable() {
        return Bukkit.getPlayer(creator) != null;
    }

    public UUID getOpposite(UUID uuid) {
        if (creator.equals(uuid)) {
            return challenger;
        }
        return creator;
    }

    public Collection<Player> getPlayers() {
        return List.of(Bukkit.getPlayer(creator), Bukkit.getPlayer(challenger));
    }

    public CoinflipGUI getGameMenu() {
        return gameMenu;
    }

    public void setGameMenu(CoinflipGUI gameMenu) {
        this.gameMenu = gameMenu;
    }

    public boolean isStarting() {
        return gameMenu != null;
    }

    public abstract String getId();

    public abstract ItemStack getItem();

    public abstract void cancel();

    public abstract void finish(UUID winner, UUID loser);

    public void start() {
        // We should directly open the game menu and immediately plays
        CoinflipGUI menu = new CoinflipGUI(this, winner -> {
            this.finish(winner, this.getOpposite(winner));
            // Remove the coinflip game from the menu
            CoinflipRepository.removeGame(this);
        });
        // Open the menu for both player
        this.getPlayers().forEach(menu::open);
    }

    public abstract void onClick(Player clicker);

}
