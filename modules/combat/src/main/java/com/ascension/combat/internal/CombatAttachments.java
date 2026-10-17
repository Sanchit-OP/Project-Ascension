package com.ascension.combat.internal;

import com.ascension.combat.AscensionCombat;
import com.ascension.combat.internal.armor.UnendingDespairState;
import com.ascension.combat.internal.mechanics.ArmedState;
import com.ascension.combat.internal.mechanics.CombatMobEffects;
import com.ascension.combat.internal.mechanics.CombatStackState;
import com.ascension.combat.internal.mechanics.DurationState;
import com.ascension.combat.internal.mechanics.LastDamageTarget;
import com.ascension.combat.internal.mechanics.PausableCooldown;
import com.ascension.combat.internal.mechanics.StackingChargeState;
import com.ascension.combat.internal.rune.RunePageState;
import com.ascension.combat.internal.weapons.RagefangState;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Data attachments owned by this module. */
public final class CombatAttachments {

    private static final DeferredRegister<AttachmentType<?>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, AscensionCombat.MOD_ID);

    /**
     * Darius's Axe's stacking armor shred (docs/technical/combat-api.md section 6), attached to
     * the target carrying the debuff, not the attacker.
     *
     * <p>Not serialised and not copied on death: fight-local state that should not outlive a
     * world reload or a respawn, the same reasoning {@code ascension-atmosphere} gives for not
     * persisting its own {@code SEALED_VOLUMES}.
     */
    public static final Supplier<AttachmentType<StackingChargeState>> ARMOR_SHRED = TYPES.register(
            "armor_shred",
            () -> AttachmentType.builder(StackingChargeState::new).build());

    /**
     * Spear of Shojin's consecutive-strike counter (docs/technical/combat-api.md section 6),
     * attached to the attacker rather than the target &mdash; unlike the shred, this is the
     * wielder's own build-up, and it follows them regardless of what they're hitting.
     *
     * <p>Same non-serialization reasoning as {@link #ARMOR_SHRED}.
     */
    public static final Supplier<AttachmentType<StackingChargeState>> SHOJIN_STACKS = TYPES.register(
            "shojin_stacks",
            () -> AttachmentType.builder(StackingChargeState::new).build());

    /**
     * Divine Sunderer's primed-next-attack flag (docs/technical/combat-api.md section 6),
     * attached to the wielder. Set by the active, consumed by the next hit that lands with the
     * weapon still equipped.
     *
     * <p>Same non-serialization reasoning as {@link #ARMOR_SHRED}: if the primed state doesn't
     * survive a world reload, the worst case is losing an already-armed charge, not a
     * correctness bug.
     */
    public static final Supplier<AttachmentType<ArmedState>> DIVINE_SUNDERER_PRIMED = TYPES.register(
            "divine_sunderer_primed",
            () -> AttachmentType.builder(ArmedState::new).build());

    /**
     * Darken Blade's alternating-hit toggle (docs/technical/combat-api.md section 6), attached to
     * the wielder: every other hit arms it, the hit after that consumes it and (cooldown
     * permitting) triggers the burst.
     *
     * <p>Same non-serialization reasoning as {@link #ARMOR_SHRED}.
     */
    public static final Supplier<AttachmentType<ArmedState>> DARKEN_BLADE_ARMED = TYPES.register(
            "darken_blade_armed",
            () -> AttachmentType.builder(ArmedState::new).build());

    /**
     * Ragefang's attack-speed build-up and every-third-hit counter (docs/technical/combat-api.md
     * section 6), attached to the wielder. Not {@link StackingChargeState}: that class expires
     * in one binary step, but Ragefang's stacks ramp down continuously over several seconds, so
     * it needed its own shape. See {@link com.ascension.combat.internal.weapons.RagefangEffects}.
     */
    public static final Supplier<AttachmentType<RagefangState>> RAGEFANG_STATE = TYPES.register(
            "ragefang_state",
            () -> AttachmentType.builder(RagefangState::new).build());

    /**
     * Wraithblade's post-blink speed-boost window (docs/technical/combat-api.md section 6),
     * attached to the wielder: the active blinks them forward, then grants bonus move speed for a
     * short duration.
     *
     * <p>Same non-serialization reasoning as {@link #ARMOR_SHRED}.
     */
    public static final Supplier<AttachmentType<DurationState>> WRAITHBLADE_SPEED_BOOST = TYPES.register(
            "wraithblade_speed_boost",
            () -> AttachmentType.builder(DurationState::new).build());

    /**
     * Heartwood of Pallas's unlimited-stacking corruption (docs/technical/combat-api.md section
     * 6), attached to the target carrying it, same shape and reasoning as {@link #ARMOR_SHRED}
     * except with no stack cap &mdash; passed {@code Integer.MAX_VALUE} as the cap at the call
     * site rather than needing a second class.
     */
    public static final Supplier<AttachmentType<StackingChargeState>> HEARTWOOD_CORRUPTION = TYPES.register(
            "heartwood_corruption",
            () -> AttachmentType.builder(StackingChargeState::new).build());

    /**
     * Heartwood of Pallas's "the next arrow I loose should be charged" flag, attached to the
     * wielder between {@code ArrowLooseEvent} (where the draw time is known) and the arrow
     * actually joining the level (where the wielder's held item may already have changed) &mdash;
     * see
     * {@link com.ascension.combat.internal.weapons.HeartwoodOfPallasEffects} for why those two
     * moments need a bridge.
     *
     * <p>Same non-serialization reasoning as {@link #ARMOR_SHRED}.
     */
    public static final Supplier<AttachmentType<ArmedState>> HEARTWOOD_CHARGE_PENDING = TYPES.register(
            "heartwood_charge_pending",
            () -> AttachmentType.builder(ArmedState::new).build());

    /**
     * Heartwood of Pallas's "this specific arrow is a charged shot" flag, attached to the arrow
     * entity itself rather than the wielder &mdash; the arrow may still be in flight, or may land
     * after the wielder has fired another (uncharged) shot, so the charge has to travel with the
     * projectile, not sit on the player.
     *
     * <p>Same non-serialization reasoning as {@link #ARMOR_SHRED}.
     */
    public static final Supplier<AttachmentType<ArmedState>> HEARTWOOD_CHARGED_ARROW = TYPES.register(
            "heartwood_charged_arrow",
            () -> AttachmentType.builder(ArmedState::new).build());

    /**
     * Heartwood of Pallas's charged-shot cooldown, attached to the wielder &mdash; deliberately
     * <em>not</em> vanilla {@code ItemCooldowns}, despite every other actived weapon in this
     * catalog using exactly that: {@code ItemCooldowns} gates the whole item, not just this one
     * ability, so putting the 15s cooldown there blocked ordinary uncharged shooting for the full
     * duration too (confirmed in vanilla's own {@code ServerPlayerGameMode}, which skips {@code
     * Item#use} outright while a stack's item is on cooldown). This tracks only whether a charged
     * shot can be armed; {@link com.ascension.combat.client.HeartwoodCooldownDecorator} draws the
     * matching hotbar swipe by hand instead, fed by {@link
     * com.ascension.combat.internal.net.HeartwoodCooldownSyncPayload}.
     *
     * <p>Same non-serialization reasoning as {@link #ARMOR_SHRED}.
     */
    public static final Supplier<AttachmentType<DurationState>> HEARTWOOD_CHARGE_COOLDOWN = TYPES.register(
            "heartwood_charge_cooldown",
            () -> AttachmentType.builder(DurationState::new).build());

    /**
     * Galeforce's dash-and-shot cooldown, attached to the wielder &mdash; same reasoning as {@link
     * #HEARTWOOD_CHARGE_COOLDOWN}: vanilla {@code ItemCooldowns} would block ordinary shooting for
     * the cooldown's full duration too, which is exactly what Sanchit's own test found ("unusable
     * after ability"). Same non-serialization reasoning as {@link #ARMOR_SHRED}.
     */
    public static final Supplier<AttachmentType<DurationState>> GALEFORCE_COOLDOWN = TYPES.register(
            "galeforce_cooldown",
            () -> AttachmentType.builder(DurationState::new).build());

    /**
     * Galeforce's "this specific arrow is the empowered dash-shot" flag (docs/technical/combat-api.md
     * section 6), attached to the arrow entity itself, not the wielder &mdash; the dash's shot is
     * spawned directly rather than riding the wielder's next normal draw, so there's exactly one
     * arrow this ever needs to be true for, and tagging it directly avoids any risk of a later,
     * unrelated shot picking up the bonus instead.
     *
     * <p>Same non-serialization reasoning as {@link #ARMOR_SHRED}.
     */
    public static final Supplier<AttachmentType<ArmedState>> GALEFORCE_EMPOWERED = TYPES.register(
            "galeforce_empowered",
            () -> AttachmentType.builder(ArmedState::new).build());

    /**
     * Galeforce's homing-target memory (docs/technical/combat-api.md section 6), attached to the
     * wielder: whoever they most recently dealt damage to, with any weapon, within the catalog's
     * usual 5s window &mdash; the dash's shot prefers this target over scanning for the nearest
     * low-HP alternative.
     *
     * <p>Same non-serialization reasoning as {@link #ARMOR_SHRED}.
     */
    public static final Supplier<AttachmentType<LastDamageTarget>> GALEFORCE_LAST_TARGET = TYPES.register(
            "galeforce_last_target",
            () -> AttachmentType.builder(LastDamageTarget::new).build());

    /**
     * The combat-state rolling window (docs/technical/combat-api.md section 4), attached to the
     * player: refreshed by {@link com.ascension.combat.internal.mechanics.CombatStateTracking} on
     * every damage dealt or received, read by {@code core}'s {@code CombatStateRegistry}.
     *
     * <p>Same non-serialization reasoning as {@link #ARMOR_SHRED}.
     */
    public static final Supplier<AttachmentType<DurationState>> COMBAT_STATE = TYPES.register(
            "combat_state",
            () -> AttachmentType.builder(DurationState::new).build());

    /**
     * The Protean Mask's resistance-stack counter (docs/technical/combat-api.md section 6),
     * attached to the wearer.
     *
     * <p>Same non-serialization reasoning as {@link #ARMOR_SHRED}.
     */
    public static final Supplier<AttachmentType<CombatStackState>> PROTEAN_MASK_STATE = TYPES.register(
            "protean_mask_state",
            () -> AttachmentType.builder(CombatStackState::new).build());

    /**
     * Edge of Night's post-dodge burst window (docs/technical/combat-api.md section 6), attached
     * to the wearer: set the instant their immunity ({@link #EDGE_OF_NIGHT_IMMUNITY}) negates a
     * hit, read by both the move-speed modifier and the temporary lethality check while active.
     *
     * <p>Same non-serialization reasoning as {@link #ARMOR_SHRED}.
     */
    public static final Supplier<AttachmentType<DurationState>> EDGE_OF_NIGHT_BURST = TYPES.register(
            "edge_of_night_burst",
            () -> AttachmentType.builder(DurationState::new).build());

    /**
     * Edge of Night's "the next hit is fully negated" flag (docs/technical/combat-api.md section
     * 6), attached to the wearer &mdash; reworked 2026-09-20 from a vanilla-absorption shield
     * (which capped at a small flat amount) into a true one-hit dodge: armed after a few seconds
     * out of combat, consumed entirely by the next incoming hit regardless of its size or damage
     * type, the same "whatever that attack could have been" spec Sanchit gave. Same shape as
     * {@link #DIVINE_SUNDERER_PRIMED}, just on the defending side instead of the attacking one.
     *
     * <p>Same non-serialization reasoning as {@link #ARMOR_SHRED}.
     */
    public static final Supplier<AttachmentType<ArmedState>> EDGE_OF_NIGHT_IMMUNITY = TYPES.register(
            "edge_of_night_immunity",
            () -> AttachmentType.builder(ArmedState::new).build());

    /**
     * Guardian's Wing's own death-save cooldown (docs/technical/combat-api.md section 6), attached
     * to the wearer &mdash; deliberately not vanilla {@code ItemCooldowns} (see {@link
     * PausableCooldown}'s own javadoc): it should only count against a player actually wearing the
     * chestplate, not tick down in the background while it sits unequipped in their inventory.
     *
     * <p>Same non-serialization reasoning as {@link #ARMOR_SHRED}.
     */
    public static final Supplier<AttachmentType<PausableCooldown>> GUARDIAN_WING_COOLDOWN = TYPES.register(
            "guardian_wing_cooldown",
            () -> AttachmentType.builder(PausableCooldown::new).build());

    /**
     * Guardian's Wing's stasis window (docs/technical/combat-api.md sections 5 and 6), attached to
     * the wearer: activated instead of dying, read every tick while it's active and once more on
     * the tick it lapses to trigger the revive.
     *
     * <p>Same non-serialization reasoning as {@link #ARMOR_SHRED}.
     */
    public static final Supplier<AttachmentType<DurationState>> GUARDIAN_WING_STASIS = TYPES.register(
            "guardian_wing_stasis",
            () -> AttachmentType.builder(DurationState::new).build());

    /**
     * Rocketbelt's pending landing-burst timer (docs/technical/combat-api.md section 6), attached
     * to the wearer: set to a short delay by the dash, read once via {@link
     * DurationState#hasExpired} to trigger the area burst at wherever the wearer ends up.
     *
     * <p>Same non-serialization reasoning as {@link #ARMOR_SHRED}.
     */
    public static final Supplier<AttachmentType<DurationState>> ROCKETBELT_BURST = TYPES.register(
            "rocketbelt_burst",
            () -> AttachmentType.builder(DurationState::new).build());

    /**
     * Unending Despair's continuous-time-in-combat tracker (docs/technical/combat-api.md section
     * 6), attached to the wearer.
     *
     * <p>Same non-serialization reasoning as {@link #ARMOR_SHRED}.
     */
    public static final Supplier<AttachmentType<UnendingDespairState>> UNENDING_DESPAIR_STATE = TYPES.register(
            "unending_despair_state",
            () -> AttachmentType.builder(UnendingDespairState::new).build());

    /**
     * Zonia's Hourglass's stasis window (docs/technical/combat-api.md sections 5 and 6), attached
     * to the wearer: the second real consumer of {@link
     * com.ascension.combat.internal.mechanics.Stasis}, after {@link #GUARDIAN_WING_STASIS}.
     * Deliberately a separate attachment rather than reusing that one &mdash; the two items have
     * independent triggers, durations, and cooldowns, even though the stasis mechanics themselves
     * are shared.
     *
     * <p>Same non-serialization reasoning as {@link #ARMOR_SHRED}.
     */
    public static final Supplier<AttachmentType<DurationState>> ZONIAS_HOURGLASS_STASIS = TYPES.register(
            "zonias_hourglass_stasis",
            () -> AttachmentType.builder(DurationState::new).build());

    /**
     * Sterak's Harness's post-shield lifesteal window (docs/technical/combat-api.md section 6),
     * attached to the wearer: armed when the auto-shield procs. Reworked 2026-09-20 from a fixed
     * 10s {@link DurationState} into an {@link ArmedState} that lasts until the wearer leaves
     * combat instead (Sanchit's own spec: "10% life steal till we are out of combat") &mdash; a
     * fixed timer doesn't fit "until X happens," only "until this game tick," so this needed the
     * consumed-on-exit shape instead. See {@code SterakHarnessEffects} for where it's consumed.
     *
     * <p>Same non-serialization reasoning as {@link #ARMOR_SHRED}.
     */
    public static final Supplier<AttachmentType<ArmedState>> STERAKS_LIFESTEAL = TYPES.register(
            "steraks_lifesteal",
            () -> AttachmentType.builder(ArmedState::new).build());

    /**
     * A player's saved rune page and its growing runes' growth (docs/technical/rune-system.md
     * sections 3 and 4.5). Unlike the fight-local state above, meant to persist: a rune choice is a
     * build decision, not combat bookkeeping, so this one attachment is serialized, and copied on
     * death so dying doesn't wipe the page. (Before 2026-10-17 it wasn't copied, and every death
     * silently cleared the player's runes.)
     */
    public static final Supplier<AttachmentType<RunePageState>> RUNE_PAGE = TYPES.register(
            "rune_page",
            () -> AttachmentType.builder(RunePageState::new)
                    .serialize(RunePageState.SAVED_CODEC)
                    .copyOnDeath()
                    .build());

    /**
     * Grievous Wounds' real heal-block gate, attached to the target carrying it. Not the vanilla
     * {@code MobEffectInstance} {@link CombatMobEffects#GRIEVOUS_WOUNDS} still applies alongside
     * this for the status icon &mdash; that effect is cosmetic now, because {@code addEffect} is
     * routinely a no-op on exactly the targets this item cares most about. Confirmed live
     * 2026-09-21: the Wither's own {@code addEffect} unconditionally returns {@code false}, and
     * every Cataclysm boss extends a shared base class whose {@code canBeAffected} only admits
     * effects tagged {@code cataclysm:effective_for_bosses} &mdash; a common, reasonable
     * "bosses ignore arbitrary debuffs" convention that just happens to make a vanilla
     * {@code MobEffect} the wrong tool for a mechanic that specifically exists to matter against
     * tough, healing enemies. This attachment bypasses that path entirely: nothing about it asks
     * the target's permission.
     *
     * <p>Same non-serialization reasoning as {@link #ARMOR_SHRED}.
     */
    public static final Supplier<AttachmentType<DurationState>> GRIEVOUS_WOUNDS_STATE = TYPES.register(
            "grievous_wounds_state",
            () -> AttachmentType.builder(DurationState::new).build());

    private CombatAttachments() {
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
    }
}
