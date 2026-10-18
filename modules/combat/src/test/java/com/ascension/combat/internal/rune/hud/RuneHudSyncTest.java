package com.ascension.combat.internal.rune.hud;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ascension.combat.internal.rune.hud.RuneHudEntry.Kind;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class RuneHudSyncTest {

    private static RuneHudEntry entry(Kind kind, int value, long end) {
        return new RuneHudEntry(ResourceLocation.fromNamespaceAndPath("test", "rune"), 0, kind, value, 10, end);
    }

    @Test
    void anUnchangedListIsNotSent() {
        List<RuneHudEntry> a = List.of(entry(Kind.STACKS, 3, 0));
        List<RuneHudEntry> b = List.of(entry(Kind.STACKS, 3, 0));
        assertFalse(RuneHudSync.differs(a, b));
    }

    @Test
    void aCooldownCountingDownIsNotAChange() {
        // The entry carries the tick it ends at, which does not move while the clock runs.
        List<RuneHudEntry> then = List.of(entry(Kind.COOLDOWN, 0, 500));
        List<RuneHudEntry> later = List.of(entry(Kind.COOLDOWN, 0, 500));
        assertFalse(RuneHudSync.differs(then, later));
    }

    @Test
    void aStackGoingUpIsAChange() {
        assertTrue(RuneHudSync.differs(List.of(entry(Kind.STACKS, 3, 0)), List.of(entry(Kind.STACKS, 4, 0))));
    }

    @Test
    void aCooldownStartingIsAChange() {
        assertTrue(RuneHudSync.differs(List.of(entry(Kind.COOLDOWN, 0, 0)), List.of(entry(Kind.COOLDOWN, 0, 900))));
    }

    @Test
    void takingARuneOffThePageIsAChange() {
        assertTrue(RuneHudSync.differs(List.of(entry(Kind.STACKS, 0, 0)), List.of()));
    }

    @Test
    void nothingToShowAndNothingSentIsQuiet() {
        assertFalse(RuneHudSync.differs(List.of(), List.of()));
    }
}
