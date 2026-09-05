package com.ascension.atmosphere.internal;

import com.ascension.atmosphere.api.Atmosphere;
import com.ascension.atmosphere.api.AtmosphereContext;
import com.ascension.atmosphere.api.AtmospherePriority;
import com.ascension.atmosphere.api.AtmosphereProvider;
import com.ascension.core.api.WorldEnvironment;
import com.ascension.core.api.WorldEnvironmentRegistry;
import java.util.Optional;

/**
 * Whatever the world itself says it is like.
 *
 * <p>This is the bridge described by ADR-0011, and it is the reason this module never mentions
 * {@code ascension-worlds}. Something else declares that a dimension is airless &mdash; our
 * planets module, or a third party's dimension mod, identically &mdash; and this reads it out of
 * the shared registry in {@code ascension-core}.
 *
 * <p><strong>It also retires {@code DebugAtmosphere} from load-bearing duty.</strong> Until now
 * the only unbreathable dimension in the project was a switchable debug vacuum built alongside
 * this module by the same hand, which is the weakest kind of test there is. From here the vacuum
 * can come from data written by something else.
 *
 * <p>Sits at the very bottom of the stack, at {@link AtmospherePriority#DIMENSION} exactly, which
 * is the band's definition rather than a coincidence: "what is this whole dimension like" is
 * precisely the question this band answers. Everything else &mdash; water, a sealed room, a
 * vehicle &mdash; describes a <em>position</em> and overrides it.
 */
public final class WorldEnvironmentAtmosphere implements AtmosphereProvider {

    @Override
    public Optional<Atmosphere> query(AtmosphereContext context) {
        return WorldEnvironmentRegistry.query(context.level().dimension())
                .map(WorldEnvironmentAtmosphere::translate);
    }

    /**
     * Two records with the same two fields, and the translation is deliberate rather than lazy.
     *
     * <p>{@link WorldEnvironment} describes a dimension; {@link Atmosphere} describes a position.
     * Keeping them separate is what lets a sealed room on an airless world answer differently
     * from the world it is standing on.
     *
     * <p>Returns the interned constants where they fit, which is the case for every world we
     * expect to author: Earth is breathable at zero drain, and an airless one drains at baseline.
     * A query on those allocates nothing beyond the {@code Optional} the frozen API requires.
     *
     * <p>No cache beyond that, on purpose. A per-dimension cache would have to be invalidated on
     * {@code /reload}, since planet data is datapack-driven and can change &mdash; real
     * complexity, and a stale cache would report the old atmosphere, for a saving of one small
     * object per player per accounting pass at 2 Hz.
     */
    private static Atmosphere translate(WorldEnvironment environment) {
        if (environment.breathable() && environment.drainMultiplier() == 0.0f) {
            return Atmosphere.BREATHABLE;
        }
        if (!environment.breathable() && environment.drainMultiplier() == 1.0f) {
            return Atmosphere.VACUUM;
        }
        return new Atmosphere(environment.breathable(), environment.drainMultiplier());
    }

    @Override
    public int priority() {
        return AtmospherePriority.DIMENSION;
    }
}
