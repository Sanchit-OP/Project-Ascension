package com.ascension.worlds.client;

import net.minecraft.client.renderer.DimensionSpecialEffects;
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
}
