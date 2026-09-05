package com.ascension.atmosphere.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Adds to a player's built-in lung reserve.
 *
 * <p>This is how breathing gear works: a helmet enchantment, a turtle shell, a sealed suit.
 * They make the reserve you carry in your own body larger.
 *
 * <p><strong>Why capacity and not a drain reduction.</strong> Both would make a full bar last
 * longer, but a drain reduction slows consumption of <em>everything</em>, including carried
 * tanks &mdash; so a helmet enchantment would quietly stretch a tank's duration too. Growing
 * lungs keeps the two supplies independent: gear improves the reserve that regenerates for
 * free, and tanks stay exactly as big as the tank you built.
 *
 * <p>Register through {@link AtmosphereRegistry}. Called during oxygen accounting on the server
 * thread, so keep it cheap: read equipment, do not scan the world.
 */
@FunctionalInterface
public interface LungCapacityModifier {

    /** Stable id, shown by {@code /ascension atmosphere why}. */
    default ResourceLocation id() {
        return null;
    }

    /**
     * Extra lung units this modifier grants right now.
     *
     * @return additional units, or {@code 0} for no effect. Never negative.
     */
    int bonusUnits(ServerPlayer player);
}
