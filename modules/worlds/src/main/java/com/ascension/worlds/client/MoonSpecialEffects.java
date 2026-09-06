package com.ascension.worlds.client;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Same visual parameters as vanilla's own {@code EndEffects} &mdash; {@code dimension_type/
 * moon.json} pointed {@code "effects"} straight at {@code minecraft:the_end} before this class
 * existed, and nothing about that appearance is wrong, so it is reproduced here rather than
 * changed.
 *
 * <p>The one thing a vanilla effects id cannot give us: {@link #renderSnowAndRain} and
 * {@link #tickRain} both unconditionally return {@code true}, which per {@code
 * IDimensionSpecialEffectsExtension}'s own contract means "prevent vanilla snow/rain rendering
 * and ticking" &mdash; belt-and-suspenders on top of both Moon biomes already declaring {@code
 * has_precipitation: false}. The Moon is airless; there is no mechanism by which weather of any
 * kind can exist on it, and this makes that a guarantee enforced in code rather than a property
 * that happens to fall out of two biome JSON files staying correctly configured forever.
 */
final class MoonSpecialEffects extends DimensionSpecialEffects {

    static final MoonSpecialEffects INSTANCE = new MoonSpecialEffects();

    private MoonSpecialEffects() {
        super(Float.NaN, false, SkyType.END, true, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return fogColor.scale(0.15);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }

    @Nullable
    @Override
    public float[] getSunriseColor(float timeOfDay, float partialTick) {
        return null;
    }

    @Override
    public boolean renderSnowAndRain(
            ClientLevel level, int ticks, float partialTick, LightTexture lightTexture,
            double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public boolean tickRain(ClientLevel level, int ticks, Camera camera) {
        return true;
    }
}
