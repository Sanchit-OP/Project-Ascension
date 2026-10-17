package com.ascension.combat.internal.rune;

import com.ascension.combat.api.rune.RunePath;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * A rune page as chosen: what a player saves and what the client asks to save
 * (docs/technical/rune-system.md section 3). Immutable; rune ids only, so it means nothing until
 * read against the rune registry ({@link RunePageRules}, {@link RunePageState#resolve}).
 *
 * <p>The minor and shard lists are sets in all but type: their order carries nothing, because
 * every rune knows its own row. Lists only because codecs and the network handle them simply.
 *
 * @param primary         the primary path, if one is chosen
 * @param keystone        its keystone, if one is chosen
 * @param primaryMinors   up to two minors of the primary path, one per row
 * @param secondary       the secondary path, if one is chosen
 * @param secondaryMinors up to two minors of the secondary path, one per row
 * @param shards          up to three stat shards, one per row
 */
public record RunePage(
        Optional<RunePath> primary,
        Optional<ResourceLocation> keystone,
        List<ResourceLocation> primaryMinors,
        Optional<RunePath> secondary,
        List<ResourceLocation> secondaryMinors,
        List<ResourceLocation> shards) {

    public static final RunePage EMPTY =
            new RunePage(Optional.empty(), Optional.empty(), List.of(), Optional.empty(), List.of(), List.of());

    /**
     * As saved with the player. The first four field names are the ones the 2026-09-19 page wrote,
     * so a page saved then still loads; {@link RunePageState} renames its old rune ids.
     */
    public static final Codec<RunePage> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(
                    RunePath.CODEC.optionalFieldOf("primary_path").forGetter(RunePage::primary),
                    ResourceLocation.CODEC.optionalFieldOf("keystone").forGetter(RunePage::keystone),
                    ResourceLocation.CODEC.listOf().optionalFieldOf("primary_minors", List.of())
                            .forGetter(RunePage::primaryMinors),
                    RunePath.CODEC.optionalFieldOf("secondary_path").forGetter(RunePage::secondary),
                    ResourceLocation.CODEC.listOf().optionalFieldOf("secondary_minors", List.of())
                            .forGetter(RunePage::secondaryMinors),
                    ResourceLocation.CODEC.listOf().optionalFieldOf("shards", List.of())
                            .forGetter(RunePage::shards))
            .apply(instance, RunePage::new));

    private static final StreamCodec<ByteBuf, RunePath> PATH_STREAM =
            ByteBufCodecs.idMapper(i -> RunePath.values()[i], RunePath::ordinal);

    private static final StreamCodec<ByteBuf, List<ResourceLocation>> IDS =
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list(8));

    public static final StreamCodec<ByteBuf, RunePage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(PATH_STREAM), RunePage::primary,
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), RunePage::keystone,
            IDS, RunePage::primaryMinors,
            ByteBufCodecs.optional(PATH_STREAM), RunePage::secondary,
            IDS, RunePage::secondaryMinors,
            IDS, RunePage::shards,
            RunePage::new);

    public RunePage {
        primaryMinors = List.copyOf(primaryMinors);
        secondaryMinors = List.copyOf(secondaryMinors);
        shards = List.copyOf(shards);
    }

    /** Every rune on the page: keystone, minors and shards. */
    public Set<ResourceLocation> runes() {
        Set<ResourceLocation> all = new HashSet<>(primaryMinors);
        keystone.ifPresent(all::add);
        all.addAll(secondaryMinors);
        all.addAll(shards);
        return all;
    }

    /**
     * Whether two pages hold the same choices. Not {@code equals}: minor and shard order means
     * nothing, so [a, b] and [b, a] are the same page.
     */
    public boolean sameAs(RunePage other) {
        return primary.equals(other.primary) && keystone.equals(other.keystone)
                && secondary.equals(other.secondary)
                && Set.copyOf(primaryMinors).equals(Set.copyOf(other.primaryMinors))
                && Set.copyOf(secondaryMinors).equals(Set.copyOf(other.secondaryMinors))
                && Set.copyOf(shards).equals(Set.copyOf(other.shards));
    }

    public RunePage withKeystone(ResourceLocation id) {
        return new RunePage(primary, Optional.of(id), primaryMinors, secondary, secondaryMinors, shards);
    }
}
