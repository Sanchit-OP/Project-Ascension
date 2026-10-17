package com.ascension.combat.internal.rune.vanguard;

import com.ascension.combat.internal.rune.RuneAttachments;
import com.ascension.combat.internal.rune.RuneDefinition;
import com.ascension.combat.internal.rune.Runes;
import java.util.Optional;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

/**
 * Vanguard's Cadence keystone (docs/technical/rune-system.md section 4.1): land three fully
 * charged hits on one enemy inside a few seconds and the third hits harder and <b>Exposes</b> it
 * ({@link com.ascension.combat.internal.rune.ExposedDamage}): it takes more damage from everyone.
 *
 * <p>A <em>fully charged</em> hit is a melee swing at full charge, or a fully drawn arrow (vanilla
 * marks those as critical arrows; a crossbow's always are). That is what makes it a rhythm rune:
 * mashing the attack button never builds it, waiting for each swing does.
 *
 * <p>Its numbers are the rune's data: {@code hits}, {@code window_seconds}, {@code bonus_damage},
 * {@code exposed_seconds}.
 */
public final class CadenceEffects {

    public static final ResourceLocation ID = Runes.id("cadence");

    /** Vanilla's own line between a full swing and a partial one. */
    private static final float FULL_CHARGE = 0.9f;

    private CadenceEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(CadenceEffects::onAttack);
        bus.addListener(CadenceEffects::onIncomingDamage);
    }

    /**
     * A melee swing's charge is read here, at the start of the attack, because by the time the
     * damage event arrives the swing's cooldown has already been reset.
     */
    private static void onAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer attacker) || Runes.active(attacker, ID).isEmpty()) {
            return;
        }

        if (attacker.getAttackStrengthScale(0.5f) >= FULL_CHARGE) {
            attacker.getData(RuneAttachments.CADENCE.get())
                    .markChargedSwing(event.getTarget().getUUID(), attacker.level().getGameTime());
        }
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getAmount() <= 0.0f || !(event.getSource().getEntity() instanceof ServerPlayer attacker)) {
            return;
        }

        Optional<RuneDefinition> rune = Runes.active(attacker, ID);
        if (rune.isEmpty() || !(attacker.level() instanceof ServerLevel level)) {
            return;
        }

        LivingEntity victim = event.getEntity();
        CadenceState state = attacker.getData(RuneAttachments.CADENCE.get());
        long now = level.getGameTime();
        if (!fullyCharged(event.getSource().getDirectEntity(), attacker, victim, state, now)) {
            return;
        }

        RuneDefinition values = rune.get();
        if (!state.hit(victim.getUUID(), now, values.count("hits", 3), values.ticks("window_seconds", 4.0))) {
            return;
        }

        event.setAmount(event.getAmount() + (float) values.value("bonus_damage", 3.0));
        victim.getData(RuneAttachments.EXPOSED.get()).activateUntil(now + values.ticks("exposed_seconds", 5.0));
        level.sendParticles(ParticleTypes.ENCHANTED_HIT, victim.getX(), victim.getY(0.6), victim.getZ(),
                16, 0.3, 0.4, 0.3, 0.3);
        level.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 0.7f);
    }

    private static boolean fullyCharged(Entity direct, ServerPlayer attacker, LivingEntity victim, CadenceState state, long now) {
        if (direct instanceof AbstractArrow arrow) {
            return arrow.isCritArrow();
        }

        return direct == attacker && state.wasChargedSwing(victim.getUUID(), now);
    }
}
