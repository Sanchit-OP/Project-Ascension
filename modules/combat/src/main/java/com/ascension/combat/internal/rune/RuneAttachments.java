package com.ascension.combat.internal.rune;

import com.ascension.combat.AscensionCombat;
import com.ascension.combat.internal.mechanics.CombatStackState;
import com.ascension.combat.internal.mechanics.DurationState;
import com.ascension.combat.internal.mechanics.StackingChargeState;
import com.ascension.combat.internal.rune.aetherwright.ArcState;
import com.ascension.combat.internal.rune.aetherwright.StarfallState;
import com.ascension.combat.internal.rune.bulwark.BraceState;
import com.ascension.combat.internal.rune.bulwark.ChallengeState;
import com.ascension.combat.internal.rune.bulwark.PlatingState;
import com.ascension.combat.internal.rune.reaver.AmbushState;
import com.ascension.combat.internal.rune.reaver.HeadshotMark;
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

    /** Deadeye's note on the shooter: which enemy the latest shot hit in the head, and when. */
    public static final Supplier<AttachmentType<HeadshotMark>> HEADSHOT = TYPES.register(
            "headshot",
            () -> AttachmentType.builder(HeadshotMark::new).build());

    /** Ambush's last hit and any priming from a dash, blink or pearl, on the player. */
    public static final Supplier<AttachmentType<AmbushState>> AMBUSH = TYPES.register(
            "ambush",
            () -> AttachmentType.builder(AmbushState::new).build());

    /** Predator's kill stacks, on the player: each is Lethality for a while, refreshed by the next kill. */
    public static final Supplier<AttachmentType<StackingChargeState>> PREDATOR = TYPES.register(
            "predator_stacks",
            () -> AttachmentType.builder(StackingChargeState::new).build());

    /** Drop In's window, on the player: until when they cut through armour. */
    public static final Supplier<AttachmentType<DurationState>> DROP_IN = TYPES.register(
            "drop_in",
            () -> AttachmentType.builder(DurationState::new).build());

    /** Vanish's window, on the player: until when mobs cannot pick them out. */
    public static final Supplier<AttachmentType<DurationState>> VANISH_WINDOW = TYPES.register(
            "vanish_window",
            () -> AttachmentType.builder(DurationState::new).build());

    /** Vanish's cooldown, on the player. */
    public static final Supplier<AttachmentType<DurationState>> VANISH_COOLDOWN = TYPES.register(
            "vanish_cooldown",
            () -> AttachmentType.builder(DurationState::new).build());

    /** Starfall's cooldown and the star in the sky, on the player. */
    public static final Supplier<AttachmentType<StarfallState>> STARFALL = TYPES.register(
            "starfall",
            () -> AttachmentType.builder(StarfallState::new).build());

    /** Arc's hit count and whether the weapon is charged, on the player. */
    public static final Supplier<AttachmentType<ArcState>> ARC = TYPES.register(
            "arc",
            () -> AttachmentType.builder(ArcState::new).build());

    /** Brace's cooldown and the brace under way, on the player. */
    public static final Supplier<AttachmentType<BraceState>> BRACE = TYPES.register(
            "brace",
            () -> AttachmentType.builder(BraceState::new).build());

    /** Plating's window and cooldown, on the player. */
    public static final Supplier<AttachmentType<PlatingState>> PLATING = TYPES.register(
            "plating",
            () -> AttachmentType.builder(PlatingState::new).build());

    /** Pack Leader's cooldown for saving a pet, on its owner. */
    public static final Supplier<AttachmentType<DurationState>> PACK_SAVE = TYPES.register(
            "pack_save",
            () -> AttachmentType.builder(DurationState::new).build());

    /** Riposte's window for a guaranteed critical hit after a parry, on the player. */
    public static final Supplier<AttachmentType<DurationState>> RIPOSTE = TYPES.register(
            "riposte",
            () -> AttachmentType.builder(DurationState::new).build());

    /** Challenge's mark, on the mob: who it must keep attacking, and until when. */
    public static final Supplier<AttachmentType<ChallengeState>> CHALLENGED = TYPES.register(
            "challenged",
            () -> AttachmentType.builder(ChallengeState::new).build());

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

    /**
     * What the HUD was last told about this player's runes, so the next tick can tell whether
     * anything changed and send only then. Not saved and not copied on death: a respawned player
     * has none, so their next check sends a fresh, correct list.
     */
    public static final Supplier<AttachmentType<java.util.List<com.ascension.combat.internal.rune.hud.RuneHudEntry>>> HUD_SENT =
            TYPES.register("hud_sent", () -> AttachmentType.<java.util.List<com.ascension.combat.internal.rune.hud.RuneHudEntry>>builder(
                    () -> java.util.List.of()).build());

    private RuneAttachments() {
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
    }
}
