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
| [0010](0010-orbit-is-one-shared-space-dimension.md) | Orbit is one shared interplanetary space dimension | Accepted |
| [0011](0011-tier-1-modules-share-contracts-through-core.md) | Tier 1 modules share contracts through `core` | Accepted |
| [0012](0012-ascension-combat-module.md) | `ascension-combat` — a new Tier 1 module for stats, items, and runes | Accepted |
| [0013](0013-mixins-in-ascension-compat-cataclysm.md) | `ascension-compat-cataclysm` uses mixins, the project's first | Accepted |
| [0014](0014-vanilla-nether-and-end-portals-are-dormant.md) | Vanilla Nether and End portals are dormant | Accepted |
| [0015](0015-the-road-to-space-is-by-airship.md) | The road to space is by airship | Accepted |
| [0016](0016-mixin-in-ascension-compat-sable.md) | One mixin in `ascension-compat-sable`: ship slots unique across dimensions | Accepted |
| [0017](0017-planet-3-is-the-nether-opened-to-the-sky.md) | Planet 3 is the real Nether, opened to the sky | Accepted |
| [0018](0018-no-world-is-required.md) | No world is required; players go where they choose | Accepted |
| [0019](0019-item-stats-are-data.md) | Item stats are data, for every item from every mod | Accepted |
| [0020](0020-rune-pages.md) | Rune pages: three keystones, two rows, shards, a cost to change, and runes as data | Accepted |
| [0021](0021-player-growth.md) | Player growth comes from milestones, kept as advancements and applied as attribute modifiers | Accepted |
| [0022](0022-enchantments-are-stat-sources.md) | Enchantments are stat sources, as data | Accepted |
| [0023](0023-teams-are-ftb-teams-parties.md) | Teams are FTB Teams parties; progress has three owners (world, team, you) | Accepted |
| [0024](0024-the-hud-is-its-own-module.md) | The HUD is its own Tier 1 module, `ascension-hud`, with a contract in `core` | Accepted |
| [0025](0025-rockets-are-thruster-ships.md) | Rockets are ships with thrusters (Create Simulated Thrusters), and helium is their fuel | Accepted |
| [0026](0026-air-is-a-fluid.md) | Air is a fluid, and a dispenser holds the room | Accepted |
| [0027](0027-max-health-ceiling-is-raised.md) | Max Health's ceiling is raised past vanilla's 1,024, so bosses can have the health the balance gives them | Accepted |
