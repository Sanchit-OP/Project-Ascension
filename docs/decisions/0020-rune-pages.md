# ADR-0020: Rune pages: three keystones, two rows, shards, a cost to change, and runes as data

- **Status:** Accepted
- **Date:** 2026-10-15
- **Amends:** [ADR-0012](0012-ascension-combat-module.md), whose rune page (one keystone and four
  minors per path, `combat-api.md` §7) this replaces, and whose Bulwark oxygen keystone it drops.
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
- Numbers players can't see frustrate them; the hover card needs the real numbers.

Sanchit's answers the same day (`rune-system.md` §13) settled the rest: slots open with growth,
changing costs something, a few runes grow, there is no oxygen keystone, and each path gets one
non-combat "world" keystone.

## Decision

**A page has eight picks:**
- **A primary path:** one of its **three keystones**, and one minor from each of its **two rows
  of three**.
- **A secondary path:** one minor from each of its two rows. No keystone.
- **Three stat shards:** offence, flex, defence.

**Each path has two combat keystones and one world keystone**, set apart on the screen. A world
keystone is a behaviour for living in Minecraft (vein mining, silence past sculk, more XP, creepers
that keep away), never a plain stat that growth already gives. Four paths, 36 runes, 9 shards
(`rune-system.md` §4–5).

**Slots open with growth; runes don't.** Every rune can be chosen from the start. The secondary
path and the shard rows open as the player reaches growth's milestones, read through growth's
public API. A server switch turns this off.

**Changing costs a Rune Chalk and XP levels**, out of combat, whenever a save removes or replaces a
rune. Filling an empty slot is free.

**Four runes grow**, one per path, from new kinds of enemy defeated, to a cap. Removing one loses
its growth. Growth is kept through death.

**Runes are data.** Each rune is a JSON entry in a synced datapack registry,
`ascension_combat:rune`, naming its path, slot, row, icon, Java behaviour (`effect`) and numbers
(`values`). Behaviours stay in Java, one listener class each. The client receives the registry, so
the screen shows real numbers, and retuning is a JSON edit (ADR-0007, data over code).

**The server validates every page** against the registry, the unlocks, combat state and the cost.
The client is never trusted.

## Consequences

- `RunePageState` changes shape and gains growth. Old saves load: a missing keystone becomes the
  path's first, missing shards stay empty.
- 31 new runes need behaviours, icons, lang text and data files. Built a path at a time.
- A new item, Rune Chalk, needs a recipe, a texture and a line in the resource ledger.
- Rune behaviours read their numbers from the registry instead of Java constants. The five built
  Vanguard runes move over first.
- A rune's numbers can be changed by a datapack, so a modpack can rebalance runes without code.
- No oxygen keystone means `combat` needs no air contract in `core`. `combat-api.md` §9's Bulwark
  question is closed.
- The rune screen's art kit (frame, cards, sockets, sounds) becomes the shared look that growth's
  tree screen reuses.
- Saved, named pages are not planned: they would undercut the cost of changing.

## Alternatives rejected

- **League's three rows of minors.** More picks than Minecraft's verbs can fill without filler.
- **Keep one keystone per path.** The keystone is the most fun pick in League; one per path means a
  path is a single build.
- **All keystones combat.** Most of Minecraft isn't fighting. A world keystone makes the page
  matter everywhere, and giving up combat power for it is a real choice.
- **A fifth, all-utility path.** Players would take it as their secondary every time, and combat
  secondaries would fade.
- **Unlock runes themselves with progress.** Locking choices discourages trying builds. Opening
  *slots* still lets growth matter.
- **Free changes.** Sanchit wants a change to be a decision, not something done on the spot.
- **Every rune grows.** A few growing runes give commitment without making every rune weak at first.
- **A rune altar block.** Every experiment would become a trip; the chalk gives the change its
  price instead.
- **Keep numbers in Java constants.** Every retune would be a rebuild, and the client could not
  show the numbers without a copy that drifts.
