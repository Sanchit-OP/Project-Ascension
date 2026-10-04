package com.ascension.compat.cataclysm.mixin;

import com.ascension.compat.cataclysm.internal.MoonRockRuleProcessor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds this project's own block-swap processor ({@link MoonRockRuleProcessor}: End stone &rarr;
 * the Moon's own rock) to every Ruined Citadel piece's placement settings. The structure and
 * structure_set JSON have no hook for this at all &mdash; {@code Piece.makeSettings} hardcodes
 * its own two processors in Java, with nothing data-driven reaching it. See ADR-0013.
 */
@Mixin(targets = "com.github.L_Ender.cataclysm.structures.RuinedCitadelStructure$Piece")
public abstract class RuinedCitadelPieceMixin {

    @Inject(method = "makeSettings", at = @At("RETURN"), cancellable = true)
    private static void ascension$reskinToMoonRock(
            Rotation rotation, CallbackInfoReturnable<StructurePlaceSettings> cir) {
        cir.setReturnValue(cir.getReturnValue().addProcessor(MoonRockRuleProcessor.INSTANCE));
    }
}
