# ADR-0013: `ascension-compat-cataclysm` uses mixins, the project's first

- **Status:** Accepted
- **Date:** 2026-09-21

## Context

`plans/m3-vertical-slice.md`'s M3.1 relocates L_Ender's Cataclysm's Ruined Citadel structure onto
the Moon via a datapack override (structure biomes, a new `fixed_position` structure_set
placement type, a loot table redirect). Live-tested 2026-09-21, this surfaced two problems a
datapack cannot fix:

1. **The structure floats.** `RuinedCitadelStructure.generatePieces` places every piece at a
   hardcoded `y = 53`, completely independent of the real terrain height vanilla's own
   `onTopOfChunkCenter` helper computes just above it in `findGenerationPoint` (confirmed by
   decompiling the class: the computed height is used only for the structure's reported
   generation-stub position, never passed into `generatePieces`). That happens to look acceptable
   in the End, where terrain near the structure's own biome tags sits close enough to y=53 that
   nobody building for that dimension had reason to notice. Relocated onto the Moon's real
   terrain (craters, mountains, checkerboard biomes), the mismatch is large enough that the whole
   citadel generates hovering above the ground.
2. **Every piece is placed with a hardcoded `StructurePlaceSettings`** (`Piece.makeSettings`) that
   adds exactly two processors (`BlockIgnoreProcessor`, `ProtectedBlockProcessor`) with no hook
   for a third. Reskinning the structure's End-stone palette to this project's own Moon rock
   (Sanchit's ask, 2026-09-21 — "it will look more cool") needs a `RuleProcessor` added to that
   same settings object, and nothing in the structure or structure_set JSON reaches this
   private, hardcoded method at all.

Both are third-party Java making an assumption specific to the dimension it shipped for, with no
data-driven escape hatch. `ascension-worlds` cannot fix either from its own code: this is
Cataclysm's own generation logic, running entirely inside Cataclysm's classes.

## Decision

**`ascension-compat-cataclysm` gets a small, targeted Mixin, the first in this project.**
Two injections, both surgical rather than replacing whole methods:

- `RuinedCitadelStructure.generatePieces`: `@ModifyConstant` on the literal `53`, replaced with
  the real surface height at the piece origin (the same `getFirstOccupiedHeight` call this
  project's own `CraterStructure` already uses).
- `RuinedCitadelStructure.Piece.makeSettings`: add this project's own `RuleProcessor` (End stone
  and its variants &rarr; the Moon's own anorthosite/basalt/regolith) to the returned
  `StructurePlaceSettings`, alongside Cataclysm's existing two processors.

Per ADR-0003 rule 2, this is exactly the case the rule anticipates: Tier 2 only, with this ADR as
the justification. No Tier-1 module is touched or aware this exists.

## Why not avoid mixins entirely

- **Regenerate the Moon's terrain to match `y = 53` at the fixed spot instead.** Rejected: fixes
  this one structure's specific magic number by coincidence, not the underlying assumption, and
  does not generalize — the next sourced boss structure may hardcode a different height, or none
  at all, and terrain-sculpting around every future compat jar's fixed coordinate does not scale
  the way `plans/m3-vertical-slice.md` already commits to (multiple boss-source mods over time).
- **Reimplement `RuinedCitadelStructure` as our own `StructureType`, copying its piece list and
  fixing both problems in original code instead of Cataclysm's.** Rejected as more code and more
  maintenance surface than the actual bug: two hardcoded values need correcting, not a structure
  generator's worth of logic re-authored and kept in sync with Cataclysm's own updates.
- **Leave the citadel floating and End-stone-colored.** Rejected — Sanchit's own call, both times.

## Consequences

- `ascension-compat-cataclysm` needs mixin tooling this project has never used before: a
  `<modid>.mixins.json`, the NeoForge/ModDevGradle mixin annotation processor, and a
  `[[mixins]]` entry in its own `neoforge.mods.toml`. Confined to this one module; no other
  module's build changes.
- Both mixins target Cataclysm's own class names and method signatures directly. A Cataclysm
  update that renames or restructures `RuinedCitadelStructure` breaks this compat jar at
  mod-load time (a mixin failing to apply), not silently — NeoForge reports mixin application
  failures on boot, which is the same "refuses to load into a no-op" posture
  `ascension-compat-create`/`ascension-compat-chunky` already take toward their own missing
  dependencies, just enforced by the mixin loader instead of a `neoforge.mods.toml` version range.
- This is now the reference pattern for the next `ascension-compat-<bossmod>` jar that hits the
  same class of bug: check whether the mismatch is data-driven (compat-create's approach) before
  reaching for a mixin, and when it isn't, keep the mixin to the smallest surgical change that
  fixes the actual assumption, not a wholesale reimplementation.

## Alternatives rejected

See "Why not avoid mixins entirely" above — folded in rather than duplicated, since both
sections were answering the same question from opposite directions.
