package com.ascension.combat.internal.armor;

/**
 * Unending Despair's "how long has this been the same fight" tracker (docs/technical/combat-api.md
 * section 6), attached to the wearer. Genuinely different shape from every other duration state in
 * this catalog: it needs to know not just whether combat is currently active but for how long it
 * has been continuous, and then gate a repeating cadence on top of that once a threshold is
 * crossed &mdash; {@link com.ascension.combat.internal.mechanics.DurationState}'s "active until
 * one instant" doesn't fit, and neither does {@link
 * com.ascension.combat.internal.armor.ProteanMaskState}'s capped stack counter. Only one consumer
 * so far, so this stays local to the armor package rather than moving to {@code
 * internal.mechanics}.
 */
public final class UnendingDespairState {

    private long combatStartGameTime;
    private long nextPulseGameTime;

    public void onInCombat(long now) {
        if (combatStartGameTime == 0L) {
            combatStartGameTime = now;
        }
    }

    public void onOutOfCombat() {
        combatStartGameTime = 0L;
        nextPulseGameTime = 0L;
    }

    /**
     * Call once per tick while the wearer is in combat. Returns {@code true} exactly on the tick
     * a pulse should fire: the fight has been continuous for at least {@code thresholdTicks}, and
     * either no pulse has fired yet (the first one is scheduled {@code intervalTicks} out, not
     * instant) or the previous pulse's interval has elapsed.
     */
    public boolean tryPulse(long now, long thresholdTicks, long intervalTicks) {
        if (combatStartGameTime == 0L || now - combatStartGameTime < thresholdTicks) {
            return false;
        }

        if (nextPulseGameTime == 0L) {
            nextPulseGameTime = now + intervalTicks;
            return false;
        }

        if (now < nextPulseGameTime) {
            return false;
        }

        nextPulseGameTime = now + intervalTicks;
        return true;
    }
}
