package com.ascension.atmosphere.internal.net;

import com.ascension.atmosphere.AscensionAtmosphere;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * A player's own oxygen situation, sent to that player only.
 *
 * <p>Under twenty bytes. Zones are never synced &mdash; the client does not need them to draw a bar,
 * and shipping them would turn a trivial packet into a stream of world state.
 *
 * <p>Sent on change, plus a low-frequency reconcile. Never per tick (ADR-0007 rule 8).
 *
 * @param units           oxygen currently held
 * @param capacity        total the player could hold
 * @param breathable      whether the surrounding air is breathable
 * @param drainPerSecond  current effective drain, for the seconds readout
 * @param suffocating     inside the failure window
 * @param refilling       lungs are below capacity and topping up in breathable air
 * @param pressurisingSeconds seconds until a freshly opened tank starts delivering, or zero
 */
public record OxygenSyncPayload(
        int units,
        int capacity,
        boolean breathable,
        float drainPerSecond,
        boolean suffocating,
        boolean refilling,
        int pressurisingSeconds) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OxygenSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(AscensionAtmosphere.MOD_ID, "oxygen_sync"));

    /**
     * Written by hand rather than with {@code StreamCodec.composite}, which stops at six fields.
     *
     * <p>The order here is the wire format and must match between {@link #write} and
     * {@link #read} &mdash; the one real hazard of dropping the combinator, and the reason the
     * two methods sit next to each other.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, OxygenSyncPayload> STREAM_CODEC =
            StreamCodec.of(OxygenSyncPayload::write, OxygenSyncPayload::read);

    private static void write(RegistryFriendlyByteBuf buffer, OxygenSyncPayload payload) {
        buffer.writeVarInt(payload.units);
        buffer.writeVarInt(payload.capacity);
        buffer.writeBoolean(payload.breathable);
        buffer.writeFloat(payload.drainPerSecond);
        buffer.writeBoolean(payload.suffocating);
        buffer.writeBoolean(payload.refilling);
        buffer.writeVarInt(payload.pressurisingSeconds);
    }

    private static OxygenSyncPayload read(RegistryFriendlyByteBuf buffer) {
        return new OxygenSyncPayload(
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readFloat(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readVarInt());
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Whether this differs from {@code other} in any way a player could notice.
     *
     * <p>Change detection lives here rather than in the tracker so that adding a field cannot
     * silently stop it being synced.
     */
    public boolean differsFrom(OxygenSyncPayload other) {
        if (other == null) {
            return true;
        }
        return units != other.units
                || capacity != other.capacity
                || breathable != other.breathable
                || suffocating != other.suffocating
                || refilling != other.refilling
                || pressurisingSeconds != other.pressurisingSeconds
                || Math.abs(drainPerSecond - other.drainPerSecond) > 0.001f;
    }
}
