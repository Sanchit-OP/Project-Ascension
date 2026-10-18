package com.ascension.combat.client.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StatTrendTest {

    @Test
    void startsQuiet() {
        StatTrend t = new StatTrend();
        t.update(10, 0);
        assertEquals(0, t.direction(0));
    }

    @Test
    void statsArrivingAfterLoginDoNotFlash() {
        StatTrend t = new StatTrend();
        t.update(1, 0);
        t.update(74, 5);
        assertEquals(0, t.direction(6));
    }

    @Test
    void aRiseIsGreenForThreeSeconds() {
        StatTrend t = new StatTrend();
        t.update(10, 0);
        long tick = StatTrend.SETTLE_TICKS + 10;
        t.update(15, tick);
        assertEquals(1, t.direction(tick));
        assertEquals(1, t.direction(tick + StatTrend.HOLD_TICKS - 1));
        assertEquals(0, t.direction(tick + StatTrend.HOLD_TICKS));
    }

    @Test
    void aFallIsRed() {
        StatTrend t = new StatTrend();
        t.update(10, 0);
        long tick = StatTrend.SETTLE_TICKS + 10;
        t.update(4, tick);
        assertEquals(-1, t.direction(tick));
    }

    @Test
    void anUnchangedValueNeverColours() {
        StatTrend t = new StatTrend();
        t.update(10, 0);
        for (long tick = 50; tick < 300; tick++) {
            t.update(10, tick);
            assertEquals(0, t.direction(tick));
        }
    }

    @Test
    void resetQuietsAndRearmsTheSettlePeriod() {
        StatTrend t = new StatTrend();
        t.update(10, 0);
        t.update(15, 100);
        t.reset();
        assertEquals(0, t.direction(100));
        t.update(1, 200);
        t.update(50, 205);
        assertEquals(0, t.direction(206));
    }
}
