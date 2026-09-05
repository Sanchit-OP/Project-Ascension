package com.ascension.atmosphere.internal.net;

import com.ascension.atmosphere.AscensionAtmosphere;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * A player's own oxygen situation, sent to that player only.
 *
 * <p>Fourteen bytes. Zones are never synced &mdash; the client does not need them to draw a bar,
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
 */
public record OxygenSyncPayload(
        int units,
        int capacity,
        boolean breathable,
        float drainPerSecond,
        boolean suffocating,
        boolean refilling) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OxygenSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(AscensionAtmosphere.MOD_ID, "oxygen_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OxygenSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, OxygenSyncPayload::units,
                    ByteBufCodecs.VAR_INT, OxygenSyncPayload::capacity,
                    ByteBufCodecs.BOOL, OxygenSyncPayload::breathable,
                    ByteBufCodecs.FLOAT, OxygenSyncPayload::drainPerSecond,
                    ByteBufCodecs.BOOL, OxygenSyncPayload::suffocating,
                    ByteBufCodecs.BOOL, OxygenSyncPayload::refilling,
                    OxygenSyncPayload::new);

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
                || Math.abs(drainPerSecond - other.drainPerSecond) > 0.001f;
    }
}
