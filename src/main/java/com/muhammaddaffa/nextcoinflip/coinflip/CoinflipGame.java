package com.muhammaddaffa.nextcoinflip.coinflip;

import com.muhammaddaffa.mdlib.utils.Placeholder;
import com.muhammaddaffa.mdlib.utils.TimeUtils;
import com.muhammaddaffa.nextcoinflip.coinflip.repository.CoinflipRepository;
import com.muhammaddaffa.nextcoinflip.gui.CoinflipGUI;
import com.muhammaddaffa.nextcoinflip.users.User;
import com.muhammaddaffa.nextcoinflip.users.repository.UserRepository;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class CoinflipGame {

    private final UUID uuid, creator;
    private UUID challenger;

    private CoinflipGUI gameMenu;
    private boolean settled;
    private List<Settlement> settlement;
    private UUID settlementWinner;

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
    public String getCreatorName() {
        Player player = Bukkit.getPlayer(creator);
        if (player == null) {
            return "&cUnknown";
        }
        return player.getName();
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
        Player creator = Bukkit.getPlayer(this.creator);
        if (creator != null) {
            placeholder.add("{creator_uuid}", creator.getUniqueId().toString());
            placeholder.add("{creator_name}", creator.getName());
            placeholder.add("{creator_display_name}", creator.getDisplayName());
        }

        if (challenger != null) {
            Player challenger = Bukkit.getPlayer(this.challenger);
            if (challenger != null) {
                placeholder.add("{challenger_uuid}", challenger.getUniqueId().toString());
                placeholder.add("{challenger_name}", challenger.getName());
                placeholder.add("{challenger_display_name}", challenger.getDisplayName());
            }
        }

        placeholder.add("{time_elapsed}", getFormattedElapsedTimestamp());

        return placeholder;
    }

    public Placeholder getDefaultPlaceholders() {
        return getDefaultPlaceholders(new Placeholder());
    }

    public boolean isAvailable() {
        return this.settlement == null && Bukkit.getPlayer(this.creator) != null;
    }

    public UUID getOpposite(UUID uuid) {
        if (creator.equals(uuid)) {
            return challenger;
        }
        return creator;
    }

    public List<Player> getPlayers() {
        List<Player> players = new ArrayList<>();
        Player creator = Bukkit.getPlayer(this.creator);
        Player challenger = this.challenger == null ? null : Bukkit.getPlayer(this.challenger);
        if (creator != null) {
            players.add(creator);
        }
        if (challenger != null) {
            players.add(challenger);
        }
        return players;
    }

    public CoinflipGUI getGameMenu() {
        return gameMenu;
    }

    public void setGameMenu(CoinflipGUI gameMenu) {
        this.gameMenu = gameMenu;
    }

    public boolean isStarting() {
        return this.gameMenu != null || this.settlement != null;
    }

    public abstract String getId();

    public abstract ItemStack getItem();

    public abstract void cancel(boolean shutdown);

    public abstract void finish(UUID winner, UUID loser);

    public boolean canStart(UUID challenger) {
        if (challenger == null || creator.equals(challenger) || settled || this.isStarting() || !CoinflipRepository.contains(this)) {
            return false;
        }
        return Bukkit.getPlayer(this.creator) != null && Bukkit.getPlayer(challenger) != null;
    }

    protected void settle(UUID winner, Supplier<List<Settlement>> plan) {
        if (this.settled) {
            return;
        }
        if (this.settlement == null) {
            this.settlement = plan.get();
            this.settlementWinner = winner;
            if (this.gameMenu != null) {
                this.gameMenu.stop();
            }
        }
        this.retrySettlement();
    }

    public boolean hasPendingSettlement() {
        return this.settlement != null && !this.settled;
    }

    public boolean involves(UUID uuid) {
        return this.creator.equals(uuid) || uuid.equals(this.challenger);
    }

    public Map<String, Object> recoveryData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("game", this.uuid.toString());
        data.put("type", this.getId());
        data.put("creator", this.creator.toString());
        data.put("challenger", this.challenger == null ? null : this.challenger.toString());
        data.put("outcome", this.settlementWinner == null ? "refund" : this.settlementWinner.toString());
        data.put("remaining-steps", this.settlement == null ? List.of()
                : this.settlement.stream().map(Settlement::remainingSteps).toList());
        data.put("deliveries", this.settlement == null ? List.of()
                : this.settlement.stream().map(Settlement::recoveryData).toList());
        return data;
    }

    public void retrySettlement() {
        if (!this.hasPendingSettlement()) {
            return;
        }
        RuntimeException failure = null;
        for (Settlement delivery : this.settlement) {
            try {
                delivery.run();
            } catch (RuntimeException ex) {
                if (failure == null) {
                    failure = new IllegalStateException("Settlement is incomplete");
                }
                failure.addSuppressed(ex);
            }
        }
        if (failure != null) {
            throw failure;
        }
        this.settled = true;
        CoinflipRepository.removeGame(this);
    }

    protected Settlement updateUser(UUID uuid, Consumer<User> update) {
        User[] user = new User[1];
        return new Settlement(uuid, "user",
                () -> user[0] = UserRepository.getUser(uuid),
                () -> update.accept(user[0]),
                () -> user[0].save(true));
    }

    protected static final class Settlement {
        private final Deque<Runnable> steps;
        private final UUID recipient;
        private final String kind;

        public Settlement(UUID recipient, String kind, Runnable... steps) {
            this.steps = new ArrayDeque<>(Arrays.asList(steps));
            this.recipient = recipient;
            this.kind = kind;
        }

        public void run() {
            while (!this.steps.isEmpty()) {
                this.steps.getFirst().run();
                this.steps.removeFirst();
            }
        }

        public int remainingSteps() {
            return this.steps.size();
        }

        public Map<String, Object> recoveryData() {
            return Map.of("recipient", this.recipient.toString(), "kind", this.kind,
                    "remaining-steps", this.steps.size());
        }
    }

    public boolean start() {
        if (!canStart(this.challenger)) {
            return false;
        }
        CoinflipGUI menu = new CoinflipGUI(this, winner -> {
            this.finish(winner, this.getOpposite(winner));
        });
        this.getPlayers().forEach(menu::open);
        return true;
    }

    public abstract void onClick(Player clicker);

}
