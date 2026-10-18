# ADR-0022: Enchantments are stat sources, as data

- **Status:** Proposed, 2026-10-18. Becomes Accepted when Sanchit answers `enchantments.md` §11.
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
4. Planets and bosses may *reward* an enchantment, never require one (ADR-0018).
5. A full enchanted set is sized against growth and items using growth's unit yardstick.

## Consequences

- No per-tick cost; the stat changes when a slot changes (ADR-0007).
- Tier 1 only; other mods' enchantments are handled by data conditions or a Tier-2 jar (ADR-0003).
- The data check (`tools/check-data.py`) gains rules for enchantment JSON.
