package com.ascension.worlds.client;

import com.ascension.worlds.AscensionWorlds;
import com.ascension.worlds.api.Planet;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Dispatches to whichever {@link PlanetWeatherEffect} is registered for the current surface's
 * weather {@code type}, per {@link ClientPlanetWeather}. Nothing here knows what a dust storm
 * looks like &mdash; that is {@link DustStormEffect}'s job entirely, so a second weather type
 * later needs no change here at all.
 */
@EventBusSubscriber(modid = AscensionWorlds.MOD_ID, value = Dist.CLIENT)
final class PlanetWeatherRenderer {

    @SubscribeEvent
    static void onRenderFog(ViewportEvent.RenderFog event) {
        withActiveEffect((effect, config, intensity) -> effect.applyFog(event, config, intensity));
    }

    @SubscribeEvent
    static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        withActiveEffect((effect, config, intensity) -> effect.applyFogColor(event, config, intensity));
    }

    /**
     * {@link ClientPlanetWeather#tick()} runs first, unconditionally &mdash; it has to keep
     * stepping the ramp down even on the tick {@link ClientPlanetWeather#isActive()} is about to
     * go false because intensity just hit zero.
     */
    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        ClientPlanetWeather.tick();
        withActiveEffect((effect, config, intensity) -> effect.tick(config, intensity));
    }

    @SubscribeEvent
    static void onRenderGui(RenderGuiEvent.Post event) {
        withActiveEffect((effect, config, intensity) -> effect.applyScreenOverlay(event, config, intensity));
    }

    private static void withActiveEffect(Action action) {
        if (!ClientPlanetWeather.isActive()) {
            return;
        }
        Planet.Weather config = ClientPlanetWeather.current();
        PlanetWeatherEffect effect = PlanetWeatherEffects.get(config.type());
        if (effect != null) {
            action.run(effect, config, ClientPlanetWeather.intensity());
        }
    }

    @FunctionalInterface
    private interface Action {
        void run(PlanetWeatherEffect effect, Planet.Weather config, float intensity);
    }

    private PlanetWeatherRenderer() {
    }
}
