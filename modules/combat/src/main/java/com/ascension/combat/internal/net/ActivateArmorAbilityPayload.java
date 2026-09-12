package com.ascension.combat.internal.net;

import com.ascension.combat.AscensionCombat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * "Activate whatever's in my armor slots" (docs/technical/combat-api.md section 6) &mdash; sent
 * client to server when the player presses the shared armor-ability key
 * ({@link com.ascension.combat.client.CombatKeybinds}). Carries no fields: the server-side handler
 * checks each equipment slot for an item with an active to trigger, the same way pressing the item
 * key in League checks whichever item sits in that slot rather than the key itself knowing what it
 * does.
 *
 * <p>Empty on purpose &mdash; ADR-0007 rule 8 asks for minimal payloads, and there is nothing here
 * for the client to tell the server that "which key was pressed" doesn't already answer.
 */
public record ActivateArmorAbilityPayload() implements CustomPacketPayload {

    public static final ActivateArmorAbilityPayload INSTANCE = new ActivateArmorAbilityPayload();

    public static final CustomPacketPayload.Type<ActivateArmorAbilityPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, "activate_armor_ability"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ActivateArmorAbilityPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
