package com.ascension.worlds.client;

import com.ascension.worlds.api.Planet;
import com.ascension.worlds.internal.WorldsContent;
import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * {@code ascension_worlds:dust_storm}: a wall of regolith kicked up by nothing (the Moon has no
 * wind, but the game does not need to explain that away to make the effect read) that blots out
 * vision and tints the world the same pale gray as the ground it came from.
 *
 * <p><strong>Grey, not tan.</strong> The first version tinted everything a warm dusty orange,
 * which read as an Earth desert sandstorm rather than lunar regolith &mdash; every colour below is
 * a neutral, slightly cool gray instead, matching {@code moon_regolith}'s own palette.
 *
 * <p><strong>Two independent visual layers, deliberately.</strong> {@link #applyFog}/{@link
 * #applyFogColor} touch the 3D world-fog pipeline, which a renderer-replacement mod (Sodium, in
 * this stack) is not guaranteed to still honour once it has reimplemented terrain rendering.
 * {@link #applyScreenOverlay} is the layer that actually guarantees the impairment: a flat wash
 * drawn on the 2D HUD pass, which no world renderer gets a chance to skip.
 */
final class DustStormEffect implements PlanetWeatherEffect {

    private static final RandomSource RANDOM = RandomSource.create();

    /** Particles spawned per client tick at full intensity. Scaled down as the storm ramps. */
    private static final int MAX_PARTICLES_PER_TICK = 24;

    private static final float SPAWN_RADIUS = 10.0f;
    private static final float SPAWN_HEIGHT = 3.0f;

    /** A single fixed heading rather than random drift each particle &mdash; wind blows one way. */
    private static final double WIND_X = 0.9;
    private static final double WIND_Z = 0.35;
    private static final double WIND_SPEED = 0.6;

    /** How opaque the screen wash gets at full intensity. Tuned to genuinely obscure, not tint. */
    private static final float MAX_OVERLAY_ALPHA = 0.78f;

    /** Neutral gray, matching {@code moon_regolith} -- not the warm tan the first version used. */
    private static final float FOG_R = 0.55f;
    private static final float FOG_G = 0.55f;
    private static final float FOG_B = 0.58f;
    private static final int OVERLAY_RGB = 0x8C8C90;

    /** How often (in ticks) to trigger the wind sound at full intensity. Vanilla's own rain-tick
     * loop replays a one-shot sound periodically rather than looping one seamlessly; same idea. */
    private static final int SOUND_INTERVAL_TICKS = 50;

    /** Below this, no sound at all -- a storm just starting or just finishing should be silent
     * for its first/last moment, not pop in at full volume. */
    private static final float SOUND_INTENSITY_THRESHOLD = 0.08f;

    private int ticksUntilNextSound = 0;

    @Override
    public void applyFog(ViewportEvent.RenderFog event, Planet.Weather config, float intensity) {
        if (intensity <= 0.0f) {
            return;
        }
        // Lerped from "may as well be unrestricted" at intensity 0 up to the configured tight
        // distance at intensity 1, so the fog itself ramps rather than snapping in.
        float far = Mth.lerp(intensity, 1000.0f, config.visibilityBlocks());
        event.setNearPlaneDistance(far * 0.1f);
        event.setFarPlaneDistance(far);
        event.setFogShape(FogShape.SPHERE);
        event.setCanceled(true);
    }

    @Override
    public void applyFogColor(ViewportEvent.ComputeFogColor event, Planet.Weather config, float intensity) {
        event.setRed(Mth.lerp(intensity, event.getRed(), (event.getRed() + FOG_R) / 2.0f));
        event.setGreen(Mth.lerp(intensity, event.getGreen(), (event.getGreen() + FOG_G) / 2.0f));
        event.setBlue(Mth.lerp(intensity, event.getBlue(), (event.getBlue() + FOG_B) / 2.0f));
    }

    @Override
    public void applyScreenOverlay(RenderGuiEvent.Post event, Planet.Weather config, float intensity) {
        if (intensity <= 0.0f) {
            return;
        }
        var window = Minecraft.getInstance().getWindow();
        int width = window.getGuiScaledWidth();
        int height = window.getGuiScaledHeight();

        int alpha = Math.round(MAX_OVERLAY_ALPHA * intensity * 255.0f);
        int color = (alpha << 24) | OVERLAY_RGB;
        event.getGuiGraphics().fill(0, 0, width, height, color);
    }

    @Override
    public void tick(Planet.Weather config, float intensity) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null || intensity <= 0.0f) {
            return;
        }

        tickParticles(level, player, intensity);
        tickSound(level, player, intensity);
    }

    private void tickParticles(ClientLevel level, LocalPlayer player, float intensity) {
        int count = Math.round(MAX_PARTICLES_PER_TICK * intensity);
        for (int i = 0; i < count; i++) {
            double x = player.getX() + (RANDOM.nextDouble() - 0.5) * SPAWN_RADIUS;
            double y = player.getEyeY() + (RANDOM.nextDouble() - 0.5) * SPAWN_HEIGHT;
            double z = player.getZ() + (RANDOM.nextDouble() - 0.5) * SPAWN_RADIUS;
            double speed = WIND_SPEED * (0.7 + RANDOM.nextDouble() * 0.6);
            level.addParticle(
                    WorldsContent.DUST_STORM_PARTICLE.get(), x, y, z,
                    WIND_X * speed, (RANDOM.nextDouble() - 0.5) * 0.02, WIND_Z * speed);
        }
    }

    /**
     * Reuses the Breeze mob's own idle-air sound &mdash; a real "moving air" whoosh already in the
     * game, rather than a new sound asset. Replayed periodically like vanilla's own rain-tick
     * sound rather than looped, so it survives the intensity ramp without needing a fade-capable
     * looping sound instance.
     */
    private void tickSound(ClientLevel level, LocalPlayer player, float intensity) {
        if (intensity < SOUND_INTENSITY_THRESHOLD) {
            ticksUntilNextSound = 0;
            return;
        }
        if (ticksUntilNextSound > 0) {
            ticksUntilNextSound--;
            return;
        }
        float volume = 0.15f + 0.35f * intensity;
        float pitch = 0.7f + RANDOM.nextFloat() * 0.2f;
        level.playLocalSound(
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.BREEZE_IDLE_AIR, SoundSource.WEATHER, volume, pitch, false);
        ticksUntilNextSound = SOUND_INTERVAL_TICKS + RANDOM.nextInt(SOUND_INTERVAL_TICKS / 2);
    }
}
