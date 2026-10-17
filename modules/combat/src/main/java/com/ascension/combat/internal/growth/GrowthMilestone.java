package com.ascension.combat.internal.growth;

import com.ascension.combat.AscensionCombat;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/**
 * What one advancement is worth to a player's growth (docs/progression/growth.md section 9, ADR-0021).
 * Data, in the datapack registry {@code ascension_combat:growth_milestone}
 * ({@code data/<namespace>/ascension_combat/growth_milestone/<path>.json}), so a new boss, gate or
 * place adds growth with two JSON files &mdash; the advancement and this &mdash; and no code.
 *
 * <p>The advancement is the record of having earned it: vanilla keeps it per player, saves and
 * syncs it, and shows the toast. This file only says what it is worth. A milestone whose
 * advancement no datapack defines never counts, and {@link GrowthDataCheck} says so.
 *
 * @param shared true for the team's milestones (bosses and gates): earned by one player, it is
 *     awarded to every player on the server, and to anyone who joins later (growth.md section 7)
 * @param points skill points it gives, to spend in the tree
 * @param stats stats it gives directly; v1's milestones give none (Sanchit, 2026-10-15: points
 *     only), but a datapack may
 */
public record GrowthMilestone(ResourceLocation advancement, boolean shared, int points, List<GrowthStat> stats) {

    public static final ResourceKey<Registry<GrowthMilestone>> REGISTRY = ResourceKey.createRegistryKey(
            ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, "growth_milestone"));

    public static final Codec<GrowthMilestone> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(
                    ResourceLocation.CODEC.fieldOf("advancement").forGetter(GrowthMilestone::advancement),
                    Codec.BOOL.optionalFieldOf("shared", false).forGetter(GrowthMilestone::shared),
                    Codec.intRange(0, 1000).optionalFieldOf("points", 0).forGetter(GrowthMilestone::points),
                    GrowthStat.CODEC.listOf().optionalFieldOf("stats", List.of()).forGetter(GrowthMilestone::stats))
            .apply(instance, GrowthMilestone::new));
}
