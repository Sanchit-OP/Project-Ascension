package com.ascension.worlds.client;

import com.ascension.worlds.AscensionWorlds;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * Which {@link PlanetWeatherEffect} handles which {@code weather.type} id. A provider registry
 * (ADR-0003 rule 3) rather than a switch on the id, so a future weather type never means editing
 * this class &mdash; only registering a new one here.
 */
final class PlanetWeatherEffects {

    private static final Map<ResourceLocation, PlanetWeatherEffect> EFFECTS = new HashMap<>();

    static {
        register(ResourceLocation.fromNamespaceAndPath(AscensionWorlds.MOD_ID, "dust_storm"),
                new DustStormEffect());
    }

    static void register(ResourceLocation type, PlanetWeatherEffect effect) {
        EFFECTS.put(type, effect);
    }

    static PlanetWeatherEffect get(ResourceLocation type) {
        return EFFECTS.get(type);
    }

    private PlanetWeatherEffects() {
    }
}
