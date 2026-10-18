package com.ascension.combat.internal.enchanting;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

/**
 * The Tome of Scrapping at an anvil: an enchanted item and one tome give the same item with every
 * enchantment (curses and Soulbound too) taken off, for one level, using up the tome. For starting an
 * item's enchanting again. Nothing here ticks; it answers only when the anvil's inputs change.
 *
 * <p>The Tome of Extraction is a crafting recipe instead ({@link ExtractionRecipe}), because an anvil
 * can return one item and extraction returns two (the book and the item it came from).
 */
public final class TomeAnvil {

    private static final int LEVEL_COST = 1;

    private TomeAnvil() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(TomeAnvil::onAnvil);
    }

    private static void onAnvil(AnvilUpdateEvent event) {
        ItemStack item = event.getLeft();
        if (!event.getRight().is(EnchantingContent.TOME_OF_SCRAPPING.get()) || !hasEnchantments(item)) {
            return;
        }

        event.setOutput(stripped(item));
        event.setCost(LEVEL_COST);
        event.setMaterialCost(1);
    }

    /** True for an item carrying enchantments of its own. */
    static boolean hasEnchantments(ItemStack stack) {
        return !stack.isEmpty() && !stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty();
    }

    /** A copy of {@code stack} with no enchantments. */
    static ItemStack stripped(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        return copy;
    }
}
