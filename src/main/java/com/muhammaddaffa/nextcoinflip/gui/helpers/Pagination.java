package com.muhammaddaffa.nextcoinflip.gui.helpers;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class Pagination {

    private final Map<Integer, List<PaginatedItem>> pageItems = new HashMap<>();

    public int totalPage;
    public int currentPage = 1;

    public Pagination(List<? extends PaginatedItem> items, int maxItemsPerPage) {
        int totalItems = items.size();

        this.totalPage = (int) Math.ceil((double) totalItems / maxItemsPerPage);

        for (int i = 0; i < totalPage; i++) {
            int start = i * maxItemsPerPage;
            int end = Math.min(start + maxItemsPerPage, totalItems);
            List<? extends PaginatedItem> pageContent = items.subList(start, end);
            pageItems.put(i + 1, new ArrayList<>(pageContent));
        }
    }

    public boolean hasNextPage() {
        return this.pageItems.get(this.currentPage + 1) != null;
    }

    public boolean hasPreviousPage() {
        return this.pageItems.get(this.currentPage - 1) != null;
    }

    public List<? extends PaginatedItem> getItems(int page) {
        return this.pageItems.get(page);
    }

    public record PaginatedItem(
            ItemStack stack,
            Consumer<InventoryClickEvent> onClick
    ) {

    }

}
