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
 * Which surfaces have a {@link Planet.Weather} of their own, flattened out of the planet registry
 * the same way {@link PlanetEnvironments} flattens environments &mdash; see that class's javadoc
 * for why a plain map resolved once at server start is correct here rather than merely
 * convenient, and why holding only {@link ResourceKey}s keeps this clear of ADR-0007 rule 1.
 *
 * <p>Most surfaces have no entry at all, which is the point: {@link PlanetWeatherMechanics} only
 * has anything to do on a level once this map says so, so a planet with no {@code weather} block
 * costs nothing beyond the one map lookup that finds it absent.
 */
public final class PlanetWeathers {

    private static volatile Map<ResourceKey<Level>, Planet.Weather> weathers = Map.of();

    public static Planet.Weather get(ResourceKey<Level> surface) {
        return weathers.get(surface);
    }

    public static void load(MinecraftServer server) {
        Registry<Planet> planets = server.registryAccess().registryOrThrow(WorldsRegistries.PLANET);

        Map<ResourceKey<Level>, Planet.Weather> resolved = new HashMap<>();
        planets.entrySet().forEach(entry ->
                entry.getValue().weather().ifPresent(weather ->
                        resolved.put(entry.getValue().surface(), weather)));

        weathers = Map.copyOf(resolved);
    }

    public static void unload() {
        weathers = Map.of();
    }

    private PlanetWeathers() {
    }
}
