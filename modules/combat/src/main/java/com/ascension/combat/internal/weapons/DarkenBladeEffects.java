package com.ascension.combat.internal.weapons;

import com.ascension.combat.internal.CombatAttachments;
import com.ascension.combat.internal.CombatContent;
import com.ascension.combat.internal.mechanics.ArmedState;
import com.ascension.combat.internal.mechanics.MagicDamage;
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
 * <p>The shield reuses vanilla's own absorption pool ({@code
 * LivingEntity#setAbsorptionAmount}) rather than a hand-rolled temporary-HP system &mdash; it
 * already renders as the golden extra hearts and already soaks damage before real HP, which is
 * exactly what combat-api.md section 5's Shield/Absorb entry asks for.
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
        float rawMagicBonus = target.getMaxHealth() * MAGIC_MAX_HP_PERCENT;
        MagicDamage.apply(event, rawMagicBonus);

        attacker.setAbsorptionAmount(Math.max(attacker.getAbsorptionAmount(), SHIELD_AMOUNT));
        attacker.getCooldowns().addCooldown(weapon.getItem(), PROC_COOLDOWN_TICKS);
    }
}
