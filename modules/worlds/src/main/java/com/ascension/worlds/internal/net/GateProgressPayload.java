package com.ascension.worlds.internal.net;

import com.ascension.worlds.AscensionWorlds;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Which planets' gates are lit, network-wide: the whole of {@code GatewayNetworkState} a client
 * needs, and nothing it doesn't (no portal positions). One source for everything on the client that
 * shows how far the road has been relit: the mark on the player's hand, and later the orrery's
 * hologram and the light on the Moon.
 *
 * <p>Sent whole, not as a delta: it is a handful of ids, sent once at login and once each time a
 * gate is lit or unlit, never per tick (ADR-0007).
 */
public record GateProgressPayload(List<ResourceLocation> litPlanets) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<GateProgressPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AscensionWorlds.MOD_ID, "gate_progress"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GateProgressPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    GateProgressPayload::litPlanets,
                    GateProgressPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
