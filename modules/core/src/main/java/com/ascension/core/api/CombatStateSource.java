package com.ascension.core.api;

import net.minecraft.server.level.ServerPlayer;

/**
 * Something that knows whether a player is in combat.
 *
 * <p>Register exactly one via {@link CombatStateRegistry#register}. {@code ascension-combat} is
 * expected to be that module &mdash; it is the one that actually listens for damage dealt and
 * received and decides what "in combat" means (a rolling window since the last combat action, not
 * a single tick). That is behaviour, and per ADR-0011 behaviour never lives in {@code core}, no
 * matter how small.
 *
 * <p>Called on the server thread while resolving. Keep it a lookup: read your own state, do not
 * touch the world.
 */
@FunctionalInterface
public interface CombatStateSource {

    /**
     * @param player the player being asked about
     * @return whether they are currently considered in combat
     */
    boolean isInCombat(ServerPlayer player);
}
