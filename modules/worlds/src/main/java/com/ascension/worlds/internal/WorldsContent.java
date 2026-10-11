package com.ascension.worlds.internal;

import com.ascension.worlds.AscensionWorlds;
import com.ascension.worlds.internal.terrain.CraterConfiguration;
import com.ascension.worlds.internal.terrain.CraterFeature;
import com.ascension.worlds.internal.terrain.CraterPiece;
import com.ascension.worlds.internal.terrain.CraterStructure;
import com.ascension.worlds.internal.terrain.RockDebrisConfiguration;
import com.ascension.worlds.internal.terrain.RockDebrisFeature;
import com.ascension.worlds.internal.worldgen.DryGroundStructure;
import com.ascension.worlds.internal.worldgen.FixedPositionStructurePlacement;
import com.ascension.worlds.internal.worldgen.GatewayFramePiece;
import com.ascension.worlds.internal.worldgen.GatewayFrameStructure;
import com.ascension.worlds.internal.worldgen.KeepClearRandomSpreadPlacement;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
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
    private static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, AscensionWorlds.MOD_ID);
    private static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, AscensionWorlds.MOD_ID);
    private static final DeferredRegister<StructurePieceType> STRUCTURE_PIECE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, AscensionWorlds.MOD_ID);
    private static final DeferredRegister<StructurePlacementType<?>> STRUCTURE_PLACEMENT_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, AscensionWorlds.MOD_ID);
    private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, AscensionWorlds.MOD_ID);
    private static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, AscensionWorlds.MOD_ID);

    /**
     * The dust-storm particle. Registered here (common code) because {@link ParticleType} itself
     * is a plain registry entry with no rendering behaviour attached &mdash; the actual look
     * ({@code DustStormParticle}) is registered separately, client-only, in
     * {@code SpaceClientSetup}, the same split every other particle type in the game uses.
     */
    public static final Supplier<SimpleParticleType> DUST_STORM_PARTICLE =
            PARTICLE_TYPES.register("dust_storm", () -> new SimpleParticleType(false));

    /**
     * Loose surface dust. The one block every airless world shares, regardless of biome &mdash;
     * real regolith settles over everything alike, so the highlands/mare visual split comes from
     * the rock underneath rather than a second dust block.
     */
    public static final Supplier<Block> MOON_REGOLITH = BLOCKS.register(
            "moon_regolith",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(0.5f)
                    .sound(SoundType.GRAVEL)));

    public static final Supplier<Item> MOON_REGOLITH_ITEM = ITEMS.register(
            "moon_regolith", () -> new BlockItem(MOON_REGOLITH.get(), new Item.Properties()));

    /** Highlands rock: pale, ancient, heavily cratered. Real anorthosite is what makes it pale. */
    public static final Supplier<Block> ANORTHOSITE = BLOCKS.register(
            "anorthosite",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.QUARTZ)
                    .requiresCorrectToolForDrops()
                    .strength(1.5f, 6.0f)
                    .sound(SoundType.STONE)));

    public static final Supplier<Item> ANORTHOSITE_ITEM = ITEMS.register(
            "anorthosite", () -> new BlockItem(ANORTHOSITE.get(), new Item.Properties()));

    /** Mare rock: dark basalt, the "seas" visible from Earth with the naked eye. */
    public static final Supplier<Block> MOON_BASALT = BLOCKS.register(
            "moon_basalt",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .requiresCorrectToolForDrops()
                    .strength(1.5f, 6.0f)
                    .sound(SoundType.BASALT)));

    public static final Supplier<Item> MOON_BASALT_ITEM = ITEMS.register(
            "moon_basalt", () -> new BlockItem(MOON_BASALT.get(), new Item.Properties()));

    /**
     * The crater shape, registered once. See {@link CraterFeature}'s own javadoc for why this is
     * the one piece of Java the Moon's terrain needs.
     */
    public static final Supplier<Feature<CraterConfiguration>> CRATER = FEATURES.register(
            "crater", () -> new CraterFeature(CraterConfiguration.CODEC));

    /**
     * The large crater's shape, as a {@link net.minecraft.world.level.levelgen.structure.Structure}
     * rather than a {@link Feature} &mdash; see {@link CraterStructure}'s javadoc for why large
     * craters specifically need this and small ones don't.
     */
    public static final Supplier<StructureType<CraterStructure>> CRATER_STRUCTURE_TYPE = STRUCTURE_TYPES.register(
            "crater", () -> () -> CraterStructure.CODEC);

    public static final Supplier<StructurePieceType> CRATER_PIECE = STRUCTURE_PIECE_TYPES.register(
            "crater", () -> (StructurePieceType.ContextlessType) CraterPiece::new);

    /**
     * Pins a {@code structure_set} to one exact chunk, every world, every seed. See
     * {@link FixedPositionStructurePlacement}'s javadoc &mdash; this is the reusable half of
     * sourcing a boss (or any other prebuilt structure) from a third-party mod: every
     * {@code ascension-compat-<bossmod>} jar references this same placement type by id
     * ({@code ascension_worlds:fixed_position}) rather than each writing its own.
     */
    public static final Supplier<StructurePlacementType<FixedPositionStructurePlacement>> FIXED_POSITION_PLACEMENT_TYPE =
            STRUCTURE_PLACEMENT_TYPES.register("fixed_position", () -> () -> FixedPositionStructurePlacement.CODEC);

    /**
     * {@code random_spread} that keeps clear of several other structure sets, where vanilla's
     * {@code exclusion_zone} allows one. The Moon's large craters use it to stay off the Archive and
     * the gateway ruin. See {@link KeepClearRandomSpreadPlacement}.
     */
    public static final Supplier<StructurePlacementType<KeepClearRandomSpreadPlacement>> KEEP_CLEAR_RANDOM_SPREAD_PLACEMENT_TYPE =
            STRUCTURE_PLACEMENT_TYPES.register("random_spread_keep_clear", () -> () -> KeepClearRandomSpreadPlacement.CODEC);

    /**
     * Loose rock scattered through the space dimension's vacuum. See
     * {@link RockDebrisFeature}'s javadoc for why this is a cluster of small pieces rather than
     * one asteroid, and {@code worldgen/placed_feature/rock_debris.json} for how rare it actually
     * is &mdash; the space dimension is still overwhelmingly empty.
     */
    public static final Supplier<Feature<RockDebrisConfiguration>> ROCK_DEBRIS = FEATURES.register(
            "rock_debris", () -> new RockDebrisFeature(RockDebrisConfiguration.CODEC));

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

    /**
     * What wakes Earth's player-built gateway frame (Sanchit's call, 2026-10-05). Dropped by the
     * Warden via a NeoForge {@code add_table} loot modifier
     * ({@code data/ascension_worlds/loot_modifiers/warden_tendril.json}), so vanilla's own Warden
     * loot table is added to, never replaced. Stacks: a dropped tendril is a resource, not a
     * one-off story item like {@link #GATEWAY_KEY}.
     */
    public static final Supplier<Item> WARDEN_TENDRIL = ITEMS.register(
            "warden_tendril", () -> new Item(new Item.Properties()));

    /**
     * Which planet's gateway a {@link #GATEWAY_KEY} unlocks &mdash; the surface dimension's own
     * {@link ResourceLocation}, the same identity {@link PlanetArrivalMemory} already keys its own
     * per-player state by. One item id for every planet rather than one item per planet: per
     * ADR-0007 rule 10, adding a planet must not mean adding a class, and it should not mean
     * adding an item either.
     */
    public static final Supplier<DataComponentType<ResourceLocation>> GATEWAY_KEY_FOR_PLANET = DATA_COMPONENT_TYPES.register(
            "gateway_key_for_planet",
            () -> DataComponentType.<ResourceLocation>builder()
                    .persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ResourceLocation.STREAM_CODEC)
                    .build());

    /**
     * What a mandatory boss drops instead of whatever reward its source mod originally intended
     * &mdash; see {@code plans/m3-vertical-slice.md}'s M3.1/M3.2. Consumed by that planet's
     * gateway repair; {@link #GATEWAY_KEY_FOR_PLANET} is what stops a Moon key from repairing a
     * later planet's gateway.
     */
    public static final Supplier<Item> GATEWAY_KEY = ITEMS.register(
            "gateway_key", () -> new Item(new Item.Properties().stacksTo(1)));

    /**
     * The block players right-click in a gateway frame &mdash; see {@link GatewayKeystoneBlock} and
     * {@code docs/technical/gateway-frame-format.md}. Obsidian-hard and breakable: Earth's players
     * craft and place their own (recipe from the gateway design session).
     */
    public static final Supplier<Block> GATEWAY_KEYSTONE = BLOCKS.register(
            "gateway_keystone",
            () -> new GatewayKeystoneBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops()
                    .strength(50.0f, 1200.0f)
                    .lightLevel(state -> state.getValue(GatewayKeystoneBlock.ACTIVE) ? 12 : 0)));

    public static final Supplier<Item> GATEWAY_KEYSTONE_ITEM = ITEMS.register(
            "gateway_keystone", () -> new BlockItem(GATEWAY_KEYSTONE.get(), new Item.Properties()));

    /**
     * A lit gateway's walk-through opening &mdash; see {@link GatewayFieldBlock}. Nether-portal
     * properties: no collision, unbreakable by hand, no item, glows.
     */
    public static final Supplier<Block> GATEWAY_FIELD = BLOCKS.register(
            "gateway_field",
            () -> new GatewayFieldBlock(BlockBehaviour.Properties.of()
                    .noCollission()
                    .noOcclusion()
                    .noLootTable()
                    .strength(-1.0f)
                    .sound(SoundType.GLASS)
                    .lightLevel(state -> 11)
                    .pushReaction(PushReaction.BLOCK)));

    /** Pre-generated gateways ({@code worldgen/structure/gateway_frame_*.json}) &mdash; see {@link GatewayFrameStructure}. */
    public static final Supplier<StructureType<GatewayFrameStructure>> GATEWAY_FRAME_STRUCTURE_TYPE =
            STRUCTURE_TYPES.register("gateway_frame", () -> () -> GatewayFrameStructure.CODEC);

    /**
     * Any structure, refused where its start would stand on a lava sea or a lake. The Nether's
     * colony ruins and Sealers' carving use it. See {@link DryGroundStructure}.
     */
    public static final Supplier<StructureType<DryGroundStructure>> DRY_GROUND_STRUCTURE_TYPE =
            STRUCTURE_TYPES.register("on_dry_ground", () -> () -> DryGroundStructure.CODEC);

    public static final Supplier<StructurePieceType> GATEWAY_FRAME_PIECE = STRUCTURE_PIECE_TYPES.register(
            "gateway_frame", () -> (StructurePieceType.ContextlessType) GatewayFramePiece::new);

    private WorldsContent() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        FEATURES.register(modBus);
        STRUCTURE_TYPES.register(modBus);
        STRUCTURE_PIECE_TYPES.register(modBus);
        STRUCTURE_PLACEMENT_TYPES.register(modBus);
        DATA_COMPONENT_TYPES.register(modBus);
        PARTICLE_TYPES.register(modBus);
        modBus.addListener(WorldsContent::addToCreativeTabs);
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(LUNAR_HELIUM_ORE_ITEM.get());
            event.accept(TITANIUM_ORE_ITEM.get());
            event.accept(MOON_REGOLITH_ITEM.get());
            event.accept(ANORTHOSITE_ITEM.get());
            event.accept(MOON_BASALT_ITEM.get());
            event.accept(GATEWAY_KEYSTONE_ITEM.get());
        }
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(RAW_HELIUM.get());
            event.accept(RAW_TITANIUM.get());
            event.accept(WARDEN_TENDRIL.get());
            event.accept(GATEWAY_KEY.get());
        }
    }
}
