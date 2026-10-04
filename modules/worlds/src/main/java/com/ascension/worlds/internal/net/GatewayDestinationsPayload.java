package com.ascension.worlds.internal.net;

import com.ascension.worlds.AscensionWorlds;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server &rarr; client: open the gateway destination screen with these choices.
 *
 * <p>The list is decided entirely server-side ({@code GatewayNetworkState.canTravel}); the client
 * only draws it. What a click sends back ({@link GatewayTravelPayload}) is re-checked from scratch
 * on the server, so nothing here is trusted.
 */
public record GatewayDestinationsPayload(List<Destination> destinations) implements CustomPacketPayload {

    /**
     * One choice: the planet's registry id (for its display name, {@code planet.<ns>.<path>}) and
     * its surface dimension (what travel is keyed by).
     */
    public record Destination(ResourceLocation planet, ResourceLocation surface) {

        static final StreamCodec<ByteBuf, Destination> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, Destination::planet,
                ResourceLocation.STREAM_CODEC, Destination::surface,
                Destination::new);
    }

    public static final CustomPacketPayload.Type<GatewayDestinationsPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(AscensionWorlds.MOD_ID, "gateway_destinations"));

    public static final StreamCodec<ByteBuf, GatewayDestinationsPayload> STREAM_CODEC = StreamCodec.composite(
            Destination.STREAM_CODEC.apply(ByteBufCodecs.list()), GatewayDestinationsPayload::destinations,
            GatewayDestinationsPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
