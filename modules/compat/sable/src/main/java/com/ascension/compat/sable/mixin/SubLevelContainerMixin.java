package com.ascension.compat.sable.mixin;

import com.ascension.compat.sable.internal.PlotSlots;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import net.minecraft.server.level.ServerLevel;
import org.joml.Vector2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives every new ship on the server a storage slot free in every dimension, not just its own, so
 * a ship can always be carried to another world (ADR-0016, {@link PlotSlots}). Server levels only:
 * a client's slots come from the server.
 */
@Mixin(SubLevelContainer.class)
public abstract class SubLevelContainerMixin {

    @Inject(method = "getFirstEmptyPlot", at = @At("HEAD"), cancellable = true)
    private void ascension$slotFreeEverywhere(CallbackInfoReturnable<Vector2i> cir) {
        SubLevelContainer self = (SubLevelContainer) (Object) this;
        if (self.getLevel() instanceof ServerLevel level) {
            cir.setReturnValue(PlotSlots.firstFreeEverywhere(self, level.getServer()));
        }
    }
}
