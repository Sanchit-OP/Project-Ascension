package com.ascension.atmosphere.internal.sealed;

import com.ascension.atmosphere.internal.AtmosphereAttachments;
import com.ascension.atmosphere.internal.AtmosphereContent;
import com.ascension.atmosphere.internal.AtmosphereTuning;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * The dispenser's air buffer and its once-a-second check.
 *
 * <p>Stores one fluid, air, in mB. Every {@link AtmosphereTuning#DISPENSER_CHECK_TICKS} it spends
 * the room's cost (see {@link AirDemand}) and tells the level index whether the room is supplied;
 * out of air, the whole room stops being held, and it recovers when air returns. It reads the
 * room's size from the index and never scans blocks (ADR-0007).
 */
public final class AirDispenserBlockEntity extends BlockEntity {

    private final FluidTank tank = new FluidTank(AtmosphereTuning.DISPENSER_CAPACITY_MB,
            stack -> stack.getFluid().isSame(AtmosphereContent.AIR.get())) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    /** What pipes see: air can be put in, never taken out. */
    private final IFluidHandler input = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int index) {
            return tank.getFluid();
        }

        @Override
        public int getTankCapacity(int index) {
            return tank.getCapacity();
        }

        @Override
        public boolean isFluidValid(int index, FluidStack stack) {
            return tank.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return tank.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    };

    private int ticks;

    public AirDispenserBlockEntity(BlockPos pos, BlockState state) {
        super(AtmosphereContent.AIR_DISPENSER_ENTITY.get(), pos, state);
    }

    public IFluidHandler fluidHandler() {
        return input;
    }

    public int airMb() {
        return tank.getFluidAmount();
    }

    public int capacityMb() {
        return tank.getCapacity();
    }

    /** Put in the level index; used on placement and chunk load. Returns the room it found. */
    SealedVolume.Result register(ServerLevel level) {
        SealedVolumeIndex index = level.getData(AtmosphereAttachments.SEALED_VOLUMES);
        SealedVolume.Result result = index.addEmitter(level, getBlockPos());
        index.setSupplied(getBlockPos(), airMb() >= AirDemand.mbPerSecond(result.size()));
        return result;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            register(serverLevel);
        }
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.getData(AtmosphereAttachments.SEALED_VOLUMES).removeEmitter(getBlockPos());
        }
        super.setRemoved();
    }

    void serverTick() {
        if (++ticks < AtmosphereTuning.DISPENSER_CHECK_TICKS || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        ticks = 0;
        SealedVolumeIndex index = serverLevel.getData(AtmosphereAttachments.SEALED_VOLUMES);
        int need = AirDemand.mbPerSecond(index.volumeSize(getBlockPos()));
        if (need == 0) {
            return;                 // not a sealed room: nothing to hold, nothing to spend
        }
        boolean supplied = tank.getFluidAmount() >= need;
        if (supplied) {
            tank.drain(need, IFluidHandler.FluidAction.EXECUTE);
        }
        if (index.setSupplied(getBlockPos(), supplied)) {
            SealedVolumeEvents.notifyNear(serverLevel, List.of(getBlockPos()), supplied
                    ? Component.literal("Pressure restored").withStyle(ChatFormatting.GREEN)
                    : Component.literal("Pressure lost — the dispenser is out of air")
                            .withStyle(ChatFormatting.RED));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("air", tank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(registries, tag.getCompound("air"));
    }
}
