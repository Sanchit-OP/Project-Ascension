package com.ascension.combat.internal.net;

import com.ascension.combat.AscensionCombat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * "This bow's own active ends its cooldown at this game tick" (docs/technical/combat-api.md
 * section 6), sent to a player's own client so it can draw the matching hotbar swipe without
 * routing the cooldown itself through vanilla {@code ItemCooldowns} &mdash; see {@link
 * com.ascension.combat.internal.CombatAttachments#HEARTWOOD_CHARGE_COOLDOWN}'s javadoc for why
 * that system can't be used directly for a bow's own active: it blocks the whole item, including
 * an ordinary uncharged draw, for the cooldown's full duration.
 *
 * @param key             which bow this cooldown belongs to
 * @param endsAtGameTime  the server game tick the cooldown ends
 * @param durationTicks   the cooldown's own total length, so the client can compute a fraction
 *                        without needing to know the constant itself
 */
public record AbilityCooldownSyncPayload(AbilityKey key, long endsAtGameTime, int durationTicks)
        implements CustomPacketPayload {

    public static final Type<AbilityCooldownSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, "ability_cooldown_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AbilityCooldownSyncPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> {
                        buffer.writeEnum(payload.key());
                        buffer.writeLong(payload.endsAtGameTime());
                        buffer.writeVarInt(payload.durationTicks());
                    },
                    buffer -> new AbilityCooldownSyncPayload(
                            buffer.readEnum(AbilityKey.class), buffer.readLong(), buffer.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
