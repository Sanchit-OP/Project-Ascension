package com.ascension.combat.internal.weapons;

/**
 * Ragefang's attack-speed build-up (docs/technical/combat-api.md section 6): unlike {@link
 * com.ascension.combat.internal.mechanics.StackingChargeState}'s hard "reset to zero the instant
 * it expires" shape, this decays continuously &mdash; a grace period with no decay at all, then a
 * linear ramp down to zero &mdash; so it needed its own state rather than reusing that class.
 * Also carries the separate every-third-hit counter for the doubled on-hit magic proc, since both
 * live on the same wielder and get touched by the same hit.
 */
public final class RagefangState {

    private float attackSpeedStacksAtLastHit;
    private long lastHitGameTime;
    private int hitsSinceDoubleProc;

    /** Bakes in whatever has already decayed, adds one stack, and resets the decay clock. */
    public void recordHit(long now, long graceTicks, long decayTicks) {
        attackSpeedStacksAtLastHit = currentAttackSpeedStacks(now, graceTicks, decayTicks) + 1.0f;
        lastHitGameTime = now;
    }

    /** The live, possibly partially-decayed stack value at {@code now}. */
    public float currentAttackSpeedStacks(long now, long graceTicks, long decayTicks) {
        long elapsed = now - lastHitGameTime;
        if (elapsed <= graceTicks) {
            return attackSpeedStacksAtLastHit;
        }

        long intoDecay = elapsed - graceTicks;
        if (intoDecay >= decayTicks) {
            return 0.0f;
        }

        float remainingFraction = 1.0f - (float) intoDecay / (float) decayTicks;
        return attackSpeedStacksAtLastHit * remainingFraction;
    }

    public boolean isAtRest() {
        return attackSpeedStacksAtLastHit <= 0.0f;
    }

    public void clearAttackSpeed() {
        attackSpeedStacksAtLastHit = 0.0f;
    }

    /** Every third call returns true and resets the count; the other two return false. */
    public boolean everyThirdHit() {
        hitsSinceDoubleProc++;
        if (hitsSinceDoubleProc >= 3) {
            hitsSinceDoubleProc = 0;
            return true;
        }
        return false;
    }
}
