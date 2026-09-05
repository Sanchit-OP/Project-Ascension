package com.ascension.atmosphere.api;

import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * What a zone's coordinates are relative to.
 *
 * <p>Zones cannot always be stored in world coordinates, because some structures move. A
 * pressurised ship is still pressurised after it flies somewhere else, so its zone is stored
 * in the ship's own local space and translated on query.
 *
 * <p>This is the seam that makes moving-vehicle atmospheres possible without this module ever
 * depending on the mod that provides the movement. See ADR-0006.
 */
public sealed interface AtmosphereAnchor {

    /** The dimension this anchor lives in. */
    ResourceKey<Level> dimension();

    /** Fixed world space. Local coordinates are world coordinates. */
    record World(ResourceKey<Level> dimension) implements AtmosphereAnchor {
    }

    /**
     * A movable region within a dimension, identified by an opaque id owned by whoever provides
     * the region. Resolved through a registered {@link AnchorSpace}.
     */
    record Region(ResourceKey<Level> dimension, UUID regionId) implements AtmosphereAnchor {
    }
}
