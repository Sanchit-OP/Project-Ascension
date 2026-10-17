package com.ascension.combat.internal.rune;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ascension.combat.api.rune.RunePath;
import com.ascension.combat.internal.rune.RunePageRules.OpenSlots;
import com.ascension.combat.internal.rune.RunePageRules.Refusal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/**
 * The page rules (docs/technical/rune-system.md sections 3, 6 and 7) against a small made-up set
 * of runes. Pure logic; says nothing about the game (ADR-0008).
 */
class RunePageRulesTest {

    private static final Map<ResourceLocation, RuneDefinition> RUNES = new HashMap<>();

    private static final ResourceLocation V_KEY = rune("v_key", RunePath.VANGUARD, RuneSlot.KEYSTONE, 0);
    private static final ResourceLocation V_KEY_2 = rune("v_key_2", RunePath.VANGUARD, RuneSlot.KEYSTONE, 0);
    private static final ResourceLocation V_ROW0_A = rune("v_row0_a", RunePath.VANGUARD, RuneSlot.MINOR, 0);
    private static final ResourceLocation V_ROW0_B = rune("v_row0_b", RunePath.VANGUARD, RuneSlot.MINOR, 0);
    private static final ResourceLocation V_ROW1 = rune("v_row1", RunePath.VANGUARD, RuneSlot.MINOR, 1);
    private static final ResourceLocation B_KEY = rune("b_key", RunePath.BULWARK, RuneSlot.KEYSTONE, 0);
    private static final ResourceLocation B_ROW0 = rune("b_row0", RunePath.BULWARK, RuneSlot.MINOR, 0);
    private static final ResourceLocation B_ROW1 = rune("b_row1", RunePath.BULWARK, RuneSlot.MINOR, 1);
    private static final ResourceLocation OFFENCE = rune("offence", null, RuneSlot.SHARD, 0);
    private static final ResourceLocation OFFENCE_2 = rune("offence_2", null, RuneSlot.SHARD, 0);
    private static final ResourceLocation DEFENCE = rune("defence", null, RuneSlot.SHARD, 2);

    private static ResourceLocation rune(String name, RunePath path, RuneSlot slot, int row) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("test", name);
        RUNES.put(id, new RuneDefinition(Optional.ofNullable(path), slot, row, 0, false, id, id, Map.of(), Optional.empty()));
        return id;
    }

    private static Optional<Refusal> check(RunePage page, OpenSlots open) {
        return RunePageRules.validate(page, id -> Optional.ofNullable(RUNES.get(id)), open);
    }

    private static Optional<Refusal> check(RunePage page) {
        return check(page, OpenSlots.ALL);
    }

    private static RunePage page(RunePath primary, ResourceLocation keystone, List<ResourceLocation> minors,
            RunePath secondary, List<ResourceLocation> secondaryMinors, List<ResourceLocation> shards) {
        return new RunePage(Optional.ofNullable(primary), Optional.ofNullable(keystone), minors,
                Optional.ofNullable(secondary), secondaryMinors, shards);
    }

    private static final RunePage FULL = page(RunePath.VANGUARD, V_KEY, List.of(V_ROW0_A, V_ROW1),
            RunePath.BULWARK, List.of(B_ROW0, B_ROW1), List.of(OFFENCE, DEFENCE));

    @Test
    void anEmptyPageIsAllowed() {
        assertEquals(Optional.empty(), check(RunePage.EMPTY));
    }

    @Test
    void aFullPageIsAllowed() {
        assertEquals(Optional.empty(), check(FULL));
    }

    @Test
    void aPartPageIsAllowed() {
        assertEquals(Optional.empty(), check(page(RunePath.VANGUARD, null, List.of(V_ROW1), null, List.of(), List.of())));
    }

    @Test
    void runesNeedAPrimaryPath() {
        assertEquals(Optional.of(Refusal.NO_PRIMARY), check(page(null, V_KEY, List.of(), null, List.of(), List.of())));
    }

    @Test
    void shardsAloneNeedNoPath() {
        assertEquals(Optional.empty(), check(page(null, null, List.of(), null, List.of(), List.of(OFFENCE))));
    }

    @Test
    void theSecondaryPathMustDiffer() {
        assertEquals(Optional.of(Refusal.SAME_PATH),
                check(page(RunePath.VANGUARD, V_KEY, List.of(), RunePath.VANGUARD, List.of(), List.of())));
    }

    @Test
    void theKeystoneMustBeThePrimaryPaths() {
        assertEquals(Optional.of(Refusal.WRONG_KEYSTONE),
                check(page(RunePath.VANGUARD, B_KEY, List.of(), null, List.of(), List.of())));
    }

    @Test
    void aMinorIsNotAKeystone() {
        assertEquals(Optional.of(Refusal.WRONG_KEYSTONE),
                check(page(RunePath.VANGUARD, V_ROW1, List.of(), null, List.of(), List.of())));
    }

    @Test
    void minorsMustBeTheirSidesPath() {
        assertEquals(Optional.of(Refusal.WRONG_MINOR),
                check(page(RunePath.VANGUARD, V_KEY, List.of(B_ROW0), null, List.of(), List.of())));
        assertEquals(Optional.of(Refusal.WRONG_MINOR),
                check(page(RunePath.VANGUARD, V_KEY, List.of(), RunePath.BULWARK, List.of(V_ROW1), List.of())));
    }

    @Test
    void secondaryMinorsNeedASecondaryPath() {
        assertEquals(Optional.of(Refusal.WRONG_MINOR),
                check(page(RunePath.VANGUARD, V_KEY, List.of(), null, List.of(B_ROW0), List.of())));
    }

    @Test
    void oneMinorPerRow() {
        assertEquals(Optional.of(Refusal.ONE_PER_ROW),
                check(page(RunePath.VANGUARD, V_KEY, List.of(V_ROW0_A, V_ROW0_B), null, List.of(), List.of())));
        assertEquals(Optional.of(Refusal.ONE_PER_ROW),
                check(page(RunePath.VANGUARD, V_KEY, List.of(V_ROW1, V_ROW1), null, List.of(), List.of())));
    }

    @Test
    void oneShardPerRow() {
        assertEquals(Optional.of(Refusal.ONE_PER_ROW),
                check(page(null, null, List.of(), null, List.of(), List.of(OFFENCE, OFFENCE_2))));
    }

    @Test
    void aShardSlotTakesOnlyShards() {
        assertEquals(Optional.of(Refusal.WRONG_SHARD),
                check(page(RunePath.VANGUARD, V_KEY, List.of(), null, List.of(), List.of(V_ROW1))));
    }

    @Test
    void unknownRunesAreRefused() {
        ResourceLocation ghost = ResourceLocation.fromNamespaceAndPath("test", "ghost");
        assertEquals(Optional.of(Refusal.UNKNOWN_RUNE),
                check(page(RunePath.VANGUARD, V_KEY, List.of(ghost), null, List.of(), List.of())));
    }

    @Test
    void lockedSlotsRefuse() {
        OpenSlots fresh = OpenSlots.atMilestones(0);
        assertEquals(Optional.of(Refusal.SECONDARY_LOCKED), check(FULL, fresh));
        assertEquals(Optional.of(Refusal.SHARD_LOCKED),
                check(page(RunePath.VANGUARD, V_KEY, List.of(), null, List.of(), List.of(OFFENCE)), fresh));
        assertEquals(Optional.empty(),
                check(page(RunePath.VANGUARD, V_KEY, List.of(V_ROW0_A, V_ROW1), null, List.of(), List.of()), fresh));
    }

    @Test
    void slotsOpenWithMilestones() {
        assertEquals(new OpenSlots(false, false, false, false), OpenSlots.atMilestones(0));
        assertEquals(new OpenSlots(true, false, false, false), OpenSlots.atMilestones(1));
        assertEquals(new OpenSlots(true, true, false, false), OpenSlots.atMilestones(2));
        assertEquals(new OpenSlots(true, true, true, true), OpenSlots.atMilestones(4));
        assertEquals(Optional.of(Refusal.SHARD_LOCKED),
                check(page(null, null, List.of(), null, List.of(), List.of(DEFENCE)), OpenSlots.atMilestones(3)));
    }

    @Test
    void fillingEmptySlotsIsFree() {
        assertFalse(RunePageRules.costsToChange(RunePage.EMPTY, FULL));
        RunePage half = page(RunePath.VANGUARD, V_KEY, List.of(V_ROW1), null, List.of(), List.of());
        assertFalse(RunePageRules.costsToChange(half, FULL));
    }

    @Test
    void removingOrReplacingCosts() {
        RunePage replaced = page(RunePath.VANGUARD, V_KEY_2, List.of(V_ROW0_A, V_ROW1),
                RunePath.BULWARK, List.of(B_ROW0, B_ROW1), List.of(OFFENCE, DEFENCE));
        assertTrue(RunePageRules.costsToChange(FULL, replaced));
        RunePage removed = page(RunePath.VANGUARD, V_KEY, List.of(V_ROW0_A, V_ROW1),
                RunePath.BULWARK, List.of(B_ROW0), List.of(OFFENCE, DEFENCE));
        assertTrue(RunePageRules.costsToChange(FULL, removed));
        assertTrue(RunePageRules.costsToChange(FULL, RunePage.EMPTY));
    }

    @Test
    void theSamePageInAnotherOrderIsTheSamePage() {
        RunePage reordered = page(RunePath.VANGUARD, V_KEY, List.of(V_ROW1, V_ROW0_A),
                RunePath.BULWARK, List.of(B_ROW1, B_ROW0), List.of(DEFENCE, OFFENCE));
        assertTrue(FULL.sameAs(reordered));
        assertFalse(RunePageRules.costsToChange(FULL, reordered));
    }
}
