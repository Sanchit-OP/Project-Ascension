package com.ascension.atmosphere.internal.supply;

import com.ascension.atmosphere.internal.AtmosphereContent;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Upgrades an oxygen tank: one tank plus {@code count} of an upgrade material (helium, named by a
 * common item tag so this module never names the Moon's mod) makes the larger tank, and the old
 * tank's air and valve state carry over. A plain crafting recipe could not keep them.
 *
 * <p>JSON: {@code ingredients} = [the tank, the material], {@code count}, {@code result}.
 */
public record TankUpgradeRecipe(List<Ingredient> ingredients, int count, ItemStack result)
        implements CraftingRecipe {

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int tanks = 0;
        int material = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (ingredients.get(0).test(stack)) {
                tanks++;
            } else if (ingredients.get(1).test(stack)) {
                material++;
            } else {
                return false;
            }
        }
        return tanks == 1 && material == count;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack out = result.copy();
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (ingredients.get(0).test(stack)) {
                OxygenTankItem.setUnits(out, OxygenTankItem.units(stack));
                OxygenTankItem.setOpen(out, OxygenTankItem.isOpen(stack));
                break;
            }
        }
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= count + 1;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> all = NonNullList.create();
        all.add(ingredients.get(0));
        for (int i = 0; i < count; i++) {
            all.add(ingredients.get(1));
        }
        return all;
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.EQUIPMENT;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return AtmosphereContent.TANK_UPGRADE_SERIALIZER.get();
    }

    public static final class Serializer implements RecipeSerializer<TankUpgradeRecipe> {

        private static final MapCodec<TankUpgradeRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Ingredient.LIST_CODEC.validate(list -> list.size() == 2
                                ? DataResult.success(list)
                                : DataResult.<List<Ingredient>>error(() -> "needs exactly two ingredients"))
                        .fieldOf("ingredients").forGetter(TankUpgradeRecipe::ingredients),
                Codec.intRange(1, 8).fieldOf("count").forGetter(TankUpgradeRecipe::count),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(TankUpgradeRecipe::result)
        ).apply(i, TankUpgradeRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, TankUpgradeRecipe> STREAM = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), TankUpgradeRecipe::ingredients,
                ByteBufCodecs.VAR_INT, TankUpgradeRecipe::count,
                ItemStack.STREAM_CODEC, TankUpgradeRecipe::result,
                TankUpgradeRecipe::new);

        @Override
        public MapCodec<TankUpgradeRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, TankUpgradeRecipe> streamCodec() {
            return STREAM;
        }
    }
}
