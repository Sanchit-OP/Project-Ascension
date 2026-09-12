package com.ascension.core.internal;

import com.ascension.core.api.CombatStateSource;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds the single registered combat-state source and answers queries against it.
 *
 * <p>Unlike {@link WorldEnvironments}, this is deliberately single-source: whether a player is
 * "in combat" is not something more than one module should be allowed to claim independently, the
 * way two mods might each own a slice of the dimension list. Exactly one module tracks it
 * ({@code ascension-combat}), and everyone else asks.
 *
 * <p>Holds no {@code Level}, {@code Player} or {@code BlockEntity} in a static field long-term;
 * {@link CombatStateSource} is handed a {@code ServerPlayer} per call and resolves its own data
 * (ADR-0007 rule 1).
 */
public final class CombatStateHolder {

    private static final Logger LOGGER = LoggerFactory.getLogger(CombatStateHolder.class);

    private static final CombatStateHolder INSTANCE = new CombatStateHolder();

    private volatile CombatStateSource source;

    /**
     * Package-private rather than private so tests can build their own instance, the same
     * reasoning {@link WorldEnvironments} gives for not exposing a production {@code reset()}.
     */
    CombatStateHolder() {
    }

    public static CombatStateHolder get() {
        return INSTANCE;
    }

    /**
     * @throws IllegalStateException if a source is already registered
     */
    public void register(CombatStateSource newSource) {
        if (source != null) {
            throw new IllegalStateException(
                    "Combat state source already registered. Only one module should track "
                            + "combat state; ascension-combat is expected to be that module.");
        }
        source = newSource;
        LOGGER.info("Combat state source registered");
    }

    /** {@code false} if nothing has registered yet &mdash; the safe default. */
    public boolean isInCombat(ServerPlayer player) {
        CombatStateSource snapshot = source;
        return snapshot != null && snapshot.isInCombat(player);
    }
}
