package com.ascension.combat.client.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StatFormatTest {

    @Test
    void pointsAreWholeNumbersExceptSmallFractions() {
        assertEquals("74", StatFormat.points(74.0));
        assertEquals("74", StatFormat.points(73.96));
        assertEquals("1", StatFormat.points(1.0));
        assertEquals("5.5", StatFormat.points(5.5));
        assertEquals("120", StatFormat.points(119.6));
    }

    @Test
    void attackSpeedHasTwoDecimals() {
        assertEquals("0.63", StatFormat.attackSpeed(0.63));
        assertEquals("4.00", StatFormat.attackSpeed(4.0));
    }

    @Test
    void percentIsAFractionTimesAHundred() {
        assertEquals("8%", StatFormat.percent(0.08));
        assertEquals("0%", StatFormat.percent(0.0));
    }

    @Test
    void moveSpeedIsTheBonusOverWalking() {
        assertEquals("0%", StatFormat.moveSpeed(0.1));
        assertEquals("+10%", StatFormat.moveSpeed(0.11));
        assertEquals("-15%", StatFormat.moveSpeed(0.085));
    }
}
