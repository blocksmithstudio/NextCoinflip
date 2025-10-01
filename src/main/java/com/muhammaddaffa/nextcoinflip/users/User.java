package com.muhammaddaffa.nextcoinflip.users;

import com.muhammaddaffa.nextcoinflip.users.models.CoinflipStatistic;
import com.muhammaddaffa.nextcoinflip.users.models.WonItem;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class User {

    private final UUID uuid;

    private final CoinflipStatistic statistic = new CoinflipStatistic();
    private final List<WonItem> wonItems = Collections.synchronizedList(new ArrayList<>());

    public User(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUniqueId() {
        return uuid;
    }

    public CoinflipStatistic getStatistic() {
        return statistic;
    }

    public List<WonItem> getWonItems() {
        return wonItems;
    }

    public void addWonItems(Collection<WonItem> items) {
        wonItems.addAll(items);
    }

    public void addWonItem(WonItem item) {
        wonItems.add(item);
    }

    public void addWonItem(ItemStack stack) {
        wonItems.add(new WonItem(stack));
    }

    public void removeWonItem(WonItem item) {
        removeWonItem(item.getUniqueId());
    }

    public void removeWonItem(UUID uuid) {
        wonItems.removeIf(item -> item.getUniqueId().equals(uuid));
    }

}
