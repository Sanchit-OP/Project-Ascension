# ADR-0022: Enchantments are stat sources, as data

- **Status:** Accepted, 2026-10-18. Sanchit answered `enchantments.md` §11 in full.
- **Date:** 2026-10-18

## Context

Sanchit, 2026-10-18, wants enchantments built on the stat system. In 1.21.1 enchantments are a
data registry, and the `minecraft:attributes` effect adds an attribute modifier by level. Our stats
are attributes, read through `StatReader` (ADR-0019).

Today Protection stacks on top of our Armor curve (up to 64% off with a full set) and Breach acts on
vanilla armour our curve replaced.

## Decision

1. An enchantment that gives power does so as an **attribute modifier on one of our stats**, in
   enchantment JSON, with no code and no mixin.
2. Modifier ids start with `ascension_combat:enchantment/`, registered with `StatSources` as
   "Enchantments", so the sheet and the HUD name where the stat came from.
3. Vanilla's Protection, Sharpness, Power and Breach are reworked this way. Hazard enchantments
   (Fire, Blast, Projectile Protection) and utility ones stay vanilla.
4. No boss, planet or tablet rewards enchantments for now; they come from the table, villagers and loot.
5. Sizes are placeholders with no cap yet; the balancing and stats session sets the ceiling.
6. Protection also gives a small all-source reduction (36% on a full set), unlike a pure stat.

## Consequences

- No per-tick cost; the stat changes when a slot changes (ADR-0007).
- Tier 1 only; other mods' enchantments are handled by data conditions or a Tier-2 jar (ADR-0003).
- The data check (`tools/check-data.py`) gains rules for enchantment JSON.
