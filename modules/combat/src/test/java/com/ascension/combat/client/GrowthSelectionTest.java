package com.ascension.combat.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ascension.combat.internal.growth.GrowthRules;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * What a click on the tree does before anything is sent: marking follows the server's own rules over the
 * nodes taken plus those marked, and unmarking cannot leave a chain the server would refuse. That the
 * screen draws it and the server accepts it is for a running client (ADR-0008).
 */
class GrowthSelectionTest {

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("ascension_combat", "web/" + path);
    }

    private static final ResourceLocation A = id("a");
    private static final ResourceLocation B = id("b");
    private static final ResourceLocation C = id("c");
    private static final ResourceLocation D = id("d");
    private static final ResourceLocation E = id("e");

    private GrowthSelection selection;

    /** a is a start node. a-b, a-c, b-d, c-d, d-e: a diamond with a tail. */
    @BeforeEach
    void setUp() {
        Map<ResourceLocation, List<ResourceLocation>> links = new LinkedHashMap<>();
        links.put(A, List.of());
        links.put(B, List.of(A));
        links.put(C, List.of(A));
        links.put(D, List.of(B, C));
        links.put(E, List.of(D));
        selection = new GrowthSelection(Set.of(A), GrowthRules.neighbours(links));
    }

    @Test
    void aStartNodeCanBeMarkedWithNothingTaken() {
        assertEquals(GrowthSelection.Result.MARKED, selection.toggle(A, Set.of(), 3));
        assertEquals(List.of(A), selection.marked());
    }

    @Test
    void aNodeNextToNothingYouHaveCannotBeMarked() {
        assertEquals(GrowthSelection.Result.NOT_CONNECTED, selection.toggle(D, Set.of(), 3));
        assertEquals(List.of(), selection.marked());
    }

    @Test
    void aMarkedNodeOpensItsNeighbours() {
        selection.toggle(A, Set.of(), 3);
        assertEquals(GrowthSelection.State.OPEN, selection.state(B, Set.of()));
        assertEquals(GrowthSelection.State.LOCKED, selection.state(D, Set.of()));
        assertEquals(GrowthSelection.Result.MARKED, selection.toggle(B, Set.of(), 3));
        assertEquals(GrowthSelection.Result.MARKED, selection.toggle(D, Set.of(), 3));
        assertEquals(List.of(A, B, D), selection.marked(), "in the order they were marked");
    }

    @Test
    void youCannotMarkMoreThanYouHavePointsFor() {
        selection.toggle(A, Set.of(), 2);
        selection.toggle(B, Set.of(), 2);
        assertEquals(GrowthSelection.Result.NO_POINTS, selection.toggle(C, Set.of(), 2));
    }

    @Test
    void takenNodesCountAsOwnedAndCannotBeMarked() {
        assertEquals(GrowthSelection.Result.ALREADY_TAKEN, selection.toggle(A, Set.of(A), 3));
        assertEquals(GrowthSelection.Result.MARKED, selection.toggle(B, Set.of(A), 3));
        assertEquals(GrowthSelection.State.TAKEN, selection.state(A, Set.of(A)));
    }

    @Test
    void clickingAMarkedNodeUnmarksIt() {
        selection.toggle(A, Set.of(), 3);
        assertEquals(GrowthSelection.Result.UNMARKED, selection.toggle(A, Set.of(), 3));
        assertEquals(List.of(), selection.marked());
    }

    @Test
    void unmarkingTheLinkDropsWhatOnlyItHeldUp() {
        selection.toggle(A, Set.of(), 5);
        selection.toggle(B, Set.of(), 5);
        selection.toggle(D, Set.of(), 5);
        selection.toggle(E, Set.of(), 5);
        selection.toggle(B, Set.of(), 5);   // b goes: d and e hung from a through b only
        assertEquals(List.of(A), selection.marked());
    }

    @Test
    void aNodeStillHeldUpByAnotherRouteStays() {
        selection.toggle(A, Set.of(), 5);
        selection.toggle(B, Set.of(), 5);
        selection.toggle(C, Set.of(), 5);
        selection.toggle(D, Set.of(), 5);
        selection.toggle(B, Set.of(), 5);   // d is still next to c
        assertEquals(List.of(A, C, D), selection.marked());
    }

    @Test
    void whatIsTakenHoldsUpWhatIsMarked() {
        selection.toggle(B, Set.of(A), 5);
        selection.toggle(D, Set.of(A), 5);
        selection.toggle(B, Set.of(A), 5);  // unmark b: d falls with it, a is taken but not next to d
        assertEquals(List.of(), selection.marked());
    }

    @Test
    void reconcileDropsWhatTheServerNowHasAndWhatNoLongerHoldsUp() {
        selection.toggle(A, Set.of(), 5);
        selection.toggle(B, Set.of(), 5);
        selection.reconcile(Set.of(A, B), 5);   // the server took both
        assertEquals(List.of(), selection.marked());

        selection.toggle(D, Set.of(A, B), 5);
        selection.reconcile(Set.of(), 5);       // a respec: d has nothing to hang from
        assertEquals(List.of(), selection.marked());
    }

    @Test
    void reconcileKeepsNoMoreMarksThanPoints() {
        selection.toggle(A, Set.of(), 3);
        selection.toggle(B, Set.of(), 3);
        selection.toggle(D, Set.of(), 3);
        selection.reconcile(Set.of(), 1);
        assertEquals(List.of(A), selection.marked());
    }
}
