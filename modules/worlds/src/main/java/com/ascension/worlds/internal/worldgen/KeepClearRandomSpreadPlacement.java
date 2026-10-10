package com.ascension.worlds.internal.worldgen;

import com.ascension.worlds.internal.WorldsContent;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Vec3i;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

/**
 * Vanilla's {@code random_spread}, kept clear of any number of other structure sets.
 *
 * <p>Vanilla's own {@code exclusion_zone} names exactly one other set, and the Moon's large craters
 * have to stay away from two: the Lunar Archive and the gateway ruin. A crater that lands on either
 * cannot carve inside its piece boxes ({@code CraterPiece}), so it hollows the ground around them and
 * leaves them standing in the bowl as square-cut blocks. Each entry of {@code keep_clear_of} is
 * vanilla's own exclusion-zone shape ({@code other_set}, {@code chunk_count}), checked the same way
 * vanilla checks its one, and the vanilla field still works alongside it.
 *
 * <p>A subclass rather than a fresh placement, so everything that asks "is this a random spread?"
 * ({@code /locate}, explorer maps) keeps working for whatever uses it. Data-driven: every future
 * planet's scattered structures can keep clear of that planet's fixed ones without new Java.
 */
public final class KeepClearRandomSpreadPlacement extends RandomSpreadStructurePlacement {

    /**
     * Vanilla's random-spread fields, spelled out, plus {@code keep_clear_of}. DFU's product builder
     * stops at eight fields, so vanilla's {@code placementCodec(...).and(...)} cannot carry a ninth:
     * the first five below are copied from {@code StructurePlacement.placementCodec} (names, codecs
     * and defaults), so any vanilla random_spread JSON reads the same here.
     */
    public static final MapCodec<KeepClearRandomSpreadPlacement> CODEC = RecordCodecBuilder
            .<KeepClearRandomSpreadPlacement>mapCodec(instance -> instance.group(
                            Vec3i.offsetCodec(16).optionalFieldOf("locate_offset", Vec3i.ZERO)
                                    .forGetter(KeepClearRandomSpreadPlacement::locateOffset),
                            FrequencyReductionMethod.CODEC
                                    .optionalFieldOf("frequency_reduction_method", FrequencyReductionMethod.DEFAULT)
                                    .forGetter(KeepClearRandomSpreadPlacement::frequencyReductionMethod),
                            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("frequency", 1.0F)
                                    .forGetter(KeepClearRandomSpreadPlacement::frequency),
                            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("salt").forGetter(KeepClearRandomSpreadPlacement::salt),
                            ExclusionZone.CODEC.optionalFieldOf("exclusion_zone")
                                    .forGetter(KeepClearRandomSpreadPlacement::exclusionZone),
                            Codec.intRange(0, 4096).fieldOf("spacing").forGetter(RandomSpreadStructurePlacement::spacing),
                            Codec.intRange(0, 4096).fieldOf("separation").forGetter(RandomSpreadStructurePlacement::separation),
                            RandomSpreadType.CODEC.optionalFieldOf("spread_type", RandomSpreadType.LINEAR)
                                    .forGetter(RandomSpreadStructurePlacement::spreadType),
                            ExclusionZone.CODEC.listOf().fieldOf("keep_clear_of")
                                    .forGetter(KeepClearRandomSpreadPlacement::keepClearOf))
                    .apply(instance, KeepClearRandomSpreadPlacement::new))
            .validate(KeepClearRandomSpreadPlacement::validate);

    private final List<ExclusionZone> keepClearOf;

    public KeepClearRandomSpreadPlacement(
            Vec3i locateOffset,
            FrequencyReductionMethod frequencyReductionMethod,
            float frequency,
            int salt,
            Optional<ExclusionZone> exclusionZone,
            int spacing,
            int separation,
            RandomSpreadType spreadType,
            List<ExclusionZone> keepClearOf) {
        super(locateOffset, frequencyReductionMethod, frequency, salt, exclusionZone, spacing, separation, spreadType);
        this.keepClearOf = List.copyOf(keepClearOf);
    }

    public List<ExclusionZone> keepClearOf() {
        return keepClearOf;
    }

    /** Vanilla's check for its one zone first, then every zone of ours the same way. */
    @Override
    public boolean applyInteractionsWithOtherStructures(ChunkGeneratorStructureState state, int x, int z) {
        if (!super.applyInteractionsWithOtherStructures(state, x, z)) {
            return false;
        }
        for (ExclusionZone zone : keepClearOf) {
            if (state.hasStructureChunkInRange(zone.otherSet(), x, z, zone.chunkCount())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public StructurePlacementType<?> type() {
        return WorldsContent.KEEP_CLEAR_RANDOM_SPREAD_PLACEMENT_TYPE.get();
    }

    /** The rule vanilla's codec enforces for its own random spread (its validator is private). */
    private static DataResult<KeepClearRandomSpreadPlacement> validate(KeepClearRandomSpreadPlacement placement) {
        return placement.spacing() <= placement.separation()
                ? DataResult.error(() -> "Spacing has to be larger than separation")
                : DataResult.success(placement);
    }
}
