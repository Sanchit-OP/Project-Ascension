package com.ascension.atmosphere.internal;

import com.ascension.atmosphere.AscensionAtmosphere;
import com.ascension.atmosphere.internal.sealed.SealedVolumeIndex;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Data attachments owned by this module. */
public final class AtmosphereAttachments {

    private static final DeferredRegister<AttachmentType<?>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, AscensionAtmosphere.MOD_ID);

    /**
     * A player's oxygen reserve.
     *
     * <p>Copied on death so respawning does not silently hand back a full tank, and so the
     * value survives dimension changes.
     */
    public static final Supplier<AttachmentType<OxygenState>> OXYGEN = TYPES.register(
            "oxygen",
            () -> AttachmentType.builder(OxygenState::new)
                    .serialize(OxygenState.CODEC)
                    .copyOnDeath()
                    .build());

    /**
     * Pressurised volumes for one level.
     *
     * <p>Deliberately not serialised. Volumes are derived data: they are recomputed from the
     * emitters themselves, so persisting them would only create a second source of truth that
     * could disagree with the blocks after a world edit.
     *
     * <p>Attached to the level rather than held in a static map keyed by dimension, so it is
     * freed when the level unloads (ADR-0007 rule 3).
     */
    public static final Supplier<AttachmentType<SealedVolumeIndex>> SEALED_VOLUMES = TYPES.register(
            "sealed_volumes",
            () -> AttachmentType.builder(() -> new SealedVolumeIndex()).build());

    private AtmosphereAttachments() {
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
    }
}
