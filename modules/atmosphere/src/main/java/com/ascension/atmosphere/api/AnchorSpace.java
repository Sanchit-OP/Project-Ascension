package com.ascension.atmosphere.api;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Translates world positions into an anchor's local block space.
 *
 * <p>Implemented by whoever owns a moving structure. This module deliberately has no idea how
 * that movement works; it only knows that some anchors need someone else to do the maths.
 *
 * <p>{@link AtmosphereAnchor.World} anchors resolve as identity and need no implementation.
 */
@FunctionalInterface
public interface AnchorSpace {

    /**
     * @return the position in local block space, or empty if it lies outside this anchor
     */
    Optional<BlockPos> toLocal(Vec3 worldPosition);

    /**
     * The same, told which level is being asked about. Added for ADR-0026 (ships): an implementation
     * that needs the level, as Sable's ships do, overrides this one. The default ignores the level.
     * The level is for the call only and must not be stored (ADR-0007).
     */
    default Optional<BlockPos> toLocal(ServerLevel level, Vec3 worldPosition) {
        return toLocal(worldPosition);
    }
}
