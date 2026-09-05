package com.ascension.worlds.internal;

import com.ascension.core.api.WorldEnvironment;
import com.ascension.core.api.WorldEnvironmentSource;
import com.ascension.worlds.api.Planet;
import com.ascension.worlds.api.WorldsRegistries;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Publishes each planet's environment for anything that cares about breathable air.
 *
 * <p>The other half of the ADR-0011 bridge. This module never mentions
 * {@code ascension-atmosphere}; it says what its worlds are like, into a registry in
 * {@code ascension-core}, and whoever wants to act on that acts on it. With no atmosphere module
 * installed, nothing reads this and nothing suffocates &mdash; which is rule 6 working.
 *
 * <p><strong>Resolved once per server session, not per query.</strong> The
 * {@link WorldEnvironmentSource} contract hands over a dimension key and nothing else &mdash;
 * deliberately, since most implementers know their answer in code and should not have to be
 * handed a {@code RegistryAccess} they will never use. Reading a datapack registry needs one, so
 * this module flattens the planet registry into a plain map when the server starts.
 *
 * <p>That is correct rather than merely convenient: datapack registries are loaded once at world
 * load and are <em>not</em> rebuilt by {@code /reload}, which reloads recipes, loot and
 * advancements only. So there is nothing for a cache to go stale against within a session.
 *
 * <p>Holds {@link ResourceKey}s and immutable records, never a {@code Level} or a
 * {@code MinecraftServer} (ADR-0007 rule 1), and is cleared on server stop so a single-player
 * client that loads two worlds in a row does not carry the first one's planets into the second.
 */
public final class PlanetEnvironments implements WorldEnvironmentSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlanetEnvironments.class);

    private static volatile Map<ResourceKey<Level>, WorldEnvironment> environments = Map.of();

    @Override
    public Optional<WorldEnvironment> environmentOf(ResourceKey<Level> dimension) {
        return Optional.ofNullable(environments.get(dimension));
    }

    /** Flatten the planet registry into a lookup. Called when the server has its registries. */
    public static void load(MinecraftServer server) {
        Registry<Planet> planets = server.registryAccess().registryOrThrow(WorldsRegistries.PLANET);

        Map<ResourceKey<Level>, WorldEnvironment> resolved = new HashMap<>();
        planets.entrySet().forEach(entry -> {
            Planet planet = entry.getValue();
            WorldEnvironment previous = resolved.put(planet.surface(), planet.environment());
            if (previous != null) {
                // Two planets sharing a surface is a datapack error, not something to resolve
                // silently. Whichever wins, one of them is not the world its author thinks it is.
                LOGGER.warn("Two planets claim the same surface dimension {}: the later one wins. "
                                + "Offending planet: {}",
                        planet.surface().location(), entry.getKey().location());
            }
        });

        environments = Map.copyOf(resolved);
        LOGGER.info("Loaded {} planet(s): {}", environments.size(),
                planets.keySet().stream().map(key -> key.getPath()).sorted().toList());
    }

    /** Forget everything. A stale map outliving its server is the leak ADR-0007 rule 3 is about. */
    public static void unload() {
        environments = Map.of();
    }

    private PlanetEnvironments() {
    }

    /** The single instance registered into {@code ascension-core}. */
    public static final PlanetEnvironments INSTANCE = new PlanetEnvironments();
}
