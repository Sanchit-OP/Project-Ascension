package com.ascension.combat.internal.net;

import com.ascension.combat.AscensionCombat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * "Use the active of the item in this place" (docs/technical/combat-api.md section 6) &mdash; sent client to server when the player
 * presses one of the ability keys ({@link com.ascension.combat.client.CombatKeybinds}). It was one empty packet for one key that
 * tried every worn item; now each place has its own key and the packet says which, so pressing the chest key never fires the
 * belt. One byte, nothing per tick (ADR-0007 rule 8). A slot the server does not know is ignored.
 */
public record ActivateAbilityPayload(AbilitySlot slot) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ActivateAbilityPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, "activate_ability"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ActivateAbilityPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(AbilitySlot::byOrdinal, slot -> slot == null ? -1 : slot.ordinal()), ActivateAbilityPayload::slot,
            ActivateAbilityPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
