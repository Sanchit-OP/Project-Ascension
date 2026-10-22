# ADR-0029: Planet loot is added to every chest by two loot types that live in core

- **Status:** Accepted, 2026-10-22 (Sanchit: loot is poor and does not improve on the planets; the Ender Golem arena's
  chests give wheat; do not edit tables one by one, and it must work with Lootr)
- **Date:** 2026-10-22
- **Builds on:** [ADR-0003](0003-modular-architecture-and-compatibility-policy.md), [ADR-0011](0011-tier-1-modules-share-contracts-through-core.md)
  (Tier 1 modules share contracts through `core`), resource-economy principle 5 (loot is added to, never replaced)
- **Amends:** the statement in `AscensionCore` that it "registers no content"

## Context

The game cannot say "every chest table" in data. `neoforge:loot_table_id` names one table exactly, so improving chests across
the board meant a list of tables nobody would keep current, and one that would miss every table another mod adds. Replacing
vanilla's tables (a datapack such as Better Loot does) breaks the add-only rule and knows nothing about planets. LootJS needs
KubeJS, a large dependency for one rule. Lootr, already in the pack, fills its chests with the game's own
`LootTable.fill`, so any loot modifier applies inside its chests.

## Decision

`core` registers two loot types, and every module's loot data names them:

- **`ascension_core:chest_table`**, a loot condition that is true when the rolled table's path is under `chests/`.
- **`ascension_core:strip`**, a global loot modifier that removes an item tag (`ascension_core:chest_junk`) from the loot.

Each planet's bonus is an ordinary `neoforge:add_table` modifier carrying the first and a `minecraft:location_check` on the
dimension. A module adds its own table for its own items (`ascension_worlds:planet/moon`, `ascension_combat:planet/moon`), so
no module references another's items and every module still loads with only `core` present.

## Why in core

A condition type and a modifier type are vocabulary that more than one Tier 1 module's data has to agree about, which is what
ADR-0011 puts in `core`. They are inert: with no loot file naming them, removing every other module leaves `core` doing
nothing observable, which is the test ADR-0011 sets.

## Consequences

- A chest on the Moon, the Nether or the End gains the planet's bonus and loses the junk, whatever mod or structure owns it.
  A new planet is data (a pool block in `tools/generate-planet-loot.py` and its dimension), not a class.
- Earth's chests lose only a mild junk tag (seeds, rotten flesh, poisonous potatoes) and gain nothing. Mob drops, block drops and
  fishing are untouched.
- The numbers, the odds measured on a dedicated server and how to change them are in `docs/progression/loot.md`.
