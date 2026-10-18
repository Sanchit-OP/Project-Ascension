package com.ascension.combat.internal.weapons;

import com.ascension.combat.internal.mechanics.AbilityScaling;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.particles.ParticleTypes;
import com.ascension.combat.internal.mechanics.Presence;
import com.ascension.combat.internal.CombatAttachments;
import com.ascension.combat.internal.mechanics.ItemAbilities;
import com.ascension.combat.internal.CombatContent;
import com.ascension.combat.internal.mechanics.Blink;
import com.ascension.combat.internal.mechanics.DurationState;
import com.ascension.combat.internal.mechanics.LastDamageTarget;
import com.ascension.combat.internal.net.AbilityCooldownSyncPayload;
import com.ascension.combat.internal.net.AbilityKey;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Galeforce's identity (docs/technical/combat-api.md section 6), revised by Sanchit 2026-09-08:
 * a dash that triggers a homing shot at the lowest-HP nearby target, or whoever the wielder last
 * dealt damage to if that's more recent, dealing bonus physical damage scaling with the target's
 * missing HP. On a cooldown.
 *
 * <p><b>Input, revised again 2026-09-20.</b> Originally gated on {@code sneak + right-click},
 * cancelling {@code PlayerInteractEvent.RightClickItem} so vanilla's own bow-draw never started on
 * that click. Dropped after Sanchit's test found the dash simply never fired: sneaking while aiming
 * at an entity (exactly when you'd want a targeted dash) routes through a different interaction
 * event entirely, one this listener never sees, so the whole combination was unreliable by
 * construction, not just awkward. {@link #tryActivate} is now the second consumer of the same
 * shared ability key {@link com.ascension.combat.internal.armor.GargoyleStoneplateEffects} and
 * {@link com.ascension.combat.internal.armor.RocketbeltEffects} already use &mdash; the first held
 * weapon to, confirming that key was always "whatever's equipped with an active you press a
 * dedicated button for," not armor-specifically. Plain right-click stays untouched vanilla bow
 * behaviour either way.
 *
 * <p><b>Cooldown, also revised 2026-09-20.</b> Sanchit's next test found the bow itself unusable
 * for the whole cooldown after one activation. It had always tracked the cooldown via vanilla
 * {@code ItemCooldowns} on the item, which gates the item's entire right-click use, not just this
 * active &mdash; and a bow's own ordinary draw goes through that same path (unlike a sword's
 * left-click attack, which never touches it). Moved to a private cooldown ({@link
 * CombatAttachments#GALEFORCE_COOLDOWN}), the same fix and the same reasoning as {@link
 * HeartwoodOfPallasEffects}'s own fourth pass, paired with the same {@link
 * com.ascension.combat.client.AbilityCooldownDecorator} for the hotbar swipe.
 *
 * <p><b>Dash mechanic, replaced 2026-09-20.</b> Originally {@link
 * com.ascension.combat.internal.mechanics.Dash#forward} (real, collidable knockback-based
 * movement) rather than a teleport, since the two were deliberately built as different mechanics
 * (docs/technical/combat-api.md section 5). Even at a strength of 4.0 Sanchit still saw no movement
 * at all -- knockback on a player is a velocity change the client has to actually integrate, and
 * whatever's suppressing that here (most likely the same class of server/client movement
 * reconciliation that makes large server-set player velocities unreliable in general) was never
 * pinned down. Rather than keep guessing at a strength value, this now calls {@link Blink#forward}
 * instead &mdash; the same raycast-clamped, wall-safe teleport Wraithblade uses and that Sanchit
 * confirmed actually works. Trades away real collidable travel (you can dash a couple blocks short
 * of a wall instead of thudding into it) for something that reliably moves the wielder at all.
 *
 * <p><b>The shot is spawned directly</b>, not read off the wielder's next normal release: it's
 * aimed at the resolved target (or the wielder's look direction if none is found) the instant the
 * dash fires, tagged via {@link CombatAttachments#GALEFORCE_EMPOWERED} on the arrow entity itself,
 * and then steered toward its target every server tick ({@link #onServerTick}) for as long as both
 * the arrow and the target stay valid &mdash; true homing, not a one-time aim-and-forget, done by
 * nudging a plain vanilla {@link Arrow}'s velocity rather than a bespoke projectile entity.
 *
 * <p><b>Target selection.</b> {@link CombatAttachments#GALEFORCE_LAST_TARGET} is updated in {@link
 * #onDamagePost} for every hit the wielder lands with <em>any</em> weapon, not just this one, and
 * is preferred if it's still alive and within the catalog's usual 5s window; otherwise {@link
 * #findTarget} scans nearby living entities for the lowest current HP.
 */
public final class GaleforceEffects {

    /** A real block distance now (Blink is raycast-clamped), matching Sanchit's own "at least 6 blocks" spec exactly. */
    private static final double DASH_DISTANCE = 6.0;

    private static final float MISSING_HEALTH_BONUS_PERCENT = 0.20f;
    private static final int COOLDOWN_TICKS = 160;
    private static final long LAST_TARGET_WINDOW_TICKS = 100L;
    private static final double NEARBY_SCAN_RADIUS = 16.0;
    private static final double HOMING_TURN_FACTOR = 0.2;
    private static final float ARROW_VELOCITY = 3.0f;
    private static final float ARROW_INACCURACY = 1.0f;

    private static final Map<UUID, HomingLink> HOMING_ARROWS = new HashMap<>();

    private GaleforceEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(GaleforceEffects::onIncomingDamage);
        bus.addListener(GaleforceEffects::onDamagePost);
        bus.addListener(GaleforceEffects::onServerTick);
    }

    /** Entry point for the shared ability key -- see this class's own javadoc for why. */
    public static void tryActivate(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(CombatContent.GALEFORCE.get())) {
            return;
        }

        long now = player.level().getGameTime();
        DurationState cooldown = player.getData(CombatAttachments.GALEFORCE_COOLDOWN);
        if (cooldown.isActive(now)) {
            return;
        }

        ItemStack ammo = player.getProjectile(stack);
        if (ammo.isEmpty()) {
            return;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        LivingEntity target = findTarget(player, level);
        Presence.burst(player, SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.8f, 1.2f, ParticleTypes.CLOUD, 20, 0.4);
        Blink.forward(player, DASH_DISTANCE);
        fireHomingShot(player, level, stack, ammo, target);

        int cooldownTicks = ItemAbilities.cooldown(player, COOLDOWN_TICKS);
        cooldown.activateUntil(now + cooldownTicks);
        PacketDistributor.sendToPlayer(
                player, new AbilityCooldownSyncPayload(AbilityKey.GALEFORCE, now + cooldownTicks, cooldownTicks));
    }

    private static LivingEntity findTarget(ServerPlayer player, ServerLevel level) {
        LastDamageTarget lastTarget = player.getData(CombatAttachments.GALEFORCE_LAST_TARGET);
        long now = level.getGameTime();
        if (lastTarget.isValid(now, LAST_TARGET_WINDOW_TICKS)
                && level.getEntity(lastTarget.targetId()) instanceof LivingEntity remembered
                && remembered.isAlive()) {
            return remembered;
        }

        LivingEntity lowestHealth = null;
        for (LivingEntity candidate
                : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(NEARBY_SCAN_RADIUS))) {
            if (candidate == player || !candidate.isAlive()) {
                continue;
            }
            if (lowestHealth == null || candidate.getHealth() < lowestHealth.getHealth()) {
                lowestHealth = candidate;
            }
        }
        return lowestHealth;
    }

    private static void fireHomingShot(
            ServerPlayer player, ServerLevel level, ItemStack weapon, ItemStack ammo, LivingEntity target) {
        ItemStack pickupStack = ammo.copyWithCount(1);
        if (!player.hasInfiniteMaterials()) {
            ammo.shrink(1);
        }

        Arrow arrow = new Arrow(level, player, pickupStack, weapon);
        Vec3 origin = player.getEyePosition();
        Vec3 direction = target != null
                ? target.getBoundingBox().getCenter().subtract(origin).normalize()
                : player.getLookAngle();
        arrow.shoot(direction.x, direction.y, direction.z, ARROW_VELOCITY, ARROW_INACCURACY);
        level.addFreshEntity(arrow);

        arrow.getData(CombatAttachments.GALEFORCE_EMPOWERED).arm();
        if (target != null) {
            HOMING_ARROWS.put(arrow.getUUID(), new HomingLink(level.dimension(), target.getUUID()));
        }
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getAmount() <= 0.0f) {
            return;
        }

        DamageSource source = event.getSource();
        ItemStack weapon = source.getWeaponItem();
        if (weapon == null || !weapon.is(CombatContent.GALEFORCE.get())) {
            return;
        }

        if (!(source.getDirectEntity() instanceof AbstractArrow arrow)) {
            return;
        }

        if (!arrow.getData(CombatAttachments.GALEFORCE_EMPOWERED).isArmed()) {
            return;
        }

        float missingHealth = event.getEntity().getMaxHealth() - event.getEntity().getHealth();
        if (missingHealth > 0.0f) {
            event.setAmount(event.getAmount() + missingHealth
                    * (MISSING_HEALTH_BONUS_PERCENT
                    + AbilityScaling.GALEFORCE_MISSING_HP_PER_AD * AbilityScaling.attackDamage(source.getEntity())));
        }
    }

    private static void onDamagePost(LivingDamageEvent.Post event) {
        if (event.getNewDamage() <= 0.0f) {
            return;
        }

        if (!(event.getSource().getEntity() instanceof Player attacker)) {
            return;
        }

        attacker.getData(CombatAttachments.GALEFORCE_LAST_TARGET)
                .record(event.getEntity().getUUID(), attacker.level().getGameTime());
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        if (HOMING_ARROWS.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<UUID, HomingLink>> iterator = HOMING_ARROWS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, HomingLink> entry = iterator.next();
            ServerLevel level = event.getServer().getLevel(entry.getValue().level());
            if (level == null) {
                iterator.remove();
                continue;
            }

            if (!(level.getEntity(entry.getKey()) instanceof AbstractArrow arrow) || arrow.isRemoved()) {
                iterator.remove();
                continue;
            }

            if (!(level.getEntity(entry.getValue().targetId()) instanceof LivingEntity target)
                    || !target.isAlive()) {
                iterator.remove();
                continue;
            }

            steerToward(arrow, target);
        }
    }

    private static void steerToward(AbstractArrow arrow, LivingEntity target) {
        Vec3 currentVelocity = arrow.getDeltaMovement();
        double speed = currentVelocity.length();
        if (speed < 1.0E-4) {
            return;
        }

        Vec3 desiredDirection = target.getBoundingBox().getCenter().subtract(arrow.position()).normalize();
        Vec3 currentDirection = currentVelocity.scale(1.0 / speed);
        Vec3 blended = currentDirection
                .scale(1.0 - HOMING_TURN_FACTOR)
                .add(desiredDirection.scale(HOMING_TURN_FACTOR))
                .normalize();
        arrow.setDeltaMovement(blended.scale(speed));
    }

    private record HomingLink(ResourceKey<Level> level, UUID targetId) {
    }
}
