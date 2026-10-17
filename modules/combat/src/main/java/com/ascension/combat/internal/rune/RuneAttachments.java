package com.ascension.combat.internal.rune;

import com.ascension.combat.AscensionCombat;
import com.ascension.combat.internal.mechanics.CombatStackState;
import com.ascension.combat.internal.mechanics.DurationState;
import com.ascension.combat.internal.mechanics.StackingChargeState;
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
    public static final Supplier<AttachmentType<ExposedState>> EXPOSED = TYPES.register(
            "exposed",
            () -> AttachmentType.builder(ExposedState::new).build());

    /** What the player's last melee swing was (charged, critical), for the damage event after it. */
    public static final Supplier<AttachmentType<SwingMarks>> SWING = TYPES.register(
            "swing",
            () -> AttachmentType.builder(SwingMarks::new).build());

    /** Cadence's hit count on one enemy, on the attacker. */
    public static final Supplier<AttachmentType<CadenceState>> CADENCE = TYPES.register(
            "cadence",
            () -> AttachmentType.builder(CadenceState::new).build());

    /**
     * Conqueror's Resolve's stack, one a second in combat, on the player (the mechanic Protean
     * Mask's {@code CombatStackState} was built for; this is its second consumer).
     */
    public static final Supplier<AttachmentType<CombatStackState>> CONQUERORS_RESOLVE = TYPES.register(
            "conquerors_resolve_state",
            () -> AttachmentType.builder(CombatStackState::new).build());

    /** Relentless Strikes' hit stack, on the player: builds per hit, fades if the next doesn't come in time. */
    public static final Supplier<AttachmentType<StackingChargeState>> RELENTLESS_STRIKES = TYPES.register(
            "relentless_strikes_stacks",
            () -> AttachmentType.builder(StackingChargeState::new).build());

    /** Momentum's chained-hit stack, on the player: the same shape as Relentless Strikes, scaling a heal. */
    public static final Supplier<AttachmentType<StackingChargeState>> MOMENTUM = TYPES.register(
            "momentum_stacks",
            () -> AttachmentType.builder(StackingChargeState::new).build());

    /** Second Wind's gap between heals, on the player: a {@link DurationState} used as a cooldown gate. */
    public static final Supplier<AttachmentType<DurationState>> SECOND_WIND_COOLDOWN = TYPES.register(
            "second_wind_cooldown",
            () -> AttachmentType.builder(DurationState::new).build());

    /** Killing Surge's attack-speed window after a kill, on the player. */
    public static final Supplier<AttachmentType<DurationState>> KILLING_SURGE_BURST = TYPES.register(
            "killing_surge_burst",
            () -> AttachmentType.builder(DurationState::new).build());

    private RuneAttachments() {
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
    }
}
