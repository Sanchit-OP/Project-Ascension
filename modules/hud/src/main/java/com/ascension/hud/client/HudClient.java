package com.ascension.hud.client;

import com.ascension.core.api.client.HudRegistry;
import com.ascension.hud.AscensionHud;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * Client-side wiring. Annotated {@link Dist#CLIENT}, so FML never loads this class or anything it
 * references on a dedicated server; keeping every client entry point here makes that checkable by
 * eye.
 */
@EventBusSubscriber(modid = AscensionHud.MOD_ID, value = Dist.CLIENT)
public final class HudClient {

    private static final ResourceLocation HOST_ID =
            ResourceLocation.fromNamespaceAndPath(AscensionHud.MOD_ID, "hud");

    private HudClient() {
    }

    /** Tell other modules a host is here, so their own fallback gauges stay quiet. */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        HudRegistry.declareHost(HOST_ID);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(HOST_ID, new HudLayer());
    }
}
