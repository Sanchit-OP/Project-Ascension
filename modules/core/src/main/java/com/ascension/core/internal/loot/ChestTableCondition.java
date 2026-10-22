package com.ascension.core.internal.loot;

import com.mojang.serialization.MapCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

/**
 * {@code ascension_core:chest_table}: true when the loot table being rolled is a chest's, meaning its id
 * has a path under {@code chests/}, which is where vanilla, Cataclysm, Lootr's chests and ours all keep
 * theirs. A block's drops, a mob's drops and a fishing catch have other paths, so a loot modifier
 * carrying this condition leaves them alone.
 *
 * <p>It exists because the game offers no way to say "every chest table" in data: {@code neoforge:loot_table_id}
 * names one table exactly, and naming them all would be a list nobody keeps current (docs/progression/loot.md).
 * Lootr fills its chests through {@code LootTable.fill}, so a modifier with this condition applies to
 * them as it does to a plain chest.
 */
public final class ChestTableCondition implements LootItemCondition {

    public static final ChestTableCondition INSTANCE = new ChestTableCondition();
    public static final MapCodec<ChestTableCondition> CODEC = MapCodec.unit(INSTANCE);

    static final String PREFIX = "chests/";

    private ChestTableCondition() {
    }

    /** Whether {@code table} is a chest loot table. */
    public static boolean isChestTable(ResourceLocation table) {
        return table.getPath().startsWith(PREFIX);
    }

    @Override
    public LootItemConditionType getType() {
        return CoreLoot.CHEST_TABLE.get();
    }

    @Override
    public boolean test(LootContext context) {
        return isChestTable(context.getQueriedLootTableId());
    }
}
