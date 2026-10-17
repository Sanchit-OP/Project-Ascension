package com.ascension.combat.client.rune;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ascension.combat.api.rune.RunePath;
import com.ascension.combat.internal.rune.RuneDefinition;
import com.ascension.combat.internal.rune.RunePage;
import com.ascension.combat.internal.rune.RuneSlot;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/** What clicking does to the unsaved page (docs/technical/rune-system.md section 10). Pure choices (ADR-0008). */
class RunePageEditTest {

    private static final Map<ResourceLocation, RuneDefinition> RUNES = new HashMap<>();

    private static final ResourceLocation KEY = rune("key", RunePath.VANGUARD, RuneSlot.KEYSTONE, 0);
    private static final ResourceLocation KEY_2 = rune("key_2", RunePath.VANGUARD, RuneSlot.KEYSTONE, 0);
    private static final ResourceLocation V_ROW0 = rune("v_row0", RunePath.VANGUARD, RuneSlot.MINOR, 0);
    private static final ResourceLocation V_ROW0_B = rune("v_row0_b", RunePath.VANGUARD, RuneSlot.MINOR, 0);
    private static final ResourceLocation V_ROW1 = rune("v_row1", RunePath.VANGUARD, RuneSlot.MINOR, 1);
    private static final ResourceLocation B_ROW0 = rune("b_row0", RunePath.BULWARK, RuneSlot.MINOR, 0);
    private static final ResourceLocation SHARD = rune("shard", null, RuneSlot.SHARD, 1);

    private static ResourceLocation rune(String name, RunePath path, RuneSlot slot, int row) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("test", name);
        RUNES.put(id, new RuneDefinition(Optional.ofNullable(path), slot, row, 0, false, id, id, Map.of(), Optional.empty()));
        return id;
    }

    private static RunePageEdit edit(RunePage saved) {
        return new RunePageEdit(saved, RUNES::get);
    }

    private static RunePageEdit fresh() {
        return edit(RunePage.EMPTY);
    }

    @Test
    void startsFromTheSavedPage() {
        RunePage saved = new RunePage(Optional.of(RunePath.VANGUARD), Optional.of(KEY), List.of(V_ROW0, V_ROW1),
                Optional.of(RunePath.BULWARK), List.of(B_ROW0), List.of(SHARD));
        RunePageEdit edit = edit(saved);
        assertTrue(edit.toPage().sameAs(saved));
        assertEquals(Optional.of(V_ROW1), edit.minor(true, 1));
        assertEquals(Optional.of(B_ROW0), edit.minor(false, 0));
        assertEquals(Optional.of(SHARD), edit.shard(1));
    }

    @Test
    void aNewPrimaryPathStartsItsPageFresh() {
        RunePageEdit edit = fresh();
        edit.choosePrimary(RunePath.VANGUARD);
        edit.toggleKeystone(KEY);
        edit.toggleMinor(true, 0, V_ROW0);
        assertTrue(edit.choosePrimary(RunePath.REAVER));
        assertEquals(Optional.empty(), edit.keystone());
        assertEquals(Optional.empty(), edit.minor(true, 0));
    }

    @Test
    void choosingTheSamePrimaryAgainChangesNothing() {
        RunePageEdit edit = fresh();
        edit.choosePrimary(RunePath.VANGUARD);
        edit.toggleKeystone(KEY);
        assertFalse(edit.choosePrimary(RunePath.VANGUARD));
        assertEquals(Optional.of(KEY), edit.keystone());
    }

    @Test
    void thePrimaryPathCannotAlsoBeTheSecondary() {
        RunePageEdit edit = fresh();
        edit.choosePrimary(RunePath.VANGUARD);
        assertFalse(edit.chooseSecondary(RunePath.VANGUARD));
        assertEquals(Optional.empty(), edit.secondary());
    }

    @Test
    void takingThePrimaryFromTheSecondaryClearsTheSecondary() {
        RunePageEdit edit = fresh();
        edit.choosePrimary(RunePath.VANGUARD);
        edit.chooseSecondary(RunePath.BULWARK);
        edit.toggleMinor(false, 0, B_ROW0);
        edit.choosePrimary(RunePath.BULWARK);
        assertEquals(Optional.empty(), edit.secondary());
        assertEquals(Optional.empty(), edit.minor(false, 0));
    }

    @Test
    void theSecondaryPathTogglesAndStartsFresh() {
        RunePageEdit edit = fresh();
        edit.choosePrimary(RunePath.VANGUARD);
        edit.chooseSecondary(RunePath.BULWARK);
        edit.toggleMinor(false, 0, B_ROW0);
        edit.chooseSecondary(RunePath.REAVER);
        assertEquals(Optional.of(RunePath.REAVER), edit.secondary());
        assertEquals(Optional.empty(), edit.minor(false, 0), "the old secondary's minors are gone");
        edit.chooseSecondary(RunePath.REAVER);
        assertEquals(Optional.empty(), edit.secondary(), "a second click takes the path off");
    }

    @Test
    void aMinorReplacesItsRowsChoiceAndAClickOnTheChosenOneTakesItOff() {
        RunePageEdit edit = fresh();
        edit.choosePrimary(RunePath.VANGUARD);
        edit.toggleMinor(true, 0, V_ROW0);
        edit.toggleMinor(true, 0, V_ROW0_B);
        assertEquals(Optional.of(V_ROW0_B), edit.minor(true, 0));
        edit.toggleMinor(true, 0, V_ROW0_B);
        assertEquals(Optional.empty(), edit.minor(true, 0));
    }

    @Test
    void theKeystoneAndShardsToggleToo() {
        RunePageEdit edit = fresh();
        edit.choosePrimary(RunePath.VANGUARD);
        edit.toggleKeystone(KEY);
        edit.toggleKeystone(KEY_2);
        assertEquals(Optional.of(KEY_2), edit.keystone());
        edit.toggleShard(1, SHARD);
        assertTrue(edit.has(SHARD));
        edit.toggleShard(1, SHARD);
        assertFalse(edit.has(SHARD));
    }

    @Test
    void thePageCarriesWhatIsChosenInRowOrder() {
        RunePageEdit edit = fresh();
        edit.choosePrimary(RunePath.VANGUARD);
        edit.toggleMinor(true, 1, V_ROW1);
        edit.toggleMinor(true, 0, V_ROW0);
        assertEquals(List.of(V_ROW0, V_ROW1), edit.toPage().primaryMinors());
    }

    @Test
    void loadingAgainDropsUnsavedChoices() {
        RunePageEdit edit = fresh();
        edit.choosePrimary(RunePath.REAVER);
        edit.load(RunePage.EMPTY, RUNES::get);
        assertEquals(Optional.empty(), edit.primary());
        assertTrue(edit.toPage().sameAs(RunePage.EMPTY));
    }
}
