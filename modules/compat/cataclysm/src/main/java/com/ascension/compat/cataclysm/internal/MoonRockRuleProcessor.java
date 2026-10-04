package com.ascension.compat.cataclysm.internal;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorRule;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleProcessor;

/**
 * Recolors the Ruined Citadel's End-stone palette to the Moon's own rock, by block-state
 * substitution at placement time rather than editing Cataclysm's binary NBT templates. Applied by
 * {@code RuinedCitadelPieceMixin} (see ADR-0013).
 *
 * <p>Looked up by resource location, not a compile dependency on {@code ascension-worlds}'
 * internal package (ADR-0003 rule 4: {@code internal} is free to change, and these blocks live
 * there) &mdash; the same "reference by id" posture this whole module already takes toward
 * Cataclysm's own structure/loot ids in JSON, just expressed in Java because the mixin needs an
 * actual object, not a string. Safe to resolve eagerly: this class is only touched during real
 * chunk generation, long after every mod's blocks are registered, and {@code ascension_worlds}
 * is a required dependency of this jar.
 */
public final class MoonRockRuleProcessor {

    public static final RuleProcessor INSTANCE = new RuleProcessor(List.of(
            new ProcessorRule(
                    new BlockMatchTest(Blocks.END_STONE),
                    AlwaysTrueTest.INSTANCE,
                    moonBlock("anorthosite").defaultBlockState()),
            new ProcessorRule(
                    new BlockMatchTest(Blocks.END_STONE_BRICKS),
                    AlwaysTrueTest.INSTANCE,
                    moonBlock("anorthosite").defaultBlockState())));

    private MoonRockRuleProcessor() {
    }

    private static Block moonBlock(String path) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("ascension_worlds", path));
    }
}
