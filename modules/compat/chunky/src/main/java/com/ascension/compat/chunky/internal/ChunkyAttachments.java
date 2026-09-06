package com.ascension.compat.chunky.internal;

import com.ascension.compat.chunky.AscensionCompatChunky;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Data attachments owned by this module. */
public final class ChunkyAttachments {

    private static final DeferredRegister<AttachmentType<?>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, AscensionCompatChunky.MOD_ID);

    /**
     * Whether a level has already had a pre-generation task started for it. Per level, not a
     * static set (ADR-0007 rule 3) &mdash; a level attachment is exactly the right lifetime for
     * "have we done this yet", and it never holds a {@code Level} reference itself, just a
     * {@code boolean}.
     */
    public static final Supplier<AttachmentType<PregenMarker>> PREGEN_MARKER = TYPES.register(
            "pregen_marker",
            () -> AttachmentType.builder(PregenMarker::new).serialize(PregenMarker.CODEC).build());

    private ChunkyAttachments() {
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
    }
}
