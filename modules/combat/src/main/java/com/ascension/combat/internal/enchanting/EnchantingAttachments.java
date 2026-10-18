package com.ascension.combat.internal.enchanting;

import com.ascension.combat.AscensionCombat;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Attachments of the enchanting work, apart from the item and rune ones so the sessions don't share a file. */
public final class EnchantingAttachments {

    private static final DeferredRegister<AttachmentType<?>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, AscensionCombat.MOD_ID);

    /** Soulbound items waiting for their owner's respawn. Saved, and copied from the dead player to the new one. */
    public static final Supplier<AttachmentType<KeptItems>> KEPT_ITEMS = TYPES.register(
            "kept_items",
            () -> AttachmentType.builder(() -> new KeptItems())
                    .serialize(KeptItems.CODEC)
                    .copyOnDeath()
                    .build());

    private EnchantingAttachments() {
    }

    static void register(IEventBus modBus) {
        TYPES.register(modBus);
    }
}
