package com.ascension.atmosphere.internal.sealed;

import com.ascension.atmosphere.api.AnchorSpace;
import com.ascension.atmosphere.api.Atmosphere;
import com.ascension.atmosphere.api.AtmosphereAnchors;
import net.minecraft.core.BlockPos;
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
        if (index.isPressurised(context.blockPos())) {
            return Optional.of(Atmosphere.BREATHABLE);
        }
        // A room inside a ship is stored in the ship's own space: ask whoever owns the ship where
        // this point is in it (ADR-0026). Usually no spaces are registered, so this is one empty loop.
        for (AnchorSpace space : AtmosphereAnchors.spaces()) {
            Optional<BlockPos> local = space.toLocal(context.level(), context.position());
            if (local.isPresent() && index.isPressurised(local.get())) {
                return Optional.of(Atmosphere.BREATHABLE);
            }
        }
        return Optional.empty();
    }

    @Override
    public int priority() {
        return AtmospherePriority.SEALED_VOLUME;
    }
}
