package com.ascension.combat.api.stat;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Names where a stat's modifiers come from, for the character sheet's breakdown
 * ({@link StatReader#breakdown}). A system that gives stats (growth, runes, another mod) registers
 * the id prefix its modifiers share, once at startup:
 *
 * <pre>{@code
 * StatSources.register(ResourceLocation.fromNamespaceAndPath("ascension_combat", "growth/"),
 *         Component.translatable("stat_source.ascension_combat.growth"));
 * }</pre>
 *
 * <p>A modifier matches when its id has the prefix's namespace and its path starts with the
 * prefix's path; the longest match wins. Worn and held items need no registration: the breakdown
 * names them by the item. Anything unmatched shows as "Other".
 *
 * <p>Holds ids and labels only, never an entity (ADR-0007 rule 1), and is read on both sides.
 */
public final class StatSources {

    private record Source(ResourceLocation prefix, Component label) {
    }

    private static final List<Source> SOURCES = new CopyOnWriteArrayList<>();

    private StatSources() {
    }

    public static void register(ResourceLocation modifierIdPrefix, Component label) {
        SOURCES.add(new Source(modifierIdPrefix, label));
    }

    /** The label of the longest registered prefix {@code modifierId} starts with, if any. */
    public static Optional<Component> labelFor(ResourceLocation modifierId) {
        Source best = null;
        for (Source source : SOURCES) {
            if (source.prefix.getNamespace().equals(modifierId.getNamespace())
                    && modifierId.getPath().startsWith(source.prefix.getPath())
                    && (best == null || source.prefix.getPath().length() > best.prefix.getPath().length())) {
                best = source;
            }
        }
        return Optional.ofNullable(best).map(Source::label);
    }
}
