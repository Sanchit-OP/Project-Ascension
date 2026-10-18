package com.ascension.compat.jei.internal;

import com.ascension.compat.jei.AscensionCompatJei;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * The "Build path" page: for one combat item, every raw material it costs across all worlds, with
 * counts, and the boss heart it needs. JEI already shows each crafting step one at a time; this
 * page adds the whole tree flattened, so a player sees at a glance which worlds an item asks for.
 *
 * <p>Nothing is hard-coded about the items: the page reads the {@code ascension_combat:build/...}
 * crafting recipes, treats an item that another build recipe consumes as a component and anything
 * else as raw, and shows a page for each item no build recipe consumes.
 */
@JeiPlugin
public final class BuildPathPlugin implements IModPlugin {

    static final String NAMESPACE = "ascension_combat";
    static final RecipeType<BuildPath> TYPE =
            RecipeType.create(AscensionCompatJei.MOD_ID, "build_path", BuildPath.class);

    private static final Set<String> NETHER = Set.of(
            "netherite_ingot", "blaze_powder", "blaze_rod", "magma_cream", "ghast_tear", "quartz",
            "glowstone_dust", "nether_wart", "wither_skeleton_skull");

    /** One raw ingredient of the tree: what can fill the slot, each stack carrying the total count. */
    record Raw(List<ItemStack> choices) {
    }

    record BuildPath(ItemStack result, List<Raw> raws) {
    }

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(AscensionCompatJei.MOD_ID, "build_path");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new Category(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        Map<ResourceLocation, CraftingRecipe> build = new LinkedHashMap<>();
        for (RecipeHolder<CraftingRecipe> holder : level.getRecipeManager()
                .getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING)) {
            if (holder.id().getNamespace().equals(NAMESPACE) && holder.id().getPath().startsWith("build/")) {
                ItemStack result = holder.value().getResultItem(level.registryAccess());
                build.put(BuiltInRegistries.ITEM.getKey(result.getItem()), holder.value());
            }
        }
        Set<ResourceLocation> consumed = new HashSet<>();
        for (CraftingRecipe recipe : build.values()) {
            for (Ingredient ingredient : recipe.getIngredients()) {
                ItemStack[] items = ingredient.getItems();
                if (items.length > 0) {
                    consumed.add(BuiltInRegistries.ITEM.getKey(items[0].getItem()));
                }
            }
        }
        List<BuildPath> pages = new ArrayList<>();
        for (var entry : build.entrySet()) {
            if (consumed.contains(entry.getKey())) {
                continue;
            }
            Map<String, int[]> counts = new LinkedHashMap<>();
            Map<String, Ingredient> kinds = new LinkedHashMap<>();
            collect(entry.getValue(), build, counts, kinds);
            List<Raw> raws = new ArrayList<>();
            counts.forEach((key, count) -> {
                List<ItemStack> choices = new ArrayList<>();
                for (ItemStack stack : kinds.get(key).getItems()) {
                    ItemStack copy = stack.copy();
                    copy.setCount(count[0]);
                    choices.add(copy);
                }
                raws.add(new Raw(choices));
            });
            pages.add(new BuildPath(new ItemStack(BuiltInRegistries.ITEM.get(entry.getKey())), raws));
        }
        registration.addRecipes(TYPE, pages);
    }

    private static void collect(CraftingRecipe recipe, Map<ResourceLocation, CraftingRecipe> build,
                                Map<String, int[]> counts, Map<String, Ingredient> kinds) {
        for (Ingredient ingredient : recipe.getIngredients()) {
            ItemStack[] items = ingredient.getItems();
            if (items.length == 0) {
                continue;
            }
            CraftingRecipe inner = build.get(BuiltInRegistries.ITEM.getKey(items[0].getItem()));
            if (inner != null) {
                collect(inner, build, counts, kinds);
            } else {
                String key = Ingredient.CODEC.encodeStart(JsonOps.INSTANCE, ingredient).result()
                        .map(Object::toString).orElse(items[0].toString());
                counts.computeIfAbsent(key, k -> new int[1])[0] += 1;
                kinds.putIfAbsent(key, ingredient);
            }
        }
    }

    /** Where a raw material comes from, for its tooltip. */
    static Component origin(ItemStack stack) {
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (path.endsWith("_heart")) {
            return Component.translatable("jei.ascension_compat_jei.origin.heart");
        }
        if (path.contains("helium")) {
            return Component.translatable("jei.ascension_compat_jei.origin.moon");
        }
        if (NETHER.contains(path)) {
            return Component.translatable("jei.ascension_compat_jei.origin.nether");
        }
        return Component.translatable("jei.ascension_compat_jei.origin.earth");
    }

    private static final class Category implements IRecipeCategory<BuildPath> {
        private static final int COLUMNS = 7;
        private final IDrawable icon;

        Category(IGuiHelper gui) {
            ItemStack iconStack = new ItemStack(BuiltInRegistries.ITEM.get(
                    ResourceLocation.fromNamespaceAndPath(NAMESPACE, "darius_axe")));
            this.icon = gui.createDrawableItemStack(iconStack);
        }

        @Override
        public RecipeType<BuildPath> getRecipeType() {
            return TYPE;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("jei.ascension_compat_jei.build_path");
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public int getWidth() {
            return 150;
        }

        @Override
        public int getHeight() {
            return 56;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, BuildPath recipe, IFocusGroup focuses) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 130, 4).addItemStack(recipe.result());
            int i = 0;
            for (Raw raw : recipe.raws()) {
                int x = 4 + (i % COLUMNS) * 18;
                int y = 22 + (i / COLUMNS) * 18;
                builder.addSlot(RecipeIngredientRole.INPUT, x, y)
                        .addItemStacks(raw.choices())
                        .addRichTooltipCallback((view, tooltip) ->
                                view.getDisplayedItemStack().ifPresent(stack -> tooltip.add(origin(stack))));
                i++;
            }
        }

        @Override
        public void draw(BuildPath recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
            graphics.drawString(Minecraft.getInstance().font,
                    Component.translatable("jei.ascension_compat_jei.raw_materials"), 4, 8, 0xFF404040, false);
        }
    }
}
