package com.ascension.worlds.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A starfield covering the full sphere around the player, always visible, in every direction.
 *
 * <p>Vanilla's own star buffer ({@code LevelRenderer.drawStars}) is already built the same way
 * (small random quads scattered across a full unit sphere, not just an upper hemisphere) &mdash;
 * what makes vanilla's read as "sky only" is everything <em>around</em> that buffer: a rotating
 * pose tied to time of day, a brightness fade gated on {@code getStarBrightness}, and a ground
 * plane/fog that hides the lower half in a normal world with a floor. The space dimension has
 * none of that context &mdash; no day/night cycle worth respecting, no ground to hide stars
 * behind, and players fly in every orientation with no fixed "down" &mdash; so this reimplements
 * only the geometry (same technique, more than vanilla's 1500 quads since this is the *only*
 * visual filling most of a flight) and draws it unconditionally: full brightness, no rotation, no
 * fog, every frame.
 *
 * <p>Built once and cached: the star positions never change, so there is nothing to regenerate
 * per frame or per world &mdash; a single static {@link VertexBuffer}, uploaded to the GPU once
 * and bound again on every draw, exactly like vanilla's own {@code starBuffer} field.
 */
final class SpaceStarField {

    private static final int STAR_COUNT = 6000;
    private static final float FIELD_RADIUS = 100.0F;

    private static VertexBuffer starBuffer;

    static void render(Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
        if (starBuffer == null) {
            starBuffer = buildStarBuffer();
        }

        RenderSystem.setShader(GameRenderer::getPositionShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.depthMask(false);
        starBuffer.bind();
        starBuffer.drawWithShader(modelViewMatrix, projectionMatrix, GameRenderer.getPositionShader());
        VertexBuffer.unbind();
        RenderSystem.depthMask(true);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static VertexBuffer buildStarBuffer() {
        VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        buffer.bind();
        buffer.upload(buildStarMesh());
        VertexBuffer.unbind();
        return buffer;
    }

    /**
     * Same technique as {@code LevelRenderer.drawStars}, copied rather than reinvented: a random
     * point rejection-sampled onto the unit sphere, normalised out to {@link #FIELD_RADIUS}, then
     * a small quad built in a frame rotated to face outward from the origin with a random roll.
     *
     * <p>Note {@code base.add(...)} below mutates {@code base} in place, so each of the four
     * corners is offset from the <em>previous corner's result</em> rather than all four from the
     * same shared centre -- this is exactly what vanilla's own version does too. It is a
     * pre-existing quirk, not a bug introduced here: at these quad sizes (a few tenths of a block
     * against a radius of 100) the resulting skew is imperceptible, which is presumably why it
     * has never been fixed upstream either. Kept identical rather than "corrected" so this stays
     * a faithful copy of known-working vanilla behaviour rather than a subtly different rewrite.
     */
    private static MeshData buildStarMesh() {
        RandomSource random = RandomSource.create(10842L);
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);

        for (int i = 0; i < STAR_COUNT; i++) {
            float x = random.nextFloat() * 2.0F - 1.0F;
            float y = random.nextFloat() * 2.0F - 1.0F;
            float z = random.nextFloat() * 2.0F - 1.0F;
            float size = 0.15F + random.nextFloat() * 0.1F;
            float lengthSquared = x * x + y * y + z * z;
            if (lengthSquared <= 0.010000001F || lengthSquared >= 1.0F) {
                continue;
            }

            Vector3f base = new Vector3f(x, y, z).normalize(FIELD_RADIUS);
            float roll = (float) (random.nextDouble() * Math.PI * 2.0);
            Quaternionf rotation = new Quaternionf().rotateTo(new Vector3f(0.0F, 0.0F, -1.0F), base).rotateZ(roll);
            builder.addVertex(base.add(new Vector3f(size, -size, 0.0F).rotate(rotation)));
            builder.addVertex(base.add(new Vector3f(size, size, 0.0F).rotate(rotation)));
            builder.addVertex(base.add(new Vector3f(-size, size, 0.0F).rotate(rotation)));
            builder.addVertex(base.add(new Vector3f(-size, -size, 0.0F).rotate(rotation)));
        }

        return builder.buildOrThrow();
    }

    private SpaceStarField() {
    }
}
