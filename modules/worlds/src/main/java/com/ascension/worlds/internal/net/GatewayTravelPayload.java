package com.ascension.worlds.internal.net;

import com.ascension.worlds.AscensionWorlds;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client &rarr; server: the player picked this destination on the gateway screen. A request, not
 * a command &mdash; the server applies the same picker-session and network checks the typed
 * {@code gateway travel} command does before moving anyone.
 */
public record GatewayTravelPayload(ResourceLocation surface) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<GatewayTravelPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(AscensionWorlds.MOD_ID, "gateway_travel"));

    public static final StreamCodec<ByteBuf, GatewayTravelPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, GatewayTravelPayload::surface,
            GatewayTravelPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
