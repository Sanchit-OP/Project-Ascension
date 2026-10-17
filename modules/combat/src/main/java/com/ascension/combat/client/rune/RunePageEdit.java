package com.ascension.combat.client.rune;

import com.ascension.combat.api.rune.RunePath;
import com.ascension.combat.internal.rune.RuneDefinition;
import com.ascension.combat.internal.rune.RunePage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;

/**
 * The page as the player is editing it on the rune screen: what a click on a path or a rune does
 * to the page that has not been saved yet (docs/technical/rune-system.md sections 3 and 10).
 *
 * <p>Pure choices, no screen and no game: the screen shows this and sends {@link #toPage()} when
 * Save is pressed, and the server checks it again with the same rules. What it keeps here is only
 * what makes clicking feel right: choosing a new primary path clears the old path's runes, the
 * secondary path can't be the primary, and a click on the rune already chosen takes it off.
 * Unit-tested ({@code RunePageEditTest}).
 */
public final class RunePageEdit {

    private RunePath primary;
    private ResourceLocation keystone;
    private final Map<Integer, ResourceLocation> primaryMinors = new HashMap<>();
    private RunePath secondary;
    private final Map<Integer, ResourceLocation> secondaryMinors = new HashMap<>();
    private final Map<Integer, ResourceLocation> shards = new HashMap<>();

    /** Starts from {@code saved}. {@code lookup} says which row a minor or shard sits in. */
    public RunePageEdit(RunePage saved, Function<ResourceLocation, RuneDefinition> lookup) {
        load(saved, lookup);
    }

    /** Throws away every unsaved choice and starts again from {@code saved}. */
    public void load(RunePage saved, Function<ResourceLocation, RuneDefinition> lookup) {
        primary = saved.primary().orElse(null);
        keystone = saved.keystone().orElse(null);
        secondary = saved.secondary().orElse(null);
        primaryMinors.clear();
        secondaryMinors.clear();
        shards.clear();
        fill(primaryMinors, saved.primaryMinors(), lookup);
        fill(secondaryMinors, saved.secondaryMinors(), lookup);
        fill(shards, saved.shards(), lookup);
    }

    private static void fill(Map<Integer, ResourceLocation> rows, List<ResourceLocation> ids,
            Function<ResourceLocation, RuneDefinition> lookup) {
        for (ResourceLocation id : ids) {
            RuneDefinition rune = lookup.apply(id);
            if (rune != null) {
                rows.put(rune.row(), id);
            }
        }
    }

    public Optional<RunePath> primary() {
        return Optional.ofNullable(primary);
    }

    public Optional<RunePath> secondary() {
        return Optional.ofNullable(secondary);
    }

    public Optional<ResourceLocation> keystone() {
        return Optional.ofNullable(keystone);
    }

    /** The minor chosen in {@code row} of the primary or the secondary side, if any. */
    public Optional<ResourceLocation> minor(boolean primarySide, int row) {
        return Optional.ofNullable((primarySide ? primaryMinors : secondaryMinors).get(row));
    }

    public Optional<ResourceLocation> shard(int row) {
        return Optional.ofNullable(shards.get(row));
    }

    public boolean has(ResourceLocation id) {
        return id.equals(keystone) || primaryMinors.containsValue(id) || secondaryMinors.containsValue(id)
                || shards.containsValue(id);
    }

    /**
     * Chooses the primary path. A different one starts its page fresh (the old path's runes are no
     * use to it), and if it was the secondary the secondary is cleared.
     *
     * @return whether the primary path changed
     */
    public boolean choosePrimary(RunePath path) {
        if (path == primary) {
            return false;
        }

        primary = path;
        keystone = null;
        primaryMinors.clear();
        if (secondary == path) {
            secondary = null;
            secondaryMinors.clear();
        }
        return true;
    }

    /**
     * Chooses the secondary path, or takes it off if it is already the one chosen. It can't be the
     * primary path.
     *
     * @return whether the secondary path changed
     */
    public boolean chooseSecondary(RunePath path) {
        if (path == primary) {
            return false;
        }

        secondary = secondary == path ? null : path;
        secondaryMinors.clear();
        return true;
    }

    public void toggleKeystone(ResourceLocation id) {
        keystone = id.equals(keystone) ? null : id;
    }

    /** Chooses a minor for its row, replacing the row's choice; a click on the chosen one takes it off. */
    public void toggleMinor(boolean primarySide, int row, ResourceLocation id) {
        toggle(primarySide ? primaryMinors : secondaryMinors, row, id);
    }

    public void toggleShard(int row, ResourceLocation id) {
        toggle(shards, row, id);
    }

    private static void toggle(Map<Integer, ResourceLocation> rows, int row, ResourceLocation id) {
        if (id.equals(rows.get(row))) {
            rows.remove(row);
        } else {
            rows.put(row, id);
        }
    }

    public RunePage toPage() {
        return new RunePage(
                Optional.ofNullable(primary),
                Optional.ofNullable(keystone),
                byRow(primaryMinors),
                Optional.ofNullable(secondary),
                byRow(secondaryMinors),
                byRow(shards));
    }

    private static List<ResourceLocation> byRow(Map<Integer, ResourceLocation> rows) {
        List<Integer> keys = new ArrayList<>(rows.keySet());
        keys.sort(null);
        List<ResourceLocation> out = new ArrayList<>();
        for (int row : keys) {
            out.add(rows.get(row));
        }
        return out;
    }
}
