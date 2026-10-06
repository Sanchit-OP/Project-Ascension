package com.ascension.worlds.internal.archive;

import com.ascension.worlds.AscensionWorlds;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Everything the Lunar Archive registers, in its own class per the art guide ("new blocks go in
 * their own registration class"). The contract with the design session, which owns how these look,
 * is {@code docs/technical/lunar-archive-format.md}.
 */
public final class ArchiveContent {

    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, AscensionWorlds.MOD_ID);
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, AscensionWorlds.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AscensionWorlds.MOD_ID);
    private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, AscensionWorlds.MOD_ID);

    /** Which {@link ArchiveEntry} a tablet carries. */
    public static final Supplier<DataComponentType<ResourceLocation>> ARCHIVE_ENTRY = DATA_COMPONENT_TYPES.register(
            "archive_entry",
            () -> DataComponentType.<ResourceLocation>builder()
                    .persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ResourceLocation.STREAM_CODEC)
                    .build());

    public static final Supplier<ArchivePlinthBlock> ARCHIVE_PLINTH = BLOCKS.register(
            "archive_plinth", () -> new ArchivePlinthBlock(unbreakable(MapColor.QUARTZ)));

    /** Lit, so the orrery chamber is never pitch dark and never a spawner around its own heart. */
    public static final Supplier<ArchiveOrreryBlock> ARCHIVE_ORRERY = BLOCKS.register(
            "archive_orrery", () -> new ArchiveOrreryBlock(unbreakable(MapColor.COLOR_BLACK).lightLevel(state -> 10)));

    public static final Supplier<Item> ARCHIVE_PLINTH_ITEM = ITEMS.register(
            "archive_plinth", () -> new BlockItem(ARCHIVE_PLINTH.get(), new Item.Properties()));

    public static final Supplier<Item> ARCHIVE_ORRERY_ITEM = ITEMS.register(
            "archive_orrery", () -> new BlockItem(ARCHIVE_ORRERY.get(), new Item.Properties()));

    public static final Supplier<ArchiveTabletItem> ARCHIVE_TABLET = ITEMS.register(
            "archive_tablet", () -> new ArchiveTabletItem(new Item.Properties().stacksTo(16)));

    @SuppressWarnings("DataFlowIssue") // the data-fixer type is null by design for mod block entities
    public static final Supplier<BlockEntityType<ArchivePlinthBlockEntity>> PLINTH_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("archive_plinth", () -> BlockEntityType.Builder
                    .of(ArchivePlinthBlockEntity::new, ARCHIVE_PLINTH.get())
                    .build(null));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        DATA_COMPONENT_TYPES.register(modBus);
        modBus.addListener(ArchiveContent::addToCreativeTabs);
    }

    /** Gateway-field rules: unbreakable by hand, immovable by pistons, still blast-proof. */
    private static BlockBehaviour.Properties unbreakable(MapColor color) {
        return BlockBehaviour.Properties.of()
                .mapColor(color)
                .sound(SoundType.DEEPSLATE_BRICKS)
                .strength(-1.0f, 3_600_000.0f)
                .noLootTable()
                .pushReaction(PushReaction.BLOCK);
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ARCHIVE_PLINTH_ITEM.get());
            event.accept(ARCHIVE_ORRERY_ITEM.get());
        }
    }

    private ArchiveContent() {
    }
}
