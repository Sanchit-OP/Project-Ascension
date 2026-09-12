# ADR-0012: `ascension-combat` — a new Tier 1 module for stats, items, and runes

- **Status:** Accepted
- **Date:** 2026-09-08

## Context

Sanchit wants to bring a curated set of League of Legends-inspired items into the game, under a
stat system built from the same vocabulary — HP, attack damage, attack speed, lifesteal,
omnivamp, move speed, armor, magic resist — plus the supporting mechanics several of those items
need: armor/magic penetration, grievous wounds, true damage, temporary shields, a stasis state,
and a phasing state. Alongside it, a two-tree rune/keystone system functions as a build-defining
class-selection layer.

No existing module owns this. `ascension-gear` (per `architecture.md`) is suit modules and hazard
protection, not general itemization or combat stats. Vanilla Minecraft has no attack-damage
scaling, no armor penetration, no lifesteal or omnivamp, no true damage, and no distinction
between physical and magic damage at all — armor and magic resist only mean something once that
distinction exists. This is a new system, not an extension of one that exists, which per
`CLAUDE.md` makes it exactly the kind of structural choice that needs an ADR rather than a
silent addition to an existing module.

**Names and identities are adapted from League of Legends deliberately closely**, with original
names, descriptions, and Minecraft-native mechanics rather than ported numbers. This is
knowingly outside Riot's "Legal Jibber Jabber" fan-content policy, which explicitly prohibits
using their IP — items named among its own examples — inside a game or app. Sanchit's call,
made with that policy read and understood: acceptable risk is a takedown request, not pursued
further, and he is not treating this as the mod's sole draw.

## Decision

**New Tier 1 module, `ascension-combat`.** Depends on `core` only, per the same rules as
`atmosphere` and `worlds` (ADR-0003, ADR-0011): no third-party hard dependency, extension through
provider registries, loadable and testable with only `core` present.

**A real physical / magic / true damage split.** Armor and magic resist are meaningless without
distinct damage types to reduce. Melee, arrows, and most mob damage read as physical by default.
Deliberately **no spellcasting system** — magic damage lives inside specific procs on a handful of
weapons rather than a new cast-a-spell mechanic, so the Aetherwright rune path amplifies existing
procs instead of casting anything new. See `docs/technical/combat-api.md` for the full design.

**`core` gains one more shared contract: combat state.** Whether a player is currently "in
combat" (entering combat on dealing or taking damage, since-last-action window) is needed by more
than one Tier 1 module — `ascension-combat` itself (several items and one rune key behaviour off
it) and `ascension-atmosphere` (a Bulwark rune increases oxygen drain while in combat, which means
`atmosphere`'s existing `DrainModifier` registry needs to read a fact that `atmosphere` does not
own). Per ADR-0011, the *contract* — a query interface and a single-source registry, in
`com.ascension.core.api` — lives in `core`. The *behaviour* that actually listens for damage
events and decides what "in combat" means does not: it lives in `ascension-combat`, which is the
one module that registers a source. `core` may hold attachment types and query interfaces; it may
never hold an event handler that makes something happen (ADR-0011's own table), and a damage-event
listener is exactly that.

## Consequences

- A fifth Tier 1 module joins `atmosphere`, `worlds`, `gear`, `progression` — all five share
  contracts through `core` and never depend on each other directly.
- `core`'s API surface grows again, the same way ADR-0011 predicted it would recur: `gear` will
  need `atmosphere`, `progression` will need `worlds`, and now `atmosphere` needs `combat`. Each
  case gets the identical treatment — a contract in `core`, never a soft dependency between Tier 1
  modules.
- Item and rune numbers are unbalanced at this stage by design. Everything is data-driven
  (ADR-0007 rule 10) specifically so Sanchit can balance by playing and retuning JSON, not by
  waiting on a rebuild.
- The module carries real, accepted IP risk. Nothing about the engineering hides that risk or
  should be read as reducing it — see `combat-api.md`'s licensing note for the record of the call.

## Alternatives rejected

- **Fold this into `ascension-gear`.** Wrong scope — `gear` is suit modules and hazard
  protection, not general itemization, and stretching it to cover a whole stat/item/rune system
  would make the module's own boundary meaningless.
- **A spellcasting system for magic damage.** Rejected — no spell-casting verb exists or is
  wanted in this project; reworking existing weapon procs to deal magic damage gets the stat
  interaction without inventing a mechanic nobody asked for.
- **A soft dependency from `atmosphere` to `combat` for reading combat state**, rather than a
  `core` contract. Rejected for the identical reason ADR-0011 rejected it for `worlds`/
  `atmosphere`: it would make our own modules cooperate without making the contract available to
  a third party who wants to describe combat state without depending on `ascension-combat` at
  all, and it would leave the pattern unproven for the next module that needs it.
