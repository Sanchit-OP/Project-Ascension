# ADR-0023: Teams are FTB Teams parties; progress has three owners

- **Status:** Accepted, 2026-10-18 (Sanchit chose all three recommended options)
- **Date:** 2026-10-18
- **Builds on:** [ADR-0003](0003-modular-architecture-and-compatibility-policy.md),
  [ADR-0007](0007-performance-contract.md), [ADR-0011](0011-tier-1-modules-share-contracts-through-core.md),
  [ADR-0018](0018-no-world-is-required.md), [ADR-0021](0021-player-growth.md)

## Context

The vision says "co-op first, solo viable" and "team progression is shared at the team level", but
the only team the game has is the whole server (`TeamGrowthState`, the gates, the mark). That is right
for one group of friends and wrong for a server with two groups: one group's boss kill pays everyone.
FTB Teams is installed with FTB Quests, and gives parties, invites, ranks, chat and a team id.

## Decision

**1. Progress has three owners** ([`docs/progression/teams.md`](../progression/teams.md) §1).
**The world** owns what is built in it: gates lit and the mark's glyphs belong to everyone on the
server. **A team** owns what the group does: boss first kills, and quest progress. **A player** owns
discoveries, their tree, their runes and their gear.

**2. A team is an FTB Teams party; a solo player is a team of one.** FTB Teams gives every player a
personal team, and it is used exactly as a party is. We build no team screen, invites or chat.

**3. A boss's points go to the killer's team** (every member, now or on next login), once per team.
Gates stay the world's.

**4. A party holds the union of its members' milestones.** Joining gives you the party's and gives it
yours. Leaving, or a party disbanding, takes nothing from anyone: points, nodes and runes stay.

**5. Teammates cannot hurt each other**, a server setting that is on by default. PvP between parties
follows the server's `pvp` setting.

**6. The contract lives in `core`, the integration in a Tier-2 jar.** A `core` provider registry says
who is on a player's team and tells when it changes; its **default provider says everyone is one team**,
so a server without FTB Teams behaves as today and every Tier-1 module loads with `core` alone. A new
`ascension-compat-ftbteams` registers a provider over FTB Teams' API.

## Consequences

- `TeamGrowthState` becomes a map from team id to earned milestones; allies for runes and friendly
  fire read the provider. An existing world's whole-server set becomes the starting set of every team
  that exists when parties turn on, so nothing is lost.
- **Two-client testing is now required**, not just owed: the join, leave and kill rules cannot be seen
  with one player (ADR-0008).
- `ascension-progression` ("team capability state, gating" on the architecture map) is not needed:
  ADR-0018 removed the gating and this provider replaces the team half.
- A team gives no gear. The gear half of the late-joiner gap is to be closed by quest rewards if FTB
  Quests lets a new member claim a finished quest's rewards; otherwise it is its own design.
- A sixth contract in `core` with a semver obligation, added when growth needs it (ADR-0011).
