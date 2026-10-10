package com.ascension.worlds.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.IllagerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.monster.AbstractIllager;

/**
 * The complete mark, all seven glyphs round an open eye, on the illagers' crossed arms: the villagers
 * who kept listening to the voice, worn for the world to see ({@code docs/lore/the-mark.md}). It is
 * always complete, whatever the player's stage, so that halfway through the game a player can look
 * at their own hand and see it becoming this.
 *
 * <p>Only while the arms are crossed: that box is drawn only then (vanilla hides it while an illager
 * attacks, casts or holds a crossbow, so pillagers rarely show it; lore-art-design.md, "For the
 * logic session", item 2).
 */
final class IllagerMarkLayer<T extends AbstractIllager> extends RenderLayer<T, IllagerModel<T>> {

    /** The crossed arms, looked up once: the renderer keeps one model for its lifetime. */
    private final ModelPart arms;

    IllagerMarkLayer(RenderLayerParent<T, IllagerModel<T>> parent) {
        super(parent);
        this.arms = parent.getModel().root().getChild("arms");
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight, T illager,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (!arms.visible || illager.isInvisible()) {
            return;
        }
        poseStack.pushPose();
        arms.translateAndRotate(poseStack);
        MarkDecal.onCrossedArms(poseStack, buffers, MarkDecal.light(illager, packedLight));
        poseStack.popPose();
    }
}
