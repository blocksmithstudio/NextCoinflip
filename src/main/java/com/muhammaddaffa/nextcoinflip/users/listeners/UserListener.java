package com.muhammaddaffa.nextcoinflip.users.listeners;

import com.muhammaddaffa.nextcoinflip.users.repository.UserRepository;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class UserListener implements Listener {

    @EventHandler
    private void onJoin(PlayerJoinEvent event) {
        UserRepository.load(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onQuit(PlayerQuitEvent event) {
        UserRepository.unload(event.getPlayer());
    }

}
