package com.ascension.worlds.client;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.Vec3;

/**
 * The minimum a dimension_type's {@code "effects"} field needs to point at &mdash; the actual sky
 * ({@link SpaceSkyRenderer}, {@link SpaceStarField}) is drawn entirely by hand in a
 * {@code RenderLevelStageEvent} listener, not by anything vanilla's own sky renderer does with
 * this class. {@code skyType() == NONE} tells that renderer not to draw its own sun/moon/normal
 * sky dome or End sky on top of ours.
 *
 * <p>{@code hasGround = false}: nothing renders below a player out here, so there is no ground
 * plane to blend into. {@code constantAmbientLight = true}: space has no day/night cycle for
 * lighting to track, the same reasoning nether uses. Fog is always off ({@code isFoggyAt} always
 * {@code false}) and the background is always pure black regardless of brightness &mdash; nothing
 * in the space dimension should ever fade distant objects out early or tint the void.
 *
 * <p>{@link #renderSnowAndRain} and {@link #tickRain} both unconditionally return {@code true}
 * (per {@code IDimensionSpecialEffectsExtension}'s contract, "prevent vanilla snow/rain rendering
 * and ticking") &mdash; belt-and-suspenders on top of {@code space_void}'s own
 * {@code has_precipitation: false}. Space is vacuum; weather cannot exist there by construction,
 * and this makes that a guarantee enforced in code rather than a property that happens to fall
 * out of one biome JSON file staying correctly configured forever. See {@link MoonSpecialEffects}
 * for the same reasoning applied to the Moon.
 */
final class SpaceSpecialEffects extends DimensionSpecialEffects {

    static final SpaceSpecialEffects INSTANCE = new SpaceSpecialEffects();

    private SpaceSpecialEffects() {
        super(Float.NaN, false, SkyType.NONE, true, true);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return Vec3.ZERO;
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
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
