package com.ascension.atmosphere.client;

import com.ascension.atmosphere.AscensionAtmosphere;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Client-side wiring.
 *
 * <p>Annotated {@link Dist#CLIENT}, so FML never loads this class or anything it references on a
 * dedicated server. Keeping every client entry point in this one class is what makes that
 * guarantee checkable by eye.
 *
 * <p>The target bus is inferred from each event type, so mod-bus and game-bus handlers coexist
 * here without declaring one.
 */
@EventBusSubscriber(modid = AscensionAtmosphere.MOD_ID, value = Dist.CLIENT)
public final class AtmosphereClient {

    private AtmosphereClient() {
    }

    /**
     * Sits directly above the vanilla air bubbles, so drowning and vacuum read as related
     * problems in the same part of the screen rather than competing for attention.
     */
    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.AIR_LEVEL,
                ResourceLocation.fromNamespaceAndPath(AscensionAtmosphere.MOD_ID, "oxygen"),
                new OxygenHudLayer());
    }

    /**
     * Hide vanilla's air bubbles while our bar is doing the same job.
     *
     * <p>Pinning the server-side air supply stops vanilla <em>draining</em>, but the bubble row
     * is still drawn whenever the client thinks air is below maximum, and client and server
     * disagree for a tick or two around entering water. That disagreement is what produced a
     * flickering row of bubbles next to our bar.
     *
     * <p>Cancelling the layer removes the ambiguity entirely: one system owns breathing, so one
     * meter shows it. Only while our bar is actually on screen, so a player in ordinary
     * conditions still sees stock behaviour.
     */
    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (VanillaGuiLayers.AIR_LEVEL.equals(event.getName()) && ClientOxygenState.shouldRender()) {
            event.setCanceled(true);
        }
    }

    /**
     * Drop the mirror on disconnect.
     *
     * <p>Without this, a stale bar from the last server survives into the next session and
     * briefly tells the player they are suffocating in a world they have not joined yet.
     */
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientOxygenState.clear();
    }
}
