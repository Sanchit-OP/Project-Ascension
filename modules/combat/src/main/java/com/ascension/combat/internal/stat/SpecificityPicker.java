package com.ascension.combat.internal.stat;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * The merge rule every stat data map shares (docs/technical/item-stats.md section 4): when two
 * entries reach one element, an entry naming the element itself beats a tag's, one field at a time,
 * whichever loaded first; between equals, the later one wins, as in any datapack.
 *
 * <p>NeoForge merges data map entries in load order and hands each merge only the newest entry's
 * source. So a merged value has to remember which of its fields came from an element's own entry;
 * {@link #recorded} is that memory, kept on the value as {@code specificFields}.
 *
 * @param <K> what names a field: a string for items, a stat for mobs
 */
final class SpecificityPicker<K> {

    private final Set<K> firstSpecific;
    private final Set<K> secondSpecific;
    private final Set<K> recorded = new HashSet<>();

    /**
     * @param firstRecorded what an earlier merge recorded on {@code first}, or null if it was never
     *     merged, in which case its own entry decides: all of its fields, or none
     */
    SpecificityPicker(@Nullable Set<K> firstRecorded, boolean firstNamesElement, Set<K> firstFields,
            @Nullable Set<K> secondRecorded, boolean secondNamesElement, Set<K> secondFields) {
        this.firstSpecific = specific(firstRecorded, firstNamesElement, firstFields);
        this.secondSpecific = specific(secondRecorded, secondNamesElement, secondFields);
    }

    private static <K> Set<K> specific(@Nullable Set<K> recorded, boolean namesElement, Set<K> fields) {
        if (recorded != null) {
            return recorded;
        }
        return namesElement ? fields : Set.of();
    }

    /** One field's choice: the later value, unless only the earlier one came from an element's own entry. */
    <V> Optional<V> pick(K key, Optional<V> first, Optional<V> second) {
        boolean keepFirst = first.isPresent()
                && (second.isEmpty() || (firstSpecific.contains(key) && !secondSpecific.contains(key)));
        Optional<V> chosen = keepFirst ? first : second;
        if (chosen.isPresent() && (keepFirst ? firstSpecific : secondSpecific).contains(key)) {
            recorded.add(key);
        }
        return chosen;
    }

    /** The fields whose chosen value came from an element's own entry, to keep on the merged value. */
    Set<K> recorded() {
        return Collections.unmodifiableSet(recorded);
    }
}
