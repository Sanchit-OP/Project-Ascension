package com.ascension.hud.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HealthBarModelTest {

    private static final float EPS = 0.001f;

    @Test
    void partsRunGreenWhiteGreyRed() {
        HealthBarModel m = new HealthBarModel();
        m.update(300, 640, 60, 20, 0);
        assertEquals(300f / 640f, m.greenEnd(), EPS);
        assertEquals(360f / 640f, m.whiteEnd(), EPS);
        assertEquals(380f / 640f, m.greyEnd(), EPS);
        assertEquals(380f / 640f, m.redEnd(), EPS);
    }

    @Test
    void aDropLeavesARedPartThatHoldsThenDrains() {
        HealthBarModel m = new HealthBarModel();
        m.update(300, 640, 0, 0, 0);
        m.update(200, 640, 0, 0, 1);
        assertEquals(100f, m.lost(), 0.5f);

        // Held for HOLD_TICKS.
        m.update(200, 640, 0, 0, 1 + HealthBarModel.HOLD_TICKS);
        assertEquals(100f, m.lost(), 0.5f);

        // Then it drains, and eventually is gone.
        long tick = 1 + HealthBarModel.HOLD_TICKS;
        float before = m.lost();
        m.update(200, 640, 0, 0, ++tick);
        assertTrue(m.lost() < before);
        for (int i = 0; i < 200; i++) {
            m.update(200, 640, 0, 0, ++tick);
        }
        assertEquals(0f, m.lost(), EPS);
    }

    @Test
    void drainDependsOnGameTicksNotFrames() {
        HealthBarModel m = new HealthBarModel();
        m.update(300, 640, 0, 0, 0);
        m.update(200, 640, 0, 0, 1);
        long tick = 1 + HealthBarModel.HOLD_TICKS + 1;
        m.update(200, 640, 0, 0, tick);
        float once = m.lost();
        // Many frames in the same tick must not drain further.
        for (int i = 0; i < 50; i++) {
            m.update(200, 640, 0, 0, tick);
        }
        assertEquals(once, m.lost(), EPS);
    }

    @Test
    void healingClearsTheRedPart() {
        HealthBarModel m = new HealthBarModel();
        m.update(300, 640, 0, 0, 0);
        m.update(200, 640, 0, 0, 1);
        m.update(400, 640, 0, 0, 2);
        assertEquals(0f, m.lost(), EPS);
    }

    @Test
    void everythingStaysInsideTheBar() {
        HealthBarModel m = new HealthBarModel();
        m.update(600, 640, 500, 500, 0);
        assertTrue(m.redEnd() <= 1.0f + EPS);
        assertTrue(m.greyEnd() <= 1.0f + EPS);
        assertEquals(1.0f, m.whiteEnd(), EPS);
    }

    @Test
    void worksFromTwentyToAThousand() {
        HealthBarModel m = new HealthBarModel();
        m.update(10, 20, 0, 0, 0);
        assertEquals(0.5f, m.greenEnd(), EPS);
        m.update(500, 1000, 0, 0, 0);
        assertEquals(0.5f, m.greenEnd(), EPS);
    }

    @Test
    void resetForgetsTheRedPart() {
        HealthBarModel m = new HealthBarModel();
        m.update(300, 640, 0, 0, 0);
        m.update(100, 640, 0, 0, 1);
        m.reset();
        m.update(640, 640, 0, 0, 2);
        assertEquals(0f, m.lost(), EPS);
    }
}
