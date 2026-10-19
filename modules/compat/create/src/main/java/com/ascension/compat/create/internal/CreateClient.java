package com.ascension.compat.create.internal;

import com.ascension.compat.create.AscensionCompatCreate;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Client only: plain-language help on this jar's block items, from the lang file. */
@EventBusSubscriber(modid = AscensionCompatCreate.MOD_ID, value = Dist.CLIENT)
public final class CreateClient {

    private CreateClient() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        var item = event.getItemStack().getItem();
        if (!(item instanceof BlockItem)
                || !BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(AscensionCompatCreate.MOD_ID)) {
            return;
        }
        String base = item.getDescriptionId();
        for (String suffix : new String[] {".tooltip", ".tooltip2", ".tooltip3"}) {
            if (I18n.exists(base + suffix)) {
                event.getToolTip().add(Component.translatable(base + suffix).withStyle(ChatFormatting.GRAY));
            }
        }
    }
}
