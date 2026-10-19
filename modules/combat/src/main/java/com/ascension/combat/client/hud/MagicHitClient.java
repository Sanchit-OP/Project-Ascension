package com.ascension.combat.client.hud;

import com.ascension.combat.AscensionCombat;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** Wires the magic-hit tint into the client: its layer, and forgetting it on leaving a world. */
@EventBusSubscriber(modid = AscensionCombat.MOD_ID, value = Dist.CLIENT)
public final class MagicHitClient {

    private MagicHitClient() {
    }

    /**
     * Just above the camera overlays (vanilla's own vignette, frost and portal haze), so it lies with the other
     * screen-edge effects and under the whole HUD.
     */
    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.CAMERA_OVERLAYS,
                ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, "magic_hit"),
                new MagicHitLayer());
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientMagicHit.clear();
    }
}
