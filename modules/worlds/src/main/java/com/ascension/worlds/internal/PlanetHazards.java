package com.ascension.worlds.internal;

import com.ascension.worlds.api.Planet;
import com.ascension.worlds.api.WorldsRegistries;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

/**
 * Which surfaces have a {@link Planet.Hazard}, flattened out of the planet registry at server
 * start the same way {@link PlanetWeathers} flattens weather. Holds only {@link ResourceKey}s and
 * records, so it is clear of ADR-0007 rule 1, and it is emptied when the server stops.
 */
public final class PlanetHazards {

    private static volatile Map<ResourceKey<Level>, Planet.Hazard> hazards = Map.of();

    public static Planet.Hazard get(ResourceKey<Level> surface) {
        return hazards.get(surface);
    }

    public static void load(MinecraftServer server) {
        Registry<Planet> planets = server.registryAccess().registryOrThrow(WorldsRegistries.PLANET);

        Map<ResourceKey<Level>, Planet.Hazard> resolved = new HashMap<>();
        planets.entrySet().forEach(entry ->
                entry.getValue().hazard().ifPresent(hazard ->
                        resolved.put(entry.getValue().surface(), hazard)));

        hazards = Map.copyOf(resolved);
    }

    public static void unload() {
        hazards = Map.of();
    }

    private PlanetHazards() {
    }
}
