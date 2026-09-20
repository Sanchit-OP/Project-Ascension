package com.ascension.combat.internal.armor;

import com.ascension.combat.AscensionCombat;
import com.ascension.combat.internal.CombatAttachments;
import com.ascension.combat.internal.CombatAttributes;
import com.ascension.combat.internal.CombatContent;
import com.ascension.combat.internal.mechanics.ArmedState;
import com.ascension.combat.internal.mechanics.DurationState;
import com.ascension.core.api.CombatStateRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Edge of Night's identity (docs/technical/combat-api.md section 6): after a few seconds out of
 * combat, arms a one-hit dodge; negating an attack with it triggers a burst of move speed and
 * lethality.
 *
 * <p><b>Reworked 2026-09-20</b> from a vanilla-absorption shield (a flat few temporary hearts,
 * capped low) into a true dodge: the next incoming hit is negated entirely regardless of its size
 * or damage type ("whatever that attack could have been" &mdash; a Warden's slam is exactly the
 * case that motivated this), rather than soaking a small fixed amount of it. {@link
 * CombatAttachments#EDGE_OF_NIGHT_IMMUNITY} is the flag ({@link ArmedState}, armed-until-consumed,
 * same shape Divine Sunderer's own primed-attack flag uses); {@link #onIncomingDamage} is
 * registered at {@link EventPriority#LOWEST} so it sees the fully-resolved damage amount after
 * every other weapon's own bonus has already been added to the same hit (the same reasoning
 * {@code CollectorsPantsEffects} uses for its own execute check), and zeroes all of it.
 *
 * <p>The burst window itself is {@link DurationState}, same as {@link
 * com.ascension.combat.internal.weapons.WraithbladeEffects}'s post-blink speed boost; lethality is
 * a tick-gated contribution to {@link CombatAttributes#LETHALITY}, same idiom that class uses for
 * its own always-on version.
 */
public final class EdgeOfNightEffects {

    private static final long BURST_DURATION_TICKS = 60L;
    private static final double MOVE_SPEED_BONUS_PERCENT = 0.25;
    private static final double FLAT_LETHALITY = 4.0;

    private static final ResourceLocation MOVE_SPEED_MODIFIER_ID = ResourceLocation
            .fromNamespaceAndPath(AscensionCombat.MOD_ID, "edge_of_night_move_speed");
    private static final ResourceLocation LETHALITY_MODIFIER_ID = ResourceLocation
            .fromNamespaceAndPath(AscensionCombat.MOD_ID, "edge_of_night_lethality");

    private EdgeOfNightEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(EdgeOfNightEffects::onPlayerTick);
        bus.addListener(EventPriority.LOWEST, EdgeOfNightEffects::onIncomingDamage);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        boolean wearing = player.getItemBySlot(EquipmentSlot.HEAD).is(CombatContent.EDGE_OF_NIGHT.get());
        ArmedState immunity = player.getData(CombatAttachments.EDGE_OF_NIGHT_IMMUNITY);
        if (wearing && !CombatStateRegistry.isInCombat(player) && !immunity.isArmed()) {
            immunity.arm();
        }

        DurationState burst = player.getData(CombatAttachments.EDGE_OF_NIGHT_BURST);
        boolean burstActive = wearing && burst.isActive(player.level().getGameTime());
        if (!burstActive) {
            burst.clear();
        }
        applyMoveSpeedModifier(player, burstActive);
        applyLethality(player, burstActive);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getAmount() <= 0.0f) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!player.getItemBySlot(EquipmentSlot.HEAD).is(CombatContent.EDGE_OF_NIGHT.get())) {
            return;
        }

        if (!player.getData(CombatAttachments.EDGE_OF_NIGHT_IMMUNITY).consume()) {
            return;
        }

        event.setAmount(0.0f);
        player.getData(CombatAttachments.EDGE_OF_NIGHT_BURST)
                .activateUntil(player.level().getGameTime() + BURST_DURATION_TICKS);
    }

    private static void applyLethality(ServerPlayer player, boolean active) {
        AttributeInstance lethality = player.getAttribute(CombatAttributes.LETHALITY);
        if (lethality == null) {
            return;
        }

        lethality.removeModifier(LETHALITY_MODIFIER_ID);
        if (!active) {
            return;
        }

        lethality.addTransientModifier(new AttributeModifier(
                LETHALITY_MODIFIER_ID, FLAT_LETHALITY, AttributeModifier.Operation.ADD_VALUE));
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
