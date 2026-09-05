package com.ascension.atmosphere.internal;

import com.ascension.atmosphere.AscensionAtmosphere;
import com.ascension.atmosphere.internal.sealed.OxygenEmitterBlock;
import com.ascension.atmosphere.internal.sealed.OxygenEmitterBlockEntity;
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
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Blocks and items owned by this module. */
public final class AtmosphereBlocks {

    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, AscensionAtmosphere.MOD_ID);
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, AscensionAtmosphere.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AscensionAtmosphere.MOD_ID);

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

    private AtmosphereBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(AtmosphereBlocks::addToCreativeTab);
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(OXYGEN_EMITTER_ITEM.get());
        }
    }
}
