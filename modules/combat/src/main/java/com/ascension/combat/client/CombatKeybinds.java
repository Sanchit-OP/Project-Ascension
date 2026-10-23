package com.ascension.combat.client;

import com.ascension.combat.internal.net.AbilitySlot;
import com.ascension.combat.internal.net.ActivateAbilityPayload;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The ability keys (docs/technical/combat-api.md section 6): one key for each place an item with an active can sit
 * ({@link AbilitySlot}: the held item, the chest, the legs, the off hand and relic slot), the way each item slot in League
 * has its own key. They were one key that fired every worn active at once (Gargoyle Stoneplate, Rocketbelt, Zonia's Hourglass
 * and Galeforce together), so a player with a shield, a dash and a stasis could not use one without the others; Sanchit,
 * 2026-10-23: that "does not sound right". Galeforce (2026-09-20) was the first <em>held</em> weapon to need a key, after
 * sneak+right-click proved unreliable whenever aimed at an entity.
 *
 * <p>The keys are unbound in code ({@link InputConstants#UNKNOWN}) &mdash; a player binds them in Controls rather than this
 * claiming a key that might already mean something else to them; the pack ships defaults for a fresh install
 * ({@code pack/config/defaultoptions/keybindings.txt}).
 * The three screens have keys: R for runes, K for growth (see {@code GrowthKeybind}), V for the
 * character sheet. While one is open the others' keys switch to them ({@code ScreenSwitch}).
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

    /** One key for each {@link AbilitySlot}, named {@code key.ascension_combat.ability_<slot>}. */
    private static final Map<AbilitySlot, KeyMapping> ABILITY_KEYS = new EnumMap<>(AbilitySlot.class);

    static {
        for (AbilitySlot slot : AbilitySlot.values()) {
            ABILITY_KEYS.put(slot, new KeyMapping("key.ascension_combat.ability_" + slot.id(), InputConstants.UNKNOWN.getValue(), CATEGORY));
        }
    }

    private static final KeyMapping OPEN_RUNE_PAGE = new KeyMapping(
            "key.ascension_combat.open_rune_page",
            InputConstants.KEY_R,
            CATEGORY);

    private static final KeyMapping OPEN_CHARACTER_SHEET = new KeyMapping(
            "key.ascension_combat.open_character_sheet",
            InputConstants.KEY_V,
            CATEGORY);

    private CombatKeybinds() {
    }

    /**
     * How an ability key reads in an item's tooltip: the key in brackets, or "(unbound)" while the player has not given it one,
     * so the line is true whatever they chose.
     */
    public static String abilityKeyLabel(AbilitySlot slot) {
        KeyMapping key = ABILITY_KEYS.get(slot);
        if (key == null || key.isUnbound()) {
            return I18n.get("tooltip.ascension_combat.key_unbound");
        }
        return "[" + key.getTranslatedKeyMessage().getString() + "]";
    }

    /** Whether a key press is the Open Rune Page key, so the rune screen can close on the key that opened it. */
    public static boolean isOpenRunePageKey(int keyCode, int scanCode) {
        return OPEN_RUNE_PAGE.matches(keyCode, scanCode);
    }

    /** Whether a key press is the Open Character Sheet key (V by default; vanilla's C is a creative hotbar key). */
    public static boolean isOpenCharacterSheetKey(int keyCode, int scanCode) {
        return OPEN_CHARACTER_SHEET.matches(keyCode, scanCode);
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(CombatKeybinds::onRegisterKeyMappings);
        com.ascension.combat.client.dev.RuneScreenshots.register(NeoForge.EVENT_BUS);
        com.ascension.combat.client.dev.GalleryShots.register(NeoForge.EVENT_BUS);
        com.ascension.combat.client.dev.RuneEffectChecks.register(NeoForge.EVENT_BUS);
        com.ascension.combat.client.dev.IntegrationChecks.register(NeoForge.EVENT_BUS);
        com.ascension.combat.client.dev.KeybindAudit.register(NeoForge.EVENT_BUS);
        com.ascension.combat.client.dev.DevChatFilter.register(NeoForge.EVENT_BUS);
        NeoForge.EVENT_BUS.addListener(CombatKeybinds::onClientTick);
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        ABILITY_KEYS.values().forEach(event::register);
        event.register(OPEN_RUNE_PAGE);
        event.register(OPEN_CHARACTER_SHEET);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        ABILITY_KEYS.forEach((slot, key) -> {
            while (key.consumeClick()) {
                PacketDistributor.sendToServer(new ActivateAbilityPayload(slot));
            }
        });
        while (OPEN_RUNE_PAGE.consumeClick()) {
            Minecraft.getInstance().setScreen(new RuneScreen());
        }
        while (OPEN_CHARACTER_SHEET.consumeClick()) {
            Minecraft.getInstance().setScreen(new CharacterSheetScreen());
        }
    }
}
