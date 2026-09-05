package com.ascension.atmosphere.internal.supply;

import com.ascension.atmosphere.internal.AtmosphereTuning;

/**
 * The per-slot decision behind {@link TankRules#enforce}, with no {@code Inventory} or
 * {@code ItemStack} in it.
 *
 * <p>Extracted in the M2.4 pass. {@link TankRules#enforce} still walks the real inventory in slot
 * order and still does the dropping and the component writes &mdash; those need a
 * {@code ServerPlayer} and stay there. What moves here is the part that was actually worth
 * getting wrong: how many tanks survive, which one keeps its valve open, and in what order.
 *
 * <p>One instance per sweep, fed one tank at a time in slot order. Order matters and is the
 * caller's to preserve &mdash; this class only ever sees "the next tank in inventory order",
 * never a slot number.
 */
final class TankCarrySweep {

    /** What to do with the tank just reported to {@link #next}. */
    enum Action {
        /** Stays exactly as it is. */
        KEEP,
        /** Stays in the inventory, but its valve is forced shut. */
        KEEP_CLOSED,
        /** Over the carry limit. Leaves the inventory. */
        DROP
    }

    private int kept;
    private boolean sawOpen;

    /**
     * Decide the next tank's fate, given only whether its valve is currently open.
     *
     * <p>The cap is checked before the valve, deliberately: a carried limit is about how many
     * tanks exist, not about which ones are open, so the count that decides {@code DROP} must not
     * itself depend on {@code isOpen}.
     */
    Action next(boolean isOpen) {
        if (kept >= AtmosphereTuning.MAX_TANKS_CARRIED) {
            return Action.DROP;
        }
        kept++;
        if (!isOpen) {
            return Action.KEEP;
        }
        // Two open valves would double the supply for free. First one seen wins; every later
        // open tank in the same sweep is forced shut.
        if (sawOpen) {
            return Action.KEEP_CLOSED;
        }
        sawOpen = true;
        return Action.KEEP;
    }
}
