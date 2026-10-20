package com.ascension.worlds.client;

import com.ascension.worlds.api.Planet;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Draws an asteroid-belt planet (the End): a faint dust ring in the belt's plane, a soft glow where the core is, then
 * every rock as one camera-facing sprite cut from the body's atlas, tumbling, tinted by how the sun reaches it and
 * violet on its night side. Three draw calls however many rocks, no allocation per frame (a reused sort buffer, the
 * billboard axes worked inline), and nothing here holds a level or an entity (ADR-0007): the rocks are immutable numbers.
 *
 * <p>Sprites, not meshes: the rocks are far, small and always turning, so a flat irregular shape reads as a lumpy rock
 * where a sphere reads as a moon (Sanchit, 2026-10-20: "it is not an asteroid belt at all").
 */
final class BeltRenderer {

    /** The atlas holds this many sprites to a row. */
    private static final int COLUMNS = 4;
    /** A rock is never drawn smaller than this, in radians of half-width (about two pixels at a normal field of view). */
    private static final double MIN_ANGULAR_SIZE = 0.0012;
    private static final int DUST_SEGMENTS = 96;
    private static final int DUST_RINGS = 12;
    /** How many times the dust texture repeats round the ring. */
    private static final float DUST_REPEAT = 8.0F;
    /** The dust sheets: height as a fraction of the belt's half thickness, and how much of the dust's strength each gets. */
    private static final float[] SHEET_HEIGHT = {0.0F, 0.6F, -0.6F};
    private static final float[] SHEET_STRENGTH = {0.55F, 0.28F, 0.28F};
    private static final double[] COS = new double[DUST_SEGMENTS + 1];
    private static final double[] SIN = new double[DUST_SEGMENTS + 1];

    static {
        for (int i = 0; i <= DUST_SEGMENTS; i++) {
            COS[i] = Math.cos(i * Math.PI * 2.0 / DUST_SEGMENTS);
            SIN[i] = Math.sin(i * Math.PI * 2.0 / DUST_SEGMENTS);
        }
    }

    /** The generated rocks of each belt, by surface dimension: immutable numbers, no world objects. */
    private static final Map<ResourceLocation, BeltRocks.Rock[]> BELTS = new HashMap<>();
    /** The sort buffer: distance bits in the high half, the rock's index in the low. Grown, never allocated per frame. */
    private static long[] keys = new long[0];

    private BeltRenderer() {
    }

    /** A resource reload may change a belt's numbers: forget the generated rocks. */
    static void clear() {
        BELTS.clear();
    }

    static void render(Vec3 cameraPos, Planet planet, SpaceBody look, SpaceBody.Belt belt, double[] sun, float seconds) {
        Planet.SpacePosition space = planet.space();
        BeltRocks.Rock[] rocks = BELTS.computeIfAbsent(planet.surface().location(), key -> BeltRocks.generate(belt.count(),
                belt.seed(), space.bodyRadius(), belt.thickness(), belt.minRadius(), belt.maxRadius(), belt.inner(),
                belt.sprites(), belt.central()));
        double centreX = space.x() - cameraPos.x;
        double centreY = -cameraPos.y;
        double centreZ = space.z() - cameraPos.z;

        belt.dust().ifPresent(texture -> drawDust(texture, belt, space.bodyRadius(), centreX, centreY, centreZ));
        belt.coreTexture().ifPresent(texture -> drawCore(texture, belt, space.bodyRadius(), centreX, centreY, centreZ));
        drawRocks(look.texture(), belt, rocks, sun, seconds, centreX, centreY, centreZ);
    }

    /** The dust: a few thin sheets of soft additive ring at the belt's plane, so it has body from a little above or below. */
    private static void drawDust(ResourceLocation texture, SpaceBody.Belt belt, float bodyRadius, double cx, double cy, double cz) {
        float[] rgb = SkyBodyGeometry.rgb(belt.dustColour());
        LitSphere.additiveBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        float rIn = bodyRadius * belt.inner();
        float rOut = bodyRadius;
        for (int sheet = 0; sheet < SHEET_HEIGHT.length; sheet++) {
            float y = (float) (cy + SHEET_HEIGHT[sheet] * belt.thickness() * bodyRadius);
            float strength = SHEET_STRENGTH[sheet];
            for (int ring = 0; ring < DUST_RINGS; ring++) {
                float v0 = (float) ring / DUST_RINGS;
                float v1 = (float) (ring + 1) / DUST_RINGS;
                float r0 = rIn + (rOut - rIn) * v0;
                float r1 = rIn + (rOut - rIn) * v1;
                for (int segment = 0; segment < DUST_SEGMENTS; segment++) {
                    float u0 = segment * DUST_REPEAT / DUST_SEGMENTS;
                    float u1 = (segment + 1) * DUST_REPEAT / DUST_SEGMENTS;
                    dustPoint(buffer, cx, y, cz, r0, segment, u0, v0, rgb, strength);
                    dustPoint(buffer, cx, y, cz, r1, segment, u0, v1, rgb, strength);
                    dustPoint(buffer, cx, y, cz, r1, segment + 1, u1, v1, rgb, strength);
                    dustPoint(buffer, cx, y, cz, r0, segment + 1, u1, v0, rgb, strength);
                }
            }
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void dustPoint(BufferBuilder buffer, double cx, float y, double cz, float radius, int segment, float u, float v,
                                  float[] rgb, float strength) {
        buffer.addVertex((float) (cx + COS[segment] * radius), y, (float) (cz + SIN[segment] * radius))
                .setUv(u, v).setColor(rgb[0], rgb[1], rgb[2], strength);
    }

    /** The core: a soft glow where the great rock hangs, facing the camera, added to the sky. */
    private static void drawCore(ResourceLocation texture, SpaceBody.Belt belt, float bodyRadius, double cx, double cy, double cz) {
        double distance = Math.sqrt(cx * cx + cy * cy + cz * cz);
        if (distance < 1.0e-3) {
            return;
        }
        double[] direction = {cx / distance, cy / distance, cz / distance};
        double[][] basis = SkyBodyGeometry.basis(direction);
        float half = (float) SkyBodyGeometry.silhouetteHalfSize(bodyRadius * 0.55F, distance);
        float[] rgb = SkyBodyGeometry.rgb(belt.coreColour());
        LitSphere.additiveBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        float[][] corners = {{-1, -1, 0, 1}, {1, -1, 1, 1}, {1, 1, 1, 0}, {-1, 1, 0, 0}};
        for (float[] corner : corners) {
            buffer.addVertex((float) (cx + (basis[0][0] * corner[0] + basis[1][0] * corner[1]) * half),
                    (float) (cy + (basis[0][1] * corner[0] + basis[1][1] * corner[1]) * half),
                    (float) (cz + (basis[0][2] * corner[0] + basis[1][2] * corner[1]) * half))
                    .setUv(corner[2], corner[3]).setColor(rgb[0], rgb[1], rgb[2], 1.0F);
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    /** Every rock, farthest first, in one buffer. */
    private static void drawRocks(ResourceLocation atlas, SpaceBody.Belt belt, BeltRocks.Rock[] rocks, double[] sun, float seconds,
                                  double cx, double cy, double cz) {
        if (keys.length < rocks.length) {
            keys = new long[rocks.length];
        }
        for (int i = 0; i < rocks.length; i++) {
            BeltRocks.Rock rock = rocks[i];
            double x = cx + rock.x();
            double y = cy + rock.y();
            double z = cz + rock.z();
            float distance = (float) Math.sqrt(x * x + y * y + z * z);
            // A positive float's bits sort the way the float does.
            keys[i] = ((long) Float.floatToIntBits(distance) << 32) | i;
        }
        Arrays.sort(keys, 0, rocks.length);

        int rows = (belt.sprites() + COLUMNS - 1) / COLUMNS;
        float cellU = 1.0F / COLUMNS;
        float cellV = 1.0F / Math.max(1, rows);
        // Half a texel in from each cell's edge, so a neighbour's pixels never bleed in.
        float inset = 0.5F / 128.0F;

        LitSphere.defaultBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, atlas);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int k = rocks.length - 1; k >= 0; k--) {
            BeltRocks.Rock rock = rocks[(int) (keys[k] & 0xFFFFFFFFL)];
            double x = cx + rock.x();
            double y = cy + rock.y();
            double z = cz + rock.z();
            double distance = Math.sqrt(x * x + y * y + z * z);
            if (distance < 1.0e-3) {
                continue;
            }
            double dx = x / distance;
            double dy = y / distance;
            double dz = z / distance;
            // The billboard's axes, as SkyBodyGeometry.basis makes them, worked inline: no arrays.
            double rl = Math.sqrt(dx * dx + dz * dz);
            double rx;
            double rz;
            if (rl < 1.0e-6) {
                rx = 1.0;
                rz = 0.0;
            } else {
                rx = -dz / rl;
                rz = dx / rl;
            }
            double ux = -rz * dy;
            double uy = rz * dx - rx * dz;
            double uz = rx * dy;
            double half = Math.max(SkyBodyGeometry.silhouetteHalfSize(rock.radius(), distance), distance * MIN_ANGULAR_SIZE);

            double angle = rock.phase() + rock.spin() * seconds;
            double c = Math.cos(angle) * half;
            double s = Math.sin(angle) * half;

            // The sun's side faces the viewer when the sun is behind the viewer; the night side is violet, not black.
            double day = Math.max(0.0, Math.min(1.0, 0.5 - 0.5 * (dx * sun[0] + dy * sun[1] + dz * sun[2])));
            float shade = rock.shade() * (float) (0.3 + 0.7 * day);
            float red = Math.min(1.0F, shade * (0.55F + 0.45F * (float) day));
            float green = Math.min(1.0F, shade * (0.42F + 0.58F * (float) day));
            float blue = Math.min(1.0F, shade * (0.75F + 0.25F * (float) day));

            int col = rock.sprite() % COLUMNS;
            int row = rock.sprite() / COLUMNS;
            float u0 = col * cellU + inset;
            float u1 = (col + 1) * cellU - inset;
            float v0 = row * cellV + inset;
            float v1 = (row + 1) * cellV - inset;
            // Corners (-1,-1) (1,-1) (1,1) (-1,1) turned by the tumble.
            rockPoint(buffer, x, y, z, rx, rz, ux, uy, uz, -c + s, -s - c, u0, v1, red, green, blue);
            rockPoint(buffer, x, y, z, rx, rz, ux, uy, uz, c + s, s - c, u1, v1, red, green, blue);
            rockPoint(buffer, x, y, z, rx, rz, ux, uy, uz, c - s, s + c, u1, v0, red, green, blue);
            rockPoint(buffer, x, y, z, rx, rz, ux, uy, uz, -c - s, -s + c, u0, v0, red, green, blue);
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    /** One corner: {@code a} along the right axis (which has no vertical part) and {@code b} along the up axis. */
    private static void rockPoint(BufferBuilder buffer, double x, double y, double z, double rx, double rz,
                                  double ux, double uy, double uz, double a, double b, float u, float v,
                                  float red, float green, float blue) {
        buffer.addVertex((float) (x + rx * a + ux * b), (float) (y + uy * b), (float) (z + rz * a + uz * b))
                .setUv(u, v).setColor(red, green, blue, 1.0F);
    }
}
