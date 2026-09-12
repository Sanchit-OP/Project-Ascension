package com.ascension.core.api;

import com.ascension.core.internal.CombatStateHolder;
import net.minecraft.server.level.ServerPlayer;

/**
 * Ask whether a player is currently in combat, or say that yours tracks it.
 *
 * <p>The meeting point for a concept more than one Tier 1 module needs (ADR-0011, ADR-0012):
 * {@code ascension-combat} determines what "in combat" means and updates it on damage dealt or
 * received; {@code ascension-atmosphere} reads it to drain oxygen faster mid-fight, without either
 * module depending on the other.
 *
 * <p>Register during mod setup, the same as {@link WorldEnvironmentRegistry}. Unlike that
 * registry, this is deliberately <strong>single-source</strong> &mdash; see
 * {@link CombatStateSource}. Whether a player is in combat is not a "several things might claim
 * this, highest priority wins" question the way atmosphere or world environment are; exactly one
 * module decides it.
 */
public final class CombatStateRegistry {

    private CombatStateRegistry() {
    }

    /**
     * Claim ownership of combat-state tracking.
     *
     * @throws IllegalStateException if a source is already registered
     */
    public static void register(CombatStateSource source) {
        CombatStateHolder.get().register(source);
    }

    /**
     * Whether this player is currently in combat, according to whoever tracks it.
     *
     * <p>{@code false} if nothing has registered &mdash; the safe default, since nothing should
     * escalate combat-only behaviour on a claim nobody made.
     */
    public static boolean isInCombat(ServerPlayer player) {
        return CombatStateHolder.get().isInCombat(player);
    }
}
