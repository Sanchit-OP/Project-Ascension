package com.ascension.atmosphere.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * The position being asked about.
 *
 * <p>Holds a live {@link ServerLevel} reference and is therefore <strong>strictly
 * short-lived</strong>. Providers must answer from it and discard it. Storing a context, or the
 * level inside it, in any field that outlives the call pins a dimension in memory after unload
 * and is forbidden by ADR-0007 rule 1.
 *
 * @param level    the level being queried
 * @param position exact position, for providers that care about sub-block precision
 */
public record AtmosphereContext(ServerLevel level, Vec3 position) {

    /** Block containing {@link #position}. */
    public BlockPos blockPos() {
        return BlockPos.containing(position);
    }
}
