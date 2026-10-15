# ADR-0021: Player growth comes from milestones, kept as advancements and applied as attribute modifiers

- **Status:** Proposed
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

**1. Growth is permanent and comes only from finite sources.** The first kill of each boss and each
gate lit (milestones), the first visit to each notable place (discoveries), and a skill tree funded
only by those two. No kill-XP, activity levels, vanilla XP or time played: Minecraft machines farm
anything repeatable.

**2. Every milestone is a vanilla advancement.** Growth reacts to advancements being earned or
revoked. What each is worth lives in a datapack registry, `ascension_combat:growth_milestone`, keyed
by advancement id. Tree nodes are a second registry, `ascension_combat:growth_node`. Both are synced
to clients.

- Bosses and discoveries are advancements with vanilla triggers (`player_killed_entity`,
  `location`): JSON only.
- `ascension-worlds` awards an advancement per gate when it lights. That is the whole connection:
  growth never names `worlds`, and `worlds` never names growth.
- Any other mod or datapack can add a milestone with two JSON files.

**3. Growth writes only attribute modifiers**, one per attribute, all with the id
`ascension_combat:growth`. They are worked out again from the player's advancements and taken nodes
on every change, never added up and stored. Attributes are named by id, so growth can raise any
mod's attribute and skips one that isn't installed. The stat-reading API counts these modifiers as
**base** (pending the stats session's agreement, through Sanchit).

**4. Shared milestones belong to the whole server.** A milestone marked `shared` (bosses and gates)
goes to every player when it is earned, and to anyone joining later on their first login. The team
is the whole server until Ascension has teams, as the gates and the mark already are. Discoveries and
the tree are personal.

**5. It lives in `ascension-combat`**, in `internal/growth` with a small read API in
`api/growth`, next to the stats it raises and the rune screen it shares a look with.

## Consequences

- **Persistence is mostly vanilla's.** Advancements are saved and synced by the game. The only new
  saved state is each player's taken nodes and the server's list of shared milestones.
- **Balancing is a data edit.** Because totals are worked out rather than stored, changing a number
  and running `/reload` changes every player's stats, and a total can never drift from what earned it.
- **The advancements screen gains an Ascension tab**, which doubles as a list of goals. That is a
  visible change players will see.
- **`ascension-worlds` changes slightly:** `GateProgress` awards the gate advancements, beside the
  sync it already sends when the network changes.
- **Revoking an advancement by command removes its growth.** That is correct, and shared ones
  return to a player at their next login while the team still holds them.
- **Growth's numbers depend on the stats session's API** for the base and bonus split. Until it
  exists, growth's modifiers are ordinary attribute modifiers and every stat still works.

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
