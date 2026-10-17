package com.ascension.combat.internal.rune.reaver;

import com.ascension.combat.internal.rune.RuneAttachments;
import com.ascension.combat.internal.rune.RuneDefinition;
import com.ascension.combat.internal.rune.RuneHits;
import com.ascension.combat.internal.rune.Runes;
import java.util.Optional;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Reaver's Deadeye keystone (docs/technical/rune-system.md section 4.2): every ranged hit deals
 * more damage the farther away the enemy is, and a hit to the <b>head</b> deals a lot more on top.
 * A headshot is announced with a chime, a flash and the word on the action bar, so there is no
 * doubt it happened.
 *
 * <p>The distance bonus needs no help from the weapon: any projectile that damages an enemy
 * ({@link RuneHits#ranged}), from any mod, gets it. Only the headshot needs to know where the shot
 * landed. For arrows that is the impact ({@link ProjectileImpactEvent}): where the arrow meets the
 * enemy ({@link RuneHits#contactHeight}; vanilla's own hit result has no contact point), at or above
 * eye height less {@code head_margin}, is the head. A guns mod, whose bullets have their own head hitbox,
 * says so through {@link com.ascension.combat.api.rune.RangedHits#markHeadshot}. Both leave the
 * same note on the shooter ({@link HeadshotMark}), which the damage event that follows reads.
 *
 * <p>Its numbers are the rune's data: {@code head_bonus}, {@code per_block} and {@code
 * start_distance} (the distance bonus grows for each block past it), {@code max_distance_bonus},
 * and {@code head_margin} (how far below the eyes still counts as the head).
 */
public final class DeadeyeEffects {

    public static final ResourceLocation ID = Runes.id("deadeye");

    private DeadeyeEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(DeadeyeEffects::onImpact);
        bus.addListener(DeadeyeEffects::onIncomingDamage);
    }

    private static void onImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof Projectile shot) || !(shot.getOwner() instanceof ServerPlayer shooter)) {
            return;
        }

        if (!(event.getRayTraceResult() instanceof EntityHitResult hit) || !(hit.getEntity() instanceof LivingEntity victim)) {
            return;
        }

        Optional<RuneDefinition> rune = Runes.active(shooter, ID);
        if (rune.isPresent() && RuneHits.contactHeight(shot, victim) >= victim.getEyeY() - rune.get().value("head_margin", 0.3)) {
            HeadshotMark.mark(shooter, victim);
        }
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getAmount() <= 0.0f || !RuneHits.ranged(event.getSource())
                || !(event.getSource().getEntity() instanceof ServerPlayer shooter)
                || !(shooter.level() instanceof ServerLevel level)) {
            return;
        }

        Optional<RuneDefinition> rune = Runes.active(shooter, ID);
        if (rune.isEmpty()) {
            return;
        }

        RuneDefinition values = rune.get();
        LivingEntity victim = event.getEntity();
        float bonus = distanceBonus(shooter.distanceTo(victim), values.value("per_block", 0.02),
                values.value("start_distance", 10.0), values.value("max_distance_bonus", 0.5));
        boolean head = shooter.getData(RuneAttachments.HEADSHOT.get()).consume(victim.getUUID(), level.getGameTime());
        if (head) {
            bonus += (float) values.value("head_bonus", 0.5);
            level.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getEyeY(), victim.getZ(), 14, 0.25, 0.25, 0.25, 0.3);
            shooter.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 1.9f);
            shooter.displayClientMessage(Component.translatable("rune.ascension_combat.deadeye.headshot"), true);
        }

        if (bonus > 0.0f) {
            event.setAmount(event.getAmount() * (1.0f + bonus));
        }
    }

    /**
     * What distance adds, as a fraction: nothing up to {@code startDistance} blocks, then {@code
     * perBlock} more for every block past it, never above {@code max}.
     */
    static float distanceBonus(double distance, double perBlock, double startDistance, double max) {
        return (float) Math.min(max, perBlock * Math.max(0.0, distance - startDistance));
    }
}
