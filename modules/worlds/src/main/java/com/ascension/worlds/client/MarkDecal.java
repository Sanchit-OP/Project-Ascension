package com.ascension.worlds.client;

import com.ascension.worlds.AscensionWorlds;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.biome.Biomes;

/**
 * The mark ({@code docs/lore/the-mark.md}) as a decal: one textured quad laid on a model part, in
 * the exact place and orientation the art contract gives ({@code docs/technical/lore-art-format.md},
 * sections 3 and 4, "Format B"). The pictures are design's ({@code tools/generate-lore-art.py});
 * this class only puts them where they belong.
 *
 * <p>Coordinates are model pixels, in the part's own space after its {@code translateAndRotate},
 * as the contract states them; {@link #quad} turns them into blocks.
 */
final class MarkDecal {

    private static final ResourceLocation[] STAGES = new ResourceLocation[8];
    static final ResourceLocation ILLAGER = texture("illager");

    static {
        for (int stage = 1; stage < STAGES.length; stage++) {
            STAGES[stage] = texture("stage_" + stage);
        }
    }

    private MarkDecal() {
    }

    /** The picture for a stage, 1 to 7. Stage 0 has none: there is nothing on the hand yet. */
    static ResourceLocation stage(int stage) {
        return STAGES[stage];
    }

    /**
     * The mark glows full-bright, except where the Observers cannot see: in a dust storm (the
     * Sealers' veil, {@code docs/lore/the-moon.md}) and in the Deep Dark. There it takes the light
     * around it like any skin, and goes dark with it. That is a clue the story leaves on purpose.
     */
    static int light(LivingEntity wearer, int ambientLight) {
        return unseen(wearer) ? ambientLight : LightTexture.FULL_BRIGHT;
    }

    private static boolean unseen(LivingEntity wearer) {
        return ClientPlanetWeather.isActive()
                || wearer.hasEffect(MobEffects.DARKNESS)
                || wearer.level().getBiome(wearer.blockPosition()).is(Biomes.DEEP_DARK);
    }

    /**
     * The player's mark: on the right arm's outer face ({@code -x}), over the back of the hand and
     * wrist ({@code y} 5 to 9, {@code z} -2 to 2), 0.3 px clear of the cube so it sits above the
     * sleeve. {@code faceX} is -3.3 on the wide arm and -2.3 on the slim. The texture's top lies
     * toward the hand ({@code y} 9) and its left toward the front ({@code z} -2), so it reads
     * upright to its wearer.
     */
    static void onRightArm(PoseStack poseStack, MultiBufferSource buffers, ResourceLocation texture,
                           float faceX, int light) {
        quad(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), light, -1, 0, 0,
                faceX, 9, -2, 0, 0,
                faceX, 9, 2, 1, 0,
                faceX, 5, 2, 1, 1,
                faceX, 5, -2, 0, 1);
    }

    /**
     * The illagers' mark: on the front ({@code -z}) of the crossed arms' middle box, centred
     * ({@code x} -2 to 2, {@code y} 2 to 6), 0.1 px in front of it. Top toward the top of the arms
     * ({@code y} 2), left toward the illager's right ({@code x} -2), so it reads upright to whoever
     * faces them.
     */
    static void onCrossedArms(PoseStack poseStack, MultiBufferSource buffers, int light) {
        quad(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(ILLAGER)), light, 0, 0, -1,
                -2, 2, -2.1F, 0, 0,
                2, 2, -2.1F, 1, 0,
                2, 6, -2.1F, 1, 1,
                -2, 6, -2.1F, 0, 1);
    }

    /** Four corners in model pixels with their uvs, and the face's outward normal. */
    private static void quad(PoseStack poseStack, VertexConsumer consumer, int light,
                             float nx, float ny, float nz,
                             float x0, float y0, float z0, float u0, float v0,
                             float x1, float y1, float z1, float u1, float v1,
                             float x2, float y2, float z2, float u2, float v2,
                             float x3, float y3, float z3, float u3, float v3) {
        PoseStack.Pose pose = poseStack.last();
        vertex(consumer, pose, light, nx, ny, nz, x0, y0, z0, u0, v0);
        vertex(consumer, pose, light, nx, ny, nz, x1, y1, z1, u1, v1);
        vertex(consumer, pose, light, nx, ny, nz, x2, y2, z2, u2, v2);
        vertex(consumer, pose, light, nx, ny, nz, x3, y3, z3, u3, v3);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, int light,
                               float nx, float ny, float nz, float x, float y, float z, float u, float v) {
        consumer.addVertex(pose, x / 16.0F, y / 16.0F, z / 16.0F)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(AscensionWorlds.MOD_ID, "textures/entity/mark/" + name + ".png");
    }
}
