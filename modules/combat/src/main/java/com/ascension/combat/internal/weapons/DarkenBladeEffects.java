package com.ascension.combat.internal.weapons;

import com.ascension.combat.internal.mechanics.AbilityScaling;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.particles.ParticleTypes;
import com.ascension.combat.internal.mechanics.Presence;
import com.ascension.combat.internal.CombatAttachments;
import com.ascension.combat.internal.CombatContent;
import com.ascension.combat.internal.mechanics.ArmedState;
import com.ascension.combat.internal.mechanics.MagicDamage;
import com.ascension.combat.internal.mechanics.Shield;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * Darken Blade's identity (docs/technical/combat-api.md section 6): alternating hits build
 * toward a burst proc dealing magic damage based on the target's max HP; the proc shields the
 * wielder, on a cooldown.
 *
 * <p>Unlike {@link DivineSundererEffects}, this doesn't need two event stages: the base hit stays
 * plain physical damage handled entirely by vanilla, and the burst is <em>entirely</em> magic, so
 * everything &mdash; the alternation bookkeeping, the burst, and the shield &mdash; happens in one
 * {@link LivingDamageEvent.Pre} listener, the same post-armor point {@link MagicDamage} expects.
 *
 * <p>The alternation ({@link ArmedState}) and the cooldown are deliberately independent: every
 * other hit consumes the {@link ArmedState} regardless of whether the proc actually fires, so a
 * burst landing on cooldown doesn't leave the wielder needing three hits instead of two to try
 * again &mdash; it just fires nothing that time and restarts the count.
 *
 * <p>The shield goes through {@link Shield#grant}, not a raw {@code setAbsorptionAmount} call
 * &mdash; found necessary 2026-09-20 after Sanchit's own test showed no shield at all against an
 * Iron Golem hit; see that class's javadoc for why the raw call silently did nothing.
 */
public final class DarkenBladeEffects {

    /** Placeholder -- combat-api.md section 3: Sanchit tunes by playing, not by spec. */
    private static final float MAGIC_MAX_HP_PERCENT = 0.05f;

    private static final float SHIELD_AMOUNT = 6.0f;
    private static final int PROC_COOLDOWN_TICKS = 160;

    private DarkenBladeEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(DarkenBladeEffects::onDamagePre);
    }

    private static void onDamagePre(LivingDamageEvent.Pre event) {
        if (event.getNewDamage() <= 0.0f) {
            return;
        }

        if (!(event.getSource().getEntity() instanceof Player attacker)) {
            return;
        }

        ItemStack weapon = attacker.getMainHandItem();
        if (!weapon.is(CombatContent.DARKEN_BLADE.get())) {
            return;
        }

        ArmedState state = attacker.getData(CombatAttachments.DARKEN_BLADE_ARMED);
        if (!state.consume()) {
            state.arm();
            return;
        }

        if (attacker.getCooldowns().isOnCooldown(weapon.getItem())) {
            return;
        }

        LivingEntity target = event.getEntity();
        float rawMagicBonus = target.getMaxHealth()
                * (MAGIC_MAX_HP_PERCENT + AbilityScaling.DARKEN_MAX_HP_PER_AD * AbilityScaling.attackDamage(attacker));
        rawMagicBonus = AbilityScaling.capped(rawMagicBonus, attacker);
        MagicDamage.apply(event, rawMagicBonus);

        Presence.burst(attacker, SoundEvents.WITHER_SHOOT, 0.6f, 1.5f, ParticleTypes.SOUL_FIRE_FLAME, 15, 0.5);
        Shield.grant(attacker, SHIELD_AMOUNT + AbilityScaling.DARKEN_SHIELD_PER_AD * AbilityScaling.attackDamage(attacker));
        attacker.getCooldowns().addCooldown(weapon.getItem(), PROC_COOLDOWN_TICKS);
    }
}
