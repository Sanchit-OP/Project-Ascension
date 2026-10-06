package com.ascension.worlds.client;

import com.ascension.worlds.AscensionWorlds;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * Draws the current dimension's {@link SkyBodies} (Earth over the Moon) as lit spheres, right
 * after the sky and before terrain, so the ground and anything built on it cover them like the
 * vanilla sun.
 *
 * <p><strong>A sphere, not a sticker.</strong> The disc is a polar mesh (rings by segments), and
 * every vertex gets its brightness from {@link SkyBodyGeometry#brightness}: the sphere's normal at
 * that point against the sun's direction. That puts the terminator where the sun says, so the body
 * goes through phases over the day with no phase textures. A ring mesh rather than a textured
 * square also means there are no corners to show through (the lesson {@link SpaceSkyRenderer}
 * recorded about square quads).
 *
 * <p><strong>Same matrix handling as vanilla's sky.</strong> At {@code AFTER_SKY}, Minecraft has
 * not yet pushed the camera rotation onto {@link RenderSystem}'s model-view stack (and the event's
 * pose stack is null), so, like {@code LevelRenderer.renderSky}, vertices are transformed by the
 * event's model-view matrix on the CPU and drawn with the projection already set. Rebuilt each
 * frame: a few hundred quads per body, the same per-frame cost vanilla pays for its sun.
 *
 * <p>Hidden whenever vanilla hides its own sky: blindness, darkness, the camera in lava or
 * powder snow. Faded by a planet weather event's intensity, so a dust storm swallows Earth.
 */
@EventBusSubscriber(modid = AscensionWorlds.MOD_ID, value = Dist.CLIENT)
final class SkyBodyRenderer {

    /** Same distance vanilla draws its sun and moon at. */
    private static final float DISTANCE = 100.0F;
    private static final int RINGS = 12;
    private static final int SEGMENTS = 48;
    /** Halo width, as a fraction of the body's radius. */
    private static final float GLOW_WIDTH = 0.08F;
    private static final float GLOW_ALPHA = 0.6F;
    private static final double RIM_SAMPLE = 0.9;

    @SubscribeEvent
    static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        List<SkyBody> bodies = SkyBodies.forDimension(level.dimension().location());
        if (bodies.isEmpty() || skyIsHidden(event.getCamera())) {
            return;
        }

        float visibility = ClientPlanetWeather.isActive() ? 1.0F - ClientPlanetWeather.intensity() : 1.0F;
        if (visibility <= 0.0F) {
            return;
        }

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        double[] sun = SkyBodyGeometry.sunDirection(level.getTimeOfDay(partialTick));
        Matrix4f pose = event.getModelViewMatrix();

        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        for (SkyBody body : bodies) {
            drawBody(pose, body, sun, visibility);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
    }

    private static void drawBody(Matrix4f pose, SkyBody body, double[] sun, float visibility) {
        double[] direction = SkyBodyGeometry.direction(body.yaw(), body.elevation());
        double[][] basis = SkyBodyGeometry.basis(direction);
        double[] right = basis[0];
        double[] up = basis[1];
        float halfSize = (float) SkyBodyGeometry.halfSize(body.angularSize(), DISTANCE);
        float[] centre = {
            (float) direction[0] * DISTANCE, (float) direction[1] * DISTANCE, (float) direction[2] * DISTANCE};

        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, body.texture());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, visibility);
        BufferBuilder face = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int ring = 0; ring < RINGS; ring++) {
            float inner = (float) ring / RINGS;
            float outer = (float) (ring + 1) / RINGS;
            for (int segment = 0; segment < SEGMENTS; segment++) {
                double a0 = segment * Math.PI * 2.0 / SEGMENTS;
                double a1 = (segment + 1) * Math.PI * 2.0 / SEGMENTS;
                facePoint(face, pose, body, centre, right, up, direction, sun, halfSize, inner, a0);
                facePoint(face, pose, body, centre, right, up, direction, sun, halfSize, outer, a0);
                facePoint(face, pose, body, centre, right, up, direction, sun, halfSize, outer, a1);
                facePoint(face, pose, body, centre, right, up, direction, sun, halfSize, inner, a1);
            }
        }
        BufferUploader.drawWithShader(face.buildOrThrow());

        body.glow().ifPresent(colour -> drawGlow(pose, body, colour, centre, right, up, direction, sun, halfSize, visibility));
    }

    private static void facePoint(
            BufferBuilder buffer, Matrix4f pose, SkyBody body, float[] centre, double[] right, double[] up,
            double[] direction, double[] sun, float halfSize, float radius, double angle) {
        float u = (float) Math.cos(angle) * radius;
        float v = (float) Math.sin(angle) * radius;
        float shade = body.lit() ? (float) SkyBodyGeometry.brightness(u, v, direction, right, up, sun) : 1.0F;
        buffer.addVertex(pose,
                        centre[0] + (float) (right[0] * u + up[0] * v) * halfSize,
                        centre[1] + (float) (right[1] * u + up[1] * v) * halfSize,
                        centre[2] + (float) (right[2] * u + up[2] * v) * halfSize)
                .setUv(0.5F + 0.5F * u, 0.5F - 0.5F * v)
                .setColor(shade, shade, shade, 1.0F);
    }

    /**
     * A thin additive ring just outside the disc, brightest on the lit limb and gone on the dark
     * one: the atmosphere catching sunlight. Its strength at each point is the face's own
     * brightness at the rim, so the halo follows the phase.
     */
    private static void drawGlow(
            Matrix4f pose, SkyBody body, int colour, float[] centre, double[] right, double[] up,
            double[] direction, double[] sun, float halfSize, float visibility) {
        float red = ((colour >> 16) & 0xFF) / 255.0F;
        float green = ((colour >> 8) & 0xFF) / 255.0F;
        float blue = (colour & 0xFF) / 255.0F;

        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, visibility);
        BufferBuilder ring = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int segment = 0; segment < SEGMENTS; segment++) {
            double a0 = segment * Math.PI * 2.0 / SEGMENTS;
            double a1 = (segment + 1) * Math.PI * 2.0 / SEGMENTS;
            float alpha0 = rimAlpha(body, a0, direction, right, up, sun);
            float alpha1 = rimAlpha(body, a1, direction, right, up, sun);
            glowPoint(ring, pose, centre, right, up, halfSize, 1.0F, a0, red, green, blue, alpha0);
            glowPoint(ring, pose, centre, right, up, halfSize, 1.0F + GLOW_WIDTH, a0, red, green, blue, 0.0F);
            glowPoint(ring, pose, centre, right, up, halfSize, 1.0F + GLOW_WIDTH, a1, red, green, blue, 0.0F);
            glowPoint(ring, pose, centre, right, up, halfSize, 1.0F, a1, red, green, blue, alpha1);
        }
        BufferUploader.drawWithShader(ring.buildOrThrow());
    }

    private static float rimAlpha(SkyBody body, double angle, double[] direction, double[] right, double[] up, double[] sun) {
        if (!body.lit()) {
            return GLOW_ALPHA;
        }
        // Sampled just inside the rim, not on it: exactly on the rim the sphere's normal lies in
        // the disc's plane, which would leave a full Earth (sun behind the viewer) with no halo.
        double lit = SkyBodyGeometry.brightness(
                RIM_SAMPLE * Math.cos(angle), RIM_SAMPLE * Math.sin(angle), direction, right, up, sun);
        double day = (lit - SkyBodyGeometry.NIGHT_SIDE) / (1.0 - SkyBodyGeometry.NIGHT_SIDE);
        return (float) (GLOW_ALPHA * day);
    }

    private static void glowPoint(
            BufferBuilder buffer, Matrix4f pose, float[] centre, double[] right, double[] up, float halfSize,
            float radius, double angle, float red, float green, float blue, float alpha) {
        float u = (float) Math.cos(angle) * radius;
        float v = (float) Math.sin(angle) * radius;
        buffer.addVertex(pose,
                        centre[0] + (float) (right[0] * u + up[0] * v) * halfSize,
                        centre[1] + (float) (right[1] * u + up[1] * v) * halfSize,
                        centre[2] + (float) (right[2] * u + up[2] * v) * halfSize)
                .setColor(red, green, blue, alpha);
    }

    /** The same conditions under which {@code LevelRenderer.renderSky} skips the sky. */
    private static boolean skyIsHidden(Camera camera) {
        FogType fluid = camera.getFluidInCamera();
        if (fluid == FogType.LAVA || fluid == FogType.POWDER_SNOW) {
            return true;
        }
        return camera.getEntity() instanceof LivingEntity living
                && (living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS));
    }

    private SkyBodyRenderer() {
    }
}
