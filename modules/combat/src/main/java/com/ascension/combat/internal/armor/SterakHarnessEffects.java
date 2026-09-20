package com.ascension.combat.internal.armor;

import com.ascension.combat.AscensionCombat;
import com.ascension.combat.internal.CombatAttachments;
import com.ascension.combat.internal.CombatAttributes;
import com.ascension.combat.internal.CombatContent;
import com.ascension.combat.internal.mechanics.ArmedState;
import com.ascension.combat.internal.mechanics.Shield;
import com.ascension.core.api.CombatStateRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Sterak's Harness's identity (docs/technical/combat-api.md section 6): converts a percentage of
 * max HP into bonus attack damage while worn; dropping below half HP auto-triggers a shield
 * (vanilla absorption, same as every other Shield/Absorb consumer) plus a lifesteal window that
 * lasts until the wearer leaves combat, on a cooldown.
 *
 * <p>The AD conversion is the same "flat modifier recomputed every tick while worn" idiom used
 * throughout this catalog, just derived from the wearer's own max HP instead of a fixed constant,
 * so it can't reuse {@link StatBoots} directly. The shield reuses vanilla absorption.
 *
 * <p><b>Reworked 2026-09-20</b> (Sanchit's own spec, after finding the original "big single hit"
 * trigger too weak to matter): the shield now fires on <em>crossing</em> below 50% max HP, not on
 * any one hit crossing a smaller threshold, and is itself 50% max HP instead of 15% &mdash;
 * {@link #maybeTrigger} reconstructs pre-hit HP from {@code target.getHealth() +
 * event.getNewDamage()} to detect the crossing, so it fires exactly once per drop below the line,
 * not on every hit taken while already below it. The lifesteal window changed shape too: {@link
 * CombatAttachments#STERAKS_LIFESTEAL} is now an {@link ArmedState} consumed the moment {@link
 * CombatStateRegistry#isInCombat} goes false, rather than a fixed 10s timer &mdash; "10% lifesteal
 * until we're out of combat" doesn't fit a fixed-duration shape at all.
 *
 * <p>The lifesteal itself is a temporary contribution to {@link CombatAttributes#OMNIVAMP} while
 * armed, recomputed every tick alongside the AD conversion; {@link
 * com.ascension.combat.internal.mechanics.OmnivampHealing} is what actually turns it into healing,
 * same as every other Omnivamp source. Genuinely omnivamp-shaped (heals off any damage dealt, not
 * physical-only) for the same reason {@link CombatAttributes#OMNIVAMP}'s own javadoc gives: nothing
 * in this catalog tags a damage instance's type generically, so a real physical-only Lifesteal
 * isn't buildable yet regardless.
 */
public final class SterakHarnessEffects {

    /** Placeholder -- combat-api.md section 3: Sanchit tunes by playing, not by spec. */
    private static final float MAX_HEALTH_TO_ATTACK_DAMAGE_PERCENT = 0.10f;

    /** Catalog-specified: triggers on crossing below half HP; the shield equals half max HP too. */
    private static final float TRIGGER_HEALTH_PERCENT = 0.5f;

    private static final float SHIELD_MAX_HEALTH_PERCENT = 0.5f;

    /** Catalog-specified: 10%. */
    private static final double LIFESTEAL_PERCENT = 0.10;

    private static final int COOLDOWN_TICKS = 1200;

    private static final ResourceLocation ATTACK_DAMAGE_MODIFIER_ID = ResourceLocation
            .fromNamespaceAndPath(AscensionCombat.MOD_ID, "steraks_harness_attack_damage");
    private static final ResourceLocation LIFESTEAL_MODIFIER_ID = ResourceLocation
            .fromNamespaceAndPath(AscensionCombat.MOD_ID, "steraks_harness_lifesteal");

    private SterakHarnessEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(SterakHarnessEffects::onPlayerTick);
        bus.addListener(SterakHarnessEffects::onDamagePost);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        boolean wearing = player.getItemBySlot(EquipmentSlot.OFFHAND).is(CombatContent.STERAKS_HARNESS.get());
        applyAttackDamage(player, wearing);
        applyLifestealOmnivamp(player, wearing);
    }

    private static void applyAttackDamage(ServerPlayer player, boolean wearing) {
        AttributeInstance attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage == null) {
            return;
        }

        attackDamage.removeModifier(ATTACK_DAMAGE_MODIFIER_ID);
        if (!wearing) {
            return;
        }

        attackDamage.addTransientModifier(new AttributeModifier(
                ATTACK_DAMAGE_MODIFIER_ID,
                player.getMaxHealth() * MAX_HEALTH_TO_ATTACK_DAMAGE_PERCENT,
                AttributeModifier.Operation.ADD_VALUE));
    }

    private static void applyLifestealOmnivamp(ServerPlayer player, boolean wearing) {
        AttributeInstance omnivamp = player.getAttribute(CombatAttributes.OMNIVAMP);
        if (omnivamp == null) {
            return;
        }

        omnivamp.removeModifier(LIFESTEAL_MODIFIER_ID);

        ArmedState lifesteal = player.getData(CombatAttachments.STERAKS_LIFESTEAL);
        if (!wearing || !lifesteal.isArmed()) {
            return;
        }

        if (!CombatStateRegistry.isInCombat(player)) {
            lifesteal.consume();
            return;
        }

        omnivamp.addTransientModifier(new AttributeModifier(
                LIFESTEAL_MODIFIER_ID, LIFESTEAL_PERCENT, AttributeModifier.Operation.ADD_VALUE));
    }

    private static void onDamagePost(LivingDamageEvent.Post event) {
        if (event.getNewDamage() <= 0.0f) {
            return;
        }

        if (event.getEntity() instanceof ServerPlayer target
                && target.getItemBySlot(EquipmentSlot.OFFHAND).is(CombatContent.STERAKS_HARNESS.get())) {
            maybeTrigger(target, event.getNewDamage());
        }
    }

    private static void maybeTrigger(ServerPlayer player, float damageTaken) {
        float threshold = player.getMaxHealth() * TRIGGER_HEALTH_PERCENT;
        float healthAfter = player.getHealth();
        float healthBefore = healthAfter + damageTaken;
        if (healthBefore < threshold || healthAfter >= threshold) {
            return;
        }

        if (player.getCooldowns().isOnCooldown(CombatContent.STERAKS_HARNESS.get())) {
            return;
        }

        float shield = player.getMaxHealth() * SHIELD_MAX_HEALTH_PERCENT;
        Shield.grant(player, shield);
        player.getData(CombatAttachments.STERAKS_LIFESTEAL).arm();
        player.getCooldowns().addCooldown(CombatContent.STERAKS_HARNESS.get(), COOLDOWN_TICKS);
    }
}
