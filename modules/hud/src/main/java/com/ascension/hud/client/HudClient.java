package com.ascension.hud.client;

import com.ascension.core.api.client.HudRegistry;
import com.ascension.hud.AscensionHud;
import com.ascension.hud.internal.HudConfig;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Client-side wiring. Annotated {@link Dist#CLIENT}, so FML never loads this class or anything it
 * references on a dedicated server; keeping every client entry point here makes that checkable by
 * eye.
 */
@EventBusSubscriber(modid = AscensionHud.MOD_ID, value = Dist.CLIENT)
public final class HudClient {

    private static final ResourceLocation HOST_ID =
            ResourceLocation.fromNamespaceAndPath(AscensionHud.MOD_ID, "hud");

    /** Holds only health numbers; never a player (ADR-0007). */
    private static final VitalsElement VITALS = new VitalsElement();

    private HudClient() {
    }

    /** Tell other modules a host is here, and add the elements this mod draws itself. */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        HudRegistry.declareHost(HOST_ID);
        HudRegistry.register(ResourceLocation.fromNamespaceAndPath(AscensionHud.MOD_ID, "vitals"), VITALS);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(HOST_ID, new HudLayer());
    }

    /**
     * Take vanilla's hearts, armour, food and experience off the screen while ours are drawn.
     *
     * <p>Armour goes with them: vanilla's icons show vanilla armour points, which no longer matter
     * (the Armor stat does, and it has its own block). Only while the master switch is on, so a pack
     * that wants vanilla's HUD back gets all of it.
     */
    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (!HudConfig.INSTANCE.enabled.getAsBoolean()) {
            return;
        }
        ResourceLocation name = event.getName();
        if (VanillaGuiLayers.PLAYER_HEALTH.equals(name)
                || VanillaGuiLayers.ARMOR_LEVEL.equals(name)
                || VanillaGuiLayers.FOOD_LEVEL.equals(name)
                || VanillaGuiLayers.EXPERIENCE_BAR.equals(name)
                || VanillaGuiLayers.EXPERIENCE_LEVEL.equals(name)) {
            event.setCanceled(true);
        }
    }

    /** Drop the remembered red part of the health bar when leaving a world. */
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        VITALS.reset();
    }
}
