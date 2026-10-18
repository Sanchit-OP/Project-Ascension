package com.ascension.combat.internal.enchanting;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Tome of Extraction (crafting grid, any shape): an enchanted item, a Tome of Extraction and a plain book
 * make an enchanted book holding all the item's enchantments, and the item comes back without them. The
 * tome and the book are used up.
 *
 * <p>A crafting recipe rather than an anvil one because it returns two things, and a crafting recipe may
 * return remaining items beside its result.
 */
public final class ExtractionRecipe extends CustomRecipe {

    public static final RecipeSerializer<ExtractionRecipe> SERIALIZER = new SimpleCraftingRecipeSerializer<>(ExtractionRecipe::new);

    public ExtractionRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return find(input) >= 0;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        int slot = find(input);
        if (slot < 0) {
            return ItemStack.EMPTY;
        }

        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        book.set(DataComponents.STORED_ENCHANTMENTS, input.getItem(slot).get(DataComponents.ENCHANTMENTS));
        return book;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        int slot = find(input);
        if (slot >= 0) {
            remaining.set(slot, TomeAnvil.stripped(input.getItem(slot)));
        }
        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 3;
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return SERIALIZER;
    }

    /** The grid slot of the enchanted item when the grid holds exactly it, one tome and one book; otherwise -1. */
    private static int find(CraftingInput input) {
        int item = -1;
        int tomes = 0;
        int books = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(EnchantingContent.TOME_OF_EXTRACTION.get())) {
                tomes++;
            } else if (stack.is(Items.BOOK)) {
                books++;
            } else if (item < 0 && TomeAnvil.hasEnchantments(stack)) {
                item = i;
            } else {
                return -1;
            }
        }
        return tomes == 1 && books == 1 ? item : -1;
    }
}
