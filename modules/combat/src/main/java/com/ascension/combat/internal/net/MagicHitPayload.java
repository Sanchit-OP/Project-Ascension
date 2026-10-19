package com.ascension.combat.internal.net;

import com.ascension.combat.AscensionCombat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Tells a player's own client that a magic hit just landed on them, so it can show the faint
 * screen-edge tint (docs/technical/damage-kinds.md). One small packet per hit that did damage, sent
 * to that player alone; nothing is sent per tick (ADR-0007).
 *
 * @param strength 0 to 1, from {@code MagicHitPulse.strengthFor}: how much of the target's health the hit took
 */
public record MagicHitPayload(float strength) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<MagicHitPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, "magic_hit"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MagicHitPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> buffer.writeFloat(payload.strength()),
                    buffer -> new MagicHitPayload(buffer.readFloat()));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
