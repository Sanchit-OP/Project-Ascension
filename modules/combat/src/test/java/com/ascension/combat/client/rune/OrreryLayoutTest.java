package com.ascension.combat.client.rune;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ascension.combat.client.rune.OrreryLayout.Point;
import org.junit.jupiter.api.Test;

/** Where things sit on the orrery (docs/technical/rune-system.md section 10). Pure geometry (ADR-0008). */
class OrreryLayoutTest {

    @Test
    void theTwoCombatKeystonesSitAboveTheHubAndTheWorldKeystoneBelow() {
        Point left = OrreryLayout.primaryKeystone(0, false, 0);
        Point right = OrreryLayout.primaryKeystone(1, false, 0);
        Point world = OrreryLayout.primaryKeystone(2, true, 0);
        assertTrue(left.y() < OrreryLayout.P_CY && right.y() < OrreryLayout.P_CY);
        assertTrue(left.x() < OrreryLayout.P_CX && right.x() > OrreryLayout.P_CX);
        assertTrue(world.y() > OrreryLayout.P_CY);
        assertEquals(OrreryLayout.P_CX, world.x());
    }

    @Test
    void theKeystonesSitOnTheirRing() {
        for (Point p : new Point[] {OrreryLayout.primaryKeystone(0, false, 0), OrreryLayout.primaryKeystone(1, false, 0),
                OrreryLayout.primaryKeystone(0, true, 0)}) {
            assertEquals(OrreryLayout.P_KEY, Math.hypot(p.x() - OrreryLayout.P_CX, p.y() - OrreryLayout.P_CY), 0.8);
        }
    }

    @Test
    void rowOneIsTheLeftHalfAndRowTwoTheRight() {
        for (int place = 0; place < 3; place++) {
            assertTrue(OrreryLayout.primaryMinor(0, place, 0).x() < OrreryLayout.P_CX);
            assertTrue(OrreryLayout.primaryMinor(1, place, 0).x() >= OrreryLayout.P_CX);
            assertTrue(OrreryLayout.secondaryMinor(0, place).x() < OrreryLayout.S_CX);
            assertTrue(OrreryLayout.secondaryMinor(1, place).x() >= OrreryLayout.S_CX);
        }
    }

    @Test
    void aTurnMovesTheSocketsRoundTheHubAndBackToWhereTheyWere() {
        Point rest = OrreryLayout.primaryMinor(0, 1, 0);
        Point turned = OrreryLayout.primaryMinor(0, 1, 90);
        assertFalse(rest.equals(turned));
        assertEquals(rest, OrreryLayout.primaryMinor(0, 1, 360));
        assertEquals(OrreryLayout.P_MINOR, Math.hypot(turned.x() - OrreryLayout.P_CX, turned.y() - OrreryLayout.P_CY), 0.8);
    }

    @Test
    void everythingFitsOnThePanel() {
        for (int row = 0; row < 2; row++) {
            for (int place = 0; place < 3; place++) {
                for (Point p : new Point[] {OrreryLayout.primaryMinor(row, place, 0), OrreryLayout.secondaryMinor(row, place),
                        OrreryLayout.shardMoon(place, row)}) {
                    assertTrue(p.x() > 10 && p.x() < OrreryLayout.PANEL_W - 10, "x " + p);
                    assertTrue(p.y() > 10 && p.y() < OrreryLayout.PANEL_H - 10, "y " + p);
                }
            }
        }
        for (int i = 0; i < 4; i++) {
            Point tab = OrreryLayout.primaryTab(i);
            assertTrue(tab.x() > 10 && tab.x() < 190 && tab.y() > 10 && tab.y() < OrreryLayout.PANEL_H - 10);
        }
        for (int i = 0; i < 3; i++) {
            assertTrue(OrreryLayout.secondaryTab(i).x() < OrreryLayout.PANEL_W - 10);
        }
    }

    @Test
    void theGrowthRingIsAThinCircleOfPixelsFilledFromTheTop() {
        var ring = OrreryLayout.growthRing();
        assertTrue(ring.size() > 60 && ring.size() < 140, "a thin ring: " + ring.size());
        long firstQuarter = ring.stream().filter(p -> p.angle() <= 90).count();
        assertEquals(ring.size() / 4.0, firstQuarter, ring.size() * 0.1);
        assertTrue(ring.stream().anyMatch(p -> p.angle() < 5), "starts at the top");
    }

    @Test
    void aPointInsideASocketIsFoundAndOneOutsideIsNot() {
        Point centre = new Point(50, 50);
        assertTrue(OrreryLayout.inside(centre, 15, 55, 58));
        assertFalse(OrreryLayout.inside(centre, 15, 70, 70));
    }
}
