package com.ascension.core.internal.loot;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The two loot types every Ascension module's loot data shares: the {@code chest_table} condition and the
 * {@code strip} modifier. They are vocabulary, not behaviour: with no loot modifier file naming them they do
 * nothing, so core still does nothing observable on its own (ADR-0029).
 */
public final class CoreLoot {

    private static final DeferredRegister<LootItemConditionType> CONDITIONS =
            DeferredRegister.create(Registries.LOOT_CONDITION_TYPE, "ascension_core");
    private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, "ascension_core");

    public static final DeferredHolder<LootItemConditionType, LootItemConditionType> CHEST_TABLE =
            CONDITIONS.register("chest_table", () -> new LootItemConditionType(ChestTableCondition.CODEC));
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<StripLoot>> STRIP =
            MODIFIERS.register("strip", () -> StripLoot.CODEC);

    private CoreLoot() {
    }

    public static void register(IEventBus modBus) {
        CONDITIONS.register(modBus);
        MODIFIERS.register(modBus);
    }
}
