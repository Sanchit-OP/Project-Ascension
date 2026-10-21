package com.ascension.combat.internal.enchanting.library;

import com.ascension.combat.AscensionCombat;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registers the Enchantment Library: block, item, block entity and menu. */
public final class LibraryContent {

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, AscensionCombat.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, AscensionCombat.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, AscensionCombat.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(BuiltInRegistries.MENU, AscensionCombat.MOD_ID);

    public static final Supplier<Block> BLOCK = BLOCKS.register("enchantment_library", () -> new EnchantmentLibraryBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(3.0f, 12.0f).sound(SoundType.WOOD)));

    public static final Supplier<Item> ITEM = ITEMS.register("enchantment_library", () -> new BlockItem(BLOCK.get(), new Item.Properties()));

    public static final Supplier<BlockEntityType<EnchantmentLibraryBlockEntity>> BLOCK_ENTITY = BLOCK_ENTITIES.register(
            "enchantment_library", () -> BlockEntityType.Builder.of(EnchantmentLibraryBlockEntity::new, BLOCK.get()).build(null));

    public static final Supplier<MenuType<LibraryMenu>> MENU = MENUS.register("enchantment_library",
            () -> IMenuTypeExtension.create((id, inventory, buffer) -> new LibraryMenu(id, inventory)));

    private LibraryContent() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        modBus.addListener(LibraryContent::addToCreativeTabs);
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ITEM.get());
        }
    }
}
