package com.muhammaddaffa.nextcoinflip.utils;

import com.muhammaddaffa.mdlib.utils.ItemStackSerializer;
import org.bukkit.inventory.ItemStack;

import java.io.*;

public class Serializer {

    // Serialize a single ItemStack
    public static String serialize(ItemStack item) {
        return ItemStackSerializer.writeItem(item);
    }

    // Deserialize a single ItemStack
    public static ItemStack deserialize(String encodedItem) {
        return ItemStackSerializer.readItem(encodedItem);
    }

}
