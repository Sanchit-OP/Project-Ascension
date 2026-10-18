package com.ascension.combat.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/**
 * Opens the skill tree: K, bound by default so players find it (docs/progression/growth.md section 10;
 * the rune screen takes R). Registered the way {@link CombatKeybinds} registers its keys, from growth's
 * own entry point, so the shared keybind file is left alone.
 */
public final class GrowthKeybind {

    static final KeyMapping OPEN = new KeyMapping(
            "key.ascension_combat.open_growth", InputConstants.KEY_K, "key.categories.ascension_combat");

    /** The key that opens the skill tree, as the player has it bound, for the HUD's hint. */
    public static net.minecraft.network.chat.Component keyName() {
        return OPEN.getTranslatedKeyMessage();
    }

    private GrowthKeybind() {
    }

    public static void register(IEventBus modBus, IEventBus gameBus) {
        modBus.addListener(GrowthKeybind::onRegisterKeyMappings);
        gameBus.addListener(GrowthKeybind::onClientTick);
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        while (OPEN.consumeClick()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null && minecraft.screen == null) {
                minecraft.setScreen(new GrowthScreen());
            }
        }
    }
}
