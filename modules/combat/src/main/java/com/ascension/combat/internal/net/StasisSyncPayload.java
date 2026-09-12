package com.ascension.combat.internal.net;

import com.ascension.combat.AscensionCombat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Tells a player's own client whether they are currently in stasis
 * (docs/technical/combat-api.md section 5), so it can force and release the third-person camera
 * &mdash; the one part of stasis with no server-side equivalent to check. Untargetability and
 * immobility are both server-authoritative (invulnerability and a move-speed modifier) and need
 * no sync of their own.
 *
 * @param active whether the player should currently be locked to third person
 */
public record StasisSyncPayload(boolean active) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StasisSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, "stasis_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StasisSyncPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> buffer.writeBoolean(payload.active()),
                    buffer -> new StasisSyncPayload(buffer.readBoolean()));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
