package com.ascension.atmosphere.internal;

import com.ascension.atmosphere.AscensionAtmosphere;
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

    private AtmosphereAttachments() {
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
    }
}
