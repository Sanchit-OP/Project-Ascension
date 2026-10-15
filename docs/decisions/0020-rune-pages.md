# ADR-0020: Rune pages: three keystones, two rows, shards, and runes as data

- **Status:** Proposed. Waiting on Sanchit's answers to `rune-system.md` §13.
- **Date:** 2026-10-15
- **Amends:** [ADR-0012](0012-ascension-combat-module.md), whose rune page (one keystone and four
  minors per path, `combat-api.md` §7) this replaces.
- **Number:** 0019 is the stats session's (`item-stats-are-data`). If the growth session also
  takes 0020, whichever merges second renumbers.

## Context

The rune page built on 2026-09-19 has one keystone and four minors per path, and only Vanguard is
filled in. Its screen is plain vanilla buttons with names only. Sanchit, 2026-10-15: the rune
screen "having minecraft buttons and text is so depressing"; he wants a professional screen and a
researched system.

The research (`docs/progression/rune-research.md`) found:
- League's runes work because each pick changes how you play, each row asks one question, and plain
  stats live apart in shards.
- Minecraft has fewer combat verbs than League, so League's full nine picks would need filler.
- A cheap change makes players experiment.
- Numbers players can't see frustrate them; the hover card needs the real numbers.

## Decision

**A page has eight picks:**
- **A primary path:** one of its **three keystones**, and one minor from each of its **two rows
  of three**.
- **A secondary path:** one minor from each of its two rows. No keystone.
- **Three stat shards:** offence, flex, defence.

Four paths, 36 runes, 9 shards (`rune-system.md` §4–5).

**Runes are data.** Each rune is a JSON entry in a synced datapack registry,
`ascension_combat:rune`, naming its path, slot, row, icon, Java behaviour (`effect`) and numbers
(`values`). Behaviours stay in Java, one listener class each. The client receives the registry, so
the screen shows real numbers, and retuning is a JSON edit (ADR-0007, data over code).

**Slots open with growth; runes don't.** Every rune can be chosen from the start. The secondary
path and the shard rows open as the player reaches growth's milestones, read through growth's
public API. A server switch turns this off.

**Change freely out of combat.** The server refuses a page saved in combat, and says why.

**The server validates every page** against the registry, the unlocks and combat state. The client
is never trusted.

## Consequences

- `RunePageState` changes shape. Old saves load: a missing keystone becomes the path's first,
  missing shards stay empty.
- 31 new runes need behaviours, icons, lang text and data files. Built a path at a time.
- Rune behaviours read their numbers from the registry instead of Java constants. The five built
  Vanguard runes move over first.
- A rune's numbers can be changed by a datapack, so a modpack can rebalance runes without code.
- The secondary path's two minors equal the primary's two, unlike League's three and two. The
  keystone is what makes the primary path primary.
- The rune screen's art kit (frame, cards, sockets, sounds) becomes the shared look that growth's
  tree screen reuses.
- Lifeline (Bulwark's oxygen keystone) needs a `core` contract for air demand. That gets its own
  ADR when it is built.

## Alternatives rejected

- **League's three rows of minors.** More picks than Minecraft's verbs can fill without filler.
- **Keep one keystone per path.** The keystone is the most fun pick in League; one per path means a
  path is a single build.
- **Unlock runes themselves with progress.** Locking choices discourages trying builds, and a
  player would meet most runes late. Opening *slots* still lets growth matter.
- **Change runes only at a block (an altar).** Gives the swap a ritual, but makes every experiment
  a trip.
- **Keep numbers in Java constants.** Every retune would be a rebuild, and the client could not
  show the numbers without a copy that drifts.
