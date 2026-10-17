# ADR-0021: Player growth comes from milestones, kept as advancements and applied as attribute modifiers

- **Status:** Proposed. Sanchit answered decisions 1, 3 and 4 on 2026-10-15; 2 and 5 follow the
  recommendation until he says otherwise (`growth.md` §13).
- **Date:** 2026-10-15
- **Number:** 0021 assumes the stats session (prompt 1) takes 0019 and the runes session (prompt 2)
  takes 0020, in the order of their prompts. Renumber at merge if that is not how it lands.

## Context

A player never grows (`docs/progression/player-power.md` §2): 20 HP all game, and every other stat
comes from items. Sanchit, 2026-10-15, wants many ways to gain stats, stats that items will scale
off, and possibly a skill tree so players grow in different directions.

Three things constrain how:

- **ADR-0018:** no world is required, so growth must come from any world in any order.
- **ADR-0003 and ADR-0011:** growth needs to hear about gates lit, which `ascension-worlds` owns, and
  Tier 1 modules never depend on each other.
- **ADR-0007:** nothing per tick, no leaks, data over code. A new boss or planet must add growth as
  data.

The full design is `docs/progression/growth.md`; the research behind it is
`docs/progression/growth-research.md`.

## Decision

**1. Growth is permanent, comes only from finite sources, and is all skill points.** The first
kill of each boss and each gate lit (milestones) and the first visit to each notable place
(discoveries) give skill points, and nothing else. Every stat a player grows is taken in a four-arm
skill tree. No kill-XP, activity levels, vanilla XP or time played: Minecraft machines farm anything
repeatable.

**2. Every milestone is a vanilla advancement.** Growth reacts to advancements being earned or
revoked. What each is worth lives in a datapack registry, `ascension_combat:growth_milestone`, keyed
by advancement id. Tree nodes are a second registry, `ascension_combat:growth_node`. Both are synced
to clients.

- Bosses and discoveries are advancements with vanilla triggers (`player_killed_entity`,
  `location`): JSON only.
- `ascension-worlds` awards an advancement per gate when it lights. That is the whole connection:
  growth never names `worlds`, and `worlds` never names growth.
- Any other mod or datapack can add a milestone with two JSON files.

**3. Growth writes only attribute modifiers**, one per attribute and operation, with ids under
`ascension_combat:growth/` (`growth/add_value`, `growth/add_multiplied_base`). They are worked out
again from the player's advancements and taken nodes on every change, never added up and stored.
They are permanent modifiers, because a player's saved attributes load before their health, so a
max-HP bonus is in place before a reloaded player's health is read. Attributes are named by id, so
growth can raise any mod's attribute and skips one that isn't installed.

There is no base/bonus split (the stats session, `item-stats.md` §6, Sanchit 2026-10-15): a stat is
one total, and items scale off it. Growth adds to that total, so it feeds item scaling directly,
which is what Sanchit's answer "bonus" asked for. The stats session's `StatSources` labels the
`ascension_combat:growth/` prefix "Growth" on the character sheet.

**4. Shared milestones belong to the whole server** (Sanchit, 2026-10-15). A milestone marked `shared` (bosses and gates)
goes to every player when it is earned, and to anyone joining later on their first login. The team
is the whole server until Ascension has teams, as the gates and the mark already are. Discoveries and
the tree are personal.

**5. It lives in `ascension-combat`**, in `internal/growth` with a small read API in
`api/growth`, next to the stats it raises and the rune screen it shares a look with.

## Consequences

- **Persistence is mostly vanilla's.** Advancements are saved and synced by the game. The only new
  saved state is each player's taken nodes and the server's list of shared milestones.
- **Balancing is a data edit.** Because totals are worked out rather than stored, a changed number
  reaches every player, and a total can never drift from what earned it. Growth's own files are
  datapack registries, which load at server start (as planets and archive entries do), so a change
  there needs a restart; advancements reload with `/reload`.
- **The advancements screen gains an Ascension tab**, which doubles as a list of goals. That is a
  visible change players will see.
- **`ascension-worlds` changes slightly:** `GateProgress` awards the gate advancements, beside the
  sync it already sends when the network changes.
- **Revoking an advancement by command removes its growth.** That is correct, and shared ones
  return to a player at their next login while the team still holds them.
- **The "Growth" label on the character sheet depends on the stats session's `StatSources`.**
  Until it lands, growth's modifiers show as "Other", and every stat still works.
- **No HP comes free.** A player who never takes Guard nodes stays at 20 HP; armour and items carry
  their defence. Sanchit chose this; the first boss fights test it.
- **Respec** is anywhere out of combat for 20 XP levels (Sanchit, 2026-10-15). It reads combat
  state through `core`'s `CombatStateRegistry`, which combat already registers.

## Alternatives rejected

- **A module of its own (`ascension-progression`).** It would make growth usable without the
  League items, which suits compatibility. But growth raises combat's stats, its screen shares the
  rune screen's look, and runes may unlock with it. Splitting it now means a contract for each of
  those before anyone needs one (ADR-0011: contracts when two modules need them, not in advance).
  Because growth names attributes and advancements by id, lifting it out later is a move, not a
  redesign.
- **A `core` contract for "a gate was lit".** Works, but advancements already are a shared,
  per-player, saved and synced record every mod can read. A new contract would duplicate them.
- **Storing each player's totals.** Simpler to read, but any balance change would leave old totals
  behind, and a missed event would drift forever.
- **Kill-XP, activity levels, or vanilla XP.** Farmable, or already spent on enchanting (see the
  research, §3).
- **Milestones that give stats directly** (a heart per boss, as in Zelda). Proposed first; Sanchit
  chose points only, so that every stat is a choice.
- **Growth as base stats**, as League's levels are. Sanchit chose bonus, and the stats session then
  dropped the split altogether: growth adds to the one total.
