package com.ascension.combat.internal.rune.aetherwright;

import com.ascension.combat.internal.mechanics.ItemAbilities;
import com.ascension.combat.internal.mechanics.MagicDamage;
import com.ascension.combat.internal.rune.RuneModifiers;
import net.neoforged.bus.api.IEventBus;

/**
 * Aetherwright's runes (docs/technical/rune-system.md section 4.3): the one place their behaviours
 * are registered. Most of the path is not listeners but <em>hooks</em> into the shared magic-damage
 * and item-ability code, so a rune that makes magic stronger needs no change to any item.
 */
public final class AetherwrightRunes {

    private AetherwrightRunes() {
    }

    public static void register(IEventBus bus) {
        StarfallEffects.register(bus);
        ArcEffects.register(bus);
        ScholarEffects.register(bus);
        ClarityEffects.register(bus);

        MagicDamage.addBooster(AttunementEffects::boost);
        MagicDamage.addBooster(ClarityEffects::boost);
        MagicDamage.addBooster(GatheringChargeEffects::boost);
        MagicDamage.addObserver(SiphonEffects::onMagicDealt);
        ItemAbilities.addHaste(QuickeningEffects::haste);

        RuneModifiers.addRefresher(UnweaveEffects::refresh);
    }
}
