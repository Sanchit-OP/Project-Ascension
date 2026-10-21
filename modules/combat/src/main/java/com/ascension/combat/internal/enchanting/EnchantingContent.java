package com.ascension.combat.internal.enchanting;

import com.ascension.combat.AscensionCombat;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The enchanting work's items and recipe type (docs/progression/enchantments.md): the Tome of Scrapping,
 * used at an anvil ({@link TomeAnvil}), and the Tome of Extraction, used in a crafting grid
 * ({@link ExtractionRecipe}). Both are crafted from a book, lapis and an ordinary material, so no world is
 * ever needed (ADR-0018). Soulbound is {@link Soulbound}.
 */
public final class EnchantingContent {

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, AscensionCombat.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, AscensionCombat.MOD_ID);

    public static final Supplier<Item> TOME_OF_SCRAPPING = ITEMS.register("tome_of_scrapping",
            () -> new TomeItem("item.ascension_combat.tome_of_scrapping.usage"));
    public static final Supplier<Item> TOME_OF_EXTRACTION = ITEMS.register("tome_of_extraction",
            () -> new TomeItem("item.ascension_combat.tome_of_extraction.usage"));

    static {
        RECIPE_SERIALIZERS.register("extraction", () -> ExtractionRecipe.SERIALIZER);
    }

    private EnchantingContent() {
    }

    public static void register(IEventBus modBus, IEventBus gameBus) {
        ITEMS.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
        EnchantingAttachments.register(modBus);
        com.ascension.combat.internal.enchanting.library.LibraryContent.register(modBus);
        modBus.addListener(EnchantingContent::addToCreativeTabs);
        TomeAnvil.register(gameBus);
        Soulbound.register(gameBus);
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(TOME_OF_SCRAPPING.get());
            event.accept(TOME_OF_EXTRACTION.get());
        }
    }
}
