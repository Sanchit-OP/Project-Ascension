package com.ascension.atmosphere.internal.supply;

import static com.ascension.atmosphere.internal.supply.TankCarrySweep.Action.DROP;
import static com.ascension.atmosphere.internal.supply.TankCarrySweep.Action.KEEP;
import static com.ascension.atmosphere.internal.supply.TankCarrySweep.Action.KEEP_CLOSED;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ascension.atmosphere.internal.AtmosphereTuning;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The carry-limit decision, isolated from the inventory it is normally read out of.
 *
 * <p>{@link AtmosphereTuning#MAX_TANKS_CARRIED} is 2 today; every case here is written against
 * the constant rather than the literal, so a future rebalance changes what these tests exercise
 * without making any of them wrong.
 */
class TankCarrySweepTest {

    private static List<TankCarrySweep.Action> sweep(boolean... open) {
        TankCarrySweep sweep = new TankCarrySweep();
        List<TankCarrySweep.Action> actions = new ArrayList<>(open.length);
        for (boolean isOpen : open) {
            actions.add(sweep.next(isOpen));
        }
        return actions;
    }

    @Test
    @DisplayName("within the limit, closed tanks are all kept as-is")
    void withinLimitClosedTanksAreKept() {
        assertEquals(List.of(KEEP), sweep(false));
        if (AtmosphereTuning.MAX_TANKS_CARRIED >= 2) {
            assertEquals(List.of(KEEP, KEEP), sweep(false, false));
        }
    }

    @Test
    @DisplayName("the first open tank in the sweep is kept open")
    void firstOpenTankIsKept() {
        assertEquals(List.of(KEEP), sweep(true));
    }

    @Test
    @DisplayName("a second open tank in the same sweep is kept but forced shut")
    void secondOpenTankIsClosed() {
        assertEquals(List.of(KEEP, KEEP_CLOSED), sweep(true, true));
    }

    /**
     * Order matters and is exactly what a player would expect: whichever open tank appears first
     * in inventory order keeps supplying air, and every later open tank within the carry limit is
     * forced shut rather than dropped. This is what makes {@link TankRules#openTank} unambiguous.
     *
     * <p>Only exercises slots within {@link AtmosphereTuning#MAX_TANKS_CARRIED} &mdash; a tank
     * past the limit is dropped regardless of its valve, which is
     * {@link #capIsCheckedRegardlessOfValveState}'s job to check, not this one's.
     */
    @Test
    @DisplayName("every open tank within the limit is kept, and only the first stays open")
    void openOrderDecidesTheWinner() {
        int limit = AtmosphereTuning.MAX_TANKS_CARRIED;
        boolean[] allOpen = new boolean[limit];
        java.util.Arrays.fill(allOpen, true);

        List<TankCarrySweep.Action> actions = sweep(allOpen);
        assertEquals(KEEP, actions.get(0), "the first open tank must stay open");
        for (int i = 1; i < limit; i++) {
            assertEquals(KEEP_CLOSED, actions.get(i), "slot " + i + " must be kept but shut");
        }
    }

    @Test
    @DisplayName("everything past the carry limit is dropped, regardless of valve state")
    void excessTanksAreDropped() {
        int limit = AtmosphereTuning.MAX_TANKS_CARRIED;
        boolean[] allClosed = new boolean[limit + 1];
        List<TankCarrySweep.Action> actions = sweep(allClosed);
        for (int i = 0; i < limit; i++) {
            assertEquals(KEEP, actions.get(i), "slot " + i + " is within the limit");
        }
        assertEquals(DROP, actions.get(limit), "slot at the limit must be dropped");
    }

    /**
     * The cap counts every tank, open or not. A player should not be able to dodge the drop by
     * making every extra tank closed &mdash; the limit is about how many tanks exist, not about
     * how many are supplying air.
     */
    @Test
    @DisplayName("the cap is checked before the valve, so closing a tank does not save it")
    void capIsCheckedRegardlessOfValveState() {
        int limit = AtmosphereTuning.MAX_TANKS_CARRIED;
        boolean[] allOpen = new boolean[limit + 1];
        java.util.Arrays.fill(allOpen, true);
        List<TankCarrySweep.Action> actions = sweep(allOpen);
        assertEquals(DROP, actions.get(limit));
    }

    @Test
    @DisplayName("a dropped tank's valve state is not decided here")
    void droppedTankIsJustDropped() {
        int limit = AtmosphereTuning.MAX_TANKS_CARRIED;
        boolean[] input = new boolean[limit + 1];
        input[limit] = true;
        List<TankCarrySweep.Action> actions = sweep(input);
        assertEquals(DROP, actions.get(limit));
    }

    @Test
    @DisplayName("an empty sweep decides nothing")
    void emptySweepIsHarmless() {
        assertEquals(List.of(), sweep());
    }
}
