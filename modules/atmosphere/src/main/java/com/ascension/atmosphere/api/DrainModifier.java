package com.ascension.atmosphere.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Multiplies how fast a player consumes oxygen, based on what they are doing.
 *
 * <p>A registry rather than a fixed list, so the modules that own an activity own its cost:
 * thruster burns belong to the gear module, hostile-atmosphere exposure to the worlds module.
 * This module never learns what a thruster is.
 *
 * <p>No modifiers ship in v0.1. The seam exists; content arrives with {@code ascension-gear}
 * and {@code ascension-worlds}.
 *
 * <p>Note that sprinting is deliberately <em>not</em> a modifier anywhere in the project.
 * Every planet involves a great deal of walking, so taxing ordinary traversal reads as
 * friction rather than tension. Drain triggers should be discrete and meaningful.
 */
public interface DrainModifier {

    /** Stable id, shown by {@code /ascension atmosphere why}. */
    ResourceLocation id();

    /**
     * @return the multiplier to apply right now; {@code 1.0} means no effect
     */
    float multiplier(ServerPlayer player);
}
