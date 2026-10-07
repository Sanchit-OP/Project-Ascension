package com.ascension.combat.client.weapon;

import com.ascension.combat.AscensionCombat;
import com.ascension.combat.internal.CombatContent;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * Wires the held-weapon models: loads {@link WeaponModels} on resource reload and gives the
 * weapons one client extension each, through which {@link WeaponRenderer} draws them. The renderer
 * only runs for an item whose model JSON parents {@code builtin/entity}; until a weapon is
 * remodelled that way, its old model still shows. Held like any sword: animating the holder is an
 * animation mod's job (ascension-compat-bettercombat). Client mod bus only.
 */
@EventBusSubscriber(modid = AscensionCombat.MOD_ID, value = Dist.CLIENT)
public final class WeaponClient {
    /** The extension every held weapon uses. */
    public static final IClientItemExtensions RENDERED = new IClientItemExtensions() {
        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            return WeaponRenderer.get();
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
