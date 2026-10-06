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
 * Wires the held-weapon models: loads {@link WeaponModels} on resource reload and points the
 * weapons at {@link WeaponRenderer}. Veigo's Blade is pointed there by
 * {@code VeigosBladeClient}, whose extension also carries its swing (an item gets one extension).
 * The renderer only runs for an item whose model JSON parents {@code builtin/entity}; until a
 * weapon's model is redrawn that way, its old model still shows. Client mod bus only.
 */
@EventBusSubscriber(modid = AscensionCombat.MOD_ID, value = Dist.CLIENT)
public final class WeaponClient {
    /** The extension a held weapon without a custom swing uses. */
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
        event.registerItem(RENDERED, CombatContent.DARKEN_BLADE.get(), CombatContent.DARIUS_AXE.get());
    }
}
