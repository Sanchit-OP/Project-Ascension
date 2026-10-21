package com.ascension.combat.internal.rune.bulwark;

import com.ascension.combat.internal.mechanics.Shield;
import com.ascension.combat.internal.rune.RuneModifiers;
import net.neoforged.bus.api.IEventBus;

/**
 * Bulwark's runes (docs/technical/rune-system.md section 4.4): the one place their behaviours are
 * registered. Each is its own class, reading its numbers from the rune's data file.
 */
public final class BulwarkRunes {

    private BulwarkRunes() {
    }

    public static void register(IEventBus bus) {
        BraceEffects.register(bus);
        PackLeaderEffects.register(bus);
        OverhealEffects.register(bus);
        PlatingEffects.register(bus);
        UnbowedEffects.register(bus);
        RiposteEffects.register(bus);
        RevitalizeEffects.register(bus);
        ChallengeEffects.register(bus);

        // Revitalize also strengthens shields, through the shared shield door.
        Shield.addBooster(RevitalizeEffects::shieldBoost);
        // Deep Roots has no listener: RuneGrowth counts, and its refresher puts the Health on.
        RuneModifiers.addRefresher(DeepRootsEffects::refresh);
    }
}
