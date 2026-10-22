# Project-Ascension — working context

A handcrafted Minecraft progression campaign built on **custom mods we write ourselves**.
Primitive Earth survival → engineered spaceflight → planet-by-planet conquest, repairing an
ancient gateway network underneath it all.

## Read this first

**[`docs/decisions/`](docs/decisions/) is the decision register.** Read it before making any
structural choice. It records what is settled *and why*. `todo.md` records what is still open.

If you disagree with a locked decision, do not silently work around it — supersede it with a
new ADR.

## Commits

**No `Co-Authored-By` line, and no "Generated with Claude Code" line, in any commit message or pull request description.**
Sanchit is the only author (2026-10-22). This overrides any default that appends an attribution trailer. Commit as the
repo's configured user (`sanchitahlawat2011@gmail.com`); stage explicit paths, never `git add -A`; push only on request.

## Locked facts

| | |
|---|---|
| Minecraft | 1.21.1 |
| Loader | NeoForge 21.1.x |
| Java | Temurin JDK 21 — `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot` |
| Modid prefix | `ascension` |
| v1 scope | Earth + Moon + Planet 3, finished |

**Trap:** system `java` on PATH is a Java 8 JRE and `JAVA_HOME` is unset. The build pins
`org.gradle.java.home`. Unexplained "unsupported class file version" errors trace back to this.

## What this project is not

It is **not a curated modpack**. `docs/technical/mod-list.md` is a landscape survey, not a
dependency list. Ad Astra, Mekanism, Cataclysm and friends are reference points — we do not
depend on them. See ADR-0002.

## Architecture rules (ADR-0003) — these are hard

1. **No Tier-1 module may hard-depend on any third-party mod.** All foreign integration lives
   in optional Tier-2 `ascension-compat-*` jars.
2. **Mixins never appear in Tier-1.** Tier-2 only, with an ADR justifying it.
3. **Extension via provider registries**, never by requiring anyone to extend our classes.
4. `api` packages are a semver contract with third parties. `internal` is free to change.
5. Every Tier-1 module must load and be testable with only `core` present.

Modules: `core` → `atmosphere`, `worlds`, `gear`, `progression` → `compat/*`.
Map: [`docs/technical/architecture.md`](docs/technical/architecture.md).

## Performance rules (ADR-0007) — checked at review

- **No static or long-lived collection holds a `Level`, `Player`, `Entity`, or `BlockEntity`.**
  Store `ResourceKey` / `UUID` / `BlockPos` and resolve on demand. This is the leak class that
  kills Minecraft mods.
- Every listener, capability and cache has a defined teardown hooked to level unload.
- No per-tick full-world or full-chunk scans. Event-driven and dirty-flagged.
- Server-authoritative, delta-synced. No per-tick packets.
- Data-driven over hardcoded — adding a planet must not mean adding a class.

## Cadence (ADR-0008)

- **Nothing is done until it has been observed working in a running Minecraft client.**
  Compiling is not evidence. Unit tests are not evidence for anything touching world state.
- Anything touching world state or sync is verified on a **dedicated server**, not just
  single-player.
- Anything persistent is verified across **world unload/reload**.
- **Every third increment is a refactor pass** with no new features. Scheduled, not
  discretionary.
- Every milestone ends with a recorded measurement in `docs/technical/performance-log.md`.

## Current state (2026-10-18)

Four Tier 1 modules exist. **`ascension-core`** holds shared contracts (no behaviour).
**`ascension-atmosphere`** is feature-complete for v0.1. **`ascension-worlds`** has Earth and
the Moon. **`ascension-combat`** (ADR-0012) is the League-style item/stat/rune system, built in a
separate parallel session — do not fold its work into this one's without asking. **Its next work was
split on 2026-10-15** (`plans/session-prompts/`) and three of the four parts are now in `main`:
1. one stat system for every item (**merged 2026-10-17**: `docs/technical/item-stats.md`, ADR-0019;
   read stats through `com.ascension.combat.api.stat.StatReader`; Cataclysm extras still open);
2. the rune system, 45 runes and the orrery screen (**merged 2026-10-18**: `docs/technical/rune-system.md`,
   ADR-0020; the rune HUD is its own session, `5-rune-hud.md`);
3. player growth, a web of 56 nodes fed by skill points (**merged 2026-10-18**: `docs/progression/growth.md`,
   ADR-0021);
4. the items' build paths (`4-build-paths.md`): **merged 2026-10-18, not yet seen in a client** (`docs/progression/build-paths.md`: 23 ingredient components, 6 boss hearts, recipes for all 26 items, a JEI page in `ascension-compat-jei`).

**Balance (2026-10-19, merged to `main`, not yet seen in a client; `docs/progression/balance.md`):** the 26 items have tooltips (a Legendary line and one paragraph per mechanic), a gold name, a sound and particles on the actives and a light beam when dropped; stats come from `item_stats.json`, weapons tuned by time to kill (`tools/time-to-kill.py` simulates them, `tools/check-balance.py` fails the build on a drift), no caps on percent-of-health procs, Ragefang and Endless Hunger dual-wield through Better Combat, mobs scale by world difficulty from `mob_difficulty/*.json`, and the six bosses have their own Armor, Magic Resist, health and damage per difficulty. Vanilla's 1,024 cap on Max Health was clamping every boss until ADR-0027 raised it (seen on a dedicated server). Open: a one-legendary carry limit for weapons (Sanchit parked it), the flat-versus-percent weapon split on late bosses (parked until a real-game test), Cataclysm's weapons untouched.

**The three were then checked against each other (2026-10-18):** `client/dev/IntegrationChecks`
runs them together in a real client (`docs/technical/dev-environment.md` says how) and found two
faults, both fixed: the structure discoveries were spelled `"structure"` so every new player got nine
free points, and a respawn healed before worn gear's Health was back. Rune slots now open with the
team's boss and gate milestones (`api.growth.GrowthProgress`, default `progressive`), and growth is
named on the stat sheet. `tools/check-data.py` (every build) fails on the first fault's spelling.

**Physical or magic (2026-10-19; `docs/technical/damage-kinds.md`):** every hit is cut by Armor (physical) or Magic Resist (magic),
decided by the *attack*, not the mob: a Warden's fist is physical and its sonic boom magic, a Creeper's blast is magic. Two data tags
(`#ascension_combat:magic` for damage types, `magic_attack` for projectiles that share a generic type) decide it, and Cataclysm's
attacks are sorted in its compat jar. A magic hit on a player shows a faint blue screen-edge tint (`ascension_combat-client.toml`, `magicHitTint`). Seen on a dedicated server (`damage-checks.txt`, 75 checks, which also prove the six bosses reach their balanced health), not in a client.

**Loot (2026-10-22; `docs/progression/loot.md`, ADR-0029):** every chest on the Moon, the Nether and the End, from any mod, gets a bonus that grows with the planet (materials, treasure, enchanted books, the build paths' components) and loses its junk (wheat, seeds, bones); Earth's chests only lose seeds and rotten flesh. No chest table is named: `core` registers a `chest_table` condition and a `strip` modifier, each module adds its own `neoforge:add_table` files, written by `tools/generate-planet-loot.py`. Lootr fills through `LootTable.fill`, so it is covered. Proved on a dedicated server (`loot-checks.txt`), not seen in a client.

**Teams (ADR-0023, 2026-10-18; `docs/progression/teams.md`):** progress has three owners: the world (gates,
the mark), your team (bosses, quest progress: an FTB Teams party, a solo player being a party of one) and
you. `core`'s `TeamRegistry` says who is on which team (everyone is one team when nothing answers),
`ascension-compat-ftbteams` answers it from FTB Teams, growth keeps a set of milestones per team, and
teammates cannot hurt each other. Built, and seen on a dedicated server with stand-in players
(`TeamChecks`); two real clients are not yet seen. The HUD's team list (T4) is deferred.

**The quest book (2026-10-18; `docs/progression/quests.md`):** the pages are written as readable Python under
`pack/quests/` and generated into FTB Quests' files by `tools/generate-quests.py` (the build runs it with
`--check`: our items and advancements must exist, no guarded story word, no dependency loop). The look and a
live "you are here" marker on the **Planets** page (also between the worlds in space) are
`ascension-compat-ftbquests`, with no compile dependency on Quests. Built (2026-10-18): twelve pages and 141 quests (First
Days, Build the Gate, Planets, The Road Up, four pages teaching Create and Aeronautics, The Moon, The Rot, Gear and Growth,
Together), loaded by the real FTB Quests on a dedicated server; the first two pages were **seen once in a client**, the
others **not yet**. Rockets are ships with Create Simulated Thrusters, and helium is their fuel (ADR-0025). The book says what and where,
never why, and never gates a path. **`docs/progression/journey.md` is the step-by-step map of the whole game, what every
material is for, and what is missing** (no survival-built ship has ever flown, helium has no job, the 26 combat items have
no source); `quests.md` section 13 says where the work stopped.

Each works in its own worktree and merges only when Sanchit says. Tier 2 compat
jars: chunky, create, cataclysm, endermanoverhaul, jade, immersivedamageindicators, distanthorizons,
sable (`docs/technical/architecture.md` has the table).

**Alpha plan (2026-10-19, Sanchit):** a CurseForge modpack, free for anyone to download, on a server we run ourselves; the alpha's stopping point is **Planet 4, the End as an asteroid belt, with the Ender Dragon and the Elytra**, and ship air is a blocker. The list of what is left, in order, is [`plans/alpha-checklist.md`](plans/alpha-checklist.md). **Planet 4 (2026-10-20, branch `planet-4-end`, ADR-0028, not merged):** the vanilla End is Planet 4, breathable; its gate is the Dragon's exit portal (`EndGate`, no key, no credits); falling out of the bottom hands you back to space (`VoidFall`, `falls_to_space`); ships land at `arrival_point` `[224, 0]`; the Dragon is 4,550 HP Normal, Armor 190, Magic Resist 90; the predator world is Planet 5; a nine-quest page, `docs/technical/end-art-format.md` for the art session. Checked on a dedicated server (`end-checks.txt` 18/18, `damage-checks.txt` 77/77), **not seen in a client**: `docs/technical/planet-4-client-checklist.md`. **Design and lore redone 2026-10-21 (branch `end-design`, off `planet-4-end`, unmerged, not seen in a client; prompt 10):** a real asteroid belt from space (a flat ring of 1,400 tumbling irregular rocks, dust, a bright core: `BeltRenderer`), five ruins (the Ring of Ten, the Quiet Cell, the Mooring, the Cloister, the Last Lamp) with three new Sealer carvings, eight tablets (seven the voice, one the Sealers' Cut Record), and the lore as prose in `docs/lore/the-end.md` (awaiting Sanchit's nod). See `docs/technical/end-art-format.md` sections 1 and 5.

**Start a new session at [`plans/m3-vertical-slice.md`](plans/m3-vertical-slice.md), section
"Where M3 stands — start here"**: what is built, what is verified, the decisions waiting on
Sanchit, and what is next.

**M3 is in progress.** Seen working in Sanchit's client: the gateways (walk-through multiblock
frames, data-driven, `gateway-frame-format.md`) and dead vanilla portals (ADR-0014) on
2026-10-06; on 2026-10-10 the **Lunar Archive** generating on the Moon with its eight tablets in
the voice, Earthrise, **the mark** on the hand stepping with lit gates, no clouds on the Moon or in
space, and our own Moon in Earth's sky in place of vanilla's. Built but not yet seen: the Warden
tendril drop and crafting Earth's keystone, the Cataclysm bosses' armour fix, and share-air
(needs two clients). **The lore** is in `docs/lore/` (start at `what-the-warden-keeps.md`): the
Observers, the trap, the mark, Tacita. **Planet 3 (M3.3) is the real Nether, opened to the sky**
(ADR-0017): reached by airship, wear gold against the rot. Its logic is built and checked headless
(roofless terrain, the rot, the gate ruin in the warped forest, the Monstrosity's key, seven tablets
in the voice); its art was made by a design session and merged (`nether-art-design.md`), and
next is one client test of the whole world. The Moon is at 4,000 blocks (2026-10-11). **Background pre-generation is off by default** since a fresh server
crashed under it (the Chunky bug in the M3 plan, root cause open).

**The road to space is by airship** (ADR-0015, Sanchit 2026-10-11): a Create Aeronautics ship
climbing past a planet's build height is carried to space with its crew, and one reaching a planet
in space comes down to it (`ascension-compat-sable`, through `ascension-worlds`' public
`SpaceTravel` and `AscentCarrier`). Survival players can no longer rise to space on foot. Aeronautics propellers need brass, which
Create only makes with a Nether blaze, so a titanium recipe for the plain propeller bearing
(2026-10-12) is what lets a survival ship leave Earth. **Seen
live 2026-10-11:** Sanchit rode a command-built ship from Earth into space and down onto the Moon;
not yet seen with a real Aeronautics ship, because no starter ship has been designed. Planets in
space are drawn as the same lit spheres the skies show. Cataclysm's Earth bosses are held for
later planets.

**The resource economy** (2026-10-12): `docs/progression/resource-economy.md` says what every
material is for. **No world is ever required** (ADR-0018): players go where they choose, and
materials reward rather than unlock. The combat items get League-style build paths across worlds,
the strong ones finished with a boss's heart; that contract is the combat session's to build.
**Every build runs `tools/resource-ledger.py --check`** (needs Python on PATH): it fails when an item
we register has no source or a material has no use, and regenerating it rewrites
`docs/progression/resource-ledger.md`. Known gaps are excused, with reasons, in
`tools/resource-ledger-rules.json`.

**Oxygen v2 (2026-10-19, merged to `main`, not pushed; seen in Sanchit's client except ships; ADR-0026, `docs/technical/oxygen-v2.md`):** air is
a fluid (shown as Liquid O2); the Create-powered **Air Compressor** (in `ascension-compat-create`, its first Java) makes it and fills tanks by right-click; the
**Air Dispenser** takes it from pipes and holds a sealed room; a **helium tank** holds twice as much; `ascension-compat-sable` registers
a ship space. Air depends on Create (the old emitter and refill station are legacy, no recipe). New art by `tools/generate-atmosphere-art.py`.
The quest book's three air quests are updated; its flight lines wait until a ship is seen holding air.

The history below is M0–M2, kept for context.

Done and verified in game on a dedicated server:

- M0 toolchain, M1.1 API design (frozen), M1.2 providers, M1.3 sync + HUD, M1.4 refactor,
  M1.5 drain / failure / water, M1.6 sealed volumes.
- Lungs (20s, refill free in breathable air, drawn last) vs tanks (drained first, never
  self-refill). Gear grows lung capacity rather than reducing drain, so enchantments cannot
  silently extend a tank.
- Water is an unbreathable atmosphere at the `DIMENSION` band; vanilla bubbles and drowning are
  suppressed while active, switchable via server config `waterIntegration`.
- Oxygen Emitter pressurises a sealed room (4096 blocks / 24 radius), invalidated on block
  change.

- M1.7 tank item and refill station. Tank charge is a data component; the station only works
  where the air is breathable, which is what makes the expedition loop a loop and composes with
  sealed rooms for free.
- M1.8 the valve (ADR-0009). Only an *open* tank supplies air, one at a time, opening costs a
  3s pressurise delay, inventory holds two. **Verified in game.**

The test environment is now a 15-mod stack — see
[`docs/technical/optimisation-stack.md`](docs/technical/optimisation-stack.md). Dev runs mirror
the CurseForge instance's mods folder into `run/*/mods` and use generational ZGC. First
full-stack reading is in `docs/technical/performance-log.md`: our whole heap footprint is
**1,408 bytes across 68 instances**, every count a singleton, which is the first direct evidence
ADR-0007 rule 1 holds.

**Outstanding from M1, carried not cancelled:** the M1.9 refactor pass (ADR-0008 schedules one
every third increment; see the refactor-debt note below) and two-client testing. Share-air
rescue is now built (2026-10-05) but, needing two players, is verified by that same two-client
session. All three are listed in `plans/m2-worlds.md`.

The M0.5 baseline is **retired as a comparison target** — it was taken in single-player with no
third-party mods, and neither is true of how we test now. See `performance-log.md`.

Scope decision, revised: **M2 (`ascension-worlds`) started before M1.9 and before the Curios
jar**, at Sanchit's call. M1.9's refactor pass and two-client testing are carried forward in
[`plans/m2-worlds.md`](plans/m2-worlds.md) under "Carried over from M1, unresolved" — they are
outstanding, not cancelled.

**M2 is done, called 2026-09-06.** Three modules now exist — `ascension-worlds` is the third.
Orbit is one shared interplanetary space dimension (ADR-0010, superseding ADR-0004's deferral);
Tier 1 modules never depend on each other and share contracts in `core` instead (ADR-0011, which
amends ADR-0003 and makes `core` more than a stub for the first time). Planet schema in
`docs/technical/worlds-api.md`; full increment-by-increment history in `plans/m2-worlds.md`.

The Moon is a datapack planet with no per-planet Java, and **it suffocates you with
`DebugAtmosphere` off** — so `atmosphere` has now been tested against a world it was not built
alongside, and the debug vacuum is a dev tool rather than the only vacuum in the project. Its
worldgen is real terrain (M2.3): two biomes, craters, mountains, no placeholder flat gravel left.
The full loop — leave Earth, orbit, descend to the Moon, survive, return — is playable and
confirmed live (M2.5/M2.6): ascent and descent through a shared void space dimension, a pre-load
ticket plus Chunky pre-generation to keep the transition off the loading-screen's back, and
per-player return-location memory keyed to where each player ascended from, not where they were
standing (co-op-safe by construction). M2.7 pulled the pure logic out of that into 13 tests
(`worlds` had zero before) and removed one public event that had no real use left.

The Moon also has real weather now: `ascension_worlds:dust_storm`, a per-planet hazard timer
independent of vanilla's weather clock (which turns out to be **shared with the Overworld
regardless of dimension** — `SpaceSpecialEffects`/`MoonSpecialEffects` exist specifically to
guarantee that can never leak into an airless world). A custom particle, a screen-space overlay
that survives a renderer swap like Sodium's, and a five-second build/wind-down ramp rather than a
snap on/off.

**Distant Horizons is back in the stack**, after a same-day drop and fix. It was corrupting
already-generated chunks against this module's own worldgen; root cause was one specific
generator-mode setting (`distantGeneratorMode = FEATURES`) that DH's own config warns can
misbehave against non-vanilla generators. Fixed by switching to `PRE_EXISTING_ONLY` — DH now only
ever renders real, Chunky-generated terrain, never guesses at its own — which also means Chunky's
pre-generation radius is now DH's effective view distance, not just an arrival-lag fix. See
`docs/technical/optimisation-stack.md`.

**M2.4 refactor pass done.** ADR-0008 schedules one every third increment; the last was M1.4
and five had landed since (M1.5–M1.8, M2.2), so this closes that debt rather than adding to it.
Added 64 unit tests across `core` and `atmosphere` (was 17, all in `core`) — not by writing tests
around existing code, but by pulling the pure arithmetic out of `OxygenTracker`, `TankRules` and
the tank/HUD display code into small classes (`OxygenAccounting`, `TankCarrySweep`, `OxygenLevel`)
that take numbers in and give numbers back, no `ServerPlayer` required. That is what ADR-0008's
"unit tests are never evidence for world state" rule leaves testable, and until now nothing in
`atmosphere` had been split out that way.

Found two real things doing it: `OxygenTracker`'s at-risk path gathered a player's oxygen sources
twice per accounting pass (once to spend, once to summarise) — the exact double inventory-walk
its own comment warned against; and the tank item's durability bar and the HUD bar each carried a
separate copy of the same two thresholds (30s/10s), which is the failure mode
`AtmosphereTuning.formatDuration`'s own javadoc names for duration strings but had not been
caught for colour bands. Both fixed; `OxygenLevel` is now the one place those numbers live.

Line count did not shrink — comment density here is deliberate (see the Style note below) and
extraction adds a docblock per class, so 4 new files land at roughly the same total. What changed
is that the arithmetic deciding how fast a player dies is now checked by 25 tests
(`OxygenAccountingTest`, `OxygenLevelTest`) instead of by standing in a vacuum and counting.

Further atmosphere ideas mostly depend on modules that do not exist yet and are queued in
`todo.md` under "Atmosphere follow-ups" — including replacing the placeholder emitter with a
Create-powered generator → tank → pressuriser chain in a Tier 2 compat jar.

## Style

Sanchit's stated priorities, in order: compatibility (mods usable standalone by others, acting
as connectors between mods) → performance (no excess RAM, no leaks) → slow and steady with
frequent refactors → test in game at every step. Do not trade the first two for speed.

**Art:** every texture is original, generated by our own seeded scripts in `tools/`, never copied
from vanilla, another mod or a pack (legal safety, Sanchit 2026-10-06). Read
[`docs/technical/art-guide.md`](docs/technical/art-guide.md) before making any texture, model or
structure look. The combat-item art track (League likeness; 3D in-hand models under way) starts at
[`plans/combat-art-3d.md`](plans/combat-art-3d.md).
