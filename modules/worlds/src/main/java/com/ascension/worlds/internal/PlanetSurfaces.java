package com.ascension.worlds.internal;

import com.ascension.worlds.api.Planet;
import com.ascension.worlds.api.WorldsRegistries;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Which planet a dimension is the surface of, for the per-tick questions asked about a level:
 * its air ({@link PlanetEnvironments}), its weather ({@link PlanetWeatherMechanics}) and its hazard
 * ({@link PlanetHazardMechanics}). Each is one map lookup, and a level that is no planet's surface
 * (space, the End) finds nothing.
 *
 * <p><strong>Resolved once per server session.</strong> Datapack registries are loaded at world
 * load and are not rebuilt by {@code /reload}, so a map flattened at server start cannot go stale
 * within a session.
 *
 * <p>Holds {@link ResourceKey}s and immutable records, never a {@code Level} or the server
 * (ADR-0007 rule 1), and is emptied when the server stops, so a single-player client that loads
 * two worlds in a row does not carry the first one's planets into the second.
 */
public final class PlanetSurfaces {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlanetSurfaces.class);

    private static volatile Map<ResourceKey<Level>, Planet> bySurface = Map.of();

    /** The planet whose surface this is, or null. */
    @Nullable
    public static Planet on(ResourceKey<Level> dimension) {
        return bySurface.get(dimension);
    }

    /** This surface's weather, or null if it has none (or is no planet's surface). */
    @Nullable
    public static Planet.Weather weatherOn(ResourceKey<Level> dimension) {
        Planet planet = on(dimension);
        return planet == null ? null : planet.weather().orElse(null);
    }

    /** This surface's hazard, or null if it has none (or is no planet's surface). */
    @Nullable
    public static Planet.Hazard hazardOn(ResourceKey<Level> dimension) {
        Planet planet = on(dimension);
        return planet == null ? null : planet.hazard().orElse(null);
    }

    /** Flatten the planet registry into a lookup. Called when the server has its registries. */
    public static void load(MinecraftServer server) {
        Registry<Planet> planets = server.registryAccess().registryOrThrow(WorldsRegistries.PLANET);

        Map<ResourceKey<Level>, Planet> resolved = new HashMap<>();
        planets.entrySet().forEach(entry -> {
            Planet planet = entry.getValue();
            if (resolved.put(planet.surface(), planet) != null) {
                // Two planets sharing a surface is a datapack error, not something to resolve
                // silently. Whichever wins, one of them is not the world its author thinks it is.
                LOGGER.warn("Two planets claim the same surface dimension {}: the later one wins. "
                                + "Offending planet: {}",
                        planet.surface().location(), entry.getKey().location());
            }
        });

        bySurface = Map.copyOf(resolved);
        LOGGER.info("Loaded {} planet(s): {}", bySurface.size(),
                planets.keySet().stream().map(key -> key.getPath()).sorted().toList());
    }

    /** Forget everything. A stale map outliving its server is the leak ADR-0007 rule 3 is about. */
    public static void unload() {
        bySurface = Map.of();
    }

    private PlanetSurfaces() {
    }
}
