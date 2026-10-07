package com.ascension.combat.client.weapon;

import com.ascension.combat.AscensionCombat;
import com.ascension.combat.internal.CombatContent;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * Wires the held-weapon models: loads {@link WeaponModels} on resource reload and gives the
 * weapons one client extension each: {@link WeaponRenderer} draws them, {@link WeaponPose} poses
 * the holder in third person ({@link WeaponFirstPerson} draws first person). The renderer only runs
 * for an item whose model JSON parents {@code builtin/entity}, and the poses only for a model with
 * a hold; until a weapon is remodelled that way, its old model and vanilla's hold still show.
 * Client mod bus only.
 */
@EventBusSubscriber(modid = AscensionCombat.MOD_ID, value = Dist.CLIENT)
public final class WeaponClient {
    /** The extension every held weapon uses. */
    public static final IClientItemExtensions RENDERED = new IClientItemExtensions() {
        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            return WeaponRenderer.get();
        }

        @Override
        public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
            return WeaponPose.armPose(stack);
        }
    };

    private WeaponClient() {
    }

    @SubscribeEvent
    static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(WeaponModels.INSTANCE);
    }

    @SubscribeEvent
    static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(RENDERED, CombatContent.VEIGOS_BLADE.get(), CombatContent.DARKEN_BLADE.get(),
                CombatContent.DARIUS_AXE.get());
    }
}
