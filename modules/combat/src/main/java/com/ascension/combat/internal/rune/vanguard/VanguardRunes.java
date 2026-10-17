package com.ascension.combat.internal.rune.vanguard;

import com.ascension.combat.internal.rune.RuneModifiers;
import net.neoforged.bus.api.IEventBus;

/**
 * Vanguard's runes (docs/technical/rune-system.md section 4.1): the one place their behaviours are
 * registered, so a path is one line in {@code AscensionCombat} however many runes it has. Each
 * behaviour is its own class, reading its numbers from the rune's data file.
 */
public final class VanguardRunes {

    private VanguardRunes() {
    }

    public static void register(IEventBus bus) {
        ConquerorsResolveEffects.register(bus);
        CadenceEffects.register(bus);
        VeinBreakerEffects.register(bus);
        SecondWindEffects.register(bus);
        MomentumEffects.register(bus);
        // Blooded has no listener: RuneGrowth counts, and its refresher puts the omnivamp on.
        RuneModifiers.addRefresher(BloodedEffects::refresh);
        RelentlessStrikesEffects.register(bus);
        KillingSurgeEffects.register(bus);
        GiantSlayerEffects.register(bus);
    }
}
