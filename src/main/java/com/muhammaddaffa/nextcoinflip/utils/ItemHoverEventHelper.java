package com.muhammaddaffa.nextcoinflip.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Content;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public final class ItemHoverEventHelper {

    private ItemHoverEventHelper() {}

    /**
     * Build a SHOW_ITEM hover event for the provided Bukkit ItemStack.
     * Prefers modern components; falls back to legacy NBT tag if unavailable.
     *
     * @param bukkitItem Bukkit ItemStack
     * @return HoverEvent with SHOW_ITEM action suitable for modern clients
     */
    public static HoverEvent createFrom(final ItemStack bukkitItem) {
        try {
            // Note: `serializeItemAsJson` is a Paper API method
            JsonObject obj = Bukkit.getUnsafe().serializeItemAsJson(bukkitItem);
            if (obj != null) {
                final String id = obj.get("id").getAsString();
                final int count = bukkitItem.getAmount();
                // Get the new Item components field
                final JsonElement components = obj.get("components");
                if (components != null) {
                    return new HoverEvent(HoverEvent.Action.SHOW_ITEM, new ComponentsShowItem(id, count, components));
                }
            }
        } catch (Exception ignored) {
            // Paper-specific API not available, or old version; fall back to legacy tag path below
        }
        // There was either no components field (<1.20.5) or there was an error, regardless attempt the legacy method
        return createFromLegacy(bukkitItem);
    }

    // Old `nbt` Tag system if we are below 1.20.5
    private static HoverEvent createFromLegacy(final ItemStack bukkitItem) {
        final String id = bukkitItem.getType().getKey().toString();
        final int count = bukkitItem.getAmount();
        String nbt = null;
        try {
            nbt = bukkitItem.getItemMeta().getAsString();
            final net.md_5.bungee.api.chat.ItemTag tag = (nbt != null && !nbt.isEmpty()) ? net.md_5.bungee.api.chat.ItemTag.ofNbt(nbt) : null;
            return new HoverEvent(HoverEvent.Action.SHOW_ITEM, new net.md_5.bungee.api.chat.hover.content.Item(id, count, tag));
        } catch (Exception ignored) {
            // Some servers may not expose ItemMeta#getAsString; continue with null nbt
            // If this is an issue for you, take a look at using Item NBT API to get the nbt string
        }
        return new HoverEvent(HoverEvent.Action.SHOW_ITEM, new net.md_5.bungee.api.chat.hover.content.Item(id, count, null));
    }

    /** Minimal show_item content carrying components for modern clients. */
    private static final class ComponentsShowItem extends Content {
        private final String id;
        private final int count;
        private final JsonElement components;

        private ComponentsShowItem(final String id, final int count, final JsonElement components) {
            this.id = id;
            this.count = count;
            this.components = components;
        }

        @Override
        public HoverEvent.Action requiredAction() {
            return HoverEvent.Action.SHOW_ITEM;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ComponentsShowItem that = (ComponentsShowItem) o;
            return count == that.count && Objects.equals(id, that.id) && Objects.equals(components, that.components);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, count, components);
        }
    }
}
