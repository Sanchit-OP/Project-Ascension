package com.ascension.compat.cataclysm.mixin;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Fixes {@code RuinedCitadelStructure.generatePieces} hardcoding {@code y = 53} for every piece,
 * independent of the real terrain height vanilla's own {@code onTopOfChunkCenter} helper already
 * computes one line above it in {@code findGenerationPoint} &mdash; confirmed by decompiling the
 * class: that computed height is used only for the structure's reported generation-stub position,
 * never passed into {@code generatePieces} at all. Acceptable by coincidence in the End; on the
 * Moon's real terrain it placed the whole citadel floating above the ground. See ADR-0013.
 *
 * <p>Same surface query {@code CraterStructure} already uses for the same reason.
 */
@Mixin(targets = "com.github.L_Ender.cataclysm.structures.RuinedCitadelStructure")
public abstract class RuinedCitadelStructureMixin {

    @ModifyConstant(method = "generatePieces", constant = @Constant(intValue = 53))
    private static int ascension$placeOnRealSurface(
            int original, StructurePiecesBuilder builder, Structure.GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        return context.chunkGenerator().getFirstOccupiedHeight(
                chunkPos.getMinBlockX(),
                chunkPos.getMinBlockZ(),
                Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(),
                context.randomState());
    }
}
