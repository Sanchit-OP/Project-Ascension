# `ascension-combat` API design

> **Status: drafted and reviewed 2026-09-08**, through direct design discussion with Sanchit
> rather than a separate review pass — the same weight M1.1 and M2.1 gave a written-before-code
> API, compressed into one session. Structure is settled; the items marked open below are named
> explicitly rather than silently deferred.
>
> Settled going in: [ADR-0003](../decisions/0003-modular-architecture-and-compatibility-policy.md)
> (provider registries, no third-party Tier 1 dependency), [ADR-0011](../decisions/0011-tier-1-modules-share-contracts-through-core.md)
> (Tier 1 modules share contracts through `core`), [ADR-0012](../decisions/0012-ascension-combat-module.md)
> (this module, the damage-type split, and the `core` combat-state contract).

## 1. What this module owns

Stats, items, and rune paths — a build-defining layer on top of vanilla combat. It does **not**
own suit modules or hazard protection (`ascension-gear`'s job), team capability gating
(`ascension-progression`'s job), or planetary environment (`ascension-worlds`'s job). Where this
module needs to know about those — a rune reading oxygen state, say — it goes through a `core`
contract, never a direct dependency, per ADR-0011.

## 2. Damage types

Three categories, not two:

| Type | Reduced by | Source |
|---|---|---|
| **Physical** | Armor | Melee, arrows, most mob attacks — the vanilla default |
| **Magic** | Magic resist | A small number of specific weapon procs, never a cast spell |
| **True** | Nothing | Spear of Shojin's stacking payoff only, for now |

**Deliberately no spellcasting system.** Adding one would mean a whole new input/targeting/cast-bar
system nobody asked for. Instead, magic damage is tuned into the *procs* of a few otherwise-physical
weapons: Ragefang's stacking discharge, Darken Blade's burst proc, and Heartwood of Pallas's
corruption consumption. Base hits from those weapons stay physical; only the proc is magic. This is
also what gives the Aetherwright rune path (§7) something real to amplify, without inventing a
category of item that doesn't otherwise exist here.

## 3. The stat model

**Confirmed base stats:** HP, Attack Damage, Attack Speed, Move Speed, Armor, Magic Resist,
Lifesteal, Omnivamp.

**Supporting stats/mechanics**, needed by the items in §6 and carried forward without objection
during design:

- **Armor penetration / lethality** and **magic penetration** — flat or percentage reduction of
  the opposing mitigation stat.
- **Grievous wounds** — a status effect, not a stat: reduces lifesteal/omnivamp healing received
  by whoever carries it.
- **Shield / absorb** — a shared temporary-HP mechanic (§5).
- **Stasis** — a shared untargetable-and-immobile state (§5).
- **Phasing** — a shared temporary ignore-entity-collision state (§5).

All numeric values — percentages, cooldowns, durations, stack caps — are **explicitly
unbalanced at this stage**. Sanchit is balancing everything himself by playing, not by spec. Every
number in this document and in the item catalog below is a placeholder for "this exists," not "this
is correct" — which is exactly what ADR-0007 rule 10's data-driven requirement is for: retuning a
value is a JSON edit, not a rebuild.

## 4. Combat state — the `core` contract

Several items and one rune (§6, §7) key behaviour off whether a player is currently fighting: the
Protean Mask gains a stack every second in combat, Unending Despair triggers after ten seconds in
the same fight, and the proposed Bulwark keystone increases oxygen drain while in combat. That last
one is why this can't stay a private concept inside `ascension-combat` — `ascension-atmosphere`'s
`DrainModifier` registry needs to read it, and per ADR-0011 that means the contract lives in `core`.

```java
// com.ascension.core.api
@FunctionalInterface
public interface CombatStateSource {
    boolean isInCombat(ServerPlayer player);
}

public final class CombatStateRegistry {
    public static void register(CombatStateSource source);
    public static boolean isInCombat(ServerPlayer player);
}
```

**Deliberately single-source, unlike `WorldEnvironmentRegistry`.** Whether a player is in combat is
not a "several things might claim this position, highest priority wins" question the way
atmosphere or world environment are — exactly one module decides what "in combat" means and
tracks it. `register` throws if a second source tries to claim it. `isInCombat` returns `false`
when nothing has registered, which is the safe default: nothing should escalate combat-only
behaviour on a claim nobody made.

**The contract is the only thing that lives in `core`.** The event listener that actually watches
for damage dealt and damage received, decides the rolling window (a default of 5 seconds since the
last combat action is proposed below, matching the window Heartwood of Pallas already uses for its
own corruption refresh — a starting point, not a locked number), and registers the source, lives
entirely in `ascension-combat`. Per ADR-0011's own table, `core` may hold an attachment type or a
query interface; it may never hold "an event handler that makes something happen." A damage-event
listener is exactly that, so it does not go here regardless of how small it is.

**Definition, as given:** entering combat on dealing *or* receiving damage. Window length and
whether specific damage sources (fall damage, fire tick with no attacking entity) should count are
open for Sanchit's own tuning — see §9.

## 5. Shared mechanics

Built once, used by more than one item, rather than reimplemented per item:

| Mechanic | Description | Used by |
|---|---|---|
| **Shield / Absorb** | Temporary HP pool that soaks damage before real HP | Gargoyle Stoneplate, Sterak's Harness, Edge of Night, Darken Blade |
| **Stasis** | Untargetable, immobile, camera forced to third person | Zonia's Hourglass, Guardian's Wing |
| **Phasing** | Temporary ignore-entity-collision, mobs only, never blocks | *Dropped 2026-09-08 — no NeoForge event exposes vanilla's entity-push, and the only field that suppresses it (`Entity#noPhysics`) also disables block collision. The clean fix (overriding `Player#isPushable()`) needs a mixin, banned in Tier 1 by ADR-0003 rule 2. Wraithblade's active is a blink + move-speed boost instead. A vanilla `Team`/`CollisionRule.NEVER` approach was also identified as technically workable (no mixin needed) but not built — Sanchit chose the blink instead.* |
| **True damage** | Third damage category (§2), bypasses armor and magic resist entirely | Spear of Shojin |

## 6. Item catalog

Every entry: original name, slot, damage type(s), and the mechanic it keeps close to its source
without porting exact numbers. `ORIGINAL` items have no direct LoL analog. Lore/flavor text is
Sanchit's to write — this catalog is mechanics only.

### Weapons — melee (8)

| Item | Damage | Mechanic |
|---|---|---|
| **Veigo's Blade** | Physical | On-hit: bonus physical damage as a % of the target's *current* HP. Active: steal a burst of the target's move speed while slowing them. *Anti-tank family.* |
| **Darken Blade** (Aatrox's Sword) | Physical + Magic | Alternating hits build toward a burst proc dealing magic damage based on the target's max HP; the proc shields you, on an 8s cooldown. |
| **Darius's Axe** (Black Cleaver) | Physical | Each hit stacks a shred that reduces the target's armor, compounding across the fight. *Anti-tank family.* |
| **Endless Hunger** | Physical | Two-handed. Attack speed scales off attack damage instead of a flat rating. Omnivamp grows the lower your own HP falls. `ORIGINAL`. |
| **Ragefang** (Guinsoo's Rageblade) | Physical + Magic | Hits build stacks of bonus attack speed; every few hits the stacks discharge as a burst of magic damage. |
| **Spear of Shojin** | True | No ability haste in this game, so reworked entirely: consecutive on-hit strikes build a stack that resolves as bonus true damage. |
| **Wraithblade** (Youmuu's Ghostblade) | Physical | Passive: flat armor penetration (lethality). Active: blink forward a short distance, then a move-speed boost for a few seconds. On a cooldown. Revised 2026-09-08 — see §5's Phasing entry. |
| **Divine Sunderer** | Physical | Active, on cooldown: primes the next attack to deal bonus physical damage as a % of the target's *max* HP, and heals you for the same amount. *Anti-tank family.* |

### Weapons — ranged (3)

| Item | Damage | Mechanic |
|---|---|---|
| **Mortal Reminder** | Physical | Ignores a % of the target's armor; on-hit applies grievous wounds, refreshed by every subsequent hit. *Anti-tank family.* |
| **Heartwood of Pallas** (Varus's bow, sealed at the Temple of Pallas) | Physical + Magic | On-hit: physical + small magic damage, and a stack of unlimited-stacking corruption, refreshed while the target's been hit in the last 5s. Active: draw for 3x normal time; a landed shot pierces and consumes every stack as magic damage equal to the target's missing HP. A miss consumes nothing. |
| **Galeforce** | Physical | Active (sneak + right-click): a real, collidable dash that fires a homing shot at the wielder's last damage target or the lowest-HP nearby entity, dealing bonus damage scaling with the target's missing HP. Revised 2026-09-08. |

### Helmet (2)

| Item | Mechanic |
|---|---|
| **The Protean Mask** (Jak'Sho) | Gains a resistance stack every second in combat (§4), up to 8. Full stacks double the bonus, and that moment carries a 0.5% chance to backfire for half your max HP in self-damage. |
| **Edge of Night** | Regenerates a shield (§5) after a few seconds out of combat. Absorbing a hit triggers a burst of move speed and lethality. |

### Chestplate (2)

| Item | Mechanic |
|---|---|
| **Gargoyle Stoneplate** | Flat armor + magic resist. Active: shield (§5) scaling with nearby enemies. |
| **Guardian's Wing** (Guardian Angel) | On death, while off cooldown: stasis (§5) then revive at half HP. |

### Leggings (3)

| Item | Mechanic |
|---|---|
| **Rocketbelt** (Hextech Rocketbelt) | Active: short dash into an area burst of damage on landing. |
| **Collector's Pants** | Damage below 5% HP executes outright; a kill this way counts as one Looting level higher than the weapon that landed it. Inspired by The Collector. |
| **Unending Despair** | After 10s in the same fight (§4), pulses magic damage in a radius and heals you for the same amount. `ORIGINAL`. |

No fourth leggings piece — closed 2026-09-08. Helmet and chestplate stay at two each.

### Boots (6)

Single clear stat each: **Mercury Treads** (magic resist), **Ninja Tabi** (armor), **Swifty**
(move speed, from Boots of Swiftness), **Berserker's Greaves** (attack speed), **Wizard's Agony**
(armor + magic penetration, `ORIGINAL`), **Tasted Blood** (omnivamp, `ORIGINAL`).

### Off-hand (2)

| Item | Mechanic |
|---|---|
| **Zonia's Hourglass** (Zhonya's Hourglass) | Active: stasis (§5) for a short duration. |
| **Sterak's Harness** (Sterak's Gage) | Converts a % of max HP into bonus attack damage. Auto-shields (§5) on a big hit taken; the shield proc also grants 10% lifesteal for 10s. On cooldown. |

**26 items total.** Veigo's Blade, Divine Sunderer, Darius's Axe, and Mortal Reminder form a
matched anti-tank family — current-HP%, max-HP%, armor-shred, armor-ignore, the four distinct ways
this stat model can cut through a tank. A shared in-fiction origin for that family is Sanchit's to
write.

## 7. Rune paths

Four paths, not LoL's five — Inspiration dropped entirely rather than kept as a weaker fifth.
**Full two-tree structure**: a primary path (keystone + minors) and a secondary path (minors only),
same shape as League's rune page, reflavored under original names and identities rather than
ported ones. How to keep that shape from reading as an obvious copy is still unresolved — see §9.

| Path | Identity |
|---|---|
| **Vanguard** | Sustained physical damage — on-hit effects, attack speed, straightforward kinetic combat. |
| **Reaver** | Burst, execute thresholds, ambush mobility. |
| **Aetherwright** | Not spellcasting — amplifies the magic-damage procs already built into items (§2, §6: Ragefang, Darken Blade, Heartwood of Pallas). Gives magic resist and magic penetration something real to answer without a spell system. |
| **Bulwark** | HP, armor, magic resist, crowd-control resistance, team protection. **Confirmed keystone idea:** ties into oxygen drain while in combat (§4) — real cross-system integration, not a reskin. Direction (does the path cost extra drain, or blunt a drain penalty everyone takes?) is open, see §9. |

**How the drain tie-in has to be wired, and why it isn't "combat calls atmosphere":** `ascension-combat`
cannot register anything directly into `ascension-atmosphere`'s `DrainModifier` registry (`atmosphere-api.md`
§5) — doing so would be exactly the direct Tier 1 → Tier 1 dependency ADR-0011 forbids, the same
wall a naive version of `worlds` → `atmosphere` hit first. The correct shape mirrors
`WorldEnvironment` exactly: `ascension-combat` is the module that *has* the information (does this
player have the relevant rune, are they in combat, per §4), so it writes what it knows through the
existing `CombatStateRegistry` contract in `core`. `ascension-atmosphere` is the module that
*needs* the information, so **`atmosphere` is the one that implements and registers its own
`DrainModifier`**, reading `CombatStateRegistry.isInCombat(player)` to decide whether to apply a
multiplier. Neither module depends on the other; both depend only on `core`. Whether the rune
itself needs to publish anything beyond a boolean (e.g. which specific keystone a player has
picked) is still open — if `atmosphere` needs more than "in combat, yes/no" to apply the right
multiplier, that is another field on the same kind of contract, decided when this is actually
built.

## 8. `api` versus `internal`

**`com.ascension.combat.api`** — the semver contract: item/stat registration entry points, the
rune-path and keystone types, any events other mods might reasonably want (a proc firing, a rune
being selected), and re-exports nothing from `core` that consumers should import directly instead.

**`com.ascension.combat.internal`** — no guarantees: the actual item implementations, the combat
state event listener and its window logic, shield/stasis/phasing bookkeeping, HUD, commands.

`com.ascension.core.api.CombatStateSource` / `CombatStateRegistry` — see §4 — ship in `core`, not
here, since `atmosphere` needs to read them without depending on this module.

## 9. Open questions

Named explicitly rather than silently deferred, per the pattern `worlds-api.md` and
`atmosphere-api.md` both use:

- **Whether Bulwark's oxygen-drain rune increases the player's own drain (a cost paid for
  picking it) or reduces a combat-drain penalty applied more broadly (a mitigation pick).** Both
  readings came up during design; not settled. Affects what `atmosphere`'s side of the
  `DrainModifier` in §7's wiring note actually needs to know.
- **Combat window length and what counts.** 5 seconds is proposed by analogy to Heartwood of
  Pallas's own corruption window, not decided. Whether fall damage or damage with no attacking
  entity should start/extend combat is also open. Sanchit's call once there's something to play.
- **Making the two-tree rune page not read as an obvious League clone**, while keeping the
  structure (§7) that makes it recognizable and functional. Unsolved by design — flagged rather
  than forced.
- **A shared in-fiction origin for the anti-tank family** (§6) — lore is Sanchit's to write,
  not this document's.
- **All numeric balance** — every percentage, cooldown, duration, and stack count above is a
  placeholder. Sanchit is balancing by playing, not spec.

## A note on names and licensing

Item and rune identities are adapted from League of Legends closely and deliberately — see
ADR-0012's context section for the actual policy text and the risk Sanchit is knowingly accepting.
Nothing in this document changes that risk or should be read as legal clearance; it is an
engineering design doc, not a licensing opinion.
