package com.ascension.combat.client;

import com.ascension.combat.internal.net.ActivateArmorAbilityPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The shared "activate armor ability" key (docs/technical/combat-api.md section 6). One key for
 * every worn item with an active, mirroring how a single item slot in League always means
 * "activate whatever is here" rather than each item needing its own bind. Gargoyle Stoneplate is
 * the first consumer; Rocketbelt and Zonia's Hourglass followed, and Galeforce (2026-09-20) is the
 * first <em>held</em> weapon to use it, after sneak+right-click proved unreliable whenever aimed at
 * an entity &mdash; confirming this key was never really armor-specific, just "whatever's equipped
 * with an active you'd rather press a dedicated button for."
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
 *
 * <p>{@link #OPEN_RUNE_PAGE} (docs/technical/combat-api.md section 7) and {@link
 * #OPEN_CHARACTER_SHEET} (section 3) both need no payload of their own &mdash; opening a screen is
 * purely local, unlike activating an armor ability, which has to reach the server. {@link
 * CharacterSheetScreen} needs even less: unlike the rune page, it has no server-saved state to
 * pull down first, since every stat it shows is already a synced attribute the client already has.
 */
public final class CombatKeybinds {

    private static final String CATEGORY = "key.categories.ascension_combat";

    private static final KeyMapping ACTIVATE_ARMOR_ABILITY = new KeyMapping(
            "key.ascension_combat.activate_armor_ability",
            InputConstants.UNKNOWN.getValue(),
            CATEGORY);

    private static final KeyMapping OPEN_RUNE_PAGE = new KeyMapping(
            "key.ascension_combat.open_rune_page",
            InputConstants.UNKNOWN.getValue(),
            CATEGORY);

    private static final KeyMapping OPEN_CHARACTER_SHEET = new KeyMapping(
            "key.ascension_combat.open_character_sheet",
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
        event.register(OPEN_RUNE_PAGE);
        event.register(OPEN_CHARACTER_SHEET);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        while (ACTIVATE_ARMOR_ABILITY.consumeClick()) {
            PacketDistributor.sendToServer(ActivateArmorAbilityPayload.INSTANCE);
        }
        while (OPEN_RUNE_PAGE.consumeClick()) {
            Minecraft.getInstance().setScreen(new RuneScreen());
        }
        while (OPEN_CHARACTER_SHEET.consumeClick()) {
            Minecraft.getInstance().setScreen(new CharacterSheetScreen());
        }
    }
}
