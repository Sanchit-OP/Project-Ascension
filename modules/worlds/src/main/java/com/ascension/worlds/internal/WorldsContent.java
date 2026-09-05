package com.ascension.worlds.internal;

import com.ascension.worlds.AscensionWorlds;
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
import java.util.function.Supplier;

/**
 * Everything this module puts into a game registry: the blocks and items a planet's worldgen
 * actually places.
 *
 * <p>Does not contradict {@link AscensionWorlds}'s "no per-planet Java" claim &mdash; that claim
 * is about <em>worlds</em>, not about resources. A block registered here is reusable content, the
 * same way {@code OxygenTankItem} is one class serving every dimension that installs
 * {@code ascension-atmosphere}; nothing about adding a resource requires a new class per planet.
 * What ties a resource to one place is worldgen JSON &mdash; see
 * {@code data/ascension_worlds/worldgen/} and the biome files that do or do not reference it.
 *
 * <p>One class rather than several, mirroring {@code AtmosphereContent}: the registries are
 * small, and the interesting question for a reader is "what does this mod add".
 */
public final class WorldsContent {

    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, AscensionWorlds.MOD_ID);
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, AscensionWorlds.MOD_ID);

    /**
     * The Moon's first exclusive resource: a fusion-fuel ore, mined in place rather than
     * refined from vacuum-exposed regolith. Nothing consumes it yet &mdash; {@code progression}
     * does not exist &mdash; and that is fine. It is content, not a datapack-registry field;
     * {@code worlds-api.md}'s "no field lands here until something reads it" rule is about the
     * {@link com.ascension.worlds.api.Planet} schema, not about whether a resource may exist
     * before something consumes it.
     *
     * <p>One ore block regardless of host stone. Vanilla splits {@code iron_ore} from
     * {@code deepslate_iron_ore} because Earth's two stone bands look different; the Moon does
     * not make that visual distinction, so one block replaces anything matched by
     * {@code #minecraft:stone_ore_replaceables} or {@code #minecraft:deepslate_ore_replaceables}
     * &mdash; see {@code worldgen/configured_feature/lunar_helium_ore.json}.
     */
    public static final Supplier<Block> LUNAR_HELIUM_ORE = BLOCKS.register(
            "lunar_helium_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .requiresCorrectToolForDrops()
                    .strength(4.5f, 3.0f)
                    .sound(SoundType.DEEPSLATE)));

    public static final Supplier<Item> LUNAR_HELIUM_ORE_ITEM = ITEMS.register(
            "lunar_helium_ore",
            () -> new BlockItem(LUNAR_HELIUM_ORE.get(), new Item.Properties()));

    /** What the ore actually drops. Unprocessed, the same convention as vanilla raw materials. */
    public static final Supplier<Item> RAW_HELIUM = ITEMS.register(
            "raw_helium", () -> new Item(new Item.Properties()));

    /**
     * Earth's counterpart to the Moon's ore: the hull metal, where the Moon supplies the fuel.
     * Real titanium is exactly what it sounds like &mdash; strong for its weight and used in
     * actual aerospace airframes &mdash; so the pairing is not an arbitrary game-resource
     * invention.
     *
     * <p>Unlike the Moon, Earth is vanilla's own dimension: this block is placed via a NeoForge
     * biome modifier rather than a biome file we own. See {@code docs/technical/datapacks.md}
     * for why that is the correct tool here and the wrong one for the Moon.
     */
    public static final Supplier<Block> TITANIUM_ORE = BLOCKS.register(
            "titanium_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .requiresCorrectToolForDrops()
                    .strength(4.5f, 3.0f)
                    .sound(SoundType.STONE)));

    public static final Supplier<Item> TITANIUM_ORE_ITEM = ITEMS.register(
            "titanium_ore",
            () -> new BlockItem(TITANIUM_ORE.get(), new Item.Properties()));

    public static final Supplier<Item> RAW_TITANIUM = ITEMS.register(
            "raw_titanium", () -> new Item(new Item.Properties()));

    private WorldsContent() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(WorldsContent::addToCreativeTabs);
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(LUNAR_HELIUM_ORE_ITEM.get());
            event.accept(TITANIUM_ORE_ITEM.get());
        }
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(RAW_HELIUM.get());
            event.accept(RAW_TITANIUM.get());
        }
    }
}
