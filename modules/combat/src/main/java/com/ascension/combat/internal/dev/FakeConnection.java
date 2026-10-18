package com.ascension.combat.internal.dev;

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
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.registration.ChannelAttributes;
import org.jetbrains.annotations.Nullable;

/**
 * A connection with nobody on the other end, for {@link DamageChecks}' stand-in player: it is open, accepts every
 * send, and keeps the mod payloads the server sent so a check can read what the player was told. The same idea as
 * the one in {@code ascension-compat-ftbteams}' {@code TeamChecks} (Tier 1 may not depend on that jar, so it is
 * written again here); that one keeps chat lines, this one keeps payloads.
 */
final class FakeConnection extends Connection {

    private final List<CustomPacketPayload> payloads = new ArrayList<>();

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

    /** Payloads of this type sent so far, oldest first. */
    <T extends CustomPacketPayload> List<T> sent(Class<T> type) {
        List<T> found = new ArrayList<>();
        for (CustomPacketPayload payload : this.payloads) {
            if (type.isInstance(payload)) {
                found.add(type.cast(payload));
            }
        }
        return found;
    }

    void forget() {
        this.payloads.clear();
    }

    @Override
    public void send(Packet<?> packet, @Nullable PacketSendListener listener, boolean flush) {
        if (packet instanceof ClientboundCustomPayloadPacket custom) {
            this.payloads.add(custom.payload());
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
