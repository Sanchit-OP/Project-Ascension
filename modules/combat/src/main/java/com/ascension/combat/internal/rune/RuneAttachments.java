package com.ascension.combat.internal.rune;

import com.ascension.combat.AscensionCombat;
import com.ascension.combat.internal.mechanics.DurationState;
import com.ascension.combat.internal.rune.vanguard.CadenceState;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Attachments that belong to runes' behaviour, kept out of {@link
 * com.ascension.combat.internal.CombatAttachments} so the rune work and the item work don't share
 * a file. (The page itself, {@code RUNE_PAGE}, stays there: it was already there.)
 *
 * <p>None of these is serialised or copied on death: they are fight-local, the same reasoning as
 * every other fight-local attachment in this module.
 */
public final class RuneAttachments {

    private static final DeferredRegister<AttachmentType<?>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, AscensionCombat.MOD_ID);

    /**
     * Cadence's Exposed, on the <em>target</em>: it takes more damage from everyone until the
     * window ends ({@link ExposedDamage}).
     *
     * <p>An attachment and not a vanilla mob effect, on purpose and for the reason Grievous Wounds
     * already found: the Wither and Cataclysm's bosses refuse ordinary effects, and Cadence's
     * payoff matters most against exactly those.
     */
    public static final Supplier<AttachmentType<DurationState>> EXPOSED = TYPES.register(
            "exposed",
            () -> AttachmentType.builder(DurationState::new).build());

    /** Cadence's hit count on one enemy, on the attacker. */
    public static final Supplier<AttachmentType<CadenceState>> CADENCE = TYPES.register(
            "cadence",
            () -> AttachmentType.builder(CadenceState::new).build());

    private RuneAttachments() {
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
    }
}
