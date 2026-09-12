package com.ascension.combat.internal.mechanics;

import com.ascension.combat.AscensionCombat;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Mob effects owned by this module. */
public final class CombatMobEffects {

    private static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, AscensionCombat.MOD_ID);

    /**
     * Grievous wounds (docs/technical/combat-api.md section 3): reduces lifesteal/omnivamp
     * healing received by whoever carries it. First applied by Mortal Reminder
     * (section 6); the reduction itself lives in {@link GrievousWoundsHealing}, not here.
     *
     * <p>Typed as a {@link DeferredHolder} rather than a plain {@code Supplier}, unlike
     * {@code CombatContent}'s items: {@link net.minecraft.world.entity.LivingEntity#hasEffect}
     * and {@link net.minecraft.world.effect.MobEffectInstance}'s constructor both take a
     * {@code Holder<MobEffect>} in this version, not a {@code MobEffect} itself.
     */
    public static final DeferredHolder<MobEffect, GrievousWoundsMobEffect> GRIEVOUS_WOUNDS =
            EFFECTS.register("grievous_wounds", GrievousWoundsMobEffect::new);

    private CombatMobEffects() {
    }

    public static void register(IEventBus modBus) {
        EFFECTS.register(modBus);
    }
}
