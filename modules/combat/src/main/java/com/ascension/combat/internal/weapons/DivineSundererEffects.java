package com.ascension.combat.internal.weapons;

import com.ascension.combat.internal.CombatAttachments;
import com.ascension.combat.internal.mechanics.ItemAbilities;
import com.ascension.combat.internal.CombatContent;
import com.ascension.combat.internal.mechanics.MagicDamage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Divine Sunderer's identity (docs/technical/combat-api.md section 6, the anti-tank family):
 * right-click primes the next attack to deal bonus damage as a percentage of the target's max
 * HP &mdash; half physical, half magic, confirmed by Sanchit 2026-09-08 &mdash; and heals the
 * wielder for the amount dealt.
 *
 * <p>Splitting one proc across two damage types means splitting it across two events, the same
 * reason {@link SpearOfShojinEffects} isn't in {@link LivingIncomingDamageEvent}: the physical
 * half has to go through vanilla's own armor reduction like any physical bonus (so it's added
 * pre-mitigation, in {@link #onIncomingDamage}, alongside the other anti-tank weapons), while the
 * magic half has to skip armor and go through {@link MagicDamage} instead (so it's added
 * post-armor, in {@link #onDamagePre}). The primed flag can only be consumed once, so
 * {@link #onIncomingDamage} only peeks it ({@link
 * com.ascension.combat.internal.mechanics.ArmedState#isArmed()}) to decide whether to add the
 * physical share; {@link #onDamagePre} is what actually consumes it.
 *
 * <p>The heal uses the physical share's face value plus the magic share's value <em>after</em>
 * {@link MagicDamage} mitigation, rather than re-deriving exactly how much of the final blended
 * hit was "this proc's" post-armor &mdash; vanilla's own armor formula (armor and toughness
 * together) isn't something worth re-implementing here just to shave that precision, and
 * combat-api.md section 3 already treats every number in this catalog as a tunable placeholder.
 */
public final class DivineSundererEffects {

    /** Raised 3% -&gt; 4% each (physical and magic) on 2026-09-20 (Sanchit's own call). 5s cooldown. */
    private static final float PHYSICAL_MAX_HP_PERCENT = 0.04f;

    private static final float MAGIC_MAX_HP_PERCENT = 0.04f;
    private static final int COOLDOWN_TICKS = 100;

    private DivineSundererEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(DivineSundererEffects::onRightClickItem);
        bus.addListener(DivineSundererEffects::onIncomingDamage);
        bus.addListener(DivineSundererEffects::onDamagePre);
    }

    private static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack stack = event.getItemStack();
        if (!stack.is(CombatContent.DIVINE_SUNDERER.get())) {
            return;
        }

        if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            return;
        }

        player.getData(CombatAttachments.DIVINE_SUNDERER_PRIMED).arm();
        player.getCooldowns().addCooldown(stack.getItem(), ItemAbilities.cooldown(player, COOLDOWN_TICKS));
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getAmount() <= 0.0f) {
            return;
        }

        Player attacker = wielder(event.getSource());
        if (attacker == null || !attacker.getData(CombatAttachments.DIVINE_SUNDERER_PRIMED).isArmed()) {
            return;
        }

        float physicalBonus = event.getEntity().getMaxHealth() * PHYSICAL_MAX_HP_PERCENT;
        event.setAmount(event.getAmount() + physicalBonus);
    }

    private static void onDamagePre(LivingDamageEvent.Pre event) {
        if (event.getNewDamage() <= 0.0f) {
            return;
        }

        Player attacker = wielder(event.getSource());
        if (attacker == null) {
            return;
        }

        if (!attacker.getData(CombatAttachments.DIVINE_SUNDERER_PRIMED).consume()) {
            return;
        }

        LivingEntity target = event.getEntity();
        float physicalBonus = target.getMaxHealth() * PHYSICAL_MAX_HP_PERCENT;
        float rawMagicBonus = target.getMaxHealth() * MAGIC_MAX_HP_PERCENT;
        float magicDelivered = MagicDamage.apply(event, rawMagicBonus);

        attacker.heal(physicalBonus + magicDelivered);
    }

    private static Player wielder(DamageSource source) {
        if (!(source.getEntity() instanceof Player attacker)) {
            return null;
        }

        ItemStack weapon = attacker.getMainHandItem();
        if (!weapon.is(CombatContent.DIVINE_SUNDERER.get())) {
            return null;
        }

        return attacker;
    }
}
