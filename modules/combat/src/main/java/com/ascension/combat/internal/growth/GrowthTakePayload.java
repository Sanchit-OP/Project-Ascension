package com.ascension.combat.internal.growth;

import com.ascension.combat.AscensionCombat;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * The tree screen's Confirm: every node the player marked, in the order they marked them. The server
 * checks each against the rules ({@link PlayerGrowth#takeAll}) and ignores whatever breaks one, so the
 * client is trusted with nothing. Capped, because a client can claim anything.
 */
public record GrowthTakePayload(List<ResourceLocation> nodes) implements CustomPacketPayload {

    /** More than a whole v1 tree; a longer list is not a screen's doing. */
    public static final int MAX_NODES = 128;

    public static final CustomPacketPayload.Type<GrowthTakePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, "growth_take"));

    public static final StreamCodec<ByteBuf, GrowthTakePayload> STREAM_CODEC = ResourceLocation.STREAM_CODEC
            .apply(ByteBufCodecs.list(MAX_NODES))
            .map(GrowthTakePayload::new, GrowthTakePayload::nodes);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
