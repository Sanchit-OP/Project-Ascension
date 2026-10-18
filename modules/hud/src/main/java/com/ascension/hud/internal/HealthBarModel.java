package com.ascension.hud.internal;

/**
 * What the health bar shows, worked out from health, absorption and a regeneration forecast.
 * Pure arithmetic with no Minecraft types, so a unit test can decide it (ADR-0008).
 *
 * <p>Left to right along the bar: green is health now, white is the shield (absorption), grey is
 * health about to regenerate, red is health just lost. The red part is remembered here: when
 * health drops it holds for a moment, then drains away.
 *
 * <p>One instance, mutated in place each game tick, so a frame allocates nothing (ADR-0007). It
 * holds numbers only, never a player.
 */
public final class HealthBarModel {

    /** Ticks the red part stays put after a hit before it starts to drain. */
    static final int HOLD_TICKS = 12;

    /** Share of the red part that drains per tick once it starts. */
    static final float DRAIN_PER_TICK = 0.12f;

    private float max = 20.0f;
    private float current;
    private float shield;
    private float regen;
    private float lostTop;

    private float previous = -1.0f;
    private long holdUntil;
    private long lastTick = Long.MIN_VALUE;

    /**
     * Call once per frame with the current values; it only advances the drain when the game tick
     * has changed, so frame rate does not change how long a hit stays visible.
     *
     * @param regenForecast health due back from a Regeneration effect, see {@link RegenForecast}
     */
    public void update(float health, float maxHealth, float absorption, float regenForecast, long gameTick) {
        max = Math.max(1.0f, maxHealth);
        current = clamp(health, 0.0f, max);
        shield = clamp(absorption, 0.0f, max - current);
        regen = clamp(regenForecast, 0.0f, max - current - shield);

        if (previous >= 0.0f && current < previous - 0.01f) {
            lostTop = Math.max(lostTop, previous);
            holdUntil = gameTick + HOLD_TICKS;
        }
        if (gameTick != lastTick) {
            lastTick = gameTick;
            if (gameTick > holdUntil) {
                lostTop -= (lostTop - current) * DRAIN_PER_TICK;
            }
        }
        if (lostTop < current + 0.05f) {
            lostTop = current;
        }
        previous = current;
    }

    /** Forget any red part, for a respawn or a new world. */
    public void reset() {
        previous = -1.0f;
        lostTop = 0.0f;
        holdUntil = 0L;
        lastTick = Long.MIN_VALUE;
    }

    public float max() {
        return max;
    }

    public float current() {
        return current;
    }

    public float shield() {
        return shield;
    }

    public float regen() {
        return regen;
    }

    /** Length of the red part, in health. Zero when nothing was lost recently. */
    public float lost() {
        return Math.min(lostTop - current, max - current - shield - regen);
    }

    // Fractions of the bar where each part ends, so drawing is one multiply per edge.

    public float greenEnd() {
        return current / max;
    }

    public float whiteEnd() {
        return (current + shield) / max;
    }

    public float greyEnd() {
        return (current + shield + regen) / max;
    }

    public float redEnd() {
        return (current + shield + regen + Math.max(0.0f, lost())) / max;
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, Math.max(lo, v)));
    }
}
