package com.ascension.combat.client.hud;

import com.ascension.combat.AscensionCombat;
import com.ascension.core.api.client.HudRegistry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/**
 * Gives the HUD combat's elements (ADR-0024). Registers through core's contract only, so combat
 * works the same whether or not a HUD host is installed.
 */
@EventBusSubscriber(modid = AscensionCombat.MOD_ID, value = Dist.CLIENT)
public final class CombatHudClient {

    /** Holds numbers and cached text only; never a player (ADR-0007). */
    private static final StatBlockElement STATS = new StatBlockElement();
    private static final RuneChipsElement RUNES = new RuneChipsElement();
    private static final SkillPointsElement POINTS = new SkillPointsElement();

    private CombatHudClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        HudRegistry.register(ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, "stat_block"), STATS);
        HudRegistry.register(ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, "rune_chips"), RUNES);
        HudRegistry.register(ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, "skill_points"), POINTS);
        // Dormant unless its trigger file is in the game directory.
        com.ascension.combat.client.dev.RuneHudScreenshots.register(net.neoforged.neoforge.common.NeoForge.EVENT_BUS);
    }

    /** Drop colour history when leaving a world, so the next one starts quiet. */
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        STATS.reset();
        RUNES.reset();
        ClientRuneHud.clear();
    }
}
