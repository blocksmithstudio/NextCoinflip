package com.muhammaddaffa.nextcoinflip.coinflip.dialog;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.papermc.paper.dialog.Dialog;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

final class DialogPackets {

    private final Logger logger;
    private final Method playerHandle;
    private final Field gameListener;
    private final Field connection;
    private final Field channel;
    private final Method dialogHolder;
    private final Method packetHolder;
    private final Object clearDialog;

    private DialogPackets(Logger logger) throws ReflectiveOperationException {
        this.logger = logger;
        this.playerHandle = Class.forName("org.bukkit.craftbukkit.entity.CraftPlayer").getMethod("getHandle");
        this.gameListener = Class.forName("net.minecraft.server.level.ServerPlayer").getField("connection");
        this.connection = Class.forName("net.minecraft.server.network.ServerCommonPacketListenerImpl").getField("connection");
        this.channel = Class.forName("net.minecraft.network.Connection").getField("channel");
        this.dialogHolder = Class.forName("io.papermc.paper.dialog.PaperDialog").getMethod("bukkitToMinecraftHolder", Dialog.class);
        this.packetHolder = Class.forName("net.minecraft.network.protocol.common.ClientboundShowDialogPacket").getMethod("dialog");
        this.clearDialog = Class.forName("net.minecraft.network.protocol.common.ClientboundClearDialogPacket").getField("INSTANCE").get(null);
    }

    @Nullable
    static DialogPackets create(Logger logger) {
        try {
            return new DialogPackets(logger);
        } catch (ReflectiveOperationException | LinkageError error) {
            logger.log(Level.WARNING, "Coinflip dialog auto-close is unavailable on this server", error);
            return null;
        }
    }

    @Nullable
    Watch watch(Player player, Dialog dialog) {
        try {
            Object listener = this.gameListener.get(this.playerHandle.invoke(player));
            if (listener == null) {
                return null;
            }
            Channel playerChannel = (Channel) this.channel.get(this.connection.get(listener));
            if (playerChannel == null || !playerChannel.isActive()) {
                return null;
            }
            Watch watch = new Watch(playerChannel, this.dialogHolder.invoke(null, dialog));
            playerChannel.pipeline().addAfter("encoder", null, watch);
            return watch;
        } catch (ReflectiveOperationException | RuntimeException error) {
            this.logger.log(Level.WARNING, "Could not track the coinflip result dialog for " + player.getName(), error);
            return null;
        }
    }

    final class Watch extends ChannelOutboundHandlerAdapter {

        private final Channel playerChannel;
        private final Object holder;
        private boolean current;

        private Watch(Channel playerChannel, Object holder) {
            this.playerChannel = playerChannel;
            this.holder = holder;
        }

        @Override
        public void write(ChannelHandlerContext context, Object packet, ChannelPromise promise) throws Exception {
            if (DialogPackets.this.packetHolder.getDeclaringClass().isInstance(packet)) {
                try {
                    this.current = DialogPackets.this.packetHolder.invoke(packet) == this.holder;
                } catch (ReflectiveOperationException error) {
                    this.current = false;
                    DialogPackets.this.logger.log(Level.WARNING, "Could not identify the active coinflip result dialog", error);
                }
                if (this.current) {
                    promise = promise.unvoid();
                    promise.addListener(result -> {
                        if (!result.isSuccess()) {
                            this.current = false;
                        }
                    });
                }
            } else if (packet == DialogPackets.this.clearDialog) {
                this.current = false;
            }
            context.write(packet, promise);
        }

        void finish(boolean close) {
            if (!this.playerChannel.isActive()) {
                return;
            }
            this.playerChannel.eventLoop().execute(() -> {
                if (this.playerChannel.pipeline().context(this) == null) {
                    return;
                }
                this.playerChannel.pipeline().remove(this);
                if (close && this.current && this.playerChannel.isActive()) {
                    this.playerChannel.writeAndFlush(DialogPackets.this.clearDialog);
                }
            });
        }
    }
}
