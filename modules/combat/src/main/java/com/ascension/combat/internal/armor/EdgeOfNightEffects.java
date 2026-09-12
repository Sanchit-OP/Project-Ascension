package com.ascension.combat.internal.armor;

import com.ascension.combat.AscensionCombat;
import com.ascension.combat.internal.CombatAttachments;
import com.ascension.combat.internal.CombatContent;
import com.ascension.combat.internal.mechanics.DurationState;
import com.ascension.core.api.CombatStateRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Edge of Night's identity (docs/technical/combat-api.md section 6): regenerates a shield after a
 * few seconds out of combat; absorbing a hit with it triggers a burst of move speed and
 * lethality.
 *
 * <p>Composes entirely out of mechanics already built for other items, rather than needing
 * anything new: the shield is vanilla absorption, same as {@link
 * com.ascension.combat.internal.weapons.DarkenBladeEffects}; lethality is the same flat
 * {@link DamageContainer.Reduction#ARMOR} modifier {@link
 * com.ascension.combat.internal.weapons.WraithbladeEffects}'s passive uses, just gated on the
 * burst window instead of being always-on; and the burst window itself is {@link DurationState},
 * same as that weapon's post-blink speed boost.
 *
 * <p>Detecting "the shield actually absorbed something this hit" doesn't need a second event or
 * any before/after bookkeeping: {@link LivingDamageEvent.Post#getReduction} reports exactly how
 * much of a given hit was reduced by {@link DamageContainer.Reduction#ABSORPTION}, computed
 * directly from the same {@link DamageContainer} the whole damage sequence already builds.
 */
public final class EdgeOfNightEffects {

    /** Placeholder -- combat-api.md section 3: Sanchit tunes by playing, not by spec. */
    private static final float SHIELD_AMOUNT = 8.0f;

    private static final long BURST_DURATION_TICKS = 60L;
    private static final double MOVE_SPEED_BONUS_PERCENT = 0.25;
    private static final float FLAT_LETHALITY = 4.0f;

    private static final ResourceLocation MOVE_SPEED_MODIFIER_ID = ResourceLocation
            .fromNamespaceAndPath(AscensionCombat.MOD_ID, "edge_of_night_move_speed");

    private EdgeOfNightEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(EdgeOfNightEffects::onPlayerTick);
        bus.addListener(EdgeOfNightEffects::onDamagePost);
        bus.addListener(EdgeOfNightEffects::onIncomingDamage);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        boolean wearing = player.getItemBySlot(EquipmentSlot.HEAD).is(CombatContent.EDGE_OF_NIGHT.get());
        if (wearing && !CombatStateRegistry.isInCombat(player) && player.getAbsorptionAmount() < SHIELD_AMOUNT) {
            player.setAbsorptionAmount(SHIELD_AMOUNT);
        }

        DurationState burst = player.getData(CombatAttachments.EDGE_OF_NIGHT_BURST);
        boolean burstActive = wearing && burst.isActive(player.level().getGameTime());
        if (!burstActive) {
            burst.clear();
        }
        applyMoveSpeedModifier(player, burstActive);
    }

    private static void onDamagePost(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!player.getItemBySlot(EquipmentSlot.HEAD).is(CombatContent.EDGE_OF_NIGHT.get())) {
            return;
        }

        if (event.getReduction(DamageContainer.Reduction.ABSORPTION) <= 0.0f) {
            return;
        }

        player.getData(CombatAttachments.EDGE_OF_NIGHT_BURST)
                .activateUntil(player.level().getGameTime() + BURST_DURATION_TICKS);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getAmount() <= 0.0f) {
            return;
        }

        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof Player attacker)) {
            return;
        }

        if (!attacker.getItemBySlot(EquipmentSlot.HEAD).is(CombatContent.EDGE_OF_NIGHT.get())) {
            return;
        }

        if (!attacker.getData(CombatAttachments.EDGE_OF_NIGHT_BURST).isActive(attacker.level().getGameTime())) {
            return;
        }

        event.addReductionModifier(
                DamageContainer.Reduction.ARMOR,
                (container, reductionIn) -> Math.max(0.0f, reductionIn - FLAT_LETHALITY));
    }

    private static void applyMoveSpeedModifier(ServerPlayer player, boolean active) {
        AttributeInstance moveSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (moveSpeed == null) {
            return;
        }

        moveSpeed.removeModifier(MOVE_SPEED_MODIFIER_ID);
        if (!active) {
            return;
        }

        moveSpeed.addTransientModifier(new AttributeModifier(
                MOVE_SPEED_MODIFIER_ID,
                MOVE_SPEED_BONUS_PERCENT,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
}
