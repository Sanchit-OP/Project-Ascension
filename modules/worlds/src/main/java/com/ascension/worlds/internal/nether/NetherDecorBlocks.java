package com.ascension.worlds.internal.nether;

import com.ascension.worlds.AscensionWorlds;
import com.ascension.worlds.internal.GatewayDecorBlocks;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Planet 3's decorative blocks: the pictures carved into the Nether colony's ruins and the Sealers'
 * carving in the soul sand valleys. Decoration only &mdash; no block here has behaviour, and the
 * colony plinth is not one of them ({@code ArchiveContent} owns it; only its look is design's).
 *
 * <p>The design side's class, kept apart from logic's the way {@link GatewayDecorBlocks} is kept
 * apart from {@code WorldsContent} ({@code docs/technical/nether-art-format.md}). The structure
 * templates name these blocks by id. Design record: {@code docs/technical/nether-art-design.md}.
 *
 * <p>Hardness matches the stone each is cut in. Mining tool:
 * {@code data/ascension_worlds/tags/block/nether_decor.json}, pulled into
 * {@code #minecraft:mineable/pickaxe}.
 */
public final class NetherDecorBlocks {

    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, AscensionWorlds.MOD_ID);
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, AscensionWorlds.MOD_ID);

    /** The colonists' carving: gold inlaid in black stone, as hard as polished blackstone. */
    public static final Supplier<ColonyMuralBlock> COLONY_MURAL = block("colony_mural",
            () -> new ColonyMuralBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .requiresCorrectToolForDrops()
                    .strength(2.0f, 6.0f)
                    .sound(SoundType.POLISHED_DEEPSLATE)));

    /** The Sealers' carving: their pale plaster, as on the Moon. */
    public static final Supplier<SealerCarvingBlock> SEALER_CARVING = block("sealer_carving",
            () -> new SealerCarvingBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.QUARTZ)
                    .requiresCorrectToolForDrops()
                    .strength(1.5f, 6.0f)
                    .sound(SoundType.STONE)));

    private NetherDecorBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(NetherDecorBlocks::addToCreativeTabs);
    }

    private static <B extends Block> Supplier<B> block(String name, Supplier<B> factory) {
        Supplier<B> block = BLOCKS.register(name, factory);
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(COLONY_MURAL.get());
            event.accept(SEALER_CARVING.get());
        }
    }
}
