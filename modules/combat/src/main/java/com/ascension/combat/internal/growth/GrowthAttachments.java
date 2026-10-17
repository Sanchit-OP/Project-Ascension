package com.ascension.combat.internal.growth;

import com.ascension.combat.AscensionCombat;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Growth's two saved attachments (docs/progression/growth.md section 9). Kept apart from
 * {@code CombatAttachments}, whose entries are almost all fight-local and unsaved, so growth stays
 * in its own package.
 */
public final class GrowthAttachments {

    private static final DeferredRegister<AttachmentType<?>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, AscensionCombat.MOD_ID);

    /** A player's taken nodes. Saved, and copied through death: growth is permanent. */
    public static final Supplier<AttachmentType<GrowthState>> GROWTH = TYPES.register(
            "growth",
            () -> AttachmentType.builder(GrowthState::new)
                    .serialize(GrowthState.CODEC)
                    .copyOnDeath()
                    .build());

    /** The team's shared milestones, on the Overworld. Saved with the level. */
    public static final Supplier<AttachmentType<TeamGrowthState>> TEAM_GROWTH = TYPES.register(
            "team_growth",
            () -> AttachmentType.builder(TeamGrowthState::new)
                    .serialize(TeamGrowthState.CODEC)
                    .build());

    private GrowthAttachments() {
    }

    static void register(IEventBus modBus) {
        TYPES.register(modBus);
    }
}
