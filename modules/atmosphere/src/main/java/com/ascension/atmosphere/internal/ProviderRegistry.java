package com.ascension.atmosphere.internal;

import com.ascension.atmosphere.api.Atmosphere;
import com.ascension.atmosphere.api.AtmosphereContext;
import com.ascension.atmosphere.api.AtmosphereProvider;
import com.ascension.atmosphere.api.DrainModifier;
import com.ascension.atmosphere.api.LungCapacityModifier;
import com.ascension.atmosphere.api.OxygenSourceCollector;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds registered providers and resolves atmosphere queries.
 *
 * <p>Two phases. During setup, registration mutates plain lists on the mod thread. At freeze,
 * entries are sorted once into an immutable array. Afterwards, queries only walk that array,
 * so resolution allocates nothing and sorts nothing on the server thread (ADR-0007 rules 4
 * and 6).
 *
 * <p>This class holds no {@code Level}, {@code Player} or {@code BlockEntity} reference, and
 * providers are supplied a short-lived context instead (ADR-0007 rule 1).
 */
public final class ProviderRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProviderRegistry.class);

    private static final ProviderRegistry INSTANCE = new ProviderRegistry();

    /** Entry pairing a provider with the id it was registered under. */
    public record Entry(ResourceLocation id, AtmosphereProvider provider) {
        int priority() {
            return provider.priority();
        }
    }

    private final Map<ResourceLocation, AtmosphereProvider> pendingProviders = new LinkedHashMap<>();
    private final Map<ResourceLocation, OxygenSourceCollector> pendingCollectors = new LinkedHashMap<>();
    private final Map<ResourceLocation, DrainModifier> pendingModifiers = new LinkedHashMap<>();
    private final Map<ResourceLocation, LungCapacityModifier> pendingLungModifiers = new LinkedHashMap<>();

    /** Sorted highest priority first, then by id for determinism. Null until frozen. */
    private volatile Entry[] providers;
    private volatile List<OxygenSourceCollector> collectors = List.of();
    private volatile List<DrainModifier> modifiers = List.of();
    private volatile List<LungCapacityModifier> lungModifiers = List.of();

    /**
     * Equal-priority conflicts already reported, so a genuine misconfiguration is logged once
     * rather than every tick for the rest of the session.
     */
    private final Set<String> reportedConflicts = ConcurrentHashMap.newKeySet();

    /**
     * Package-private rather than private so tests can build their own instance.
     *
     * <p>Same reasoning as {@code WorldEnvironments} in core, and the same rejected alternative:
     * a {@code reset()} on the singleton is test-only API living in production code, one careless
     * call away from emptying the provider list on a running server. Freezing is one-way by
     * design; a test that needs a frozen registry should get a fresh one.
     */
    ProviderRegistry() {
    }

    public static ProviderRegistry get() {
        return INSTANCE;
    }

    // --- registration -------------------------------------------------------

    public void addProvider(ResourceLocation id, AtmosphereProvider provider) {
        requireOpen();
        if (pendingProviders.putIfAbsent(id, provider) != null) {
            throw new IllegalArgumentException("Duplicate AtmosphereProvider id: " + id);
        }
    }

    public void addCollector(ResourceLocation id, OxygenSourceCollector collector) {
        requireOpen();
        if (pendingCollectors.putIfAbsent(id, collector) != null) {
            throw new IllegalArgumentException("Duplicate OxygenSourceCollector id: " + id);
        }
    }

    public void addModifier(DrainModifier modifier) {
        requireOpen();
        if (pendingModifiers.putIfAbsent(modifier.id(), modifier) != null) {
            throw new IllegalArgumentException("Duplicate DrainModifier id: " + modifier.id());
        }
    }

    public void addLungModifier(ResourceLocation id, LungCapacityModifier modifier) {
        requireOpen();
        if (pendingLungModifiers.putIfAbsent(id, modifier) != null) {
            throw new IllegalArgumentException("Duplicate LungCapacityModifier id: " + id);
        }
    }

    private void requireOpen() {
        if (providers != null) {
            throw new IllegalStateException(
                    "Atmosphere registration is closed. Register during mod setup, not later.");
        }
    }

    /**
     * Sort once and close registration. Called at the end of common setup.
     */
    public void freeze() {
        if (providers != null) {
            return;
        }
        List<Entry> sorted = new ArrayList<>(pendingProviders.size());
        pendingProviders.forEach((id, provider) -> sorted.add(new Entry(id, provider)));
        sorted.sort(Comparator
                .comparingInt(Entry::priority).reversed()
                .thenComparing(e -> e.id().toString()));

        collectors = List.copyOf(pendingCollectors.values());
        modifiers = List.copyOf(pendingModifiers.values());
        lungModifiers = List.copyOf(pendingLungModifiers.values());
        providers = sorted.toArray(new Entry[0]);

        LOGGER.info("Atmosphere registry frozen: {} provider(s), {} collector(s), "
                        + "{} drain modifier(s), {} lung modifier(s)",
                providers.length, collectors.size(), modifiers.size(), lungModifiers.size());
    }

    // --- resolution ---------------------------------------------------------

    /**
     * First claiming provider in priority order wins.
     *
     * <p>Falls back to {@link Atmosphere#BREATHABLE} when nothing claims the position: an
     * atmosphere system that is present but silent should never suffocate anyone.
     */
    public Atmosphere resolve(AtmosphereContext context) {
        Entry[] snapshot = providers;
        if (snapshot == null) {
            return Atmosphere.BREATHABLE;
        }
        for (int i = 0; i < snapshot.length; i++) {
            Optional<Atmosphere> claim = snapshot[i].provider().query(context);
            if (claim.isPresent()) {
                warnIfAmbiguous(snapshot, i, context);
                return claim.get();
            }
        }
        return Atmosphere.BREATHABLE;
    }

    /**
     * Report the case the design document originally expected to catch at startup.
     *
     * <p>It cannot be caught there: two providers sharing a priority is perfectly legal and
     * common (every dimension baseline sits at {@code DIMENSION}), and whether they actually
     * conflict depends on the position being asked about. So the check happens here, where the
     * conflict is real.
     *
     * <p>The winner is still deterministic &mdash; ties break on id, fixed at freeze &mdash; so
     * this is a diagnostic, not a correctness fix. Logged once per pair.
     */
    private void warnIfAmbiguous(Entry[] snapshot, int winner, AtmosphereContext context) {
        int priority = snapshot[winner].priority();
        for (int i = winner + 1; i < snapshot.length && snapshot[i].priority() == priority; i++) {
            if (snapshot[i].provider().query(context).isEmpty()) {
                continue;
            }
            String pair = snapshot[winner].id() + " vs " + snapshot[i].id();
            if (reportedConflicts.add(pair)) {
                LOGGER.warn("Atmosphere conflict at priority {}: {} both claim the same position. "
                        + "'{}' wins by id ordering. Give one a different priority band.",
                        priority, pair, snapshot[winner].id());
            }
        }
    }

    // --- accessors ----------------------------------------------------------

    /** Providers in resolution order. Empty before freeze. */
    public List<Entry> providerEntries() {
        Entry[] snapshot = providers;
        return snapshot == null ? List.of() : List.of(snapshot);
    }

    public List<OxygenSourceCollector> oxygenCollectors() {
        return collectors;
    }

    public List<DrainModifier> drainModifiers() {
        return modifiers;
    }

    public List<LungCapacityModifier> lungCapacityModifiers() {
        return lungModifiers;
    }
}
