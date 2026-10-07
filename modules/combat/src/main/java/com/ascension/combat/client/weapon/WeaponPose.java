package com.ascension.combat.client.weapon;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

/**
 * The third-person arm pose of a weapon with a {@link HoldAnimation}: the arms, the body's twist
 * and the weapon's turn in the hand, set every frame from the holder's state. Standing and
 * walking it plays the hold's idle pose; swinging, one of its attacks, picked from the tick the
 * swing began, so the same swing looks the same to everyone and nothing is remembered between
 * frames.
 *
 * <p>Vanilla adds its own swing to the arms right after an arm pose is applied. This pose plays
 * the swing itself, so it reads the swing's progress and then clears {@code attackTime} on the
 * model for this frame, which leaves vanilla's swing nothing to add. Purely visual: the attack
 * itself, its timing and what it hits are untouched.
 *
 * <p>The arm pose is a new constant of vanilla's {@link HumanoidModel.ArmPose}, added by NeoForge
 * from {@code META-INF/enumextensions.json}; it is two-handed, so the off hand gives way.
 */
public final class WeaponPose {
    public static final EnumProxy<HumanoidModel.ArmPose> ARM_POSE = new EnumProxy<>(
            HumanoidModel.ArmPose.class, true, (IArmPoseTransformer) WeaponPose::apply);

    // The weapon's turn about its grip for the held-item render that follows this pose on the
    // render thread. Numbers only: no entity is kept (ADR-0007).
    private static final float[] turn = new float[3];
    private static final float[] grip = new float[3];
    private static boolean turnPending;

    private WeaponPose() {
    }

    /** The arm pose for this stack, or null to be held like any item. */
    static HumanoidModel.ArmPose armPose(ItemStack stack) {
        WeaponModel model = WeaponModels.INSTANCE.get(stack.getItem());
        return model != null && model.hold() != null ? ARM_POSE.getValue() : null;
    }

    /**
     * The turn the last pose asked of the weapon about its grip, as (x, y, z) degrees then the
     * grip (x, y, z) in model pixels, or null if none is waiting. Taking it clears it.
     */
    static float[] takeTurn() {
        if (!turnPending) {
            return null;
        }
        turnPending = false;
        return new float[]{turn[0], turn[1], turn[2], grip[0], grip[1], grip[2]};
    }

    private static void apply(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        InteractionHand hand = arm == entity.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        WeaponModel weapon = WeaponModels.INSTANCE.get(entity.getItemInHand(hand).getItem());
        if (weapon == null || weapon.hold() == null) {
            return;
        }
        HoldAnimation hold = weapon.hold();
        HumanoidArm swingArm = entity.swingingArm == InteractionHand.OFF_HAND
                ? entity.getMainArm().getOpposite() : entity.getMainArm();
        HoldAnimation.Pose pose;
        boolean idle;
        if (model.attackTime > 0 && swingArm == arm) {
            long start = entity.level().getGameTime() - entity.swingTime;
            int h = (int) (start ^ (start >>> 32)) * 0x45d9f3b;
            h ^= h >>> 16;
            pose = hold.attack(Math.floorMod(h + entity.getId(), hold.attackCount()), model.attackTime);
            model.attackTime = 0;
            idle = false;
        } else {
            pose = hold.idle;
            idle = true;
        }
        float m = arm == HumanoidArm.LEFT ? -1 : 1;
        ModelPart held = arm == HumanoidArm.LEFT ? model.leftArm : model.rightArm;
        ModelPart other = arm == HumanoidArm.LEFT ? model.rightArm : model.leftArm;
        // Standing or walking, keep some of vanilla's walking swing (already on the arm).
        set(held, pose.rightArm(), m, idle ? held.xRot * hold.walkSwing : 0);
        if (pose.twoHanded()) {
            set(other, pose.leftArm(), m, 0);
        }
        float twist = m * pose.bodyTwist() * Mth.DEG_TO_RAD;
        if (twist != 0) {
            // The arms follow the body round, as vanilla's own swing moves them.
            model.body.yRot = twist;
            model.rightArm.z = Mth.sin(twist) * 5;
            model.rightArm.x = -Mth.cos(twist) * 5;
            model.leftArm.z = -Mth.sin(twist) * 5;
            model.leftArm.x = Mth.cos(twist) * 5;
        }
        float[] item = pose.item();
        turn[0] = item[0];
        turn[1] = m * item[1];
        turn[2] = m * item[2];
        System.arraycopy(hold.grip, 0, grip, 0, 3);
        turnPending = true;
    }

    private static void set(ModelPart part, float[] degrees, float mirror, float extraX) {
        part.xRot = degrees[0] * Mth.DEG_TO_RAD + extraX;
        part.yRot = mirror * degrees[1] * Mth.DEG_TO_RAD;
        part.zRot = mirror * degrees[2] * Mth.DEG_TO_RAD;
    }
}
