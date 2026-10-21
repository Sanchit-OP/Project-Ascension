package com.ascension.compat.curios.internal;

import com.ascension.combat.api.WornStacks;
import java.util.function.Consumer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

/** Whatever sits in {@code ascension_relic} counts as worn, exactly as the off hand does. */
public final class RelicSlot implements WornStacks.Provider {

    public static final String SLOT = "ascension_relic";

    @Override
    public void collect(LivingEntity entity, Consumer<ItemStack> sink) {
        CuriosApi.getCuriosInventory(entity).flatMap(inv -> inv.getStacksHandler(SLOT)).ifPresent(handler -> {
            var stacks = handler.getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack stack = stacks.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    sink.accept(stack);
                }
            }
        });
    }
}
