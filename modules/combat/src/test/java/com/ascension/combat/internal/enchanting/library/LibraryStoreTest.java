package com.ascension.combat.internal.enchanting.library;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class LibraryStoreTest {

    @Test
    void aHigherLevelReplacesALowerOne() {
        LibraryStore store = new LibraryStore();
        store.absorb("minecraft:sharpness", 2, 5);
        assertEquals(4, store.absorb("minecraft:sharpness", 4, 5));
        assertEquals(4, store.absorb("minecraft:sharpness", 1, 5));
    }

    @Test
    void twoEqualLevelsMakeTheNextOneUpButNeverPastTheMaximum() {
        LibraryStore store = new LibraryStore();
        store.absorb("minecraft:sharpness", 3, 5);
        assertEquals(4, store.absorb("minecraft:sharpness", 3, 5));
        store.set("minecraft:sharpness", 5);
        assertEquals(5, store.absorb("minecraft:sharpness", 5, 5));
    }

    @Test
    void aFirstBookStoresItsOwnLevel() {
        LibraryStore store = new LibraryStore();
        assertEquals(3, store.absorb("minecraft:sharpness", 3, 5));
        assertEquals(1, store.view().size());
    }

    @Test
    void costIsTheSumOfLevelsAndAtLeastOne() {
        assertEquals(9, LibraryStore.cost(List.of(5, 3, 1)));
        assertEquals(1, LibraryStore.cost(List.of()));
    }

    @Test
    void settingZeroRemoves() {
        LibraryStore store = new LibraryStore();
        store.set("a:b", 2);
        store.set("a:b", 0);
        assertTrue(store.isEmpty());
    }
}
