package com.ascension.atmosphere.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The arithmetic that decides how fast a player dies.
 *
 * <p>Every one of these numbers has been shipping since M1.5 and none had ever been checked
 * except by standing in a vacuum and counting. The extraction that made this file possible was
 * the point of the M2.4 pass: this is not world state, it is subtraction, and ADR-0008 has no
 * objection to subtraction being tested cheaply.
 */
class OxygenAccountingTest {

    private static final float BASELINE = AtmosphereTuning.BASE_UNITS_PER_SECOND;

    // --- drain, and the fraction carried between passes ----------------------

    @Test
    @DisplayName("baseline drain costs exactly its per-second rate over two passes")
    void baselineDrainOverOneSecond() {
        OxygenAccounting.Debt first = OxygenAccounting.debt(BASELINE, 0.0f);
        OxygenAccounting.Debt second = OxygenAccounting.debt(BASELINE, first.carry());

        assertEquals(AtmosphereTuning.BASE_UNITS_PER_SECOND, first.units() + second.units(),
                "two accounting passes are one second and must cost one second of air");
    }

    /**
     * The reason the carry exists at all. A gentle drain owes less than a whole unit per pass;
     * truncating each pass would make it free, and rounding up would make it cost four times its
     * rate. Neither is a multiplier, and modifiers are advertised as multipliers.
     */
    @Test
    @DisplayName("a fractional drain accumulates instead of vanishing")
    void fractionalDrainAccumulates() {
        float gentle = 0.5f;
        int spent = 0;
        float carry = 0.0f;
        for (int pass = 0; pass < 40; pass++) {
            OxygenAccounting.Debt debt = OxygenAccounting.debt(gentle, carry);
            spent += debt.units();
            carry = debt.carry();
        }
        // 40 passes is 20 seconds; half a unit per second is 10 units.
        assertEquals(10, spent);
    }

    @Test
    @DisplayName("a drain too small to cost anything in one pass still costs eventually")
    void tinyDrainIsNotFree() {
        float trickle = 0.1f;
        int spent = 0;
        float carry = 0.0f;
        for (int pass = 0; pass < 200; pass++) {
            OxygenAccounting.Debt debt = OxygenAccounting.debt(trickle, carry);
            spent += debt.units();
            carry = debt.carry();
        }
        // 200 passes is 100 seconds at 0.1/s.
        assertEquals(10, spent);
    }

    @Test
    @DisplayName("the carry is always a fraction, never a whole unit waiting to be lost")
    void carryStaysFractional() {
        float carry = 0.0f;
        for (float drain : new float[] {0.1f, 0.5f, 1.0f, BASELINE, 7.3f, 100.0f}) {
            for (int pass = 0; pass < 50; pass++) {
                OxygenAccounting.Debt debt = OxygenAccounting.debt(drain, carry);
                carry = debt.carry();
                assertTrue(carry >= 0.0f && carry < 1.0f,
                        "carry escaped [0,1) at drain " + drain + ": " + carry);
            }
        }
    }

    /**
     * Reaching air clears the debt. Otherwise a player who surfaced mid-fraction would owe it on
     * their next dive, minutes or hours later, which is not a rule anyone could ever observe and
     * therefore not a rule worth having.
     */
    @Test
    @DisplayName("breathable air clears the carry rather than banking it")
    void breathableAirClearsTheCarry() {
        OxygenAccounting.Debt owed = OxygenAccounting.debt(0.9f, 0.0f);
        assertTrue(owed.carry() > 0.0f, "precondition: this drain leaves a fraction owing");

        OxygenAccounting.Debt safe = OxygenAccounting.debt(0.0f, owed.carry());
        assertEquals(0, safe.units());
        assertEquals(0.0f, safe.carry());
    }

    @Test
    @DisplayName("a full tank lasts its advertised five minutes at baseline drain")
    void aTankLastsFiveMinutes() {
        int units = AtmosphereTuning.TANK_CAPACITY;
        float carry = 0.0f;
        int passes = 0;
        while (units > 0) {
            OxygenAccounting.Debt debt = OxygenAccounting.debt(BASELINE, carry);
            units -= debt.units();
            carry = debt.carry();
            passes++;
        }
        float seconds = passes * AtmosphereTuning.accountingSeconds();
        assertEquals(300.0f, seconds, 1.0f,
                "the tank tooltip promises five minutes; the drain loop has to deliver it");
    }

    // --- lungs ---------------------------------------------------------------

    @Test
    @DisplayName("lungs fill from empty in about four seconds")
    void lungsRefillInAboutFourSeconds() {
        int capacity = AtmosphereTuning.LUNG_CAPACITY;
        int units = 0;
        int passes = 0;
        while (units < capacity) {
            units = OxygenAccounting.lungsAfterRefill(units, capacity);
            passes++;
        }
        float seconds = passes * AtmosphereTuning.accountingSeconds();
        assertTrue(seconds <= 6.0f, "refill took " + seconds + "s; it should feel like surfacing");
        assertTrue(seconds >= 1.0f, "refill took " + seconds + "s; that is not a breath, that is a tap");
    }

    @Test
    @DisplayName("refilling never overfills")
    void refillStopsAtCapacity() {
        assertEquals(80, OxygenAccounting.lungsAfterRefill(79, 80));
        assertEquals(80, OxygenAccounting.lungsAfterRefill(80, 80));
    }

    /**
     * A refill rate that rounds to zero would leave a player one unit short forever, failing the
     * tracker fast path on every pass and resyncing twice a second for the rest of the session.
     */
    @Test
    @DisplayName("a player below capacity always gains at least one unit")
    void refillAlwaysMakesProgress() {
        for (int capacity : new int[] {1, 2, 80, 4000}) {
            for (int current = 0; current < capacity; current++) {
                assertTrue(OxygenAccounting.lungsAfterRefill(current, capacity) > current,
                        "stalled at " + current + "/" + capacity);
            }
        }
    }

    @Test
    @DisplayName("over-full lungs are left for the caller to trim, not trimmed twice")
    void refillLeavesOverfullLungsAlone() {
        assertEquals(120, OxygenAccounting.lungsAfterRefill(120, 80));
    }

    // --- the failure window --------------------------------------------------

    @Test
    @DisplayName("any air at all resets the failure window")
    void airResetsTheWindow() {
        assertEquals(0, OxygenAccounting.suffocationTicksAfter(0, true));
        assertEquals(0, OxygenAccounting.suffocationTicksAfter(200, true));
    }

    @Test
    @DisplayName("the window advances one accounting pass at a time")
    void windowAdvancesByOnePass() {
        assertEquals(AtmosphereTuning.ACCOUNTING_INTERVAL_TICKS,
                OxygenAccounting.suffocationTicksAfter(0, false));
    }

    /**
     * ADR-0008 aside, this is the number a player feels: run out of air and you get a warning
     * before you get hurt. Long enough to turn and run for a door, short enough that ignoring the
     * bar kills you.
     */
    @Test
    @DisplayName("running out of air warns first and hurts after two seconds")
    void gracePrecedesDamage() {
        int ticks = 0;
        int passesBeforeDamage = 0;
        while (!OxygenAccounting.damageDue(ticks)) {
            ticks = OxygenAccounting.suffocationTicksAfter(ticks, false);
            passesBeforeDamage++;
        }
        float seconds = (passesBeforeDamage - 1) * AtmosphereTuning.accountingSeconds();
        assertEquals(2.0f, seconds, 0.001f,
                "the grace window is what makes a tank swap survivable");
    }

    @Test
    @DisplayName("the boundary tick is still grace, not damage")
    void boundaryIsStillGrace() {
        assertFalse(OxygenAccounting.damageDue(AtmosphereTuning.SUFFOCATION_GRACE_TICKS));
        assertTrue(OxygenAccounting.damageDue(AtmosphereTuning.SUFFOCATION_GRACE_TICKS + 1));
    }

    /**
     * A tank swap has to be survivable on lungs alone, or ADR-0009 chose a pressurise delay that
     * silently kills people. Checked end to end here rather than as a constant comparison: drain,
     * lung reserve, pressurise delay and the grace window all have to agree.
     */
    @Test
    @DisplayName("a full-lung player survives a pressurise delay with air to spare")
    void aTankSwapIsSurvivable() {
        int units = AtmosphereTuning.LUNG_CAPACITY;
        float carry = 0.0f;
        int ticks = 0;

        for (int elapsed = 0; elapsed < AtmosphereTuning.TANK_PRESSURISE_TICKS;
                elapsed += AtmosphereTuning.ACCOUNTING_INTERVAL_TICKS) {
            OxygenAccounting.Debt debt = OxygenAccounting.debt(BASELINE, carry);
            units = Math.max(0, units - debt.units());
            carry = debt.carry();
            ticks = OxygenAccounting.suffocationTicksAfter(ticks, units > 0);
            assertFalse(OxygenAccounting.damageDue(ticks),
                    "took damage while the valve was still coming up to pressure");
        }

        assertTrue(units > 0, "lungs ran dry during a pressurise delay they are meant to cover");
    }

    @Test
    @DisplayName("damage is scaled to the pass, not applied at the per-second rate")
    void damageIsScaledToThePass() {
        assertEquals(AtmosphereTuning.SUFFOCATION_DAMAGE * AtmosphereTuning.accountingSeconds(),
                OxygenAccounting.damagePerPass(), 0.0001f);
        assertTrue(OxygenAccounting.damagePerPass() < AtmosphereTuning.SUFFOCATION_DAMAGE,
                "a half-second pass must not deal a full second of damage");
    }
}
