package com.ascension.combat.internal.enchanting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/**
 * What a dying player's Soulbound items are waiting as, between death and respawn: each item with the
 * inventory slot it came from. Held as a saved, copied-on-death attachment, so it survives a player
 * closing the game at the death screen, and holds item stacks only: no player, level or entity
 * (ADR-0007 rule 1).
 */
public record KeptItems(List<Kept> items) {

    public record Kept(int slot, ItemStack stack) {

        public static final Codec<Kept> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("slot").forGetter(Kept::slot),
                ItemStack.CODEC.fieldOf("stack").forGetter(Kept::stack)).apply(i, Kept::new));
    }

    public static final Codec<KeptItems> CODEC = RecordCodecBuilder.create(i -> i.group(
            Kept.CODEC.listOf().fieldOf("items").forGetter(KeptItems::items)).apply(i, KeptItems::new));

    public KeptItems() {
        this(List.of());
    }

    public KeptItems with(Kept kept) {
        List<Kept> all = new ArrayList<>(items);
        all.add(kept);
        return new KeptItems(List.copyOf(all));
    }
}
