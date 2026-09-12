package com.ascension.combat.internal.weapons;

import com.ascension.combat.internal.CombatAttachments;
import com.ascension.combat.internal.CombatContent;
import com.ascension.combat.internal.mechanics.Dash;
import com.ascension.combat.internal.mechanics.LastDamageTarget;
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
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Galeforce's identity (docs/technical/combat-api.md section 6), revised by Sanchit 2026-09-08:
 * a dash that triggers a homing shot at the lowest-HP nearby target, or whoever the wielder last
 * dealt damage to if that's more recent, dealing bonus physical damage scaling with the target's
 * missing HP. On a cooldown.
 *
 * <p><b>Input.</b> Unlike the sword actives, this doesn't fire on plain right-click: a bow's
 * right-click already means "start drawing" ({@code BowItem#use}), and a normal draw-and-release
 * shot needs to keep working untouched. The active is gated on {@code sneak + right-click}
 * instead, the standard way modded items disambiguate a secondary use from an item's own vanilla
 * one; that combination cancels {@link PlayerInteractEvent.RightClickItem} outright so vanilla's
 * draw never starts on that click.
 *
 * <p><b>Dash, not blink.</b> Uses {@link Dash#forward}, not {@link
 * com.ascension.combat.internal.mechanics.Blink} &mdash; it's real movement that collides with
 * terrain and entities, unlike Wraithblade's instant teleport.
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

    /** Placeholder -- combat-api.md section 3: Sanchit tunes by playing, not by spec. */
    private static final double DASH_STRENGTH = 1.8;

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
        bus.addListener(GaleforceEffects::onRightClickItem);
        bus.addListener(GaleforceEffects::onIncomingDamage);
        bus.addListener(GaleforceEffects::onDamagePost);
        bus.addListener(GaleforceEffects::onServerTick);
    }

    private static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack stack = event.getItemStack();
        if (!stack.is(CombatContent.GALEFORCE.get()) || !player.isShiftKeyDown()) {
            return;
        }

        event.setCanceled(true);

        if (player.getCooldowns().isOnCooldown(stack.getItem())) {
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
        Dash.forward(player, DASH_STRENGTH);
        fireHomingShot(player, level, stack, ammo, target);
        player.getCooldowns().addCooldown(stack.getItem(), COOLDOWN_TICKS);
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
            event.setAmount(event.getAmount() + missingHealth * MISSING_HEALTH_BONUS_PERCENT);
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
