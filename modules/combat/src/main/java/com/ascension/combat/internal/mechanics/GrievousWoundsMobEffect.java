package com.ascension.combat.internal.mechanics;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * A pure marker effect (docs/technical/combat-api.md section 3): it does nothing on its own.
 * {@link GrievousWoundsHealing} is what actually reduces healing while a target carries it;
 * {@code MobEffect}'s own tick/attribute hooks are unused here, since "reduce healing received"
 * isn't something the vanilla effect pipeline can express as an attribute modifier.
 */
public final class GrievousWoundsMobEffect extends MobEffect {

    public GrievousWoundsMobEffect() {
        super(MobEffectCategory.HARMFUL, 0xCC3333);
    }
}
