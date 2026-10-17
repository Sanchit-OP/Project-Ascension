package com.ascension.combat.internal.rune;

/**
 * A guard against a rune triggering itself: some runes deal damage, or break blocks, or add effects,
 * and that in turn fires the very events they listen to. Wrap the doing in {@link #run} and test
 * {@link #active} at the top of the listener.
 *
 * <p>One instance per rune, in a static field: it holds a single flag, never an entity, and is only
 * ever used on the server thread, the one that deals damage and breaks blocks (ADR-0007). The flag
 * is cleared even if what runs throws.
 */
public final class Reentry {

    private boolean running;

    /** Whether this rune's own action is running right now. */
    public boolean active() {
        return running;
    }

    /** Runs {@code action} with {@link #active} true. */
    public void run(Runnable action) {
        running = true;
        try {
            action.run();
        } finally {
            running = false;
        }
    }
}
