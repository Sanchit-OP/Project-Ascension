package com.ascension.combat.internal.mechanics;

import com.ascension.combat.AscensionCombat;
import com.ascension.combat.internal.net.StasisSyncPayload;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Untargetable, immobile, camera forced to third person (docs/technical/combat-api.md section 5).
 * Guardian's Wing (originally built this) and Zonia's Hourglass are both this exact shape &mdash;
 * enter, hold, exit &mdash; just with different triggers and durations, so the mechanics live here
 * once rather than twice.
 *
 * <p>"Untargetable" is approximated as invulnerable ({@link
 * net.minecraft.world.entity.Entity#setInvulnerable}) rather than literally clearing every nearby
 * mob's target lock &mdash; nothing can land damage either way, and reaching into live mob AI
 * state for the same practical result isn't worth it. Immobile is a {@code -100%} {@link
 * Attributes#MOVEMENT_SPEED} modifier plus {@link net.minecraft.world.entity.Entity#setNoGravity}
 * (added 2026-09-20 alongside the lift below &mdash; without it the wearer just falls back down
 * mid-animation). The camera lock is the one part with no server-side equivalent to check, so it's
 * synced separately via {@link StasisSyncPayload}.
 *
 * <p><b>Reworked 2026-09-20</b> (Sanchit's own words: "doesn't feel that legendary") from a plain
 * invulnerability flag into a real moment: {@link #enter} lifts the wearer bodily, sets them
 * glowing, and bursts totem-gold particles with the totem sound, the same particle and sound
 * vanilla's own Totem of Undying uses for a death-save &mdash; deliberately reused rather than
 * invented, since "a golden flash that saves you from death" is exactly what a totem pop already
 * reads as, and Minecraft's client-side rendering has no supported (non-mixin) way to actually
 * recolor an entity's skin the way "make it full golden" would need literally. {@link #tick} keeps
 * a light particle trickle going while held; {@link #exit} plays the same burst once more on the
 * way down.
 *
 * <p>Split into three calls (was one {@code apply(active)}) because the old single method was
 * called every tick while active, which would have repeated the lift and particle burst every
 * tick instead of once: {@link #enter} is the one-shot "this just started" moment, {@link #tick}
 * is the per-tick "still holding" reassertion (idempotent, safe to call repeatedly, same as every
 * other modifier-reapplication idiom in this catalog), {@link #exit} is the one-shot "this just
 * ended" moment. Callers own their own duration window (a {@link DurationState}, checked per tick)
 * and decide when to call each.
 */
public final class Stasis {

    private static final ResourceLocation MOVE_SPEED_MODIFIER_ID = ResourceLocation
            .fromNamespaceAndPath(AscensionCombat.MOD_ID, "stasis_immobile");

    private static final double LIFT_HEIGHT = 2.0;
    private static final int BURST_PARTICLE_COUNT = 30;
    private static final int TRICKLE_PARTICLE_COUNT = 2;
    private static final long TRICKLE_INTERVAL_TICKS = 5L;

    private Stasis() {
    }

    /** The one-shot moment stasis begins: lift, glow, gravity off, a totem-style burst. */
    public static void enter(ServerPlayer player) {
        applyLock(player, true);
        player.setNoGravity(true);
        player.setGlowingTag(true);
        player.teleportTo(player.getX(), player.getY() + LIFT_HEIGHT, player.getZ());
        player.setDeltaMovement(Vec3.ZERO);

        if (player.level() instanceof ServerLevel level) {
            burst(level, player, 1.0f);
        }
    }

    /** Called every tick while stasis is active: reasserts the lock, and a light particle trickle. */
    public static void tick(ServerPlayer player) {
        applyLock(player, true);

        if (player.level() instanceof ServerLevel level && level.getGameTime() % TRICKLE_INTERVAL_TICKS == 0) {
            level.sendParticles(
                    ParticleTypes.TOTEM_OF_UNDYING,
                    player.getX(),
                    player.getY(1.0),
                    player.getZ(),
                    TRICKLE_PARTICLE_COUNT,
                    0.3,
                    0.5,
                    0.3,
                    0.02);
        }
    }

    /** The one-shot moment stasis ends: lock and gravity restored, glow off, a final burst. */
    public static void exit(ServerPlayer player) {
        applyLock(player, false);
        player.setNoGravity(false);
        player.setGlowingTag(false);

        if (player.level() instanceof ServerLevel level) {
            burst(level, player, 1.4f);
        }
    }

    private static void burst(ServerLevel level, ServerPlayer player, float soundPitch) {
        level.sendParticles(
                ParticleTypes.TOTEM_OF_UNDYING,
                player.getX(),
                player.getY(1.0),
                player.getZ(),
                BURST_PARTICLE_COUNT,
                0.4,
                0.6,
                0.4,
                0.08);
        level.playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, soundPitch);
    }

    private static void applyLock(ServerPlayer player, boolean active) {
        player.setInvulnerable(active);

        AttributeInstance moveSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (moveSpeed == null) {
            return;
        }

        moveSpeed.removeModifier(MOVE_SPEED_MODIFIER_ID);
        if (!active) {
            return;
        }

        moveSpeed.addTransientModifier(new AttributeModifier(
                MOVE_SPEED_MODIFIER_ID, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    public static void syncToClient(ServerPlayer player, boolean active) {
        PacketDistributor.sendToPlayer(player, new StasisSyncPayload(active));
    }
}
