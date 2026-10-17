package com.ascension.combat.internal.stat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ascension.combat.api.stat.CombatStats;
import org.junit.jupiter.api.Test;

/** How stats read as text on the sheet, in the command and in tooltips. Pure formatting. */
class StatTextTest {

    @Test
    void pointsDropTrailingZeros() {
        assertEquals("62", StatText.value(CombatStats.ARMOR, 62.0));
        assertEquals("9.3", StatText.value(CombatStats.ATTACK_DAMAGE, 9.33));
    }

    @Test
    void percentStatsReadAsPercentages() {
        assertEquals("8%", StatText.value(CombatStats.OMNIVAMP, 0.08));
        assertEquals("12.5%", StatText.value(CombatStats.ARMOR_PENETRATION, 0.125));
    }

    @Test
    void speedsReadAsTheSpeedItself() {
        assertEquals("1.6", StatText.value(CombatStats.ATTACK_SPEED, 1.6));
        assertEquals("0.115", StatText.value(CombatStats.MOVE_SPEED, 0.115));
    }

    @Test
    void sharesCarryASignOnlyWhenPositive() {
        assertEquals("+7", StatText.signedValue(CombatStats.ARMOR, 7.0));
        assertEquals("-2.4", StatText.signedValue(CombatStats.ATTACK_SPEED, -2.4));
        assertEquals("0", StatText.signedValue(CombatStats.ARMOR, 0.0));
    }

    @Test
    void writtenValuesReadTheWayDataWritesThem() {
        assertEquals("+15", StatText.written(CombatStats.ARMOR, 15));
        assertEquals("+8%", StatText.written(CombatStats.OMNIVAMP, 8));
        assertEquals("+20%", StatText.written(CombatStats.ATTACK_SPEED, 20));
        assertEquals("-10%", StatText.written(CombatStats.MOVE_SPEED, -10));
    }
}
