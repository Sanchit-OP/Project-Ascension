package com.ascension.worlds.internal.archive;

import com.ascension.worlds.AscensionWorlds;
import com.ascension.worlds.internal.GatewayDecorBlocks;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Lunar Archive's decorative blocks: the stairs its builders cut from the same lunar stone as
 * the gateway's masonry, the tiles of its carved murals, and the glyphs cut into its pylon.
 * Decoration only &mdash; no block here has behaviour, and none of them is the plinth, the orrery or
 * the tablet, which {@link ArchiveContent} owns.
 *
 * <p>The design side's class, kept apart from {@code ArchiveContent} the way
 * {@link GatewayDecorBlocks} is kept apart from {@code WorldsContent}: the Archive's look and its
 * logic are owned separately ({@code docs/technical/lunar-archive-format.md}). The structure
 * templates name these blocks by id. Design record: {@code docs/technical/lunar-archive-design.md}.
 *
 * <p>Hardness matches the stone each is cut from, except the pylon's glyph, which cannot be broken
 * in survival. Mining tool: {@code data/ascension_worlds/tags/block/archive_decor.json}, pulled into
 * {@code #minecraft:mineable/pickaxe}.
 */
public final class ArchiveDecorBlocks {

    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, AscensionWorlds.MOD_ID);
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, AscensionWorlds.MOD_ID);

    /** The Archive's covered stair and its deep passage are cut in these. */
    public static final Supplier<StairBlock> MOON_BASALT_BRICK_STAIRS = block("moon_basalt_brick_stairs",
            () -> new StairBlock(GatewayDecorBlocks.MOON_BASALT_BRICKS.get().defaultBlockState(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .requiresCorrectToolForDrops()
                    .strength(1.5f, 6.0f)
                    .sound(SoundType.DEEPSLATE_BRICKS)));

    /** The open steps down into the cutting, pale under the sky. */
    public static final Supplier<StairBlock> ANORTHOSITE_BRICK_STAIRS = block("anorthosite_brick_stairs",
            () -> new StairBlock(GatewayDecorBlocks.ANORTHOSITE_BRICKS.get().defaultBlockState(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.QUARTZ)
                    .requiresCorrectToolForDrops()
                    .strength(1.5f, 6.0f)
                    .sound(SoundType.STONE)));

    /** Carved plaster: one tile of a mural, or bare plaster (tile 0, what the item places). */
    public static final Supplier<ArchiveMuralBlock> ARCHIVE_MURAL = block("archive_mural",
            () -> new ArchiveMuralBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.QUARTZ)
                    .requiresCorrectToolForDrops()
                    .strength(1.5f, 6.0f)
                    .sound(SoundType.STONE)));

    /**
     * One glyph of the script, cut into the pylon (lore art, section 5). Unbreakable in survival and
     * dropping nothing, like the plinths, because the story reads it; light 7 once its gate is lit.
     */
    public static final Supplier<MarkGlyphBlock> MARK_GLYPH = block("mark_glyph",
            () -> new MarkGlyphBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.QUARTZ)
                    .sound(SoundType.STONE)
                    .strength(-1.0f, 3_600_000.0f)
                    .noLootTable()
                    .pushReaction(PushReaction.BLOCK)
                    .lightLevel(state -> state.getValue(MarkGlyphBlock.LIT) ? 7 : 0)));

    private ArchiveDecorBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(ArchiveDecorBlocks::addToCreativeTabs);
    }

    private static <B extends Block> Supplier<B> block(String name, Supplier<B> factory) {
        Supplier<B> block = BLOCKS.register(name, factory);
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(MOON_BASALT_BRICK_STAIRS.get());
            event.accept(ANORTHOSITE_BRICK_STAIRS.get());
            event.accept(ARCHIVE_MURAL.get());
            event.accept(MARK_GLYPH.get());
        }
    }
}
