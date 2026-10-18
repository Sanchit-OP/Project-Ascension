package com.ascension.atmosphere.internal;

import com.ascension.atmosphere.AscensionAtmosphere;
import com.ascension.atmosphere.internal.sealed.AirDispenserBlock;
import com.ascension.atmosphere.internal.sealed.AirDispenserBlockEntity;
import com.ascension.atmosphere.internal.sealed.OxygenEmitterBlock;
import com.ascension.atmosphere.internal.sealed.OxygenEmitterBlockEntity;
import com.ascension.atmosphere.internal.supply.OxygenRefillStationBlock;
import com.ascension.atmosphere.internal.supply.OxygenTankItem;
import com.ascension.atmosphere.internal.supply.TankFluidHandler;
import com.ascension.atmosphere.internal.supply.TankUpgradeRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Everything this module puts into a game registry: blocks, items, block entities and the one
 * data component that makes a tank a tank.
 *
 * <p>One class rather than four, because the registries are small and the interesting question
 * for a reader is "what does this mod add", not "which registry did it go in".
 */
public final class AtmosphereContent {

    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, AscensionAtmosphere.MOD_ID);
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, AscensionAtmosphere.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AscensionAtmosphere.MOD_ID);
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, AscensionAtmosphere.MOD_ID);

    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, AscensionAtmosphere.MOD_ID);
    private static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, AscensionAtmosphere.MOD_ID);
    private static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, AscensionAtmosphere.MOD_ID);

    /**
     * Air as a fluid (ADR-0026): what Create tanks and pipes carry and an Air Dispenser takes.
     * No bucket and no world block, so it exists only inside containers.
     */
    public static final Supplier<FluidType> AIR_TYPE = FLUID_TYPES.register(
            "air",
            () -> new FluidType(FluidType.Properties.create()
                    .density(1)
                    .viscosity(1)
                    .temperature(293)
                    .canSwim(false)
                    .canDrown(false)
                    .canPushEntity(false)
                    .canExtinguish(false)
                    .supportsBoating(false)));
    public static final Supplier<BaseFlowingFluid.Source> AIR =
            FLUIDS.register("air", () -> new BaseFlowingFluid.Source(airProperties()));
    public static final Supplier<BaseFlowingFluid.Flowing> AIR_FLOWING =
            FLUIDS.register("air_flowing", () -> new BaseFlowingFluid.Flowing(airProperties()));

    private static BaseFlowingFluid.Properties airProperties() {
        return new BaseFlowingFluid.Properties(AIR_TYPE, AIR, AIR_FLOWING);
    }

    /**
     * Units of oxygen stored in one item stack.
     *
     * <p>A data component rather than raw NBT, so the value survives the 1.20.5 component
     * migration cleanly, is diffed by vanilla for inventory sync, and is visible to anything
     * that wants to read a tank without knowing our classes.
     *
     * <p>{@code networkSynchronized} is what lets the client draw the durability-style bar and
     * the tooltip; without it the item would look empty on the client no matter how full it is.
     */
    public static final Supplier<DataComponentType<Integer>> OXYGEN_UNITS = COMPONENTS.register(
            "oxygen_units",
            () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    /**
     * Whether this tank's valve is open.
     *
     * <p>A closed tank is inert: it supplies nothing and drains nothing. That is what makes a
     * stowed tank a saved tank, and it is the answer to a tank quietly emptying itself because
     * its owner swam across a river.
     *
     * <p>Absent means closed, so a freshly crafted tank carries no component at all.
     */
    public static final Supplier<DataComponentType<Boolean>> TANK_OPEN = COMPONENTS.register(
            "tank_open",
            () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    public static final Supplier<Block> OXYGEN_EMITTER = BLOCKS.register(
            "oxygen_emitter",
            () -> new OxygenEmitterBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)));

    public static final Supplier<Item> OXYGEN_EMITTER_ITEM = ITEMS.register(
            "oxygen_emitter",
            () -> new BlockItem(OXYGEN_EMITTER.get(), new Item.Properties()));

    public static final Supplier<BlockEntityType<OxygenEmitterBlockEntity>> OXYGEN_EMITTER_ENTITY =
            BLOCK_ENTITIES.register(
                    "oxygen_emitter",
                    () -> BlockEntityType.Builder
                            .of(OxygenEmitterBlockEntity::new, OXYGEN_EMITTER.get())
                            .build(null));

    public static final Supplier<Block> AIR_DISPENSER = BLOCKS.register(
            "air_dispenser",
            () -> new AirDispenserBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)));

    public static final Supplier<Item> AIR_DISPENSER_ITEM = ITEMS.register(
            "air_dispenser",
            () -> new BlockItem(AIR_DISPENSER.get(), new Item.Properties()));

    public static final Supplier<BlockEntityType<AirDispenserBlockEntity>> AIR_DISPENSER_ENTITY =
            BLOCK_ENTITIES.register(
                    "air_dispenser",
                    () -> BlockEntityType.Builder
                            .of(AirDispenserBlockEntity::new, AIR_DISPENSER.get())
                            .build(null));

    public static final Supplier<Block> OXYGEN_REFILL_STATION = BLOCKS.register(
            "oxygen_refill_station",
            () -> new OxygenRefillStationBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)));

    public static final Supplier<Item> OXYGEN_REFILL_STATION_ITEM = ITEMS.register(
            "oxygen_refill_station",
            () -> new BlockItem(OXYGEN_REFILL_STATION.get(), new Item.Properties()));

    /**
     * The portable reserve.
     *
     * <p>Deliberately registered with <em>no</em> default oxygen component. Two reasons, one
     * mechanical and one about how it plays: a default would force {@code OXYGEN_UNITS.get()}
     * to resolve while items are still being registered, which is an ordering trap; and a
     * crafted tank arriving empty means the refill station is on the critical path the first
     * time you build one, rather than a block you never need to press.
     */
    public static final Supplier<Item> OXYGEN_TANK = ITEMS.register(
            "oxygen_tank",
            () -> new OxygenTankItem(new Item.Properties()));

    /** Level 2 (ADR-0026): a helium upgrade of the tank, twice the air, same valve and rules. */
    public static final Supplier<Item> OXYGEN_TANK_HELIUM = ITEMS.register(
            "oxygen_tank_helium",
            () -> new OxygenTankItem(new Item.Properties()));

    public static final Supplier<RecipeSerializer<TankUpgradeRecipe>> TANK_UPGRADE_SERIALIZER =
            RECIPE_SERIALIZERS.register("tank_upgrade", TankUpgradeRecipe.Serializer::new);

    private AtmosphereContent() {
    }

    public static void register(IEventBus modBus) {
        RECIPE_SERIALIZERS.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        COMPONENTS.register(modBus);
        FLUID_TYPES.register(modBus);
        FLUIDS.register(modBus);
        modBus.addListener(AtmosphereContent::addToCreativeTabs);
        modBus.addListener(AtmosphereContent::registerCapabilities);
    }

    /** Pipes and tanks reach the dispenser through the standard fluid capability. */
    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, AIR_DISPENSER_ENTITY.get(),
                (dispenser, side) -> dispenser.fluidHandler());
        // Any mod can fill a tank with air through the standard item capability (fill only).
        event.registerItem(Capabilities.FluidHandler.ITEM,
                (stack, context) -> new TankFluidHandler(stack), OXYGEN_TANK.get(), OXYGEN_TANK_HELIUM.get());
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            // The emitter and the refill station are legacy (ADR-0026): registered so old rooms
            // keep working, but no recipe and no creative tab entry.
            event.accept(AIR_DISPENSER_ITEM.get());
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            // Full, not empty. A crafted tank starts empty on purpose, but an empty one in the
            // creative menu just looks broken to anyone grabbing it to test with.
            event.accept(OxygenTankItem.filled());
            ItemStack helium = new ItemStack(OXYGEN_TANK_HELIUM.get());
            OxygenTankItem.setUnits(helium, OxygenTankItem.capacity(helium));
            event.accept(helium);
        }
    }
}
