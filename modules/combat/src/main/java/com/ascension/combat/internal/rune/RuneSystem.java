package com.ascension.combat.internal.rune;

import com.ascension.combat.internal.rune.vanguard.VanguardRunes;
import net.neoforged.bus.api.IEventBus;

/**
 * Everything the rune system listens to, registered in one call so {@code AscensionCombat} stays
 * a few lines (docs/technical/rune-system.md section 12). Each path adds one line here.
 */
public final class RuneSystem {

    private RuneSystem() {
    }

    /** Game-event listeners; the mod-bus half is {@link RuneRegistries#register} and {@link RuneContent#register}. */
    public static void register(IEventBus gameBus) {
        SwingTracking.register(gameBus);
        RuneGrowth.register(gameBus);
        RuneModifiers.register(gameBus);
        ExposedDamage.register(gameBus);
        VanguardRunes.register(gameBus);
    }
}
