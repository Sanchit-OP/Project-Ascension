# Decision Register

Every locked decision for Project-Ascension, with the reasoning that produced it.

## Why this exists

`todo.md` records what is still **open**. This register records what is **settled** and why.
Without it, decisions get relitigated every time work resumes in a new session or after a
break, and the reasoning behind a constraint is lost the moment the person who set it
forgets it.

## How to use it

- **Starting a new session?** Read `docs/decisions/` top to bottom before touching anything else.
  It is the fastest path to full project context.
- **About to make a structural choice?** Check whether an ADR already covers it.
- **Disagree with an ADR?** Do not silently work around it. Supersede it with a new ADR that
  states what changed and why. Set the old one's status to `Superseded by ADR-XXXX`.

## Status values

- `Accepted` — in force.
- `Superseded by ADR-XXXX` — replaced; kept for the historical reasoning.
- `Proposed` — under discussion, not yet binding.

## Index

| ADR | Title | Status |
|---|---|---|
| [0001](0001-target-minecraft-version-and-loader.md) | Target Minecraft 1.21.1 on NeoForge | Accepted |
| [0002](0002-custom-mods-not-curated-modpack.md) | Build custom mods, not a curated modpack | Accepted |
| [0003](0003-modular-architecture-and-compatibility-policy.md) | Modular architecture and compatibility policy | Accepted |
| [0004](0004-fully-custom-dimensions.md) | Fully custom dimensions | Accepted |
| [0005](0005-v1-scope-vertical-slice.md) | v1 scope is a three-world vertical slice | Accepted |
| [0006](0006-sable-integration-posture.md) | Sable is an optional integration, never a hard dependency | Accepted |
| [0007](0007-performance-contract.md) | Performance contract | Accepted |
| [0008](0008-build-and-test-cadence.md) | Build-and-test-in-game cadence | Accepted |
| [0009](0009-carry-limits-enforced-on-the-player.md) | Carry limits are enforced on the player, not on containers | Accepted |
