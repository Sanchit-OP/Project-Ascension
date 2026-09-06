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
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Registry;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * Draws every registered planet as a camera-facing disc, sized and positioned by its real
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
 * <p><strong>Real circular geometry, not a textured square.</strong> An earlier version drew a
 * textured quad and relied on {@code moon_phases.png}'s alpha channel to fake a round silhouette
 * &mdash; correct in principle, but whenever the corner pixels weren't cleanly transparent against
 * whatever the background happened to be, the square's corners showed through as a mismatched
 * colour block. A triangle fan is an actually-round shape: there is no corner for anything to show
 * through, and no texture or alpha channel is involved at all. This also means every planet is
 * flat-shaded in its own {@link Planet#color()} rather than textured &mdash; a plain colour was
 * already doing all the identification work (a blue disc reads as Earth, a grey one as the Moon);
 * the texture was never load-bearing for that, just a shape hack that is no longer needed now that
 * the shape itself is right.
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
        // 8000 (Planet 7 at 100,000). Left alone, that clips a planet out of existence well before
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

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.depthMask(false);
        // Whatever drew last before this stage may have left backface culling enabled (most of
        // Minecraft's own terrain/entity rendering wants it, for performance). A billboard's
        // winding order relative to the camera isn't something this code controls or guarantees,
        // so leaving culling on meant the disc only rendered from certain angles and vanished from
        // others -- exactly the "sometimes visible" symptom this was.
        RenderSystem.disableCull();

        for (Planet planet : planets) {
            renderPlanet(cameraPos, planet);
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.restoreProjectionMatrix();
    }

    private static void renderPlanet(Vec3 cameraPos, Planet planet) {
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

        int color = planet.color();
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

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
