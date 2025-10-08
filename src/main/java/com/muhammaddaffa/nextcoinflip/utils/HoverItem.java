package com.muhammaddaffa.nextcoinflip.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Content;

public final class HoverItem extends Content {
    private final String id;
    private final int count;
    private final JsonElement components;

    public HoverItem (JsonObject itemJson)
    {
        JsonElement itemId = itemJson.get("id");
        JsonElement itemCount = itemJson.get("count");

        this.id = itemId != null ? itemId.getAsString() : null;
        this.count = itemCount != null ? itemCount.getAsInt() : -1;
        this.components = itemJson.get("components");
    }

    @Override
    public HoverEvent.Action requiredAction() {
        return HoverEvent.Action.SHOW_ITEM;
    }
}
