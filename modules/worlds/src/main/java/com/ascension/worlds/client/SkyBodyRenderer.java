package com.ascension.worlds.client;

import com.ascension.worlds.AscensionWorlds;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
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
 * Draws the current dimension's {@link SkyBodies} (Earth over the Moon, the Moon over Earth) as lit
 * spheres, right after the sky and before terrain, so the ground and anything built on it cover
 * them like the vanilla sun.
 *
 * <p><strong>The Moon over Earth replaces vanilla's.</strong> Vanilla still draws its moon, but
 * from {@code minecraft:textures/environment/moon_phases.png}, which this module ships fully
 * transparent ({@code tools/generate-sky-art.py}); vanilla's moon blends additively, so a
 * transparent one adds nothing. That is the whole override: no mixin, and the sun, the stars and
 * the sky colours stay vanilla's. A resource pack that puts its own moon texture back gets two
 * moons, which is the pack's call.
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
 * powder snow. Faded by a planet weather event's intensity, so a dust storm swallows Earth, and by
 * rain under a vanilla sky, as vanilla fades its own moon.
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

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float visibility = ClientPlanetWeather.isActive() ? 1.0F - ClientPlanetWeather.intensity() : 1.0F;
        if (level.effects().skyType() == DimensionSpecialEffects.SkyType.NORMAL) {
            // Under a vanilla sky, rain hides what vanilla's own sun and moon hide. Only there:
            // the weather clock is shared with the Overworld, so an airless world's level can
            // report rain that is not falling on it.
            visibility *= 1.0F - level.getRainLevel(partialTick);
        }
        if (visibility <= 0.0F) {
            return;
        }

        float timeOfDay = level.getTimeOfDay(partialTick);
        int moonPhase = level.getMoonPhase();
        Matrix4f pose = event.getModelViewMatrix();

        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        for (SkyBody body : bodies) {
            drawBody(pose, body, Disc.of(pose, body, timeOfDay, moonPhase), visibility);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
    }

    private static void drawBody(Matrix4f pose, SkyBody body, Disc disc, float visibility) {
        if (body.additive()) {
            additiveBlend();
        } else {
            RenderSystem.defaultBlendFunc();
        }
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, body.texture());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, visibility);
        BufferBuilder face = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int ring = 0; ring < RINGS; ring++) {
            float inner = (float) ring / RINGS;
            float outer = (float) (ring + 1) / RINGS;
            for (int segment = 0; segment < SEGMENTS; segment++) {
                double a0 = angle(segment);
                double a1 = angle(segment + 1);
                facePoint(face, disc, body.lit(), inner, a0);
                facePoint(face, disc, body.lit(), outer, a0);
                facePoint(face, disc, body.lit(), outer, a1);
                facePoint(face, disc, body.lit(), inner, a1);
            }
        }
        BufferUploader.drawWithShader(face.buildOrThrow());

        body.glow().ifPresent(colour -> drawGlow(disc, body.lit(), colour, visibility));
        body.gateLight().ifPresent(light -> drawGateLight(disc, light, visibility));
    }

    /**
     * The gate light: one quad flat on the face, added on top, unshaded, as bright as the gates
     * lit so far allow ({@link SkyBody.GateLight#alpha}). Nothing at all before the first gate.
     */
    private static void drawGateLight(Disc disc, SkyBody.GateLight light, float visibility) {
        float alpha = SkyBody.GateLight.alpha(ClientGateProgress.markStage()) * visibility;
        if (alpha <= 0.0F) {
            return;
        }
        additiveBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, light.texture());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        float left = light.x() - light.size();
        float right = light.x() + light.size();
        float bottom = light.y() - light.size();
        float top = light.y() + light.size();
        BufferBuilder quad = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        disc.vertex(quad, left, top).setUv(0.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, 1.0F);
        disc.vertex(quad, left, bottom).setUv(0.0F, 1.0F).setColor(1.0F, 1.0F, 1.0F, 1.0F);
        disc.vertex(quad, right, bottom).setUv(1.0F, 1.0F).setColor(1.0F, 1.0F, 1.0F, 1.0F);
        disc.vertex(quad, right, top).setUv(1.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, 1.0F);
        BufferUploader.drawWithShader(quad.buildOrThrow());
    }

    private static void facePoint(BufferBuilder buffer, Disc disc, boolean lit, float radius, double angle) {
        float u = (float) Math.cos(angle) * radius;
        float v = (float) Math.sin(angle) * radius;
        float shade = lit ? (float) disc.brightness(u, v) : 1.0F;
        disc.vertex(buffer, u, v)
                .setUv(0.5F + 0.5F * u, 0.5F - 0.5F * v)
                .setColor(shade, shade, shade, 1.0F);
    }

    /**
     * A thin additive ring just outside the disc, brightest on the lit limb and gone on the dark
     * one: the atmosphere catching sunlight. Its strength at each point is the daylight just inside
     * the rim, so the halo follows the phase.
     */
    private static void drawGlow(Disc disc, boolean lit, int colour, float visibility) {
        float red = ((colour >> 16) & 0xFF) / 255.0F;
        float green = ((colour >> 8) & 0xFF) / 255.0F;
        float blue = (colour & 0xFF) / 255.0F;

        additiveBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, visibility);
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

    /** The blend vanilla uses for its sun and moon: light added to the sky, alpha untouched. */
    private static void additiveBlend() {
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
    }

    private static double angle(int segment) {
        return segment * Math.PI * 2.0 / SEGMENTS;
    }

    /**
     * One body's disc for this frame: where it is, which way it faces, and the light on it (the
     * sun, or a moon phase). {@code (u, v)} below is a point on the disc, each -1 to 1 across it.
     */
    private record Disc(Matrix4f pose, float[] centre, double[] direction, double[] right, double[] up,
                        double[] sun, float halfSize) {

        static Disc of(Matrix4f pose, SkyBody body, float timeOfDay, int moonPhase) {
            double[] direction;
            double[][] basis;
            double[] light;
            if (body.motion() == SkyBody.Motion.VANILLA_MOON) {
                direction = SkyBodyGeometry.moonDirection(timeOfDay);
                basis = SkyBodyGeometry.arcBasis(direction);
                light = SkyBodyGeometry.phaseLight(moonPhase, direction, basis[0]);
            } else {
                direction = SkyBodyGeometry.direction(body.yaw(), body.elevation());
                basis = SkyBodyGeometry.basis(direction);
                light = SkyBodyGeometry.sunDirection(timeOfDay);
            }
            float[] centre = {
                (float) direction[0] * DISTANCE, (float) direction[1] * DISTANCE, (float) direction[2] * DISTANCE};
            float halfSize = (float) SkyBodyGeometry.halfSize(body.angularSize(), DISTANCE);
            return new Disc(pose, centre, direction, basis[0], basis[1], light, halfSize);
        }

        VertexConsumer vertex(BufferBuilder buffer, float u, float v) {
            return buffer.addVertex(pose,
                    centre[0] + (float) (right[0] * u + up[0] * v) * halfSize,
                    centre[1] + (float) (right[1] * u + up[1] * v) * halfSize,
                    centre[2] + (float) (right[2] * u + up[2] * v) * halfSize);
        }

        double brightness(double u, double v) {
            return SkyBodyGeometry.brightness(u, v, direction, right, up, sun);
        }

        double daylight(double u, double v) {
            return SkyBodyGeometry.daylight(u, v, direction, right, up, sun);
        }
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
