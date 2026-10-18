# Architecture

Living map of what we build. The *reasoning* behind this shape is
[ADR-0003](../decisions/0003-modular-architecture-and-compatibility-policy.md); this file
tracks the current state.

## Shape

Every module is a **separately publishable NeoForge mod jar with its own modid**. Someone can
take one module and use it without the rest.

```
Tier 0   ascension-core            NeoForge only. Shared contracts, no behaviour.
           |
Tier 1   ascension-atmosphere      Breathable zones + oxygen.  <- flagship, reusable
         ascension-worlds          Planets, orbit, gateways.   (data-driven)
         ascension-gear            Suit modules, hazard protection.
         ascension-progression     Team capability state, gating.
         ascension-combat          Stats, items, rune paths.   (data-driven)
         ascension-hud             Layout, theme and frame of the HUD; elements come from
                                   the other modules through a contract in core (ADR-0024)
           |
Tier 2   ascension-compat-sable    Airships carry their crew to space and back (ADR-0015)
         ascension-compat-create   Connector
         ascension-compat-jei      Connector
         ascension-compat-ftbquests Connector
```

## Dependency rules

| Rule | |
|---|---|
| Tier 1 -> third-party mod | **Never.** All foreign integration lives in Tier 2. |
| Tier 2 optional at runtime | Yes. Absent compat jar must not degrade Tier 1. |
| Mixins in Tier 1 | **Never.** Tier 2 only, with an ADR justifying it. |
| Extension mechanism | Provider registries. Never require extending our classes. |
| Tier 1 testable alone | Yes, with only `core` present. |
| **Tier 1 -> Tier 1** | **Never directly.** Shared concepts are contracts in `core`; modules stay ignorant of each other ([ADR-0011](../decisions/0011-tier-1-modules-share-contracts-through-core.md)). |
| What `core` may hold | Contracts only — interfaces, records, registry keys, codecs. No behaviour. The test: removing every Tier 1 module must leave `core` doing nothing observable. |

## Package convention

```
com.ascension.<module>.api.*        stable, semver, third-party contract
com.ascension.<module>.internal.*   free to change, no external guarantees
```

If a third party has to import from `internal`, the `api` is wrong.

## Build

Gradle multi-project, NeoForge ModDevGradle, versions centralised in
`gradle/libs.versions.toml`, shared config in a `buildSrc` convention plugin so per-module
build files stay near-empty.

```
settings.gradle.kts
gradle/libs.versions.toml
buildSrc/                     convention plugin
modules/
  core/
  atmosphere/
  worlds/
  combat/
  hud/
  gear/
  progression/
  compat/
    chunky/
    create/
    cataclysm/
    endermanoverhaul/
    jade/
    immersivedamageindicators/
    distanthorizons/
    sable/
run/                          dev client/server, gitignored
```

## Current status

| Module | Status |
|---|---|
| `ascension-core` | Tier 0: contracts only (`WorldEnvironmentRegistry`, `CombatStateRegistry`, the HUD contract, and since 2026-10-18 `TeamRegistry`: who is on which team, single-source, everyone one team by default, [ADR-0023](../decisions/0023-teams-are-ftb-teams-parties.md)). |
| `ascension-atmosphere` | **v0.1 feature-complete**, verified on a dedicated server through M1.8. Refactor pass M1.9 outstanding. |
| `ascension-worlds` | **M2 done** (2026-09-06); M3 in progress. Earth + Moon, full orbit/descent/return loop, real Moon terrain, per-planet weather, plus a reusable `fixed_position` structure placement type (2026-09-21) for pinning sourced structures to a planet and `random_spread_keep_clear` (2026-10-10) for keeping scattered ones (craters, wild citadels) clear of several fixed ones at once, the gateway network as data-driven walk-through multiblock frames (2026-10-06, `gateway-frame-format.md`), dormant vanilla Nether/End portals (ADR-0014), sky bodies drawn from client resources (Earth over the Moon, 2026-10-06, `worlds-api.md`), and the Lunar Archive: its lore as a synced datapack registry, `archive_entry`, read from plinths and tablets, and since 2026-10-10 the building itself generating on the Moon (`internal/archive/`, `lunar-archive-format.md`). [`plans/m2-worlds.md`](../../plans/m2-worlds.md), [`plans/m3-vertical-slice.md`](../../plans/m3-vertical-slice.md) |
| `ascension-gear`, `ascension-progression` | Not started |
| `ascension-combat` | **Built.** [ADR-0012](../decisions/0012-ascension-combat-module.md) plus [`combat-api.md`](combat-api.md); 19 items, physical/magic/true damage, armor/magic-resist, rune pages. This table was stale until 2026-09-21 — five commits landed it after the ADR was accepted, never reflected here. |
| `ascension-hud` | **Shell built 2026-10-18, not yet seen in a client** ([ADR-0024](../decisions/0024-the-hud-is-its-own-module.md)). The contract is in `core` (`com.ascension.core.api.client`: `HudElement`, `HudAnchor`, `HudRegistry`); the module has a layout, a layer and a client config switch, and no elements yet. Design in [`hud.md`](hud.md). |
| `ascension-compat-curios` | Designed, not built. A tank slot that opens the valve (ADR-0009 §4). |
| `ascension-compat-chunky` | **Built and confirmed live 2026-09-06** (M2.6 lever #2). Walks the planet registry in `Planet.order()` on server start and pre-generates each one's surface, one at a time, chained through Chunky's own completion callback. Reads `ascension-worlds`' planet registry directly rather than through an event — the original per-arrival-event design was retired the same day it shipped, once the proactive server-start approach replaced it. Compiles against Chunky's real API (Modrinth maven); confirmed running for real in a live client via a Spark profile. **Off by default since 2026-10-10** (`[pregeneration] background` in its server config): a fresh server crashed while it ran, because generated chunks were not unloaded while Chunky worked and the heap filled. That cause is not found yet (`plans/m3-vertical-slice.md`, the Chunky bug). Fixed the same day: it took Chunky's end-of-task event, which also fires on a pause or cancel, for a finished task, so pausing one planet started the next; it now moves on only when a task's chunks have run out. A governor (`PregenGovernor`) pauses our task while ticks are over 40 ms, so it never lags players. |
| `ascension-compat-create` | **Built** (2026-09-07). Disables Create's Copper/Netherite Backtank and Diving Helmet recipes and hides them from recipe viewers — a wearable pressurised-air tank is exactly `ascension-atmosphere`'s own oxygen tank under a different name. Pure datapack, no Java integration. |
| `ascension-compat-cataclysm` | **Built, boot-verified** (2026-09-21). Relocates L_Ender's Cataclysm's Ruined Citadel to a fixed spot on the Moon (plus rarer wild copies), spawns the Ender Guardian when the Golem dies, and adds `ascension-worlds`' own gateway key to the Guardian's drops. Carries the project's only mixins (ADR-0013). First of what M3 expects to be several boss-source compat jars. Not yet confirmed live in a client. **2026-10-11:** also takes Cataclysm's five Earth bosses off Earth (Sunken City, Acropolis, Frosted Prison, Cursed Pyramid, Ancient Factory), each now in its own empty `ascension_worlds:<structure>_biomes` tag for a later planet to fill; its small structures and mobs stay. Checked with `/locate` on a fresh dedicated server. |
| `ascension-compat-endermanoverhaul` | **Built, boot-verified** (2026-09-24). One NeoForge `add_spawns` biome modifier adding Enderman Overhaul's End Islands Enderman to `ascension_worlds:moon_biomes`. First of what M3's Moon content backlog expects to be several ambient-population compat jars. Not yet confirmed live in a client. |
| `ascension-compat-ftbteams` | **Built 2026-10-18, compiles and has its jar, not yet seen in a game** ([ADR-0023](../decisions/0023-teams-are-ftb-teams-parties.md)). Answers `core`'s `TeamRegistry` from FTB Teams' parties, passes on team changes, and says once where the team screen is. Needs only `core`; compiles against FTB Teams, Library and Architectury, bundles none. |
| `ascension-compat-jei` | **Built 2026-10-18, compiled only, not yet seen in a client.** Client-only. A "Build path" page in JEI for each combat item no build recipe consumes: every raw material the whole tree costs, with counts and a tooltip naming the world (or boss) it comes from. Reads the `ascension_combat:build/...` crafting recipes, so it needs no list of items. JEI is compile-only. |
| `ascension-compat-ftbquests` | **Built 2026-10-18, server-checked with the real FTB Quests, not yet seen in a client** ([quests.md](../progression/quests.md)). The quest book's look (a theme, three quest shapes, the night-sky chart and planet pictures, all original and generated by `tools/generate-quest-art.py`) plus a live "you are here" marker on the Planets page: a texture drawn on the client, on a world's surface and between the worlds in space. **No compile dependency on Quests, on any Ascension module, and no mixin**: the look is resources Quests reads from every pack, and the marker is plain Minecraft. The quest pages themselves are data under `pack/quests` (twelve pages, 141 quests as of 2026-10-18), turned into Quests' files by `tools/generate-quests.py`; two dev checks (`quest-checks.txt` on a server, `quest-screen-checks.txt` in a client) report through one `CheckReport`. |
| `ascension-compat-jade`, `ascension-compat-immersivedamageindicators` | **Built.** Client-only QoL: Armor/Magic Resist on Jade's tooltip, floating combat-text tuning. |
| `ascension-compat-distanthorizons` | **Built and seen live** (2026-10-10). Client-only. Distant Horizons 2.4.5 draws its own LOD clouds in every dimension without a ceiling, whatever the dimension says, so it put clouds over the Moon and in space. This turns them off, through DH's public config api (an in-memory override, never the player's file), wherever the dimension's special effects have no cloud height, and gives the player's own setting back everywhere else. Depends on no Ascension module. |
| `ascension-compat-sable` | **Built, proven headless** (2026-10-11). The airship road to space (ADR-0015): a ship climbing past a planet's build height is carried to space with its crew, and one reaching a planet in space comes down to it, through Sable's own ship save and load and `ascension-worlds`' public `SpaceTravel`/`AscentCarrier`. `dimension_physics` data: no gravity in space; Earth's and the Moon's air thins toward y 320 without fading to zero. One mixin (ADR-0016) gives new ships storage slots free in every dimension. A titanium recipe for Aeronautics' propeller bearing (2026-10-12), since brass needs a Nether blaze and a survival ship could not otherwise cross space. Pinned to Sable 2.0.5–2.0.6 (internal API). Not yet seen with a crew or a real Aeronautics ship. Sub-levels as pressurised habitats (ADR-0006) is still to come. |

## Constraints inherited from the register

- [ADR-0001](../decisions/0001-target-minecraft-version-and-loader.md) — MC 1.21.1, NeoForge, Java 21
- [ADR-0006](../decisions/0006-sable-integration-posture.md) — atmosphere zones must be able to
  ride a moving region, or the Sable ship bridge becomes impossible later
- [ADR-0007](../decisions/0007-performance-contract.md) — binding performance rules on all code
