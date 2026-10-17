package com.ascension.combat.internal.rune.reaver;

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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Reaver's Deadeye keystone (docs/technical/rune-system.md section 4.2): an arrow that lands near
 * the head is a <b>headshot</b>, with bonus damage that grows with the distance of the shot, and a
 * clear ding for the archer.
 *
 * <p>Two steps, because the hit's position is only known when the arrow reaches the enemy
 * ({@link ProjectileImpactEvent}) and the damage is changed by the event after it: the first
 * leaves a {@link HeadshotMark} on the arrow, the second reads it. A body shot clears the mark, so
 * a piercing arrow is judged afresh on every enemy it passes through.
 *
 * <p>Its numbers are the rune's data: {@code base_bonus}, {@code per_block} and {@code
 * start_distance} (the bonus grows for each block past it), {@code max_bonus}, and {@code
 * head_margin} (how far below the eyes still counts as the head).
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
        if (!(event.getProjectile() instanceof AbstractArrow arrow) || !(arrow.getOwner() instanceof ServerPlayer shooter)) {
            return;
        }

        if (!(event.getRayTraceResult() instanceof EntityHitResult hit) || !(hit.getEntity() instanceof LivingEntity victim)) {
            return;
        }

        Optional<RuneDefinition> rune = Runes.active(shooter, ID);
        if (rune.isEmpty() || !(shooter.level() instanceof ServerLevel level)) {
            return;
        }

        RuneDefinition values = rune.get();
        HeadshotMark mark = arrow.getData(RuneAttachments.HEADSHOT.get());
        mark.set(0.0f);
        if (hit.getLocation().y < victim.getEyeY() - values.value("head_margin", 0.3)) {
            return;
        }

        double distance = shooter.position().distanceTo(hit.getLocation());
        mark.set(bonus(distance, values.value("base_bonus", 0.5), values.value("per_block", 0.02),
                values.value("start_distance", 10.0), values.value("max_bonus", 1.0)));
        level.sendParticles(ParticleTypes.CRIT, hit.getLocation().x, hit.getLocation().y, hit.getLocation().z,
                12, 0.2, 0.2, 0.2, 0.3);
        shooter.playNotifySound(SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getAmount() <= 0.0f || !(event.getSource().getDirectEntity() instanceof AbstractArrow arrow)) {
            return;
        }

        arrow.getExistingData(RuneAttachments.HEADSHOT.get()).ifPresent(mark -> {
            if (mark.bonus() > 0.0f) {
                event.setAmount(event.getAmount() * (1.0f + mark.bonus()));
            }
        });
    }

    /**
     * The headshot bonus as a fraction: {@code base} up to {@code startDistance} blocks, then
     * {@code perBlock} more for every block past it, never above {@code max}.
     */
    static float bonus(double distance, double base, double perBlock, double startDistance, double max) {
        return (float) Math.min(max, base + perBlock * Math.max(0.0, distance - startDistance));
    }
}
