package com.ascension.combat.internal.armor;

import com.ascension.combat.internal.CombatAttachments;
import com.ascension.combat.internal.CombatContent;
import com.ascension.combat.internal.mechanics.Blink;
import com.ascension.combat.internal.mechanics.DurationState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Rocketbelt's identity (docs/technical/combat-api.md section 6): a short dash into an area burst
 * of damage on landing.
 *
 * <p>Triggered by {@link com.ascension.combat.internal.net.ActivateArmorAbilityPayload}, same as
 * {@link GargoyleStoneplateEffects} &mdash; the second real consumer of the shared armor-ability
 * key, exactly what it was built for.
 *
 * <p><b>Dash mechanic, replaced 2026-09-20.</b> Originally {@code Dash.forward} (real, collidable
 * knockback-based movement), the same mechanic {@link
 * com.ascension.combat.internal.weapons.GaleforceEffects} used to use. Never actually tested by
 * Sanchit before Galeforce's own identical dash was confirmed not to produce any visible movement
 * at all &mdash; fixed there by switching to {@link Blink#forward} instead, and Sanchit asked for
 * the same swap here proactively rather than waiting to hit the same dead dash twice. Trades away
 * real collidable travel for a reliable, wall-safe teleport.
 *
 * <p>"On landing" doesn't need actual ground-contact detection: the burst fires a short, fixed
 * delay after activation ({@link #BURST_DELAY_TICKS}), wherever the wearer has ended up by then.
 * Reuses {@link DurationState#hasExpired} for that delay &mdash; the same one-shot-after-a-window
 * shape {@link GuardianWingEffects} uses for its revive, just with a much shorter window and a
 * different payoff.
 */
public final class RocketbeltEffects {

    /** A real block distance now (Blink is raycast-clamped), same as Galeforce's own fix. */
    private static final double DASH_DISTANCE = 6.0;

    private static final long BURST_DELAY_TICKS = 10L;
    private static final double BURST_RADIUS = 4.0;
    private static final float BURST_DAMAGE = 6.0f;
    private static final int COOLDOWN_TICKS = 300;

    private RocketbeltEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(RocketbeltEffects::onPlayerTick);
    }

    public static void tryActivate(ServerPlayer player) {
        if (!player.getItemBySlot(EquipmentSlot.LEGS).is(CombatContent.ROCKETBELT.get())) {
            return;
        }

        if (player.getCooldowns().isOnCooldown(CombatContent.ROCKETBELT.get())) {
            return;
        }

        Blink.forward(player, DASH_DISTANCE);

        long now = player.level().getGameTime();
        player.getData(CombatAttachments.ROCKETBELT_BURST).activateUntil(now + BURST_DELAY_TICKS);
        player.getCooldowns().addCooldown(CombatContent.ROCKETBELT.get(), COOLDOWN_TICKS);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        DurationState burst = player.getData(CombatAttachments.ROCKETBELT_BURST);
        long now = player.level().getGameTime();
        if (!burst.hasExpired(now)) {
            return;
        }

        burst.clear();
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        for (LivingEntity target
                : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(BURST_RADIUS))) {
            if (target == player || !target.isAlive()) {
                continue;
            }
            target.hurt(player.damageSources().playerAttack(player), BURST_DAMAGE);
        }
    }
}
