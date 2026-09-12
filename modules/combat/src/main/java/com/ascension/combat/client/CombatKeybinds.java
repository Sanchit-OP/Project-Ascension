package com.ascension.combat.client;

import com.ascension.combat.internal.net.ActivateArmorAbilityPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The shared "activate armor ability" key (docs/technical/combat-api.md section 6). One key for
 * every worn item with an active, mirroring how a single item slot in League always means
 * "activate whatever is here" rather than each item needing its own bind. Gargoyle Stoneplate is
 * the first consumer; Zonia's Hourglass and Sterak's Harness are expected to be the next two.
 *
 * <p>Unbound by default ({@link InputConstants#UNKNOWN}) &mdash; Sanchit binds it himself in
 * Controls rather than this claiming a key that might already mean something else to him.
 *
 * <p>{@link RegisterKeyMappingsEvent} fires on the mod bus, client side only; the tick poll that
 * actually reads {@link KeyMapping#consumeClick()} runs on the game bus ({@link
 * NeoForge#EVENT_BUS}), since {@link ClientTickEvent} is a plain event, not a mod-bus one. Neither
 * this class nor its registration is reached via {@code @EventBusSubscriber} &mdash; that
 * annotation's {@code bus} attribute is deprecated, and this module already registers everything
 * explicitly in {@code AscensionCombat}'s constructor, so this follows the same convention rather
 * than mixing two registration styles.
 *
 * <p>{@code AscensionCombat} is the one place non-client code names this class, gated behind
 * {@code Dist.CLIENT} at the call site &mdash; the same isolation {@code AtmosphereNetwork} uses
 * for its own client-only handler reference.
 */
public final class CombatKeybinds {

    private static final String CATEGORY = "key.categories.ascension_combat";

    private static final KeyMapping ACTIVATE_ARMOR_ABILITY = new KeyMapping(
            "key.ascension_combat.activate_armor_ability",
            InputConstants.UNKNOWN.getValue(),
            CATEGORY);

    private CombatKeybinds() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(CombatKeybinds::onRegisterKeyMappings);
        NeoForge.EVENT_BUS.addListener(CombatKeybinds::onClientTick);
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ACTIVATE_ARMOR_ABILITY);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        while (ACTIVATE_ARMOR_ABILITY.consumeClick()) {
            PacketDistributor.sendToServer(ActivateArmorAbilityPayload.INSTANCE);
        }
    }
}
