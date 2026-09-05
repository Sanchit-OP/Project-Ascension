package com.ascension.atmosphere.internal;

import com.ascension.atmosphere.api.Atmosphere;
import com.ascension.atmosphere.api.AtmosphereContext;
import com.ascension.atmosphere.api.AtmosphereProvider;
import com.ascension.atmosphere.api.AtmospherePriority;
import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * A switchable vacuum, on demand, in any dimension &mdash; including one that is meant to be
 * breathable.
 *
 * <p>Until M2.2 this was the <em>only</em> unbreathable place in the project, which made it a
 * crutch: the atmosphere system was only ever tested against a vacuum built by the same hand
 * that built the thing reading it. The Moon retired that dependency. What is left is its honest
 * job &mdash; forcing vacuum anywhere, instantly, via {@code /ascension atmosphere debug}, for
 * testing gear, HUD states and failure timing without a rocket trip. Kept for exactly that,
 * reviewed and confirmed still worth having at the M2.4 pass rather than removed.
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

    /**
     * Just above a dimension baseline, and far below sealed volumes.
     *
     * <p>Originally {@code OVERRIDE}, which was wrong for what this is for. This stands in for a
     * planet with no atmosphere, and a planet is something you are meant to be able to build a
     * pressurised room on. At {@code OVERRIDE} it beat sealed volumes, structures and vehicles,
     * which made every one of them impossible to test against it — the room worked and simply
     * could never win.
     *
     * <p>Offset from {@code DIMENSION} rather than equal to it so it does not tie with the water
     * provider, which sits on the same band. The 1000-spacing between bands exists precisely so
     * things can slot between them.
     */
    @Override
    public int priority() {
        return AtmospherePriority.DIMENSION + 100;
    }
}
