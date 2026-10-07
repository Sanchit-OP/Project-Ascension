package com.ascension.combat.client.weapon;

import com.ascension.combat.AscensionCombat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * First person for a weapon with a {@link FirstPersonHold}: takes over vanilla's hand rendering
 * (which shows no arm while an item is held) and draws the weapon where the hold puts it, with the
 * player's own arms, in their skin, reaching to its grips: the right hand always, the left too
 * when the pose is two-handed. Vanilla's off-hand item is not drawn while such a weapon is held,
 * since that hand belongs on the hilt. Purely visual; the swing's timing and effect are vanilla's.
 *
 * <p>Holds nothing between frames: everything is read from the player and the model each time.
 */
@EventBusSubscriber(modid = AscensionCombat.MOD_ID, value = Dist.CLIENT)
public final class WeaponFirstPerson {
    /** Where the bottom middle of each arm's box sits in the arm model's own space, in pixels. */
    private static final Vector3f RIGHT_HAND_END = new Vector3f(-6, 12, 0);
    private static final Vector3f LEFT_HAND_END = new Vector3f(6, 12, 0);

    private WeaponFirstPerson() {
    }

    @SubscribeEvent
    static void onRenderHand(RenderHandEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        WeaponModel model = WeaponModels.INSTANCE.get(player.getMainHandItem().getItem());
        FirstPersonHold hold = model == null ? null : model.firstPerson();
        if (hold == null) {
            return;
        }
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return; // that hand is on the hilt
        }
        float swing = event.getSwingProgress();
        FirstPersonHold.Pose pose = swing > 0
                ? hold.attack(HoldAnimation.pickAttack(player, hold.attackCount()), swing) : hold.idle;
        float m = player.getMainArm() == HumanoidArm.LEFT ? -1 : 1;

        // The weapon, placed by its right-hand grip point.
        Matrix4f weapon = new Matrix4f()
                .translate(m * pose.pos()[0], pose.pos()[1] - 0.6f * event.getEquipProgress(), pose.pos()[2])
                .rotateZ(m * pose.rotation()[2] * DEG)
                .rotateY(m * pose.rotation()[1] * DEG)
                .rotateX(pose.rotation()[0] * DEG)
                .scale(hold.scale)
                .translate(-hold.rightHand[0] / 16f, -hold.rightHand[1] / 16f, -hold.rightHand[2] / 16f);
        PoseStack stack = event.getPoseStack();
        stack.pushPose();
        stack.mulPose(weapon);
        WeaponRenderer.drawModel(model, stack, event.getMultiBufferSource(), event.getPackedLight(),
                OverlayTexture.NO_OVERLAY);
        stack.popPose();

        // The arms: each hand on its grip, the arm reaching back toward its shoulder.
        PlayerRenderer renderer = (PlayerRenderer) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
        Vector3f rightGrip = weapon.transformPosition(new Vector3f(hold.rightHand).div(16));
        drawArm(event, renderer, player, m > 0, rightGrip, mirrored(hold.rightShoulder, m));
        if (pose.twoHands()) {
            Vector3f leftGrip = weapon.transformPosition(new Vector3f(hold.leftHand).div(16));
            drawArm(event, renderer, player, m < 0, leftGrip, mirrored(hold.leftShoulder, m));
        }
    }

    private static final float DEG = (float) (Math.PI / 180);

    private static Vector3f mirrored(float[] v, float m) {
        return new Vector3f(m * v[0], v[1], v[2]);
    }

    /** One arm whose hand end sits on {@code grip}, its length pointing back toward {@code shoulder}. */
    private static void drawArm(RenderHandEvent event, PlayerRenderer renderer, LocalPlayer player, boolean rightArm,
                                Vector3f grip, Vector3f shoulder) {
        Vector3f toHand = new Vector3f(grip).sub(shoulder);
        if (toHand.lengthSquared() < 1e-6f) {
            return;
        }
        toHand.normalize();
        Vector3f end = rightArm ? RIGHT_HAND_END : LEFT_HAND_END;
        PoseStack stack = event.getPoseStack();
        stack.pushPose();
        stack.translate(grip.x, grip.y, grip.z);
        // The arm model hangs along +y from shoulder to hand.
        stack.mulPose(new Quaternionf().rotationTo(0, 1, 0, toHand.x, toHand.y, toHand.z));
        stack.translate(-end.x / 16f, -end.y / 16f, -end.z / 16f);
        if (rightArm) {
            renderer.renderRightHand(stack, event.getMultiBufferSource(), event.getPackedLight(), player);
        } else {
            renderer.renderLeftHand(stack, event.getMultiBufferSource(), event.getPackedLight(), player);
        }
        stack.popPose();
    }
}
