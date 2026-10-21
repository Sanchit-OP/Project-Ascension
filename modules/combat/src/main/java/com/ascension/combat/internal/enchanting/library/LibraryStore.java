package com.ascension.combat.internal.enchanting.library;

import java.util.Map;
import java.util.TreeMap;

/**
 * What an Enchantment Library holds: for each enchantment (by id), the best level stored. Pure arithmetic, no Minecraft types, so
 * it can be tested without a server.
 *
 * <p>Putting a book in keeps the higher of the stored and the new level; two equal levels make the next one up, the way an anvil
 * joins two Sharpness III books into a IV, never past the enchantment's own maximum. Taking out costs the library nothing: it is a
 * collection, not a stock, so one stored Sharpness V can be put on a book any number of times (the price is paid in experience and
 * in blank books, see {@link #cost}).
 */
public final class LibraryStore {

    private final Map<String, Integer> levels = new TreeMap<>();

    public int level(String id) {
        return levels.getOrDefault(id, 0);
    }

    public Map<String, Integer> view() {
        return java.util.Collections.unmodifiableMap(levels);
    }

    public void set(String id, int level) {
        if (level > 0) {
            levels.put(id, level);
        } else {
            levels.remove(id);
        }
    }

    public boolean isEmpty() {
        return levels.isEmpty();
    }

    /**
     * Joins a book's enchantment into the library.
     *
     * @return the level now stored
     */
    public int absorb(String id, int level, int maxLevel) {
        int stored = level(id);
        int result = Math.max(stored, level);
        if (stored > 0 && stored == level && level < maxLevel) {
            result = level + 1;
        }
        levels.put(id, Math.min(result, Math.max(maxLevel, level)));
        return levels.get(id);
    }

    /** Experience levels to put a combination on a book: the sum of its levels, at least one. */
    public static int cost(Iterable<Integer> levelsTaken) {
        int total = 0;
        for (int level : levelsTaken) {
            total += level;
        }
        return Math.max(1, total);
    }
}
