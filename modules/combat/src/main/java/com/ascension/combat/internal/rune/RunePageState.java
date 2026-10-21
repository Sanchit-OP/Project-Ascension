package com.ascension.combat.internal.rune;

import com.ascension.combat.AscensionCombat;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

/**
 * A player's saved rune page (docs/technical/rune-system.md sections 3 and 4.5), and the growth
 * of its growing runes: for each, the kinds of enemy it has counted.
 *
 * <p>Attached to the player, serialized, and copied on death (see
 * {@code CombatAttachments#RUNE_PAGE}): a rune choice is a build decision, and growth is kept
 * through death; only changing the rune resets it.
 *
 * <p><b>Pages saved before 2026-10-17</b> load: their four fields keep their names
 * ({@link RunePage#CODEC}), and their rune ids, written then as {@code vanguard_<name>}, are
 * renamed to the plain {@code <name>} the data files use. They had no keystone field, because a
 * path had only one keystone; {@link #fillMissingKeystone} gives them their path's first.
 */
public final class RunePageState {

    private static final MapCodec<Map<ResourceLocation, List<ResourceLocation>>> GROWTH =
            Codec.unboundedMap(ResourceLocation.CODEC, ResourceLocation.CODEC.listOf())
                    .optionalFieldOf("growth", Map.of());

    public static final Codec<RunePageState> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(
                    RunePage.CODEC.fieldOf("page").forGetter(RunePageState::page),
                    GROWTH.forGetter(RunePageState::growthForSave))
            .apply(instance, (page, growth) -> new RunePageState(page, growth, false)));

    /** Reads either today's shape or the 2026-09-19 one, which had the page's fields at the top. */
    public static final Codec<RunePageState> SAVED_CODEC = Codec.withAlternative(
            CODEC, RunePage.CODEC.xmap(page -> new RunePageState(page, Map.of(), true), RunePageState::page));

    private RunePage page;
    private final Map<ResourceLocation, Set<ResourceLocation>> growth = new HashMap<>();

    /** The page resolved against the registry it was resolved with; see {@link #resolve}. */
    private Registry<RuneDefinition> resolvedAgainst;
    private List<RuneDefinition> resolved = List.of();

    /** Set while a page loaded in the 2026-09-19 shape still lacks the keystone it implicitly had. */
    private boolean legacy;

    public RunePageState() {
        this(RunePage.EMPTY, Map.of(), false);
    }

    private RunePageState(RunePage page, Map<ResourceLocation, List<ResourceLocation>> growth, boolean legacy) {
        this.page = replaceRemovedKeystone(legacy ? renameLegacyIds(page) : page);
        this.legacy = legacy;
        growth.forEach((rune, kinds) -> this.growth.put(rune, new LinkedHashSet<>(kinds)));
    }

    /**
     * 2026-10-22: Silent Step and Cat's Eye were replaced (Predator and Overheal took their places on the dial). A page saved with one
     * of them keeps a keystone in that place, so nobody is left with an empty socket and a refusal at their next Save.
     */
    private static RunePage replaceRemovedKeystone(RunePage page) {
        return page.keystone().map(id -> {
            if (id.getPath().equals("silent_step")) {
                return page.withKeystone(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "predator"));
            }
            if (id.getPath().equals("cats_eye")) {
                return page.withKeystone(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "overheal"));
            }
            return page;
        }).orElse(page);
    }

    public RunePage page() {
        return page;
    }

    /**
     * Replaces the page. Callers have already validated it ({@link RunePageRules}). A rune taken
     * off the page loses its growth.
     */
    public void set(RunePage newPage) {
        this.page = newPage;
        this.growth.keySet().retainAll(newPage.runes());
        this.resolvedAgainst = null;
    }

    public boolean has(ResourceLocation rune) {
        return page.runes().contains(rune);
    }

    /**
     * The page's runes as definitions, in a fixed order (keystone, minors, shards). Cached per
     * registry instance: a datapack registry never changes while a server runs, and a new server
     * means a new instance. Ids the registry no longer holds (a datapack removed the rune) are
     * skipped, so a removed rune simply does nothing.
     */
    public List<RuneDefinition> resolve(Registry<RuneDefinition> runes) {
        if (resolvedAgainst != runes) {
            List<RuneDefinition> out = new ArrayList<>();
            List<ResourceLocation> ids = new ArrayList<>();
            page.keystone().ifPresent(ids::add);
            ids.addAll(page.primaryMinors());
            ids.addAll(page.secondaryMinors());
            ids.addAll(page.shards());
            for (ResourceLocation id : ids) {
                RuneDefinition rune = runes.get(id);
                if (rune != null) {
                    out.add(rune);
                }
            }
            resolved = List.copyOf(out);
            resolvedAgainst = runes;
        }
        return resolved;
    }

    /** The rune on this page that runs {@code effect}, if any. */
    public Optional<RuneDefinition> active(Registry<RuneDefinition> runes, ResourceLocation effect) {
        for (RuneDefinition rune : resolve(runes)) {
            if (rune.effect().equals(effect)) {
                return Optional.of(rune);
            }
        }
        return Optional.empty();
    }

    /**
     * Gives a page saved before keystones were a choice its path's first keystone, which it had
     * then without picking. A page saved since keeps an empty keystone if the player left it so.
     *
     * @return whether the page changed
     */
    public boolean fillMissingKeystone(Registry<RuneDefinition> runes) {
        if (!legacy || page.primary().isEmpty() || page.keystone().isPresent()) {
            legacy = false;
            return false;
        }
        Optional<ResourceLocation> first = runes.entrySet().stream()
                .filter(e -> e.getValue().slot() == RuneSlot.KEYSTONE && e.getValue().path().equals(page.primary()))
                .min(Comparator.comparingInt(e -> e.getValue().position()))
                .map(e -> e.getKey().location());
        if (first.isEmpty()) {
            return false;
        }
        set(page.withKeystone(first.get()));
        legacy = false;
        return true;
    }

    /** How many kinds of enemy a growing rune has counted. */
    public int growth(ResourceLocation rune) {
        Set<ResourceLocation> kinds = growth.get(rune);
        return kinds == null ? 0 : kinds.size();
    }

    /**
     * Counts a kind of enemy for a growing rune on the page. A kind already counted counts no
     * more, which is what keeps a mob farm from filling it.
     *
     * @return whether the rune grew
     */
    public boolean grow(ResourceLocation rune, ResourceLocation kind, int cap) {
        if (!has(rune)) {
            return false;
        }
        Set<ResourceLocation> kinds = growth.computeIfAbsent(rune, r -> new LinkedHashSet<>());
        return kinds.size() < cap && kinds.add(kind);
    }

    /** Every growing rune's count, for the client. */
    public Map<ResourceLocation, Integer> growthCounts() {
        Map<ResourceLocation, Integer> counts = new HashMap<>();
        growth.forEach((rune, kinds) -> counts.put(rune, kinds.size()));
        return counts;
    }

    private Map<ResourceLocation, List<ResourceLocation>> growthForSave() {
        Map<ResourceLocation, List<ResourceLocation>> out = new HashMap<>();
        growth.forEach((rune, kinds) -> out.put(rune, List.copyOf(kinds)));
        return out;
    }

    // --- pages saved before 2026-10-17 --------------------------------------------------------

    private static final String LEGACY_PREFIX = "vanguard_";

    private static RunePage renameLegacyIds(RunePage saved) {
        return new RunePage(saved.primary(), saved.keystone(),
                saved.primaryMinors().stream().map(RunePageState::renameLegacyId).toList(),
                saved.secondary(),
                saved.secondaryMinors().stream().map(RunePageState::renameLegacyId).toList(),
                saved.shards());
    }

    private static boolean isLegacyId(ResourceLocation id) {
        return id.getNamespace().equals(AscensionCombat.MOD_ID) && id.getPath().startsWith(LEGACY_PREFIX);
    }

    private static ResourceLocation renameLegacyId(ResourceLocation id) {
        return isLegacyId(id)
                ? ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath().substring(LEGACY_PREFIX.length()))
                : id;
    }
}
