package com.ascension.combat.internal.rune;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

/** Where a projectile meets its victim: the geometry Deadeye's headshot rests on (ADR-0008: pure numbers). */
class RuneHitsTest {

    /** A zombie's box, grown by the 0.3 blocks vanilla grows it by when a projectile looks for its victim. */
    private static final AABB ZOMBIE = new AABB(-0.3, 0.0, -0.3, 0.3, 1.95, 0.3).inflate(0.3);

    @Test
    void aLevelArrowMeetsTheVictimAtTheHeightItFliesAt() {
        assertEquals(1.7, RuneHits.contactHeight(ZOMBIE, new Vec3(-10.0, 1.7, 0.0), new Vec3(12.0, 0.0, 0.0), -1.0), 1e-9);
        assertEquals(0.4, RuneHits.contactHeight(ZOMBIE, new Vec3(-10.0, 0.4, 0.0), new Vec3(12.0, 0.0, 0.0), -1.0), 1e-9);
    }

    @Test
    void anArrowFallingOntoTheVictimMeetsItsHead() {
        assertEquals(1.95 + 0.3, RuneHits.contactHeight(ZOMBIE, new Vec3(0.0, 6.0, 0.0), new Vec3(0.0, -8.0, 0.0), -1.0), 1e-9);
    }

    @Test
    void anArrowThatCannotReachTheVictimThisTickUsesItsOwnHeight() {
        assertEquals(-1.0, RuneHits.contactHeight(ZOMBIE, new Vec3(-30.0, 1.7, 0.0), new Vec3(3.0, 0.0, 0.0), -1.0), 1e-9);
    }
}
