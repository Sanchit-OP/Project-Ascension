package com.ascension.combat.internal.weapons;

import com.ascension.combat.AscensionCombat;
import com.ascension.combat.internal.CombatAttributes;
import com.ascension.combat.internal.CombatContent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Endless Hunger's identity (docs/technical/combat-api.md section 6, {@code ORIGINAL}): attack
 * speed scales off attack damage instead of carrying its own flat rating, and omnivamp grows the
 * lower the wielder's own HP falls.
 *
 * <p>The attack-speed link isn't a one-shot proc &mdash; it has to stay correct as the wielder's
 * Attack Damage changes from any source, gear this catalog doesn't have yet included &mdash; so
 * it's recomputed every player tick ({@link #onPlayerTick}) rather than only when a hit lands,
 * the same cheap per-player-tick idiom {@code ascension-atmosphere} uses for its own always-on
 * bookkeeping. The modifier is removed the instant the weapon leaves the main hand.
 *
 * <p><b>Omnivamp, reworked 2026-09-20.</b> Used to heal the wielder directly in a {@code
 * LivingDamageEvent.Post} listener, computing its own missing-HP-scaled percentage on every hit.
 * Now that {@link CombatAttributes#OMNIVAMP} is a real attribute, this only keeps that attribute
 * topped up with the same dynamic percentage, recomputed every tick alongside the attack-speed
 * link; {@link com.ascension.combat.internal.mechanics.OmnivampHealing} is the one place that
 * actually turns it into healing now, the same shared payoff every Omnivamp source goes through.
 */
public final class EndlessHungerEffects {

    /** Placeholder -- combat-api.md section 3: Sanchit tunes by playing, not by spec. */
    private static final float ATTACK_SPEED_PERCENT_PER_AD_POINT = 0.012f;

    private static final float BASE_OMNIVAMP_PERCENT = 0.0f;
    private static final float MAX_BONUS_OMNIVAMP_PERCENT = 0.07f;

    private static final ResourceLocation ATTACK_SPEED_MODIFIER_ID = ResourceLocation
            .fromNamespaceAndPath(AscensionCombat.MOD_ID, "endless_hunger_attack_speed");
    private static final ResourceLocation OMNIVAMP_MODIFIER_ID = ResourceLocation
            .fromNamespaceAndPath(AscensionCombat.MOD_ID, "endless_hunger_omnivamp");

    private EndlessHungerEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(EndlessHungerEffects::onPlayerTick);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        boolean holding = player.getMainHandItem().is(CombatContent.ENDLESS_HUNGER.get());
        applyAttackSpeedFromDamage(player, holding);
        applyOmnivamp(player, holding);
    }

    private static void applyAttackSpeedFromDamage(Player player, boolean holding) {
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed == null) {
            return;
        }

        attackSpeed.removeModifier(ATTACK_SPEED_MODIFIER_ID);
        if (!holding) {
            return;
        }

        AttributeInstance attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage == null) {
            return;
        }

        double bonusPercent = attackDamage.getValue() * ATTACK_SPEED_PERCENT_PER_AD_POINT;
        if (bonusPercent <= 0.0) {
            return;
        }

        attackSpeed.addTransientModifier(new AttributeModifier(
                ATTACK_SPEED_MODIFIER_ID, bonusPercent, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static void applyOmnivamp(Player player, boolean holding) {
        AttributeInstance omnivamp = player.getAttribute(CombatAttributes.OMNIVAMP);
        if (omnivamp == null) {
            return;
        }

        omnivamp.removeModifier(OMNIVAMP_MODIFIER_ID);
        if (!holding) {
            return;
        }

        float missingHealthFraction = 1.0f - clamp01(player.getHealth() / player.getMaxHealth());
        double omnivampPercent = BASE_OMNIVAMP_PERCENT + missingHealthFraction * MAX_BONUS_OMNIVAMP_PERCENT;
        omnivamp.addTransientModifier(new AttributeModifier(
                OMNIVAMP_MODIFIER_ID, omnivampPercent, AttributeModifier.Operation.ADD_VALUE));
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
