package com.ascension.compat.ftbteams.internal.dev;

import io.netty.channel.embedded.EmbeddedChannel;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.registration.ChannelAttributes;
import org.jetbrains.annotations.Nullable;

/**
 * A connection with nobody on the other end, for {@link TeamChecks} stand-in players: it is open,
 * accepts every send, and keeps the chat lines the server sent so a check can read what a player was told.
 *
 * <p>Backed by an {@link EmbeddedChannel} so code that asks the connection for its channel (the network
 * library does) finds one, and with its protocol set-up left out, since nothing is encoding or decoding.
 * NeoForge refuses to send a mod's payload down a connection that never agreed to carry it; this one
 * has no peer to agree with, so its list of ad-hoc channels says yes to every payload.
 */
final class FakeConnection extends Connection {

    private final List<Component> chat = new ArrayList<>();

    /** A set that contains every channel and lists none: "this connection can carry anything". */
    private static final class EveryChannel extends AbstractSet<ResourceLocation> {
        @Override
        public boolean contains(Object o) {
            return true;
        }

        @Override
        public Iterator<ResourceLocation> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public int size() {
            return 0;
        }
    }

    FakeConnection() {
        super(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(this);
        this.channel().attr(ChannelAttributes.ADHOC_CHANNELS).set(new EveryChannel());
    }

    /** The system chat lines sent to this player so far, oldest first. */
    List<Component> chat() {
        return this.chat;
    }

    @Override
    public void send(Packet<?> packet, @Nullable PacketSendListener listener, boolean flush) {
        if (packet instanceof ClientboundSystemChatPacket system) {
            this.chat.add(system.content());
        }
    }

    @Override
    public <T extends PacketListener> void setupInboundProtocol(ProtocolInfo<T> protocolInfo, T packetListener) {
    }

    @Override
    public void setupOutboundProtocol(ProtocolInfo<?> protocolInfo) {
    }

    @Override
    public void tick() {
    }

    @Override
    public void disconnect(Component reason) {
    }

    @Override
    public void setReadOnly() {
    }

    @Override
    public void handleDisconnection() {
    }
}
