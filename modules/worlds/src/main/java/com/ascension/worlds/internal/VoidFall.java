package com.ascension.worlds.internal;

import com.ascension.worlds.api.Planet;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Falling out of the bottom of a world whose planet says {@code falls_to_space}: you drift down
 * gently (Slow Falling) and, a few blocks under the floor, are handed back to space beside the planet,
 * as if you had passed the asteroid. Built for the End (ADR-0028, Sanchit 2026-10-20: the void is not death).
 *
 * <p>Data-driven: it reads one planet flag, so another belt world needs no class. Only players, and
 * only below the world's floor; the check is one comparison per player per tick, and the planet lookup
 * runs only while someone is actually below the floor. Nothing is stored.
 */
public final class VoidFall {

    /** Under the floor by this much the player is carried out. At Slow Falling's pace, about this many seconds. */
    private static final int HANDOFF_DEPTH = 12;

    /** The dose of Slow Falling: long enough to read the message, refreshed while below the floor. */
    private static final int DOSE_TICKS = 200;

    /** And the dose that goes with them into space, where the planet's pull is not theirs to steer. */
    private static final int SPACE_DOSE_TICKS = 600;

    private VoidFall() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(VoidFall::onPlayerTick);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Level level = player.level();
        if (player.getY() >= level.getMinBuildHeight() || level.dimension().equals(SpaceDimension.KEY)) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        Planet planet = SpaceMechanics.planetWithSurface(server, level.dimension()).orElse(null);
        if (planet == null || !planet.fallsToSpace()) {
            return;
        }
        if (!player.hasEffect(MobEffects.SLOW_FALLING) || player.getEffect(MobEffects.SLOW_FALLING).getDuration() < DOSE_TICKS / 2) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, DOSE_TICKS, 0, true, false));
        }
        if (player.getY() >= level.getMinBuildHeight() - HANDOFF_DEPTH) {
            return;
        }
        ServerLevel space = server.getLevel(SpaceDimension.KEY);
        if (space == null) {
            return;
        }
        player.changeDimension(new DimensionTransition(
                space, SpaceMechanics.arrivalInSpace(planet, player.getYRot()), Vec3.ZERO,
                player.getYRot(), player.getXRot(), DimensionTransition.DO_NOTHING));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, SPACE_DOSE_TICKS, 0, true, false));
        player.displayClientMessage(
                Component.translatable("ascension_worlds.void_fall").withStyle(ChatFormatting.DARK_PURPLE), true);
    }
}
