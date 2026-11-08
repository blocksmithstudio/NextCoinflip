package com.muhammaddaffa.nextcoinflip.coinflip.dialog;

import com.destroystokyo.paper.profile.PlayerProfile;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.set.RegistrySet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.object.ObjectContents;
import net.kyori.adventure.text.object.PlayerHeadObjectContents;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.packs.ResourcePack;
import org.bukkit.plugin.Plugin;
import org.bukkit.profile.PlayerTextures;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public final class CoinflipDialog implements Listener {

    public static final int REVEAL_TICK = 62;
    private static final long RESULT_DURATION_TICKS = 60L;

    private final UUID packId;
    private final Plugin plugin;
    private final DialogPackets dialogPackets;
    private final Set<UUID> readyPlayers = new HashSet<>();
    private final Map<UUID, ResultClose> resultCloses = new HashMap<>();

    public CoinflipDialog(UUID packId, Plugin plugin) {
        this.packId = packId;
        this.plugin = plugin;
        this.dialogPackets = DialogPackets.create(plugin.getLogger());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        ResourcePack serverPack = Bukkit.getServerResourcePack();
        Player player = event.getPlayer();
        if (serverPack != null && this.packId.equals(serverPack.getId())
                && player.getResourcePackStatus() == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED) {
            this.readyPlayers.add(player.getUniqueId());
        }
    }

    @EventHandler
    public void onPackStatus(PlayerResourcePackStatusEvent event) {
        if (this.packId.equals(event.getID())) {
            if (event.getStatus() == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED) {
                this.readyPlayers.add(event.getPlayer().getUniqueId());
            } else {
                this.readyPlayers.remove(event.getPlayer().getUniqueId());
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        this.readyPlayers.remove(playerId);
        this.cancelResultClose(playerId);
    }

    public boolean show(Player viewer, Player creator, Player challenger, UUID winner) {
        UUID viewerId = viewer.getUniqueId();
        if (!this.readyPlayers.contains(viewerId)) {
            return false;
        }

        DialogPackets.Watch watch = null;
        try {
            boolean revealed = winner != null;
            boolean heads = revealed && winner.equals(creator.getUniqueId());
            int phase = revealed ? (heads ? 236 : 237) : 235;
            int tick = Math.floorMod(viewer.getWorld().getGameTime(), 24000);
            PlayerProfile creatorProfile = creator.getPlayerProfile();
            PlayerProfile challengerProfile = challenger.getPlayerProfile();
            PlayerHeadObjectContents creatorSkin = this.skin(creator, creatorProfile);
            PlayerHeadObjectContents challengerSkin = this.skin(challenger, challengerProfile);
            Component scene = Component.empty()
                    .append(this.markedGlyph(creatorSkin, this.portraitMarker(creatorProfile, 0, revealed, heads, tick)))
                    .append(this.markedGlyph(creatorSkin, phase << 16 | tick))
                    .append(this.markedGlyph(challengerSkin, this.portraitMarker(challengerProfile, 1, revealed, revealed && !heads, tick)));
            DialogBase base = DialogBase.builder(scene)
                    .pause(false)
                    .canCloseWithEscape(revealed)
                    .build();
            DialogType type = DialogType.dialogList(RegistrySet.keySet(RegistryKey.DIALOG)).build();
            Dialog dialog = Dialog.create(factory -> factory.empty().base(base).type(type));
            if (!revealed) {
                viewer.closeInventory();
            } else if (this.dialogPackets != null) {
                watch = this.dialogPackets.watch(viewer, dialog);
            }
            viewer.showDialog(dialog);
            this.cancelResultClose(viewerId);
            if (watch != null) {
                this.scheduleResultClose(viewerId, watch);
            }
            return true;
        } catch (RuntimeException | LinkageError error) {
            if (watch != null) {
                watch.finish(false);
            }
            this.plugin.getLogger().log(Level.WARNING, "Could not show the coinflip dialog to " + viewer.getName(), error);
            return false;
        }
    }

    public void shutdown() {
        this.resultCloses.values().forEach(ResultClose::cancel);
        this.resultCloses.clear();
    }

    private void scheduleResultClose(UUID viewerId, DialogPackets.Watch watch) {
        BukkitTask task = Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            this.resultCloses.remove(viewerId);
            watch.finish(true);
        }, RESULT_DURATION_TICKS);
        this.resultCloses.put(viewerId, new ResultClose(task, watch));
    }

    private void cancelResultClose(UUID viewerId) {
        ResultClose close = this.resultCloses.remove(viewerId);
        if (close != null) {
            close.cancel();
        }
    }

    private record ResultClose(BukkitTask task, DialogPackets.Watch watch) {

        private void cancel() {
            this.task.cancel();
            this.watch.finish(false);
        }
    }

    private PlayerHeadObjectContents skin(Player player, PlayerProfile profile) {
        PlayerHeadObjectContents.Builder head = ObjectContents.playerHead()
                .id(player.getUniqueId())
                .name(player.getName())
                .hat(false);
        profile.getProperties().stream()
                .filter(property -> property.getName().equals("textures"))
                .forEach(property -> head.profileProperty(PlayerHeadObjectContents.property(
                        property.getName(), property.getValue(), property.getSignature())));
        return head.build();
    }

    private int portraitMarker(PlayerProfile profile, int side, boolean revealed, boolean winner, int tick) {
        boolean slim = profile.getTextures().getSkinModel() == PlayerTextures.SkinModel.SLIM;
        int flags = side | (slim ? 2 : 0) | (revealed ? 4 : 0) | (winner ? 8 : 0);
        return ((208 + flags) << 16) | tick;
    }

    private Component markedGlyph(PlayerHeadObjectContents skin, int marker) {
        return Component.object(skin).color(TextColor.color(marker)).shadowColor(ShadowColor.none());
    }
}
