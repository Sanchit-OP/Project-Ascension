package com.ascension.combat.internal.rune.reaver;

import com.ascension.combat.internal.rune.RuneModifiers;
import net.neoforged.bus.api.IEventBus;

/**
 * Reaver's runes (docs/technical/rune-system.md section 4.2): the one place their behaviours are
 * registered. Each is its own class, reading its numbers from the rune's data file.
 */
public final class ReaverRunes {

    private ReaverRunes() {
    }

    public static void register(IEventBus bus) {
        AmbushEffects.register(bus);
        DeadeyeEffects.register(bus);
        SilentStepEffects.register(bus);
        PounceEffects.register(bus);
        DropInEffects.register(bus);
        VanishEffects.register(bus);
        FinishingBlowEffects.register(bus);
        OverkillEffects.register(bus);
        // Trophy Hunter has no listener: RuneGrowth counts, and its refresher puts the Lethality on.
        RuneModifiers.addRefresher(TrophyHunterEffects::refresh);
    }
}
