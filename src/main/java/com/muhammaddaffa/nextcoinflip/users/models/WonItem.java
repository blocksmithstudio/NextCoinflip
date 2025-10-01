package com.muhammaddaffa.nextcoinflip.users.models;

import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

public class WonItem {

    private final UUID uuid;
    private final ItemStack itemStack;
    private final long timestamp;

    public WonItem(ItemStack itemStack) {
        this.uuid = UUID.randomUUID();
        this.itemStack = itemStack;
        this.timestamp = System.currentTimeMillis();
    }

    public WonItem(UUID uuid, ItemStack itemStack, long timestamp) {
        this.uuid = uuid;
        this.itemStack = itemStack;
        this.timestamp = timestamp;
    }

    public UUID getUniqueId() {
        return uuid;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getFormattedDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy hh:mm a")
                .withLocale(Locale.ENGLISH)
                .withZone(ZoneId.systemDefault());
        return formatter.format(Instant.ofEpochMilli(timestamp));
    }

}
