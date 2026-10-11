package com.ascension.worlds.internal;

import com.ascension.core.api.WorldEnvironment;
import com.ascension.core.api.WorldEnvironmentSource;
import com.ascension.worlds.api.Planet;
import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Publishes each planet's environment for anything that cares about breathable air.
 *
 * <p>The other half of the ADR-0011 bridge. This module never mentions
 * {@code ascension-atmosphere}; it says what its worlds are like, into a registry in
 * {@code ascension-core}, and whoever wants to act on that acts on it. With no atmosphere module
 * installed, nothing reads this and nothing suffocates &mdash; which is rule 6 working.
 *
 * <p>The {@link WorldEnvironmentSource} contract hands over a dimension key and nothing else
 * &mdash; deliberately, since most implementers know their answer in code and should not have to
 * be handed a {@code RegistryAccess} they will never use. Reading a datapack registry needs one,
 * so the answer comes from {@link PlanetSurfaces}, flattened when the server starts.
 */
public final class PlanetEnvironments implements WorldEnvironmentSource {

    /** The single instance registered into {@code ascension-core}. */
    public static final PlanetEnvironments INSTANCE = new PlanetEnvironments();

    @Override
    public Optional<WorldEnvironment> environmentOf(ResourceKey<Level> dimension) {
        return Optional.ofNullable(PlanetSurfaces.on(dimension)).map(Planet::environment);
    }

    private PlanetEnvironments() {
    }
}
