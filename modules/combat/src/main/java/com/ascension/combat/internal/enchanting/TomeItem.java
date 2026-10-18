package com.ascension.combat.internal.enchanting;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

/** A tome: a plain item whose use is elsewhere (an anvil, a crafting grid), with a line saying what it does. */
public final class TomeItem extends Item {

    private final String usageKey;

    public TomeItem(String usageKey) {
        super(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
        this.usageKey = usageKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(usageKey).withStyle(ChatFormatting.GRAY));
    }
}
