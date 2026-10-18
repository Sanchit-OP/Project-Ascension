package com.ascension.atmosphere.internal.supply;

import com.ascension.atmosphere.internal.AtmosphereContent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/**
 * Lets any mod fill an oxygen tank with air (ADR-0026), through the standard item fluid capability.
 *
 * <p>1 unit of oxygen = 1 mB. Fill only: air leaves a tank through its valve, never through a pipe,
 * so the valve and the two-tanks rule (ADR-0009) stay the only way out. It edits the stack in place
 * and keeps no reference to anything after the call.
 */
public final class TankFluidHandler implements IFluidHandlerItem {

    private final ItemStack container;

    public TankFluidHandler(ItemStack container) {
        this.container = container;
    }

    @Override
    public ItemStack getContainer() {
        return container;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        int units = OxygenTankItem.units(container);
        return units <= 0 ? FluidStack.EMPTY : new FluidStack(AtmosphereContent.AIR.get(), units);
    }

    @Override
    public int getTankCapacity(int tank) {
        return OxygenTankItem.capacity(container);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return stack.getFluid().isSame(AtmosphereContent.AIR.get());
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (!isFluidValid(0, resource)) {
            return 0;
        }
        int units = OxygenTankItem.units(container);
        int moved = Math.min(resource.getAmount(), OxygenTankItem.capacity(container) - units);
        if (moved > 0 && action.execute()) {
            OxygenTankItem.setUnits(container, units + moved);
        }
        return Math.max(moved, 0);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return FluidStack.EMPTY;
    }
}
