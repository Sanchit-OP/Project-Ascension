package com.ascension.compat.curios.internal;

import com.ascension.atmosphere.api.OxygenSource;
import com.ascension.atmosphere.api.OxygenSourceCollector;
import com.ascension.atmosphere.api.OxygenTanks;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * The tank worn in {@code ascension_tank} is a source with no valve to open: wearing it is opening it. The slot holds
 * one, so one worn tank is open at a time. Its charge lives on the stack, so there is nothing here to save or tear down.
 */
public final class TankSlotCollector implements OxygenSourceCollector {

    public static final String SLOT = "ascension_tank";

    @Override
    public void collect(ServerPlayer player, Consumer<OxygenSource> sink) {
        CuriosApi.getCuriosInventory(player).flatMap(inv -> inv.getStacksHandler(SLOT)).ifPresent(handler -> {
            var stacks = handler.getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack stack = stacks.getStackInSlot(i);
                if (OxygenTanks.isTank(stack)) {
                    sink.accept(OxygenTanks.asSource(stack));
                }
            }
        });
    }
}
