package com.ascension.combat.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Where the off-hand relics (Zonia's Hourglass, Sterak's Harness) count as worn.
 *
 * <p>The off hand always counts. A mod that adds another place to wear things (a curio slot) registers a
 * {@link Provider} once at startup and hands over the stacks it finds; the relics then work from there exactly as
 * they do in the off hand, with no change to this module and no dependency on that mod (ADR-0003 rule 3).
 *
 * <p>Holds providers only, never an entity or a stack (ADR-0007 rule 1). Providers are called on the logical
 * server, once per check, so keep them cheap.
 */
public final class WornStacks {

    /** Gives every stack this provider considers worn by {@code entity}. */
    @FunctionalInterface
    public interface Provider {
        void collect(LivingEntity entity, Consumer<ItemStack> sink);
    }

    private static final List<Provider> PROVIDERS = new CopyOnWriteArrayList<>();

    private WornStacks() {
    }

    public static void register(Provider provider) {
        PROVIDERS.add(provider);
    }

    /** True if {@code item} is in the off hand or in any registered provider's worn stacks. */
    public static boolean isWorn(LivingEntity entity, Item item) {
        if (entity.getItemBySlot(EquipmentSlot.OFFHAND).is(item)) {
            return true;
        }
        if (PROVIDERS.isEmpty()) {
            return false;
        }
        boolean[] found = {false};
        for (Provider provider : PROVIDERS) {
            provider.collect(entity, stack -> {
                if (stack.is(item)) {
                    found[0] = true;
                }
            });
            if (found[0]) {
                return true;
            }
        }
        return false;
    }
}
