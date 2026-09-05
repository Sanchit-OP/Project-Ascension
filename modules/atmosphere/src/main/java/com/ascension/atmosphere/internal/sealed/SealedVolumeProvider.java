package com.ascension.atmosphere.internal.sealed;

import com.ascension.atmosphere.api.Atmosphere;
import com.ascension.atmosphere.api.AtmosphereContext;
import com.ascension.atmosphere.api.AtmosphereProvider;
import com.ascension.atmosphere.api.AtmospherePriority;
import com.ascension.atmosphere.internal.AtmosphereAttachments;
import java.util.Optional;

/**
 * Claims breathable air inside a pressurised room.
 *
 * <p>Sits at the {@code SEALED_VOLUME} band, above dimension baselines and structures but below
 * vehicles, so a sealed room beats the vacuum outside it and a ship interior would beat both.
 *
 * <p>Abstains the moment there are no emitters in the level, which keeps the common case &mdash;
 * every player who has not built one &mdash; down to a single map lookup.
 */
public final class SealedVolumeProvider implements AtmosphereProvider {

    @Override
    public Optional<Atmosphere> query(AtmosphereContext context) {
        SealedVolumeIndex index = context.level().getData(AtmosphereAttachments.SEALED_VOLUMES);
        if (index.isEmpty()) {
            return Optional.empty();
        }
        return index.isPressurised(context.blockPos())
                ? Optional.of(Atmosphere.BREATHABLE)
                : Optional.empty();
    }

    @Override
    public int priority() {
        return AtmospherePriority.SEALED_VOLUME;
    }
}
