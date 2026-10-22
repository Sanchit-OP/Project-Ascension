package com.ascension.core.internal.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * {@code ascension_core:strip}: a global loot modifier that takes every stack of an item tag out of what a
 * table rolled. It is how a chest on another planet stops handing out wheat: the rest of the table is left
 * exactly as it was, and the loot added on top by {@code neoforge:add_table} modifiers is not in the tag.
 * Its conditions decide where it applies, as with any loot modifier.
 */
public final class StripLoot extends LootModifier {

    public static final MapCodec<StripLoot> CODEC = RecordCodecBuilder.mapCodec(instance -> codecStart(instance)
            .and(TagKey.codec(Registries.ITEM).fieldOf("tag").forGetter(strip -> strip.tag))
            .apply(instance, StripLoot::new));

    private final TagKey<Item> tag;

    public StripLoot(LootItemCondition[] conditions, TagKey<Item> tag) {
        super(conditions);
        this.tag = tag;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        generatedLoot.removeIf(stack -> stack.is(tag));
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CoreLoot.STRIP.get();
    }
}
