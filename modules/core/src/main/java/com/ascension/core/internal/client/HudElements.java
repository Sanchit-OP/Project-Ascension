package com.ascension.core.internal.client;

import com.ascension.core.api.client.HudElement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds registered HUD elements.
 *
 * <p>The same two-phase shape as {@code WorldEnvironments}: registration mutates a concurrent
 * map during setup, and at freeze the entries are sorted once into an immutable list. After that
 * a host reads the one list per frame and nothing sorts or allocates (ADR-0007).
 *
 * <p>Elements are client objects. They are only ever drawn, never stored against a world, and
 * hold no {@code Level}, {@code Player} or {@code Entity} themselves.
 */
public final class HudElements {

    private static final Logger LOGGER = LoggerFactory.getLogger(HudElements.class);
    private static final HudElements INSTANCE = new HudElements();

    private record Entry(ResourceLocation id, HudElement element) {
    }

    private final Map<ResourceLocation, HudElement> pending = new ConcurrentHashMap<>();

    /** Null until frozen, which is also how "still open" is tested. */
    private volatile List<Entry> sorted;
    private volatile List<HudElement> elements = List.of();
    private volatile List<ResourceLocation> ids = List.of();

    private volatile ResourceLocation host;

    /** Package-private so tests can build their own instance. */
    HudElements() {
    }

    public static HudElements get() {
        return INSTANCE;
    }

    public void add(ResourceLocation id, HudElement element) {
        if (sorted != null) {
            throw new IllegalStateException(
                    "HUD registration is closed. Register during client setup, not later. "
                            + "Attempted id: " + id);
        }
        if (pending.putIfAbsent(id, element) != null) {
            throw new IllegalArgumentException("Duplicate HudElement id: " + id);
        }
    }

    public void declareHost(ResourceLocation hostId) {
        ResourceLocation previous = host;
        host = hostId;
        if (previous != null && !previous.equals(hostId)) {
            LOGGER.warn("Two HUD hosts declared themselves: {} and {}. Both will draw.",
                    previous, hostId);
        }
    }

    public boolean hostPresent() {
        return host != null;
    }

    /** Sort once by order, then id, and close registration. Idempotent. */
    public void freeze() {
        if (sorted != null) {
            return;
        }
        List<Entry> list = new ArrayList<>(pending.size());
        pending.forEach((id, element) -> list.add(new Entry(id, element)));
        list.sort(Comparator.<Entry>comparingInt(e -> e.element().order())
                .thenComparing(e -> e.id().toString()));
        List<HudElement> els = new ArrayList<>(list.size());
        List<ResourceLocation> idList = new ArrayList<>(list.size());
        for (Entry e : list) {
            els.add(e.element());
            idList.add(e.id());
        }
        elements = List.copyOf(els);
        ids = List.copyOf(idList);
        sorted = list;
        LOGGER.info("HUD registry frozen: {} element(s)", list.size());
    }

    public List<HudElement> elements() {
        return elements;
    }

    public List<ResourceLocation> ids() {
        return ids;
    }
}
