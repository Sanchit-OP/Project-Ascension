package com.ascension.hud.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RegenForecastTest {

    @Test
    void noEffectMeansNothing() {
        assertEquals(0f, RegenForecast.pending(0, 0));
        assertEquals(0f, RegenForecast.pending(-5, 3));
    }

    @Test
    void levelOneHealsOnePerFiftyTicks() {
        assertEquals(2f, RegenForecast.pending(100, 0));
    }

    @Test
    void higherLevelsHealFaster() {
        // Level II: every 25 ticks. Level III: every 12.
        assertEquals(4f, RegenForecast.pending(100, 1));
        assertEquals(8f, RegenForecast.pending(100, 2));
    }

    @Test
    void shortEffectsOnlyCountWhatIsLeft() {
        assertEquals(1f, RegenForecast.pending(50, 0));
    }

    @Test
    void longEffectsAreCappedAtTheHorizon() {
        assertEquals(RegenForecast.pending(100, 0), RegenForecast.pending(6000, 0));
    }

    @Test
    void extremeAmplifiersDoNotBreak() {
        assertEquals(100f, RegenForecast.pending(100, 99));
    }
}
