package com.ascension.combat.internal.enchanting;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEnchantItemEvent;

/**
 * How many enchantments one item may carry, and a richer enchanting table so that many are reachable (Sanchit, 2026-10-22: the
 * list is growing, so the table must be able to give a build, and an item should have a ceiling).
 *
 * <p><b>The ceiling</b> is {@link #LIMIT} distinct enchantments on an ordinary item and {@link #LEGENDARY_LIMIT} on one of the
 * legendary items (item tag {@code ascension_combat:legendary}). The anvil refuses a combine that would add a new enchantment past
 * it (the output slot stays empty); raising the level of one the item already has is always allowed. Books are not limited, and
 * neither are commands or creative. An item that already has more (made with commands) keeps them.
 *
 * <p><b>The table</b> still gives what vanilla gives, then {@link #onEnchant} rolls extra enchantments the item can take and that
 * fit with what it has: a first with {@link #FIRST_EXTRA_CHANCE}, each further one less likely, never past the ceiling.
 *
 * <p>No state is kept (ADR-0007): everything is read from the stacks when the event fires.
 */
public final class EnchantmentLimits {

    public static final int LIMIT = 6;
    public static final int LEGENDARY_LIMIT = 7;

    /** Chance of the first extra enchantment at the table, and how much each further one loses. Placeholders to tune by playing. */
    public static final float FIRST_EXTRA_CHANCE = 0.75f;
    public static final float CHANCE_STEP = 0.15f;

    private static final TagKey<Item> LEGENDARY = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("ascension_combat", "legendary"));

    private EnchantmentLimits() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(EnchantmentLimits::onAnvil);
        gameBus.addListener(EnchantmentLimits::onEnchant);
    }

    /** The most enchantments this item may carry. */
    public static int limitFor(ItemStack stack) {
        return stack.is(LEGENDARY) ? LEGENDARY_LIMIT : LIMIT;
    }

    private static void onAnvil(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        if (left.isEmpty() || right.isEmpty() || left.is(Items.ENCHANTED_BOOK) || left.is(Items.BOOK)) {
            return;
        }
        ItemEnchantments incoming = EnchantmentHelper.getEnchantmentsForCrafting(right);
        if (incoming.isEmpty()) {
            return;
        }
        // Follow vanilla's merge: a new enchantment joins only if the item supports it and it fits with those already there.
        ItemEnchantments.Mutable merged = new ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(left));
        boolean addedNew = false;
        for (var entry : incoming.entrySet()) {
            Holder<Enchantment> enchantment = entry.getKey();
            if (merged.getLevel(enchantment) > 0 || !enchantment.value().isSupportedItem(left)) {
                continue;
            }
            boolean fits = true;
            for (Holder<Enchantment> present : merged.keySet()) {
                if (!Enchantment.areCompatible(present, enchantment)) {
                    fits = false;
                    break;
                }
            }
            if (fits) {
                merged.set(enchantment, entry.getIntValue());
                addedNew = true;
            }
        }
        if (addedNew && merged.keySet().size() > limitFor(left)) {
            event.setCanceled(true);
        }
    }

    private static void onEnchant(PlayerEnchantItemEvent event) {
        ItemStack stack = event.getEnchantedItem();
        if (stack.isEmpty() || stack.is(Items.ENCHANTED_BOOK)) {
            return;
        }
        var level = event.getEntity().level();
        RandomSource random = level.random;
        int limit = limitFor(stack);
        float chance = FIRST_EXTRA_CHANCE;
        while (chance > 0.0f && stack.getEnchantments().size() < limit && random.nextFloat() < chance) {
            List<Holder<Enchantment>> candidates = new ArrayList<>();
            level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(EnchantmentTags.IN_ENCHANTING_TABLE)
                    .ifPresent(set -> set.forEach(candidate -> {
                        if (stack.getEnchantments().getLevel(candidate) == 0 && candidate.value().isSupportedItem(stack)
                                && fitsWithAll(stack, candidate)) {
                            candidates.add(candidate);
                        }
                    }));
            if (candidates.isEmpty()) {
                break;
            }
            Holder<Enchantment> pick = candidates.get(random.nextInt(candidates.size()));
            int max = pick.value().getMaxLevel();
            // Mostly the middle of its range, sometimes the top: the table is generous, not perfect.
            int rolled = Math.max(1, Math.min(max, Math.round(max * (0.5f + random.nextFloat() * 0.5f))));
            stack.enchant(pick, rolled);
            chance -= CHANCE_STEP;
        }
    }

    private static boolean fitsWithAll(ItemStack stack, Holder<Enchantment> candidate) {
        for (Holder<Enchantment> present : stack.getEnchantments().keySet()) {
            if (!Enchantment.areCompatible(present, candidate)) {
                return false;
            }
        }
        return true;
    }
}
