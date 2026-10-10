package com.ascension.worlds.api;

import net.minecraft.server.level.ServerPlayer;

/**
 * A vehicle that carries its riders across the line between a planet's surface and space itself,
 * crew and all, so this module must not move them on its own: a player riding a ship up past the
 * build height stays aboard and goes with the ship, rather than leaving alone a moment before it.
 *
 * <p>Registered through {@link SpaceTravel#registerCarrier}. Registering one also tells this module
 * that a way to space exists other than flying there on foot, which is what lets the
 * {@code vehicleOnly} server setting close the on-foot route without stranding anyone.
 */
@FunctionalInterface
public interface AscentCarrier {

    /**
     * True when {@code player} rides a vehicle this carrier moves across planet boundaries.
     *
     * <p>Asked only while a player is at a boundary (at or above a planet's build height, or inside
     * a planet's body in space), never every tick for everyone, so it may look around a little.
     */
    boolean carries(ServerPlayer player);
}
