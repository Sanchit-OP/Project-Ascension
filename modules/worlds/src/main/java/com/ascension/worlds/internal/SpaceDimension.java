package com.ascension.worlds.internal;

import com.ascension.worlds.AscensionWorlds;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * Identity and constants for the one shared interplanetary space dimension (ADR-0010).
 *
 * <p>Defined here rather than left implicit in JSON alone because several pieces of code need to
 * recognise this specific dimension: ascent has to know where to send a player, arrival detection
 * has to know it is running, and the vertical-bound warning and void-damage suppression both need
 * to scope themselves to it and nothing else.
 */
public final class SpaceDimension {

    public static final ResourceKey<Level> KEY = ResourceKey.create(
            Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(AscensionWorlds.MOD_ID, "space"));

    /**
     * How far a player may go on either side of the planetary plane (y=0) before
     * {@link SpaceMechanics} tells them they have left the charted volume.
     *
     * <p><strong>Not a build limit.</strong> Minecraft does not clamp entity movement to a
     * dimension's declared height &mdash; only block placement and generation respect it &mdash;
     * so a player can fly straight past this on either axis with nothing stopping them. That is
     * deliberate: the warning is the only thing marking this boundary, by design (see
     * {@code plans/m2-worlds.md}'s M2.5 section) &mdash; no forced correction, the player stays in
     * full control.
     */
    public static final int VERTICAL_BOUND = 512;

    private SpaceDimension() {
    }
}
