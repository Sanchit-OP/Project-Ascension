package com.ascension.combat.internal.growth;

import com.ascension.combat.AscensionCombat;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * The tree screen's Respec button. Carries nothing: the server decides everything, from the player's
 * combat state and XP levels ({@link PlayerGrowth#respec}).
 */
public record GrowthRespecPayload() implements CustomPacketPayload {

    public static final GrowthRespecPayload INSTANCE = new GrowthRespecPayload();

    public static final CustomPacketPayload.Type<GrowthRespecPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, "growth_respec"));

    public static final StreamCodec<ByteBuf, GrowthRespecPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
