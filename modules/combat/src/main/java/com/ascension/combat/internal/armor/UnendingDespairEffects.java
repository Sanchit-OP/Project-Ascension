package com.ascension.combat.internal.armor;

import com.ascension.combat.internal.CombatAttachments;
import com.ascension.combat.internal.CombatContent;
import com.ascension.combat.internal.mechanics.MagicDamage;
import com.ascension.core.api.CombatStateRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Unending Despair's identity (docs/technical/combat-api.md section 6, {@code ORIGINAL}): after
 * 10s in the same fight, pulses magic damage in a radius around the wearer and heals them for the
 * same amount, repeating as long as the fight continues.
 *
 * <p>Unlike The Protean Mask's simple in-combat check, this needs to know how long the <em>same</em>
 * fight has been running, which {@code CombatStateRegistry} doesn't track on its own &mdash; it
 * only answers "in combat right now, yes or no." {@link UnendingDespairState} builds the
 * continuous-duration and pulse-cadence bookkeeping on top of that single boolean, entirely inside
 * this module, the same way combat-api.md section 4 expects any richer combat-adjacent state to be
 * built by whichever module actually needs it rather than pushed into the shared contract.
 *
 * <p>The pulse damage uses {@link MagicDamage#applyStandalone} &mdash; the first consumer that
 * needed a fresh magic hit rather than a bonus riding an existing one, since nothing is attacking
 * the wearer when a pulse fires.
 */
public final class UnendingDespairEffects {

    /** Catalog-specified: 10s continuous combat before pulsing starts. */
    private static final long PULSE_THRESHOLD_TICKS = 200L;

    /** Placeholder -- combat-api.md section 3: Sanchit tunes by playing, not by spec. */
    private static final long PULSE_INTERVAL_TICKS = 40L;

    private static final double PULSE_RADIUS = 4.0;
    private static final float PULSE_DAMAGE = 4.0f;

    private UnendingDespairEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(UnendingDespairEffects::onPlayerTick);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        UnendingDespairState state = player.getData(CombatAttachments.UNENDING_DESPAIR_STATE);
        if (!player.getItemBySlot(EquipmentSlot.LEGS).is(CombatContent.UNENDING_DESPAIR.get())
                || !CombatStateRegistry.isInCombat(player)) {
            state.onOutOfCombat();
            return;
        }

        long now = player.level().getGameTime();
        state.onInCombat(now);
        if (!state.tryPulse(now, PULSE_THRESHOLD_TICKS, PULSE_INTERVAL_TICKS)) {
            return;
        }

        pulse(player);
    }

    private static void pulse(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        float totalDealt = 0.0f;
        for (LivingEntity target
                : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(PULSE_RADIUS))) {
            if (target == player || !target.isAlive()) {
                continue;
            }
            totalDealt += MagicDamage.applyStandalone(target, player.damageSources().magic(), PULSE_DAMAGE);
        }

        if (totalDealt > 0.0f) {
            player.heal(totalDealt);
        }
    }
}
