package com.ascension.combat.client;

import com.ascension.combat.AscensionCombat;
import com.ascension.combat.client.weapon.WeaponRenderer;
import com.ascension.combat.internal.CombatContent;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * Greatsword first-person motion. The rest pose and size remain editable in the item JSON;
 * this extension replaces the short vanilla sword swing with wind-up, cut, and recovery.
 * Uses vanilla swing progress, so no timer, retained player, network traffic, or damage change.
 * It also points the blade at {@link WeaponRenderer}, which draws its held model (an item gets one
 * extension, so this one carries both).
 * Discovered on the client mod bus only; common item registration never loads client classes.
 */
@EventBusSubscriber(modid = AscensionCombat.MOD_ID, value = Dist.CLIENT)
public final class VeigosBladeClient {
    private VeigosBladeClient() {
    }

    @SubscribeEvent
    public static void registerExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return WeaponRenderer.get();
            }

            @Override
            public boolean applyForgeHandTransform(
                    PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack,
                    float partialTick, float equipProgress, float swingProgress) {
                if (player.isAutoSpinAttack() || player.isUsingItem()) {
                    return false;
                }
                float side = arm == HumanoidArm.RIGHT ? 1.0f : -1.0f;
                float swing = Mth.clamp(swingProgress, 0.0f, 1.0f);
                float windUp = ease(swing / 0.18f) * (1.0f - ease((swing - 0.18f) / 0.18f));
                float cut = ease((swing - 0.18f) / 0.28f)
                        * (1.0f - ease((swing - 0.55f) / 0.45f));

                // Keep the standard equip lowering, while the model defines the large idle pose.
                pose.translate(side * (0.56f + 0.08f * windUp - 0.32f * cut),
                        -0.52f - 0.6f * equipProgress + 0.06f * windUp - 0.10f * cut,
                        -0.72f + 0.08f * windUp - 0.16f * cut);
                pose.mulPose(Axis.YP.rotationDegrees(side * (10.0f * windUp - 28.0f * cut)));
                pose.mulPose(Axis.ZP.rotationDegrees(side * (-14.0f * windUp + 90.0f * cut)));
                pose.mulPose(Axis.XP.rotationDegrees(-25.0f * cut));
                return true;
            }
        }, CombatContent.VEIGOS_BLADE.get());
    }

    private static float ease(float value) {
        float t = Mth.clamp(value, 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }
}
