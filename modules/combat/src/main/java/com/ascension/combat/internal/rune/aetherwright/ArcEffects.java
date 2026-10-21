package com.ascension.combat.internal.rune.aetherwright;

import com.ascension.combat.api.stat.CombatStats;
import com.ascension.combat.api.stat.StatReader;
import com.ascension.combat.internal.mechanics.ItemAbilities;
import com.ascension.combat.internal.mechanics.MagicDamage;
import com.ascension.combat.internal.rune.Reentry;
import com.ascension.combat.internal.rune.RuneAttachments;
import com.ascension.combat.internal.rune.RuneDefinition;
import com.ascension.combat.internal.rune.RuneHits;
import com.ascension.combat.internal.rune.RuneTargets;
import com.ascension.combat.internal.rune.Runes;
import com.ascension.core.api.CombatStateRegistry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Aetherwright's Arc keystone (docs/technical/rune-system.md section 4.3), redesigned 2026-10-22 (Sanchit's design): a few hits
 * charge the weapon, and <b>from then on every hit throws a bolt until the fight ends</b>. A bolt is {@code strikes} strikes of
 * lightning (four), shared among up to that many enemies near the one you hit; if fewer enemies stand there, the strikes come round
 * again, each {@code falloff} as strong as the last one on the same enemy, so a lone enemy takes four strikes of 100%, 70%, 49% and
 * 34%, and four enemies take one full strike each.
 *
 * <p>A strike is {@code ad_ratio} of the wielder's Attack Damage, as magic damage (so Magic Resist and the Aetherwright boosters
 * all apply). The strikes meant for the enemy you hit are <b>added to the landing hit</b>, as Ragefang's magic is ({@link
 * MagicDamage#apply}), so its invulnerability frames cannot swallow them; the rest land as {@link MagicDamage#applyStrike}. A bolt
 * leaves at most once every {@code bolt_gap_seconds}, so a weapon that swings seven times a second does not throw seven. Using an
 * item's active charges the weapon outright; the charge ends when the player is out of combat.
 *
 * <p>Only a weapon hit counts toward the charge or throws a bolt ({@link RuneHits#weaponHit}): the lightning is magic and cannot
 * feed itself, and {@link #RELEASING} is a second guard.
 */
public final class ArcEffects {
    public static final ResourceLocation ID = Runes.id("arc");

    /** Guards the lightning's own damage from counting as hits. */
    private static final Reentry RELEASING = new Reentry();

    private ArcEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(ArcEffects::onDamagePre);
        bus.addListener(ArcEffects::onPlayerTick);
        ItemAbilities.addUseObserver(ArcEffects::onAbilityUsed);
    }

    private static void onAbilityUsed(ServerPlayer player) {
        if (Runes.active(player, ID).isPresent()) {
            player.getData(RuneAttachments.ARC.get()).charge();
        }
    }

    /** The fight ended: the charge goes. Reads no page, so a swapped-out rune cannot leave a charge behind. */
    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ArcState state = player.getData(RuneAttachments.ARC.get());
            if (state.charged() && !CombatStateRegistry.isInCombat(player)) {
                state.discharge();
            }
        }
    }

    private static void onDamagePre(LivingDamageEvent.Pre event) {
        if (RELEASING.active() || event.getNewDamage() <= 0.0f || !(event.getSource().getEntity() instanceof ServerPlayer attacker)
                || !RuneHits.weaponHit(event.getSource()) || !(attacker.level() instanceof ServerLevel level)) {
            return;
        }
        Optional<RuneDefinition> rune = Runes.active(attacker, ID);
        if (rune.isEmpty()) {
            return;
        }
        RuneDefinition values = rune.get();
        ArcState state = attacker.getData(RuneAttachments.ARC.get());
        if (!state.charged()) {
            state.count(values.count("hits", 3));
            return;
        }
        long now = level.getGameTime();
        if (state.boltReady(now, values.ticks("bolt_gap_seconds", 0.3))) {
            state.bolt(now);
            throwBolt(attacker, level, event, values);
        }
    }

    private static void throwBolt(ServerPlayer player, ServerLevel level, LivingDamageEvent.Pre event, RuneDefinition values) {
        LivingEntity first = event.getEntity();
        double radius = values.value("jump_radius", 5.0);
        int strikes = values.count("strikes", 4);
        List<LivingEntity> targets = new ArrayList<>();
        targets.add(first);
        RuneTargets.foes(level, player, first.getBoundingBox().inflate(radius)).stream()
                .filter(e -> e != first)
                .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(first)))
                .limit(Math.max(0, strikes - 1))
                .forEach(targets::add);

        float strike = (float) (values.value("ad_ratio", 0.05) * StatReader.value(player, CombatStats.ATTACK_DAMAGE));
        float[] shares = shares(targets.size(), strikes, (float) values.value("falloff", 0.7));
        RELEASING.run(() -> {
            // What the enemy you hit takes rides its own hit; the others are struck where they stand.
            MagicDamage.apply(event, strike * shares[0]);
            for (int i = 1; i < targets.size(); i++) {
                MagicDamage.applyStrike(targets.get(i), level.damageSources().indirectMagic(player, player), strike * shares[i]);
            }
        });
        spark(level, player.getEyePosition(), first.getEyePosition());
        for (int i = 1; i < targets.size(); i++) {
            spark(level, targets.get(i - 1).getEyePosition(), targets.get(i).getEyePosition());
        }
        if (targets.size() < strikes) {
            // The extra strikes on the same enemies: a burst of sparks round each, one for every strike beyond the first.
            for (LivingEntity target : targets) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY(0.6), target.getZ(),
                        6 * (strikes - targets.size()) / targets.size(), 0.3, 0.4, 0.3, 0.2);
            }
        }
        level.playSound(null, first.getX(), first.getY(), first.getZ(), SoundEvents.TRIDENT_THUNDER.value(),
                SoundSource.PLAYERS, 0.4f, 2.0f);
    }

    /**
     * How much of one strike each enemy takes from a bolt: the {@code strikes} go to the {@code targets} in turn, and each time round an
     * enemy is struck again it takes {@code falloff} times what it took before. Four strikes on four enemies is 1 each; on one
     * enemy 1 + 0.7 + 0.49 + 0.343; on two, 1.7 each.
     */
    static float[] shares(int targets, int strikes, float falloff) {
        float[] shares = new float[Math.max(1, targets)];
        for (int strike = 0; strike < strikes; strike++) {
            int target = strike % shares.length;
            int round = strike / shares.length;
            shares[target] += (float) Math.pow(falloff, round);
        }
        return shares;
    }

    /** A line of electric sparks between two points. */
    private static void spark(ServerLevel level, Vec3 from, Vec3 to) {
        int steps = Math.max(2, (int) (from.distanceTo(to) * 3));
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.lerp(to, i / (double) steps);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
    }
}
