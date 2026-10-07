package com.ascension.combat.client.weapon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Draws a held weapon from its {@link WeaponModel}: the model with its texture, lit like any
 * entity, then the same model again with its glow texture through the "eyes" render type, which
 * adds full-bright light and so stays lit in the dark. The item's model JSON parents
 * {@code builtin/entity} for its display transforms; by the time this runs the pose is already in
 * the item's own frame (0..1 is one block), so model pixels are divided by 16.
 *
 * <p>One instance serves every weapon, made on first use (by then the game's renderers exist).
 * It holds no state of its own.
 */
public final class WeaponRenderer extends BlockEntityWithoutLevelRenderer {
    private static WeaponRenderer instance;

    private WeaponRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static WeaponRenderer get() {
        if (instance == null) {
            instance = new WeaponRenderer();
        }
        return instance;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers,
                             int light, int overlay) {
        WeaponModel model = WeaponModels.INSTANCE.get(stack.getItem());
        if (model == null) {
            return;
        }
        drawModel(model, pose, buffers, light, overlay);
    }

    /** The model in the pose's frame (model pixels / 16): texture pass, then the glow pass. */
    private static void drawModel(WeaponModel model, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        draw(model, pose.last(), buffers.getBuffer(RenderType.entityCutoutNoCull(model.texture())), light, overlay);
        if (model.glowTexture() != null) {
            draw(model, pose.last(), buffers.getBuffer(RenderType.eyes(model.glowTexture())),
                    LightTexture.FULL_BRIGHT, overlay);
        }
    }

    private static void draw(WeaponModel model, PoseStack.Pose pose, VertexConsumer out, int light, int overlay) {
        float[] q = model.quads();
        for (int i = 0; i < q.length; i += WeaponModel.QUAD_FLOATS) {
            float nx = q[i + 20], ny = q[i + 21], nz = q[i + 22];
            for (int v = 0; v < 4; v++) {
                int o = i + v * 5;
                out.addVertex(pose, q[o] / 16f, q[o + 1] / 16f, q[o + 2] / 16f)
                        .setColor(255, 255, 255, 255)
                        .setUv(q[o + 3], q[o + 4])
                        .setOverlay(overlay)
                        .setLight(light)
                        .setNormal(pose, nx, ny, nz);
            }
        }
    }
}
