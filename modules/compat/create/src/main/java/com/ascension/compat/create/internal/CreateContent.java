package com.ascension.compat.create.internal;

import com.ascension.compat.create.AscensionCompatCreate;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** What this jar registers: the Air Compressor and its block entity. */
public final class CreateContent {

    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, AscensionCompatCreate.MOD_ID);
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, AscensionCompatCreate.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AscensionCompatCreate.MOD_ID);

    public static final Supplier<Block> AIR_COMPRESSOR = BLOCKS.register(
            "air_compressor",
            () -> new AirCompressorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)));

    public static final Supplier<Item> AIR_COMPRESSOR_ITEM = ITEMS.register(
            "air_compressor",
            () -> new BlockItem(AIR_COMPRESSOR.get(), new Item.Properties()));

    public static final Supplier<BlockEntityType<AirCompressorBlockEntity>> AIR_COMPRESSOR_ENTITY =
            BLOCK_ENTITIES.register(
                    "air_compressor",
                    () -> BlockEntityType.Builder
                            .of((pos, state) -> new AirCompressorBlockEntity(
                                    CreateContent.AIR_COMPRESSOR_ENTITY.get(), pos, state),
                                    AIR_COMPRESSOR.get())
                            .build(null));

    private CreateContent() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(CreateContent::registerCapabilities);
        modBus.addListener(CreateContent::addToCreativeTabs);
    }

    /** Pumps and pipes take air from the buffer through the standard fluid capability. */
    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, AIR_COMPRESSOR_ENTITY.get(),
                (compressor, side) -> compressor.output());
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(AIR_COMPRESSOR_ITEM.get());
        }
    }
}
