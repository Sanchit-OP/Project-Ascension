package com.ascension.compat.create.internal;

import com.ascension.atmosphere.api.AirFluid;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Turns rotation into air (see {@link AirProduction}). Arithmetic every tick, no scan, no packet:
 * the buffer is shown to goggles by syncing at most once a second, and only when it changed.
 */
public final class AirCompressorBlockEntity extends KineticBlockEntity {

    private static final int SYNC_INTERVAL = 20;

    private final FluidTank buffer = new FluidTank(AirProduction.BUFFER_MB,
            stack -> stack.getFluid().isSame(AirFluid.get()));

    /** What pumps and pipes see: air can be taken out, never put in. */
    private final IFluidHandler output = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return buffer.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return buffer.getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return buffer.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return buffer.drain(maxDrain, action);
        }
    };

    private float owed;       // fractional mB not yet made
    private int sinceSync;
    private int lastSynced = -1;

    public AirCompressorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public IFluidHandler output() {
        return output;
    }

    @Override
    public float calculateStressApplied() {
        this.lastStressApplied = AirProduction.STRESS_PER_RPM;
        return AirProduction.STRESS_PER_RPM;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        owed += AirProduction.mbPerTick(getSpeed());
        int whole = (int) owed;
        if (whole > 0) {
            owed -= whole;
            buffer.fill(new FluidStack(AirFluid.get(), whole), IFluidHandler.FluidAction.EXECUTE);
        }
        if (++sinceSync >= SYNC_INTERVAL) {
            sinceSync = 0;
            if (buffer.getFluidAmount() != lastSynced) {
                lastSynced = buffer.getFluidAmount();
                setChanged();
                sendData();
            }
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        tooltip.add(Component.translatable("block.ascension_compat_create.air_compressor.rate",
                String.format("%.0f", AirProduction.mbPerSecond(getSpeed()))));
        return containedFluidTooltip(tooltip, isPlayerSneaking, output) || added;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("air", buffer.writeToNBT(registries, new CompoundTag()));
        tag.putFloat("owed", owed);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        buffer.readFromNBT(registries, tag.getCompound("air"));
        owed = tag.getFloat("owed");
    }
}
