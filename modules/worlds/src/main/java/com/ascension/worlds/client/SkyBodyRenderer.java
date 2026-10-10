package com.ascension.worlds.client;

import com.ascension.worlds.AscensionWorlds;
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
 * <p><strong>A sphere, not a sticker</strong> ({@link LitSphere}, shared with the planets in
 * space): shaded against the sun's direction, or against a moon phase for a body riding vanilla's
 * moon, so it goes through phases with no phase textures.
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
            drawBody(body, disc(pose, body, timeOfDay, moonPhase), visibility);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
    }

    private static void drawBody(SkyBody body, LitSphere.Disc disc, float visibility) {
        LitSphere.drawFace(disc, body.texture(), body.lit(), body.additive(), visibility);
        body.glow().ifPresent(colour -> LitSphere.drawGlow(disc, body.lit(), colour, visibility));
        body.gateLight().ifPresent(light -> drawGateLight(disc, light, visibility));
    }

    /**
     * The gate light: one quad flat on the face, added on top, unshaded, as bright as the gates
     * lit so far allow ({@link SkyBody.GateLight#alpha}). Nothing at all before the first gate.
     */
    private static void drawGateLight(LitSphere.Disc disc, SkyBody.GateLight light, float visibility) {
        float alpha = SkyBody.GateLight.alpha(ClientGateProgress.markStage()) * visibility;
        if (alpha <= 0.0F) {
            return;
        }
        LitSphere.additiveBlend();
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

    /** Where a sky body's disc is this frame, which way it faces, and the light on it. */
    private static LitSphere.Disc disc(Matrix4f pose, SkyBody body, float timeOfDay, int moonPhase) {
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
        return new LitSphere.Disc(pose, centre, direction, basis[0], basis[1], light, halfSize);
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
