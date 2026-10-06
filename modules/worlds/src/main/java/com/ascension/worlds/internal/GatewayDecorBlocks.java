package com.ascension.worlds.internal;

import com.ascension.worlds.AscensionWorlds;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

/**
 * The gateway portal's masonry: dressed lunar stone for the Moon's pre-generated ruin, and the
 * inscribed gatestone every gateway's ring is cut from. Decoration only &mdash; no block here has
 * behaviour, and none of them is the keystone or the field, which {@link WorldsContent} owns.
 *
 * <p>Kept apart from {@code WorldsContent} because the gateway's look and its logic are owned
 * separately (see {@code docs/technical/gateway-frame-format.md}): this class is the design
 * side's, and nothing in it is read by any Java. The frame file names these blocks by id; a
 * player-built Earth gateway may use them or the vanilla blocks its tags also accept. Design
 * record: {@code docs/technical/gateway-portal-design.md}.
 *
 * <p>Every block mirrors its raw stone's hardness, the way vanilla's stone bricks match stone.
 * Mining tool: {@code data/ascension_worlds/tags/block/gateway_decor.json}, pulled into
 * {@code #minecraft:mineable/pickaxe}.
 */
public final class GatewayDecorBlocks {

    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, AscensionWorlds.MOD_ID);
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, AscensionWorlds.MOD_ID);

    // --- lunar basalt: the gateway's dark body -------------------------------------------------

    public static final Supplier<Block> POLISHED_MOON_BASALT =
            block("polished_moon_basalt", () -> new Block(basalt(SoundType.POLISHED_DEEPSLATE)));

    public static final Supplier<Block> MOON_BASALT_BRICKS =
            block("moon_basalt_bricks", () -> new Block(basalt(SoundType.DEEPSLATE_BRICKS)));

    /** Ruin dressing for the Moon site; never part of a validated frame. */
    public static final Supplier<Block> CRACKED_MOON_BASALT_BRICKS =
            block("cracked_moon_basalt_bricks", () -> new Block(basalt(SoundType.DEEPSLATE_BRICKS)));

    public static final Supplier<SlabBlock> MOON_BASALT_BRICK_SLAB =
            block("moon_basalt_brick_slab", () -> new SlabBlock(basalt(SoundType.DEEPSLATE_BRICKS)));

    // --- anorthosite: the gateway's pale trim ----------------------------------------------------

    public static final Supplier<Block> ANORTHOSITE_BRICKS =
            block("anorthosite_bricks", () -> new Block(anorthosite()));

    /** Ruin dressing for the Moon site; never part of a validated frame. */
    public static final Supplier<Block> CRACKED_ANORTHOSITE_BRICKS =
            block("cracked_anorthosite_bricks", () -> new Block(anorthosite()));

    public static final Supplier<SlabBlock> ANORTHOSITE_BRICK_SLAB =
            block("anorthosite_brick_slab", () -> new SlabBlock(anorthosite()));

    public static final Supplier<RotatedPillarBlock> ANORTHOSITE_PILLAR =
            block("anorthosite_pillar", () -> new RotatedPillarBlock(anorthosite()));

    /** Carries the network's eye motif, the same diamond the gateway's crown is built in. */
    public static final Supplier<Block> CHISELED_ANORTHOSITE =
            block("chiseled_anorthosite", () -> new Block(anorthosite()));

    // --- the ring ----------------------------------------------------------------------------------

    /**
     * The stone that borders every gateway's field, on every planet, in the builders' script.
     * The one frame block with no tag alternative: a gateway's body is local stone, its ring is
     * always this. Crafted on Earth from polished deepslate and sculk. The one block here
     * with a state: its runes light when the gate is attuned ({@link InscribedGatestoneBlock}).
     */
    public static final Supplier<InscribedGatestoneBlock> INSCRIBED_GATESTONE = block(
            "inscribed_gatestone",
            () -> new InscribedGatestoneBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .requiresCorrectToolForDrops()
                    .strength(3.0f, 9.0f)
                    .sound(SoundType.DEEPSLATE_TILES)
                    .lightLevel(state -> state.getValue(InscribedGatestoneBlock.LIT) ? 9 : 0)));

    private GatewayDecorBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(GatewayDecorBlocks::addToCreativeTabs);
    }

    private static <B extends Block> Supplier<B> block(String name, Supplier<B> factory) {
        Supplier<B> block = BLOCKS.register(name, factory);
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    /** Same hardness as {@code moon_basalt} itself. */
    private static BlockBehaviour.Properties basalt(SoundType sound) {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .requiresCorrectToolForDrops()
                .strength(1.5f, 6.0f)
                .sound(sound);
    }

    /** Same hardness as {@code anorthosite} itself. */
    private static BlockBehaviour.Properties anorthosite() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.QUARTZ)
                .requiresCorrectToolForDrops()
                .strength(1.5f, 6.0f)
                .sound(SoundType.STONE);
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(POLISHED_MOON_BASALT.get());
            event.accept(MOON_BASALT_BRICKS.get());
            event.accept(CRACKED_MOON_BASALT_BRICKS.get());
            event.accept(MOON_BASALT_BRICK_SLAB.get());
            event.accept(ANORTHOSITE_BRICKS.get());
            event.accept(CRACKED_ANORTHOSITE_BRICKS.get());
            event.accept(ANORTHOSITE_BRICK_SLAB.get());
            event.accept(ANORTHOSITE_PILLAR.get());
            event.accept(CHISELED_ANORTHOSITE.get());
            event.accept(INSCRIBED_GATESTONE.get());
        }
    }
}
