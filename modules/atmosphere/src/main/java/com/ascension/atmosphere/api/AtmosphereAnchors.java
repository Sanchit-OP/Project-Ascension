package com.ascension.atmosphere.api;

import com.ascension.atmosphere.internal.AtmosphereAttachments;
import com.ascension.atmosphere.internal.sealed.SealedVolumeEvents;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.server.level.ServerLevel;

/**
 * Where a mod that owns a moving structure (a ship) tells this module about it (ADR-0026).
 *
 * <p>A pressurised room inside a ship is stored in the ship's own block space, so a player
 * standing in it is asked about by translating their world position through the registered
 * {@link AnchorSpace}s. Registered spaces are stateless and hold no level, entity or block
 * entity (ADR-0007). Register once, at setup.
 */
public final class AtmosphereAnchors {

    private static final List<AnchorSpace> SPACES = new CopyOnWriteArrayList<>();

    private AtmosphereAnchors() {
    }

    public static void register(AnchorSpace space) {
        SPACES.add(space);
    }

    /** The registered spaces, for the sealed-room provider. */
    public static List<AnchorSpace> spaces() {
        return SPACES;
    }

    /**
     * Ask for every room in this level to be worked out again, after something moved blocks in bulk
     * with no block events (a ship assembled, or loaded into the level). Done a tick later, on the
     * server thread. Cheap when the level has no emitters.
     */
    public static void refresh(ServerLevel level) {
        if (level.getData(AtmosphereAttachments.SEALED_VOLUMES).isEmpty()) {
            return;
        }
        SealedVolumeEvents.scheduleRecomputeAll(level);
    }
}
