package com.ascension.combat.internal.rune;

import com.ascension.combat.api.rune.RunePath;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;

/**
 * The rules a rune page must keep (docs/technical/rune-system.md sections 3, 6 and 7), as plain
 * logic: a page and a way to look runes up go in, a refusal or nothing comes out. No player, no
 * level, so it is checked by unit tests; the server handler adds what only the world knows
 * (combat, the player's chalk and levels).
 *
 * <p>The client never decides any of this. It may show the same rules early, but the server runs
 * them again on every save.
 */
public final class RunePageRules {

    /** Why a page was not saved. Each has a line in the lang file, at {@link #langKey()}. */
    public enum Refusal {
        UNKNOWN_RUNE,
        NO_PRIMARY,
        SAME_PATH,
        WRONG_KEYSTONE,
        WRONG_MINOR,
        ONE_PER_ROW,
        WRONG_SHARD,
        SECONDARY_LOCKED,
        SHARD_LOCKED,
        IN_COMBAT,
        NO_CHALK,
        NO_LEVELS;

        public String langKey() {
            return "gui.ascension_combat.rune_refused." + name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * Which slots this player has opened (section 6): the primary path is always open; the
     * secondary path and each shard row open with growth's milestones.
     */
    public record OpenSlots(boolean secondary, boolean offence, boolean flex, boolean defence) {

        public static final OpenSlots ALL = new OpenSlots(true, true, true, true);

        /** Slots open at this many milestones: the secondary at 1, then a shard row at each of 2, 3, 4. */
        public static OpenSlots atMilestones(int milestones) {
            return new OpenSlots(milestones >= 1, milestones >= 2, milestones >= 3, milestones >= 4);
        }

        public boolean shardRow(int row) {
            return row == 0 ? offence : row == 1 ? flex : defence;
        }
    }

    private RunePageRules() {
    }

    /**
     * Checks the page's shape against the registry and the open slots. Empty slots are allowed: a
     * page may be left part-empty.
     *
     * @param runes looks a rune up by id; empty for an id the registry doesn't hold
     */
    public static Optional<Refusal> validate(
            RunePage page, Function<ResourceLocation, Optional<RuneDefinition>> runes, OpenSlots open) {
        if (page.primary().isEmpty()) {
            boolean anything = page.keystone().isPresent() || !page.primaryMinors().isEmpty()
                    || page.secondary().isPresent() || !page.secondaryMinors().isEmpty();
            return anything ? Optional.of(Refusal.NO_PRIMARY) : shards(page, runes, open);
        }
        RunePath primary = page.primary().get();
        if (page.secondary().isPresent() && page.secondary().get() == primary) {
            return Optional.of(Refusal.SAME_PATH);
        }
        if (page.secondary().isPresent() && !open.secondary()) {
            return Optional.of(Refusal.SECONDARY_LOCKED);
        }
        if (page.secondary().isEmpty() && !page.secondaryMinors().isEmpty()) {
            return Optional.of(Refusal.WRONG_MINOR);
        }

        if (page.keystone().isPresent()) {
            Optional<RuneDefinition> keystone = runes.apply(page.keystone().get());
            if (keystone.isEmpty()) {
                return Optional.of(Refusal.UNKNOWN_RUNE);
            }
            if (keystone.get().slot() != RuneSlot.KEYSTONE || !keystone.get().path().equals(page.primary())) {
                return Optional.of(Refusal.WRONG_KEYSTONE);
            }
        }

        Optional<Refusal> side = minors(page.primaryMinors(), primary, runes);
        if (side.isPresent()) {
            return side;
        }
        if (page.secondary().isPresent()) {
            side = minors(page.secondaryMinors(), page.secondary().get(), runes);
            if (side.isPresent()) {
                return side;
            }
        }
        return shards(page, runes, open);
    }

    private static Optional<Refusal> minors(
            List<ResourceLocation> ids, RunePath path, Function<ResourceLocation, Optional<RuneDefinition>> runes) {
        if (ids.size() > RuneDefinition.MINOR_ROWS || Set.copyOf(ids).size() != ids.size()) {
            return Optional.of(Refusal.ONE_PER_ROW);
        }
        Set<Integer> rows = new HashSet<>();
        for (ResourceLocation id : ids) {
            Optional<RuneDefinition> rune = runes.apply(id);
            if (rune.isEmpty()) {
                return Optional.of(Refusal.UNKNOWN_RUNE);
            }
            if (rune.get().slot() != RuneSlot.MINOR || rune.get().path().orElse(null) != path) {
                return Optional.of(Refusal.WRONG_MINOR);
            }
            if (!rows.add(rune.get().row())) {
                return Optional.of(Refusal.ONE_PER_ROW);
            }
        }
        return Optional.empty();
    }

    private static Optional<Refusal> shards(
            RunePage page, Function<ResourceLocation, Optional<RuneDefinition>> runes, OpenSlots open) {
        List<ResourceLocation> ids = page.shards();
        if (ids.size() > RuneDefinition.SHARD_ROWS || Set.copyOf(ids).size() != ids.size()) {
            return Optional.of(Refusal.ONE_PER_ROW);
        }
        Set<Integer> rows = new HashSet<>();
        for (ResourceLocation id : ids) {
            Optional<RuneDefinition> rune = runes.apply(id);
            if (rune.isEmpty()) {
                return Optional.of(Refusal.UNKNOWN_RUNE);
            }
            if (rune.get().slot() != RuneSlot.SHARD) {
                return Optional.of(Refusal.WRONG_SHARD);
            }
            if (!open.shardRow(rune.get().row())) {
                return Optional.of(Refusal.SHARD_LOCKED);
            }
            if (!rows.add(rune.get().row())) {
                return Optional.of(Refusal.ONE_PER_ROW);
            }
        }
        return Optional.empty();
    }

    /**
     * Whether saving {@code proposed} over {@code saved} costs a Rune Chalk and levels (section
     * 7): it does when any rune on the saved page is gone from the new one, whether removed or
     * replaced. Filling an empty slot is free, so a first page, and every slot growth opens,
     * cost nothing.
     */
    public static boolean costsToChange(RunePage saved, RunePage proposed) {
        Set<ResourceLocation> kept = proposed.runes();
        for (ResourceLocation id : saved.runes()) {
            if (!kept.contains(id)) {
                return true;
            }
        }
        return false;
    }
}
