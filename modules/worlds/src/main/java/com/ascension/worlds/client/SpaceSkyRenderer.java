package com.ascension.worlds.client;

import com.ascension.worlds.AscensionWorlds;
import com.ascension.worlds.api.Planet;
import com.ascension.worlds.api.WorldsRegistries;
import com.ascension.worlds.internal.SpaceDimension;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * Draws every registered planet as a camera-facing sphere, sized and positioned by its real
 * distance in {@link SpaceDimension} &mdash; the primary navigation instrument ADR-0010 requires
 * (see {@code worlds-api.md} §3, "Apparent size is the navigation"). A planet you cannot see from
 * space turns the shared dimension into an empty void with an invisible waypoint. Also draws the
 * starfield ({@link SpaceStarField}) that fills the rest of that void, so the other 99%+ of a
 * flight is not a featureless black screen.
 *
 * <p><strong>True positional rendering, not a skybox decoration.</strong> Vanilla's own sun/moon
 * are drawn at a fixed angular position derived purely from time of day &mdash; they never get
 * closer. A planet has to: this billboards a disc at the planet's actual {@code (x, 0, z)} world
 * position, sized to its real {@code body_radius}, so ordinary perspective projection produces the
 * correct apparent size on its own (matching {@code worlds-api.md}'s
 * {@code 2 * atan(radius / distance)} table) without this class doing any angle math itself.
 *
 * <p><strong>Billboarded around the world Y axis only, not the full camera orientation.</strong>
 * An earlier version matched the disc's local frame to {@code camera.rotation()} in full (the
 * technique vanilla particles use to always face the viewer edge-on) &mdash; correct for a
 * particle, but it carries the camera's pitch and roll into the disc's own orientation, which
 * visibly "spins" a recognisable texture as the camera looks up or down. The right/up axes below
 * come from the object-to-camera direction and world-up instead, with no pitch or roll term
 * anywhere in the math, so the disc stays upright and cannot show that spin regardless of where
 * the camera looks.
 *
 * <p><strong>The face it shows in other skies.</strong> A planet with a {@link SpaceBody} (its
 * look from space, a client resource) is drawn the way it hangs in another world's sky: textured,
 * lit by the sun into a phase, with its atmosphere's halo ({@link LitSphere}, Sanchit 2026-10-11).
 * The disc covers the sphere's true outline ({@link SkyBodyGeometry#silhouetteHalfSize}), which up
 * close is wider than its radius. A planet with no look is a disc flat-shaded in its own
 * {@link Planet#color()}, a triangle fan: a real round shape, no corners to show through, which is
 * what an earlier textured square got wrong.
 *
 * <p>Farthest first: nothing here writes depth, so a nearer planet must be drawn over a farther one.
 */
@EventBusSubscriber(modid = AscensionWorlds.MOD_ID, value = Dist.CLIENT)
public final class SpaceSkyRenderer {

    /** Segments in the disc's triangle fan. Enough to read as round at typical viewing sizes. */
    private static final int DISC_SEGMENTS = 32;

    /**
     * Comfortably past the world border ADR-0010 sets for {@link SpaceDimension} (~150,000, sized
     * to hold all seven planets) &mdash; a planet must render at any distance a player can
     * actually be at, and this is the largest that distance can ever be.
     */
    private static final float FAR_PLANE = 200_000.0F;

    /** Model-view for {@link LitSphere.Disc}: none, as RenderSystem's already holds the camera's. */
    private static final Matrix4f IDENTITY = new Matrix4f();

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Level level = Minecraft.getInstance().level;
        if (level == null || !level.dimension().equals(SpaceDimension.KEY)) {
            return;
        }

        // Deliberately NOT AFTER_SKY. LevelRenderer dispatches that stage with a null pose stack
        // (it fires before entity rendering pushes the camera's rotation onto RenderSystem's own
        // modelview stack), which crashes anything that expects one. AFTER_PARTICLES fires well
        // after that push, inside the same window vanilla's own sky elements already assume.

        // Stars are effectively infinitely far away -- draw them with the projection Minecraft
        // already set up for this frame, before swapping in the far-plane projection below that
        // planets need. Vanilla's own star geometry (LevelRenderer.drawStars) is built the same
        // way: positioned at a fixed small radius around the camera using only its rotation, no
        // translation, which is exactly what event.getModelViewMatrix() is at this stage.
        SpaceStarField.render(event.getModelViewMatrix(), event.getProjectionMatrix());

        Registry<Planet> planets = level.registryAccess().registryOrThrow(WorldsRegistries.PLANET);
        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();

        // Minecraft's own projection far plane is renderDistance(chunks) * 64 blocks -- a
        // generous 64-chunk render distance only reaches ~4096 blocks, and the Moon alone sits at
        // 4000 (Planet 7 at 100,000). Left alone, that clips a planet out of existence well before
        // a player could ever see it. Space has nothing else in it worth clipping against, so
        // swap in a projection whose far plane covers the whole dimension's world border for the
        // duration of this draw, and put the real one back immediately after.
        Window window = Minecraft.getInstance().getWindow();
        float aspectRatio = (float) window.getWidth() / (float) window.getHeight();
        float fovDegrees = Minecraft.getInstance().options.fov().get().floatValue();
        Matrix4f farProjection = new Matrix4f()
                .perspective((float) Math.toRadians(fovDegrees), aspectRatio, 0.05F, FAR_PLANE);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(farProjection, VertexSorting.DISTANCE_TO_ORIGIN);

        // Same reasoning as the far plane: normal fog fades objects out well within render
        // distance, which would hide a planet long before the far-plane fix above lets it draw at
        // all. Nothing else exists out here for fog to usefully apply to anyway.
        FogRenderer.setupNoFog();

        double[] sun = SkyBodyGeometry.sunDirection(
                level.getTimeOfDay(event.getPartialTick().getGameTimeDeltaPartialTick(false)));
        RenderSystem.depthMask(false);
        // Whatever drew last before this stage may have left backface culling enabled (most of
        // Minecraft's own terrain/entity rendering wants it, for performance). A billboard's
        // winding order relative to the camera isn't something this code controls or guarantees,
        // so leaving culling on meant the disc only rendered from certain angles and vanished from
        // others -- exactly the "sometimes visible" symptom this was.
        RenderSystem.disableCull();
        RenderSystem.enableBlend();

        List<Map.Entry<ResourceKey<Planet>, Planet>> farthestFirst = new ArrayList<>(planets.entrySet());
        farthestFirst.sort(Comparator.comparingDouble(
                (Map.Entry<ResourceKey<Planet>, Planet> entry) -> distanceSquared(cameraPos, entry.getValue())).reversed());
        for (Map.Entry<ResourceKey<Planet>, Planet> entry : farthestFirst) {
            SpaceBody look = SpaceBodies.forPlanet(entry.getKey().location());
            if (look == null) {
                renderFlatDisc(cameraPos, entry.getValue());
            } else {
                renderSphere(cameraPos, entry.getValue(), look, sun);
            }
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.restoreProjectionMatrix();
    }

    private static double distanceSquared(Vec3 cameraPos, Planet planet) {
        double x = planet.space().x() - cameraPos.x;
        double z = planet.space().z() - cameraPos.z;
        return x * x + cameraPos.y * cameraPos.y + z * z;
    }

    /** A planet with a look from space: its face as a sphere, lit by the sun, haloed if it has air. */
    private static void renderSphere(Vec3 cameraPos, Planet planet, SpaceBody look, double[] sun) {
        Planet.SpacePosition space = planet.space();
        double relX = space.x() - cameraPos.x;
        double relY = -cameraPos.y;
        double relZ = space.z() - cameraPos.z;
        double distance = Math.sqrt(relX * relX + relY * relY + relZ * relZ);
        if (distance < 1.0e-3) {
            return;
        }
        double[] direction = {relX / distance, relY / distance, relZ / distance};
        double[][] basis = SkyBodyGeometry.basis(direction);
        float halfSize = (float) SkyBodyGeometry.silhouetteHalfSize(space.bodyRadius(), distance);
        LitSphere.Disc disc = new LitSphere.Disc(IDENTITY, new float[] {(float) relX, (float) relY, (float) relZ},
                direction, basis[0], basis[1], sun, halfSize);
        LitSphere.drawFace(disc, look.texture(), look.lit(), false, 1.0F);
        look.emissive().ifPresent(emissive -> LitSphere.drawEmissive(disc, emissive, look.lit(), 1.0F));
        look.glow().ifPresent(colour -> LitSphere.drawGlow(disc, look.lit(), colour, 1.0F));
    }

    /** A planet with no look from space: a disc in its own colour. */
    private static void renderFlatDisc(Vec3 cameraPos, Planet planet) {
        Planet.SpacePosition space = planet.space();
        float relX = (float) (space.x() - cameraPos.x);
        float relY = (float) (0.0 - cameraPos.y);
        float relZ = (float) (space.z() - cameraPos.z);
        float radius = space.bodyRadius();

        // The disc's right axis is perpendicular to world-up and the direction toward the
        // camera; its up axis is fixed world-up. That is the entire billboard -- no roll, no
        // pitch, nothing derived from the camera's own orientation at all, so there is no term
        // left in this math that camera pitch could ever feed into.
        float horizontalDistance = (float) Math.sqrt(relX * relX + relZ * relZ);
        float rightX;
        float rightZ;
        if (horizontalDistance < 0.001F) {
            // Degenerate only when the camera sits exactly on the planet's own (x, z) -- deep
            // inside its body_radius, so not a real flight position. Arbitrary fallback axis
            // avoids a NaN disc rather than solving for an orientation that cannot matter here.
            rightX = 1.0F;
            rightZ = 0.0F;
        } else {
            rightX = relZ / horizontalDistance;
            rightZ = -relX / horizontalDistance;
        }

        float[] rgb = SkyBodyGeometry.rgb(planet.color());
        float red = rgb[0];
        float green = rgb[1];
        float blue = rgb[2];

        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        addPoint(buffer, relX, relY, relZ, red, green, blue);
        for (int i = 0; i <= DISC_SEGMENTS; i++) {
            double angle = (double) i / DISC_SEGMENTS * (Math.PI * 2.0);
            float cos = (float) Math.cos(angle) * radius;
            float sin = (float) Math.sin(angle) * radius;
            addPoint(buffer, relX + rightX * cos, relY + sin, relZ + rightZ * cos, red, green, blue);
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void addPoint(
            BufferBuilder buffer, float x, float y, float z, float red, float green, float blue) {
        buffer.addVertex(x, y, z).setColor(red, green, blue, 1.0F);
    }

    private SpaceSkyRenderer() {
    }
}
