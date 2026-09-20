package com.ascension.combat.internal.armor;

import com.ascension.combat.internal.CombatAttachments;
import com.ascension.combat.internal.CombatContent;
import com.ascension.combat.internal.mechanics.MagicDamage;
import com.ascension.core.api.CombatStateRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
 *
 * <p><b>Feedback, added after Sanchit's first test pass 2026-09-12</b> (the same problem
 * Heartwood of Pallas had): every pulse is otherwise entirely invisible &mdash; no particle, no
 * sound, nothing to distinguish "this did nothing because it hasn't been 10s yet" from "this fired
 * and just didn't reach anything." {@link #pulse} now always plays a sound and spawns particles on
 * the wearer, whether or not anything was in range to hit, specifically so a whiffed pulse is still
 * perceptible as a pulse.
 */
public final class UnendingDespairEffects {

    /** Catalog-specified: 10s continuous combat before pulsing starts. */
    private static final long PULSE_THRESHOLD_TICKS = 200L;

    /** Placeholder -- combat-api.md section 3: Sanchit tunes by playing, not by spec. */
    private static final long PULSE_INTERVAL_TICKS = 40L;

    /** Radius 4.0 -&gt; 6.0, damage 4.0 -&gt; 12.0 on 2026-09-20 (Sanchit's own call: "too low"). */
    private static final double PULSE_RADIUS = 6.0;

    private static final float PULSE_DAMAGE = 12.0f;

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

        level.playSound(
                null, player.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.8f, 1.2f);
        level.sendParticles(
                ParticleTypes.WITCH,
                player.getX(),
                player.getY(1.0),
                player.getZ(),
                20,
                PULSE_RADIUS * 0.5,
                0.5,
                PULSE_RADIUS * 0.5,
                0.0);

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
