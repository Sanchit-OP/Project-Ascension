package com.ascension.combat.internal.mechanics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The numbers behind the magic-hit screen tint: small, quick, and never more than asked for. */
class MagicHitPulseTest {

    @Test
    void aHitThatDidNothingShowsNothing() {
        assertEquals(0.0f, MagicHitPulse.strengthFor(0.0f, 20.0f));
        assertEquals(0.0f, MagicHitPulse.strengthFor(5.0f, 0.0f));
    }

    @Test
    void aScratchIsStillSeenButNotLouderThanAFullHit() {
        assertEquals(MagicHitPulse.MIN_STRENGTH, MagicHitPulse.strengthFor(0.1f, 20.0f));
        assertEquals(1.0f, MagicHitPulse.strengthFor(20.0f, 20.0f));
        assertEquals(1.0f, MagicHitPulse.strengthFor(5.0f, 20.0f), "a quarter of health is already the strongest");
        assertTrue(MagicHitPulse.strengthFor(2.0f, 20.0f) < MagicHitPulse.strengthFor(4.0f, 20.0f));
    }

    @Test
    void theTintStartsAtItsPeakAndFadesToNothing() {
        float peak = MagicHitPulse.alpha(0.0f, 1.0f, 1.0f);
        assertEquals(MagicHitPulse.PEAK_ALPHA, peak, 1e-6f);
        float previous = peak;
        for (int tick = 1; tick < MagicHitPulse.DURATION_TICKS; tick++) {
            float now = MagicHitPulse.alpha(tick, 1.0f, 1.0f);
            assertTrue(now < previous, "fades every tick");
            previous = now;
        }
        assertEquals(0.0f, MagicHitPulse.alpha(MagicHitPulse.DURATION_TICKS, 1.0f, 1.0f));
        assertEquals(0.0f, MagicHitPulse.alpha(-1.0f, 1.0f, 1.0f), "nothing before the hit");
    }

    @Test
    void theTintIsNeverMoreThanAFifthOpaque() {
        assertTrue(MagicHitPulse.alpha(0.0f, 5.0f, 5.0f) <= 0.20f + 1e-6f, "out-of-range inputs are clamped");
    }

    @Test
    void theSettingScalesItAndZeroTurnsItOff() {
        assertEquals(MagicHitPulse.alpha(0.0f, 1.0f, 1.0f) / 2, MagicHitPulse.alpha(0.0f, 1.0f, 0.5f), 1e-6f);
        assertEquals(0.0f, MagicHitPulse.alpha(0.0f, 1.0f, 0.0f));
    }

    @Test
    void theBandIsFaintestInward() {
        assertEquals(1.0f, MagicHitPulse.falloff(0));
        for (int band = 1; band < MagicHitPulse.BANDS; band++) {
            assertTrue(MagicHitPulse.falloff(band) < MagicHitPulse.falloff(band - 1));
        }
        assertEquals(0.0f, MagicHitPulse.falloff(MagicHitPulse.BANDS));
    }
}
