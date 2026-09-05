package com.ascension.atmosphere.internal;

import com.ascension.atmosphere.api.Atmosphere;
import com.ascension.atmosphere.api.AtmosphereContext;
import com.ascension.atmosphere.api.AtmosphereProvider;
import com.ascension.atmosphere.api.AtmospherePriority;
import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * A switchable vacuum, so the system can be exercised before any planet exists.
 *
 * <p>ADR-0008 requires every increment to be observable in a running client. Until
 * {@code ascension-worlds} can supply a genuinely airless dimension, there is nothing to
 * stand in that would prove breathability resolution works &mdash; so this provides it on
 * demand, per dimension, via {@code /ascension atmosphere debug}.
 *
 * <p>Off unless explicitly switched on, and it stores only a {@link ResourceKey}, never a
 * {@code Level} (ADR-0007 rule 1).
 */
public final class DebugAtmosphere implements AtmosphereProvider {

    /** Dimension currently forced to vacuum, or null. */
    private static volatile ResourceKey<Level> vacuumDimension;

    public static void setVacuum(ResourceKey<Level> dimension) {
        vacuumDimension = dimension;
    }

    public static void clear() {
        vacuumDimension = null;
    }

    public static ResourceKey<Level> vacuumDimension() {
        return vacuumDimension;
    }

    @Override
    public Optional<Atmosphere> query(AtmosphereContext context) {
        ResourceKey<Level> forced = vacuumDimension;
        if (forced != null && context.level().dimension().equals(forced)) {
            return Optional.of(Atmosphere.VACUUM);
        }
        return Optional.empty();
    }

    @Override
    public int priority() {
        return AtmospherePriority.OVERRIDE;
    }
}
