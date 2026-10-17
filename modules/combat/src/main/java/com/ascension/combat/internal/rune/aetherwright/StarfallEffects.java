package com.ascension.combat.internal.rune.aetherwright;

import com.ascension.combat.api.stat.CombatStats;
import com.ascension.combat.api.stat.StatReader;
import com.ascension.combat.internal.mechanics.MagicDamage;
import com.ascension.combat.internal.rune.RuneAttachments;
import com.ascension.combat.internal.rune.RuneDefinition;
import com.ascension.combat.internal.rune.RuneHits;
import com.ascension.combat.internal.rune.RuneTargets;
import com.ascension.combat.internal.rune.Runes;
import java.util.Optional;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Aetherwright's Starfall keystone (docs/technical/rune-system.md section 4.3): a fully charged
 * hit, or any magic damage you deal, calls a star down on the target. It lands a moment later and
 * hurts everything near it, as magic. Every magic hit shortens the cooldown, so a magic build
 * rains stars.
 *
 * <p>The star is a place and a game tick on the player ({@link StarfallState}), landed by their
 * own next tick: no entity, no scheduler. While it falls the player's tick draws its streak. Its
 * own damage is magic, so {@link #landing} keeps it from calling another star in turn.
 *
 * <p>Its numbers are the rune's data: {@code base_damage}, {@code ad_ratio} (a share of your Attack
 * Damage), {@code radius}, {@code delay_seconds}, {@code cooldown_seconds},
 * {@code magic_hit_reduction_seconds}.
 */
public final class StarfallEffects {

    public static final ResourceLocation ID = Runes.id("starfall");

    /** True while a star's own damage lands. The server thread is the only one that deals damage. */
    private static boolean landing;

    private StarfallEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(StarfallEffects::onIncomingDamage);
        bus.addListener(StarfallEffects::onPlayerTick);
        MagicDamage.addObserver(StarfallEffects::onMagicDealt);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getAmount() <= 0.0f || !(event.getSource().getEntity() instanceof ServerPlayer attacker)
                || !(attacker.level() instanceof ServerLevel level)) {
            return;
        }

        Optional<RuneDefinition> rune = Runes.active(attacker, ID);
        if (rune.isPresent() && RuneHits.charged(event.getSource(), attacker, event.getEntity(), level.getGameTime())) {
            call(attacker, level, event.getEntity(), rune.get());
        }
    }

    private static void onMagicDealt(LivingEntity attacker, LivingEntity target, float amount) {
        if (landing || !(attacker instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        Optional<RuneDefinition> rune = Runes.active(player, ID);
        if (rune.isEmpty()) {
            return;
        }

        StarfallState state = player.getData(RuneAttachments.STARFALL.get());
        if (state.ready(level.getGameTime())) {
            call(player, level, target, rune.get());
        } else {
            state.shorten(rune.get().ticks("magic_hit_reduction_seconds", 1.0));
        }
    }

    private static void call(ServerPlayer player, ServerLevel level, LivingEntity target, RuneDefinition values) {
        StarfallState state = player.getData(RuneAttachments.STARFALL.get());
        long now = level.getGameTime();
        if (state.ready(now)) {
            state.call(now, target.position(), values.ticks("delay_seconds", 1.0), values.ticks("cooldown_seconds", 10.0));
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                    SoundSource.PLAYERS, 0.8f, 1.6f);
        }
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        StarfallState state = player.getData(RuneAttachments.STARFALL.get());
        if (!state.falling()) {
            return;
        }

        long now = level.getGameTime();
        Vec3 where = state.where();
        if (!state.due(now)) {
            double height = 1.0 + state.remaining(now) * 0.6;
            level.sendParticles(ParticleTypes.END_ROD, where.x, where.y + height, where.z, 2, 0.05, 0.1, 0.05, 0.0);
            return;
        }

        state.land();
        Optional<RuneDefinition> rune = Runes.active(player, ID);
        if (rune.isEmpty()) {
            return;
        }

        RuneDefinition values = rune.get();
        double radius = values.value("radius", 2.0);
        float damage = (float) (values.value("base_damage", 4.0)
                + values.value("ad_ratio", 0.20) * StatReader.value(player, CombatStats.ATTACK_DAMAGE));
        landing = true;
        try {
            for (LivingEntity foe : RuneTargets.foes(level, player, new AABB(where, where).inflate(radius, radius + 1.0, radius))) {
                MagicDamage.applyStandalone(foe, level.damageSources().indirectMagic(player, player), damage);
            }
        } finally {
            landing = false;
        }
        level.sendParticles(ParticleTypes.FIREWORK, where.x, where.y + 0.2, where.z, 24, radius * 0.4, 0.2, radius * 0.4, 0.15);
        level.playSound(null, where.x, where.y, where.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.5f, 1.8f);
    }
}
