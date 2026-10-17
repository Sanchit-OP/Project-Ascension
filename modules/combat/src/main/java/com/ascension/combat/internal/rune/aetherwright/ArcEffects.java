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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Aetherwright's Arc keystone (docs/technical/rune-system.md section 4.3): every few hits charge
 * your weapon, and the next hit releases lightning that jumps from enemy to enemy as magic damage.
 * Using an item's active (anything that goes through {@link ItemAbilities}) charges it outright.
 *
 * <p>Only a weapon hit counts toward a charge ({@link RuneHits#weaponHit}): the lightning is
 * magic, so it can't charge itself, and {@link #RELEASING} is a second guard. The jump targets are
 * {@link RuneTargets foes}, found once, when the lightning is released.
 *
 * <p>Its numbers are the rune's data: {@code hits}, {@code base_damage}, {@code ad_ratio},
 * {@code jumps}, {@code jump_radius}.
 */
public final class ArcEffects {

    public static final ResourceLocation ID = Runes.id("arc");

    /** Guards the lightning's own damage from counting as hits that charge the weapon. */
    private static final Reentry RELEASING = new Reentry();

    private ArcEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(ArcEffects::onIncomingDamage);
        ItemAbilities.addUseObserver(ArcEffects::onAbilityUsed);
    }

    private static void onAbilityUsed(ServerPlayer player) {
        if (Runes.active(player, ID).isPresent()) {
            player.getData(RuneAttachments.ARC.get()).charge();
        }
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (RELEASING.active() || event.getAmount() <= 0.0f || !(event.getSource().getEntity() instanceof ServerPlayer attacker)
                || !RuneHits.weaponHit(event.getSource()) || !(attacker.level() instanceof ServerLevel level)) {
            return;
        }

        Optional<RuneDefinition> rune = Runes.active(attacker, ID);
        if (rune.isEmpty()) {
            return;
        }

        ArcState state = attacker.getData(RuneAttachments.ARC.get());
        if (state.charged()) {
            state.release();
            release(attacker, level, event.getEntity(), rune.get());
        } else {
            state.count(rune.get().count("hits", 4));
        }
    }

    private static void release(ServerPlayer player, ServerLevel level, LivingEntity first, RuneDefinition values) {
        float damage = (float) (values.value("base_damage", 3.0)
                + values.value("ad_ratio", 0.15) * StatReader.value(player, CombatStats.ATTACK_DAMAGE));
        double radius = values.value("jump_radius", 5.0);
        List<LivingEntity> struck = new ArrayList<>();
        RELEASING.run(() -> {
            LivingEntity current = first;
            for (int jump = 0; jump <= values.count("jumps", 3) && current != null; jump++) {
                struck.add(current);
                MagicDamage.applyStandalone(current, level.damageSources().indirectMagic(player, player), damage);
                LivingEntity from = current;
                current = RuneTargets.foes(level, player, from.getBoundingBox().inflate(radius)).stream()
                        .filter(e -> !struck.contains(e))
                        .min(Comparator.comparingDouble(e -> e.distanceToSqr(from)))
                        .orElse(null);
            }
        });

        spark(level, player.getEyePosition(), struck.get(0).getEyePosition());
        for (int i = 1; i < struck.size(); i++) {
            spark(level, struck.get(i - 1).getEyePosition(), struck.get(i).getEyePosition());
        }
        level.playSound(null, first.getX(), first.getY(), first.getZ(), SoundEvents.TRIDENT_THUNDER.value(),
                SoundSource.PLAYERS, 0.5f, 2.0f);
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
