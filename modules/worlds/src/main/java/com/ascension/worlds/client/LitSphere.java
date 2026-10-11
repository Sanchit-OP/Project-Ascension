package com.ascension.worlds.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.function.DoubleBinaryOperator;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * A planet drawn as a sphere seen from outside, the one way this module draws one: hanging in a
 * world's sky ({@link SkyBodyRenderer}) or sitting in space ({@link SpaceSkyRenderer}).
 *
 * <p>A ring mesh facing the viewer (rings by segments, so a round silhouette with no corners to
 * show through), textured with the body's face, every vertex shaded by
 * {@link SkyBodyGeometry#brightness}: the sphere's normal at that point against the light. That
 * puts the terminator where the light says, so the body goes through phases with no phase textures.
 * An atmosphere's halo is a thin ring outside the disc, brightest on the lit limb.
 *
 * <p>Callers set depth writes, culling and blending around a batch of bodies; this sets the shader,
 * texture, colour and blend function each draw needs.
 */
final class LitSphere {

    private static final int RINGS = 12;
    private static final int SEGMENTS = 48;
    /** Halo width, as a fraction of the body's radius. */
    private static final float GLOW_WIDTH = 0.08F;
    private static final float GLOW_ALPHA = 0.6F;
    private static final double RIM_SAMPLE = 0.9;
    /** How much daylight drowns an emissive layer: on the full-lit face it keeps 15 %. */
    private static final double EMISSIVE_DAY_DIMMING = 0.85;

    private LitSphere() {
    }

    /**
     * One body's disc for this frame: where its centre is, which way it faces (the unit
     * {@code direction} from the viewer toward it, with the disc's own {@code right} and
     * {@code up}), the light on it, and its half-width. {@code (u, v)} below is a point on the
     * disc, each -1 to 1 across it. Vertices go through {@code pose}.
     */
    record Disc(Matrix4f pose, float[] centre, double[] direction, double[] right, double[] up,
                double[] light, float halfSize) {

        VertexConsumer vertex(BufferBuilder buffer, float u, float v) {
            return buffer.addVertex(pose,
                    centre[0] + (float) (right[0] * u + up[0] * v) * halfSize,
                    centre[1] + (float) (right[1] * u + up[1] * v) * halfSize,
                    centre[2] + (float) (right[2] * u + up[2] * v) * halfSize);
        }

        double brightness(double u, double v) {
            return SkyBodyGeometry.brightness(u, v, direction, right, up, light);
        }

        double daylight(double u, double v) {
            return SkyBodyGeometry.daylight(u, v, direction, right, up, light);
        }
    }

    /**
     * The face: {@code texture}'s inscribed circle on the disc, shaded into a phase when
     * {@code lit}. {@code additive} adds its light to what is behind it, the way vanilla draws its
     * sun and moon; otherwise it covers what is behind it.
     */
    static void drawFace(Disc disc, ResourceLocation texture, boolean lit, boolean additive, float alpha) {
        if (additive) {
            additiveBlend();
        } else {
            RenderSystem.defaultBlendFunc();
        }
        drawTextured(disc, texture, alpha, (u, v) -> lit ? disc.brightness(u, v) : 1.0);
    }

    /**
     * A body's own light, added unshaded over its face and weighted to its night side: a burning
     * world's lava seas glowing where the sun is not (the Nether, from space). Mostly transparent,
     * so only what glows adds anything.
     */
    static void drawEmissive(Disc disc, ResourceLocation texture, boolean lit, float alpha) {
        additiveBlend();
        drawTextured(disc, texture, alpha,
                (u, v) -> lit ? 1.0 - EMISSIVE_DAY_DIMMING * disc.daylight(u, v) : 1.0);
    }

    /** The disc's rings and segments, textured, each vertex shaded by {@code shade}. */
    private static void drawTextured(Disc disc, ResourceLocation texture, float alpha, DoubleBinaryOperator shade) {
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        BufferBuilder face = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int ring = 0; ring < RINGS; ring++) {
            float inner = (float) ring / RINGS;
            float outer = (float) (ring + 1) / RINGS;
            for (int segment = 0; segment < SEGMENTS; segment++) {
                double a0 = angle(segment);
                double a1 = angle(segment + 1);
                facePoint(face, disc, shade, inner, a0);
                facePoint(face, disc, shade, outer, a0);
                facePoint(face, disc, shade, outer, a1);
                facePoint(face, disc, shade, inner, a1);
            }
        }
        BufferUploader.drawWithShader(face.buildOrThrow());
    }

    /**
     * A thin additive ring just outside the disc, brightest on the lit limb and gone on the dark
     * one: the atmosphere catching sunlight. Its strength at each point is the daylight just inside
     * the rim, so the halo follows the phase.
     */
    static void drawGlow(Disc disc, boolean lit, int colour, float alpha) {
        float red = ((colour >> 16) & 0xFF) / 255.0F;
        float green = ((colour >> 8) & 0xFF) / 255.0F;
        float blue = (colour & 0xFF) / 255.0F;

        additiveBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        BufferBuilder ring = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float outer = 1.0F + GLOW_WIDTH;
        for (int segment = 0; segment < SEGMENTS; segment++) {
            double a0 = angle(segment);
            double a1 = angle(segment + 1);
            float cos0 = (float) Math.cos(a0);
            float sin0 = (float) Math.sin(a0);
            float cos1 = (float) Math.cos(a1);
            float sin1 = (float) Math.sin(a1);
            disc.vertex(ring, cos0, sin0).setColor(red, green, blue, rimAlpha(disc, lit, a0));
            disc.vertex(ring, outer * cos0, outer * sin0).setColor(red, green, blue, 0.0F);
            disc.vertex(ring, outer * cos1, outer * sin1).setColor(red, green, blue, 0.0F);
            disc.vertex(ring, cos1, sin1).setColor(red, green, blue, rimAlpha(disc, lit, a1));
        }
        BufferUploader.drawWithShader(ring.buildOrThrow());
    }

    /** The blend vanilla uses for its sun and moon: light added to the sky, alpha untouched. */
    static void additiveBlend() {
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
    }

    private static void facePoint(BufferBuilder buffer, Disc disc, DoubleBinaryOperator shade, float radius, double angle) {
        float u = (float) Math.cos(angle) * radius;
        float v = (float) Math.sin(angle) * radius;
        float light = (float) shade.applyAsDouble(u, v);
        disc.vertex(buffer, u, v)
                .setUv(0.5F + 0.5F * u, 0.5F - 0.5F * v)
                .setColor(light, light, light, 1.0F);
    }

    /**
     * Sampled just inside the rim, not on it: exactly on the rim the sphere's normal lies in the
     * disc's plane, which would leave a full Earth (sun behind the viewer) with no halo.
     */
    private static float rimAlpha(Disc disc, boolean lit, double angle) {
        if (!lit) {
            return GLOW_ALPHA;
        }
        return (float) (GLOW_ALPHA * disc.daylight(RIM_SAMPLE * Math.cos(angle), RIM_SAMPLE * Math.sin(angle)));
    }

    private static double angle(int segment) {
        return segment * Math.PI * 2.0 / SEGMENTS;
    }
}
