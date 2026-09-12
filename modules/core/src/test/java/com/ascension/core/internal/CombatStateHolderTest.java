package com.ascension.core.internal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ascension.core.api.CombatStateSource;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the shared combat-state holder.
 *
 * <p>Per ADR-0008, appropriate here for the same reason {@code WorldEnvironmentsTest} is: this is
 * a field assignment and a null check, decidable without a running game. The fake sources below
 * never touch their {@code ServerPlayer} argument, so a real one is never needed &mdash; {@code
 * null} stands in for it, which is fine since {@link CombatStateHolder} only ever passes it
 * through opaquely.
 */
class CombatStateHolderTest {

    private static final CombatStateSource ALWAYS_IN_COMBAT = player -> true;
    private static final CombatStateSource NEVER_IN_COMBAT = player -> false;

    @Test
    void falseWhenNothingIsRegistered() {
        CombatStateHolder holder = new CombatStateHolder();

        assertFalse(holder.isInCombat(null));
    }

    @Test
    void answersAccordingToTheRegisteredSource() {
        CombatStateHolder holder = new CombatStateHolder();
        holder.register(ALWAYS_IN_COMBAT);

        assertTrue(holder.isInCombat(null));
    }

    @Test
    void registeringASecondSourceThrows() {
        CombatStateHolder holder = new CombatStateHolder();
        holder.register(NEVER_IN_COMBAT);

        assertThrows(IllegalStateException.class, () -> holder.register(ALWAYS_IN_COMBAT));
    }

    @Test
    void aRejectedSecondRegistrationDoesNotReplaceTheFirst() {
        CombatStateHolder holder = new CombatStateHolder();
        holder.register(NEVER_IN_COMBAT);

        assertThrows(IllegalStateException.class, () -> holder.register(ALWAYS_IN_COMBAT));

        // The failed registration must not have partially applied.
        assertFalse(holder.isInCombat(null));
    }
}
