package com.ascension.combat.internal.rune.reaver;

import com.ascension.combat.internal.rune.RuneDefinition;
import com.ascension.combat.internal.rune.RuneHits;
import com.ascension.combat.internal.rune.Runes;
import java.util.Optional;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Reaver's Ambush keystone (docs/technical/rune-system.md section 4.2): a hit on an enemy that is
 * <em>not targeting you</em> deals much more damage, and more still if it is a critical hit. Sneak
 * up on things, or hit what is looking at someone else.
 *
 * <p>An enemy that has noticed you has you as its target, so the bonus belongs to the first blow
 * and to anything that never looks at you. Bosses go for you at once, which is why the rune is
 * weak against them without a line of code saying so.
 *
 * <p>Its numbers are the rune's data: {@code bonus} and {@code crit_bonus}, as fractions.
 */
public final class AmbushEffects {

    public static final ResourceLocation ID = Runes.id("ambush");

    private AmbushEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(AmbushEffects::onIncomingDamage);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getAmount() <= 0.0f || !(event.getSource().getEntity() instanceof ServerPlayer attacker)) {
            return;
        }

        if (!(event.getEntity() instanceof Mob victim) || victim.getTarget() == attacker) {
            return;
        }

        Optional<RuneDefinition> rune = Runes.active(attacker, ID);
        if (rune.isEmpty() || !(attacker.level() instanceof ServerLevel level)) {
            return;
        }

        boolean crit = RuneHits.critical(event.getSource(), attacker, victim, level.getGameTime());
        float bonus = bonus(rune.get().value("bonus", 0.40), rune.get().value("crit_bonus", 0.20), crit);
        event.setAmount(event.getAmount() * (1.0f + bonus));
        level.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY(0.6), victim.getZ(), 10, 0.25, 0.3, 0.25, 0.2);
        level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.PLAYER_ATTACK_CRIT,
                SoundSource.PLAYERS, 0.8f, 1.3f);
    }

    /** The extra damage as a fraction: the base, plus the crit bonus if it was a critical hit. */
    static float bonus(double base, double critBonus, boolean crit) {
        return (float) (base + (crit ? critBonus : 0.0));
    }
}
