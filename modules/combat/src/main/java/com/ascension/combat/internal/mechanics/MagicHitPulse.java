package com.ascension.combat.internal.mechanics;

/**
 * The numbers behind the faint screen-edge tint a player sees when a magic hit lands on them
 * (docs/technical/damage-kinds.md). Plain arithmetic with no Minecraft types, so it is tested
 * without a game; the layer that draws it is {@code client.hud.MagicHitLayer}.
 *
 * <p>Deliberately small: a thin band at the very edge of the screen, at most a fifth opaque, gone in
 * half a second. It is there to be noticed in passing, not to cover what the player is looking at.
 */
public final class MagicHitPulse {

    /** How long a pulse lasts, in ticks (half a second). */
    public static final int DURATION_TICKS = 10;

    /** Opacity of the outermost strip at the start of the strongest pulse. */
    public static final float PEAK_ALPHA = 0.20f;

    /** The weakest pulse, so even a scratch of magic is seen. */
    public static final float MIN_STRENGTH = 0.35f;

    /** A hit of this share of the target's health or more is a full-strength pulse. */
    public static final float FULL_STRENGTH_SHARE = 0.25f;

    /** How far in the band reaches, as a share of the screen's height. */
    public static final float THICKNESS_FRACTION = 0.05f;

    /** The band is drawn as this many nested strips, each fainter than the one outside it. */
    public static final int BANDS = 10;

    private MagicHitPulse() {
    }

    /**
     * How strong the pulse for one hit is, 0 to 1: bigger hits show a little more, never less than
     * {@link #MIN_STRENGTH}, and nothing at all for a hit that did no damage.
     */
    public static float strengthFor(float damage, float maxHealth) {
        if (damage <= 0.0f || maxHealth <= 0.0f) {
            return 0.0f;
        }
        float share = damage / maxHealth;
        return Math.min(1.0f, Math.max(MIN_STRENGTH, share / FULL_STRENGTH_SHARE));
    }

    /**
     * Opacity of the outermost strip {@code ageTicks} after the hit: eases out, so the tint is there at once and
     * fades fast. Zero before the hit and from {@link #DURATION_TICKS} on.
     *
     * @param strength the pulse's own strength from {@link #strengthFor}
     * @param setting  the player's setting, 0 (off) to 1
     */
    public static float alpha(float ageTicks, float strength, float setting) {
        if (ageTicks < 0.0f || ageTicks >= DURATION_TICKS || strength <= 0.0f || setting <= 0.0f) {
            return 0.0f;
        }
        float left = 1.0f - ageTicks / DURATION_TICKS;
        return PEAK_ALPHA * Math.min(1.0f, strength) * Math.min(1.0f, setting) * left * left;
    }

    /** How much of the outermost strip's opacity strip {@code band} keeps: 1 at the edge, falling to nothing inward. */
    public static float falloff(int band) {
        if (band < 0 || band >= BANDS) {
            return 0.0f;
        }
        float inside = 1.0f - (float) band / BANDS;
        return inside * inside;
    }
}
