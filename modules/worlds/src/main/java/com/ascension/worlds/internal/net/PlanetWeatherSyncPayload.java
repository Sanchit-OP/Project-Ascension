package com.ascension.worlds.internal.net;

import com.ascension.worlds.AscensionWorlds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Whether the surface a player is currently on has an active weather event right now. One
 * boolean, under two bytes on the wire &mdash; the client already has {@code visibility_blocks}
 * and everything else it needs from the (already-synced) planet registry entry, so nothing else
 * needs to travel here.
 *
 * <p>Sent whenever {@link com.ascension.worlds.internal.PlanetWeatherMechanics} flips a surface's
 * clock, to everyone standing on it, and once more directly to a player the moment they arrive on
 * any surface (dimension change or login) so joining mid-storm, or leaving one behind, is never
 * stale for longer than that one packet.
 */
public record PlanetWeatherSyncPayload(boolean active) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PlanetWeatherSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(AscensionWorlds.MOD_ID, "planet_weather_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlanetWeatherSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    PlanetWeatherSyncPayload::active,
                    PlanetWeatherSyncPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
