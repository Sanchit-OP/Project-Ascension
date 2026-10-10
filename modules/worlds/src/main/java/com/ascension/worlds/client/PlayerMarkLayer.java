package com.ascension.worlds.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

/**
 * The mark on a player in third person: on the back of the right hand, following the arm through
 * every pose and swing because it is drawn in the arm's own space. Seen by anyone but its wearer it
 * hangs upside down, as a hand tattoo does (lore-art-design.md, section 4). Every player on the
 * server shows the same stage: the gates are shared, so the mark is too ({@code GateProgress}).
 *
 * <p>First person is {@link MarkClientEvents#onRenderArm}, which poses the arm as vanilla does and
 * then draws through the same {@link #draw}.
 */
final class PlayerMarkLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private final boolean slim;

    PlayerMarkLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, boolean slim) {
        super(parent);
        this.slim = slim;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        int stage = ClientGateProgress.markStage();
        if (stage == 0 || player.isInvisible() || !getParentModel().rightArm.visible) {
            return;
        }
        draw(poseStack, buffers, getParentModel().rightArm, slim, stage, player, packedLight);
    }

    /**
     * The mark on a right arm already posed for this frame, in third person or first. The wide
     * arm's cube spans x -3..1 and the slim one's -2..1, and the decal stands 0.3 px out from it.
     */
    static void draw(PoseStack poseStack, MultiBufferSource buffers, ModelPart rightArm, boolean slim, int stage,
                     AbstractClientPlayer player, int packedLight) {
        poseStack.pushPose();
        rightArm.translateAndRotate(poseStack);
        MarkDecal.onRightArm(poseStack, buffers, MarkDecal.stage(stage), slim ? -2.3F : -3.3F,
                MarkDecal.light(player, packedLight));
        poseStack.popPose();
    }
}
