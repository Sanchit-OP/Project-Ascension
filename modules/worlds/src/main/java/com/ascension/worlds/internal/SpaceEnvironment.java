package com.ascension.worlds.internal;

import com.ascension.core.api.WorldEnvironment;
import com.ascension.core.api.WorldEnvironmentSource;
import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * The shared space dimension is vacuum. Always.
 *
 * <p>{@link PlanetEnvironments} cannot say this &mdash; it only knows about
 * {@link com.ascension.worlds.api.Planet#surface()}, and {@link SpaceDimension#KEY} is nobody's
 * surface. A shared space dimension and a planet's surface are different kinds of thing (see
 * {@code worlds-api.md} §2's distinction between {@code WorldEnvironment} and
 * {@code Atmosphere}), so this is a separate, fixed source rather than folded into the
 * data-driven one.
 */
public final class SpaceEnvironment implements WorldEnvironmentSource {

    public static final SpaceEnvironment INSTANCE = new SpaceEnvironment();

    private SpaceEnvironment() {
    }

    @Override
    public Optional<WorldEnvironment> environmentOf(ResourceKey<Level> dimension) {
        return dimension.equals(SpaceDimension.KEY)
                ? Optional.of(WorldEnvironment.AIRLESS)
                : Optional.empty();
    }
}
