# Development Environment

## Toolchain (pinned from maven, 2026-09-05)

| | Version | Pinned in |
|---|---|---|
| Minecraft | 1.21.1 | `gradle.properties` |
| NeoForge | 21.1.249 (final 21.1 release) | `gradle.properties` |
| Parchment | 2024.11.17 | `gradle.properties` |
| Java | Temurin 21.0.12.1 LTS | `gradle.properties` (`org.gradle.java.home`) |
| Gradle | 8.14.4 | `gradle/wrapper/gradle-wrapper.properties` |
| ModDevGradle | 2.0.146 | `gradle/libs.versions.toml` |

**Why Gradle 8.x and not 9.x:** ModDevGradle documents compatibility with Gradle 8.8 and
states nothing about Gradle 9. 8.14.4 is the final 8.x release. Revisit when MDG declares
Gradle 9 support.

**Why NeoForge 21.1.249:** it is the last release of the 21.1 line — development moved to 26.x
— so our target platform is frozen rather than drifting under us.

### Dependency version ranges: floor, not pin

`neoforge_version` is what we **compile against** (21.1.249).
`neoforge_version_range` is the **compatibility floor** we declare to the loader, and it is
deliberately looser: `[21.1,)`.

Pinning the declared range to the compile version would make our mods refuse to load on any
slightly older 21.1.x build — for example the NeoForge 21.1.227 instance already on this
machine. For a project whose first priority is that other people can actually use these
modules (ADR-0003), that is the wrong default.

**Rule:** the declared floor is the lowest version a module genuinely needs. Raise it only
when a module starts using an API that requires it, and say so in the commit. Never raise it
automatically just because we bumped what we compile against.

### Version single-sourcing

Two files, one rule each. Never duplicate a version across both.

- **`gradle.properties`** — runtime platform versions (Minecraft, NeoForge, Parchment) and mod
  metadata. These live here because they are also interpolated into `neoforge.mods.toml` at
  build time.
- **`gradle/libs.versions.toml`** — Gradle plugin and Java library versions.

## Machine setup (done 2026-09-05)

- **Temurin JDK 21.0.12.1 LTS** — `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`
- `JAVA_HOME` set at user scope; `%JAVA_HOME%\bin` is first on the user PATH.
- CurseForge at `C:\Users\sanch\curseforge` — used for production-instance testing.
- Gradle is not installed system-wide and does not need to be; the wrapper provides it.
- **Python 3 on PATH as `python`.** Since 2026-10-12 every module's `check`, and so every `build`,
  runs `tools/resource-ledger.py --check` (standard library only). The art generators in `tools/`
  also need Pillow.

### The Java trap, and what was actually wrong

The user PATH contained `C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot\bin`, but that
directory **no longer existed**. PATH fell through to Oracle's `javapath` shim, so `java`
resolved to a **Java 8 JRE** and `javac` was absent entirely.

Fixed by removing the dead entry, setting `JAVA_HOME`, and putting JDK 21 first on PATH.
The previous PATH was backed up to `C:\Users\sanch\user-path-backup-20260905.txt`.

Belt and braces: the build also pins `org.gradle.java.home` in `gradle.properties`, so it uses
the right JDK regardless of PATH or of which shell it is launched from. If you ever see an
"unsupported class file version" error, check that pin first.

### Wrapper distribution check

`validateDistributionUrl=false` in the wrapper properties. The wrapper's URL liveness check
fails behind redirect-restricted networks; `distributionSha256Sum` is retained and is the
stronger integrity guarantee — it was verified against Gradle's published checksum before the
wrapper was generated.

## Commands

Build everything:

```bash
./gradlew build
```

Launch a dev client:

```bash
./gradlew :modules:core:runClient
```

Launch a dedicated server with every module loaded — required before merging anything touching
world state, per ADR-0008:

```bash
./gradlew runDevServer
```

List modules and their tier:

```bash
./gradlew moduleReport
```

Run directories are created under `run/` at the repo root and are gitignored.

## Build structure

Shared configuration lives in `buildSrc` convention plugins, so a module build script is two
lines and conventions cannot drift between modules.

- `ascension.java-conventions` — Java 21 toolchain, UTF-8, lint flags, reproducible jars
- `ascension.mod-conventions` — NeoForge, Parchment, run configs, `mods.toml` interpolation,
  jar manifest
- `ascension.dev-runtime-conventions` — seeds fresh `run/*` directories with dev-friendly
  Minecraft options

#### Dev game options

Minecraft writes `options.txt` only *after* first launch, so every brand-new run directory
shows the accessibility onboarding prompt before the main menu, pauses when it loses focus,
and replays the movement tutorial. We create run directories constantly, so `seedDevGameOptions`
pre-writes four keys:

| Key | Value | Why |
|---|---|---|
| `onboardAccessibility` | `false` | Skips the narrator prompt before the main menu |
| `pauseOnLostFocus` | `false` | Game keeps running while alt-tabbed — pausing mid-test
invalidates timing observations |
| `tutorialStep` | `none` | No movement tutorial toasts |
| `narrator` | `0` | Narrator explicitly off |

It is idempotent and never clobbers existing settings: it creates `options.txt` when missing,
and otherwise appends only keys that are absent. It runs automatically before `runClient`,
`runServer` and `runData`.

This covers *development* only. Shipping the same defaults to players on their first launch
is a separate pack-level task, tracked in `todo.md`.

**Mod ids are derived from the project path, never hand-written:**

| Project path | Mod id |
|---|---|
| `:modules:core` | `ascension_core` |
| `:modules:atmosphere` | `ascension_atmosphere` |
| `:modules:compat:sable` | `ascension_compat_sable` |

This makes the ADR-0003 tier structure visible in the mod id and removes a class of
copy-paste mistakes.

### Configuration cache

Currently **off** (`org.gradle.configuration-cache=false`). ModDevGradle's compatibility with
it is unverified. Enable it, confirm `runClient` still works, and record the result before
leaving it on.

## The test loop we actually use

1. `./gradlew build`
2. Copy the jars into the CurseForge test instance:
   `C:\Users\sanch\curseforge\minecraft\Instances\Ascension Dev\mods\`
3. **`./gradlew runDevServer`** (dev server on `localhost`, port 25565)
4. Join from the **Ascension Dev** CurseForge instance via Multiplayer

### The scripted integration run (combat: stats, runes and growth together)

`client/dev/IntegrationChecks` plays the merged systems against each other in a real client, on the
real server inside it, and prints `PASS` / `FAIL` lines tagged `[integration]`. No one has to click.
It takes about three minutes a part, and opens a game window.

1. Copy a **fresh** test world: `run/server/runes-test` to `run/client/saves/runes-test` (replace the old
   copy; a run leaves its changes in the world, and the next part one then fails on the leftovers).
2. Create an empty file `run/client/integration-checks.txt` (**part one**), then `./gradlew runDevClient`.
   It opens the world by itself, earns milestones, wears netherite, dies and respawns, respecs, takes
   a hit, writes `run/client/integration-state.txt`, and quits (which saves the world).
3. Create `run/client/integration-checks-2.txt` (**part two**) and run it again: the same world is
   loaded from disk and compared with the state file.
4. Read the result with `grep "\[integration\]" <log>`; the last line is `RESULT: N checks, M failures`.

Enchantments are checked in the same run, by their own class (`EnchantmentChecks`): enchanted gear shows its
stat under "Enchantments", the data enchantments are offered by the table, trades and loot, and Respiration
grows the lungs. **Wait for the previous client to close before starting the next run**: two clients at once
starve each other and make timing checks fail for no reason (seen 2026-10-18, 13 false failures).

The trigger file is deleted when it is read, so it arms one run. The screen-link check presses the
character sheet's own default key, not a fixed one, because the pack moves it (`pack/config/defaultoptions`).
A dev client also hides FTB Library's "Hello from FTB Library!" (`client/dev/DevChatFilter`): the mod
sends it only in a development environment, so players of the finished pack never see it. Add a check there whenever two of
the three systems meet somewhere new; a unit test cannot see these (ADR-0008). The checks on 2026-10-18
found two real faults: discovery advancements that gave every player nine free points, and a respawn
that healed before gear's Health was back.

### The scripted team run (a dedicated server with stand-in players)

`ascension-compat-ftbteams`' `TeamChecks` plays the team rules (`docs/progression/teams.md`) on a
dedicated server with three stand-in players, real `ServerPlayer`s with no client behind them (the
connection is `FakeConnection`, which tells NeoForge it can carry any payload). It uses FTB Teams' own
commands, the real advancements and the real damage event, prints `PASS` / `FAIL` lines tagged
`[team-checks]`, and stops the server. No game window opens.

1. In `run/server/server.properties` set `level-name` to a throwaway name (and put it back afterwards),
   and copy a world there, for example `run/server/runes-test` to `run/server/team-test` (delete its
   `playerdata` and `advancements` folders for a clean start). The dev server defaults to creative; the
   stand-ins switch themselves to survival.
2. Create an empty `run/server/team-checks.txt` (**part one**), then `./gradlew runDevServer`. About two
   minutes. It leaves one stand-in missing a milestone his party holds.
3. When the server has stopped, create `run/server/team-checks-2.txt` (**part two**) and run it again:
   the party and the saved team state are read back from disk.
4. `grep "team-checks\]" run/server/logs/latest.log`; the last line is `RESULT: N checks, M failures`.

What it cannot see is anything a client draws or sends: FTB's My Team screen and button, the chat line as
a player reads it, and the HUD's team list when it exists. Those wait for a real client.

### The quest book (`tools/generate-quests.py`, `ascension-compat-ftbquests`)

The book is data, written as readable Python under `pack/quests/` (one module per page, listed in
`book.py`; the rules for writing one are in `docs/progression/quests.md`). Nothing is edited in the game.

1. **Write or change a page**, then `python tools/generate-quests.py`. It checks the whole book (items,
   advancements and dimensions that are ours must exist, no guarded story word from
   `pack/quests/guarded-words.txt`, no dependency loop, no two quests on one spot, pictures and shapes
   present) and writes `pack/config/ftbquests/quests/` (what FTB Quests loads) and
   `modules/compat/ftbquests/.../quests/chart_layout.json` (where everything is on the Planets picture).
   `./gradlew build` runs it with `--check` and fails if the files are stale; its own tests run beside it
   (`python -m unittest discover -s tools/quests/tests -t tools`).
2. **Pictures** (needs Pillow and numpy): `python tools/generate-quest-art.py` redraws the theme, quest
   shapes, chart, planet spheres, rings, halos and icons. Seeded, so a rerun changes nothing. The planets
   are drawn from the planet files' colours; a new planet is its JSON, a rerun of both tools, and a page.
3. **Copy into a game:** `python tools/sync-pack-config.py` puts the quest files into `run/client/config`,
   `run/server/config` and the CurseForge instance, and **removes quest files the pack no longer has**.
   The repo is the truth: anything changed with FTB's in-game editor is overwritten.
4. **Ids come from slugs**, so a page can be edited freely, but **never rename a slug that has shipped**:
   a player's progress follows the id.

Two checks that need a game, both dormant until their trigger file exists in the game directory:

- **`quest-checks.txt`** (a dedicated server, about a minute, no window): the real FTB Quests loads the
  generated files, every id in them must be an object it knows, and every item must be a real item. Prints
  `[quest-checks]` lines. Seen 2026-10-18: 2 chapters, 14 quests, 53 ids, 21 items, 0 failures.
- **`quest-screen-checks.txt`** (a client, about four minutes): opens `runes-test` (copy a fresh one into
  `run/client/saves`, as for the integration run), opens the real quest book at both pages in four states
  (new player, tank done, flying in space over the Moon, standing on the Moon), photographs each to
  `screenshots/quest-book/`, and reads the marker's pixels back. Prints `[quest-screen]` lines.

### Use `runDevServer`, not a module's own `runServer`

Each module also has its own `runClient` / `runServer`, and **each loads only itself and what it
depends on**. `:modules:atmosphere:runServer` has no idea `ascension-worlds` exists, so a
dimension that module declares is simply absent — and the symptom is

```
Unknown dimension ascension_worlds:moon
```

which points at the datapack rather than at the build, and costs an hour.

`./gradlew runDevServer` and `runDevClient` always launch the whole stack, and do not change as
modules are added. The per-module runs are still the right tool for one job: confirming a module
works alone (ADR-0003 rule 6), which is what `-PnoDevMods` is for.

### A dimension's generator settings are frozen into the world the first time it's created

Edit `dimension/moon.json` after `run/server/ascension-dev` already has a Moon &mdash; nothing
happens. Minecraft snapshots each dimension's `ChunkGenerator` config into `level.dat` the moment
that dimension is first touched in a save, and never re-reads the datapack for it again on later
starts. This bit us for real: flipping the flat generator's `"features"` flag from `false` to
`true` so Lunar Helium Ore could place did nothing for the existing test world, in any chunk, no
matter how far from spawn &mdash; the frozen `false` from the Moon's original M2.2 creation kept
governing every chunk generated afterward.

**Biome content is not subject to this.** A biome's own `features` list, and anything a
`neoforge:add_features`/`remove_features` biome modifier patches onto one, is read fresh from the
registry at each chunk's generation — which is why Titanium Ore worked on Earth immediately and
Helium Ore did not on the Moon, in the same session, from the same kind of edit.

**The fix is a new world, every time a dimension's generator settings change** — deleting only
that dimension's own subfolder is not enough, since the stale setting lives in `level.dat`, not
in the per-dimension terrain files. Stop the server first (`Get-Process`/`Stop-Process` on the
`DevLaunch` java process, or the process holding Distant Horizons' `.sqlite` files, if `rm -rf`
refuses with "resource busy") before deleting `run/server/ascension-dev`.

The instance is MC 1.21.1 / neoforge-21.1.249, matching what we compile against. `Devil0701` is
opped at level 4 in `run/server/ops.json`, using the offline-mode UUID because the dev server
runs `online-mode=false`.

**Restart the server after every rebuild** — the jar is read at startup.

### Third-party mods: the dev runs mirror the instance

This is a **development environment**, not a modpack.
[ADR-0002](../decisions/0002-custom-mods-not-curated-modpack.md) still stands and
[`mod-list.md`](mod-list.md) is still a landscape survey, not a dependency list. The performance
set and the reasoning behind it is [`optimisation-stack.md`](optimisation-stack.md).

Install mods through the CurseForge app for the instance, so it resolves the correct
1.21.1 / NeoForge build and any dependencies. **Then do nothing else** — the Gradle dev runs
read that folder and load whatever is in it:

```bash
./gradlew devMods
```

`dev_mods_dir` in `gradle.properties` points at the instance's mods folder. `prepareClientRun`
and `prepareServerRun` copy from there into `run/client/mods` and `run/server/mods`, so what a
run will load is a directory you can list:

```bash
ls run/server/mods
```

Our own jars are always skipped: the dev run already has that code from source, and loading the
built jar alongside it is a duplicate mod id and an immediate crash. The target folder is
**cleared** each time — a mod removed from the instance has to disappear here too, or the server
ends up requiring a channel the client no longer has.

> **Not the runtime classpath.** The first version of this added the jars to
> `clientAdditionalRuntimeClasspath` / `serverAdditionalRuntimeClasspath`. Everything about that
> looked right from the build's side: coordinates resolved, jars appeared in the generated
> classpath file, server booted clean. **None of them loaded.** FML does not discover mods by
> scanning the runtime classpath in a dev run, and a mod that never loads breaks nothing at
> startup — so the failure was completely silent until a client tried to connect and was refused
> over a missing channel. `<gameDir>/mods` is the folder FML genuinely scans.

#### Why mirroring rather than a list of versions

The dev server must carry the same mods as the client, and **both directions bite.** Curios
registers a network channel, so a client that has it refuses a server that does not:

```
Curios break channel of mod curios api failed to connect:
the channel is missing on the server side but required on the client
```

— and a server that has it refuses a client that does not. That second case is not
hypothetical: it is what happens if a mod is removed from the instance while a build file still
names it.

Two mods pinned by version in `gradle.properties` was already a list that could drift. Fourteen
would be a list that *would* drift, silently, in whichever direction someone last clicked in
CurseForge — and the symptom is a connection refused, which does not look like a stale build
file at all. Mirroring makes the two impossible to disagree.

JEI needs its server half too, incidentally. Recipe *viewing* works from a client alone because
recipe data is synced anyway, but recipe **transfer** — the `+` button that fills a crafting
grid — sends a packet nothing was there to receive.

#### Client-only mods are kept off the server automatically

`@Mod(dist = Dist.CLIENT)` is *not* enough. Sodium registers an early-bootstrap window service,
which FML loads through `ServiceLoader` **before** mod loading — so the dist annotation never
gets consulted and the dedicated server dies before printing a single mod name:

```
Exception in thread "main" java.lang.NoClassDefFoundError: org/lwjgl/Version
    at ...sodium.client.compatibility.checks.PreLaunchChecks.isUsingKnownCompatibleLwjglVersion
```

So the build works it out from each jar. NeoForge has no mod-level `side` field in
`neoforge.mods.toml` — the toml carries a `side` on each *dependency* — and a mod whose every
declared dependency is `CLIENT` is asserting it needs nothing from a server. Across this
instance that reads exactly right: it picks out Sodium, ImmediatelyFast and BadOptimizations,
and correctly leaves ModernFix alone, which has one client-side soft dependency but genuinely
runs on both.

It is a heuristic, so `./gradlew devMods` reports the split rather than applying it silently:

```
  client   sodium-neoforge-0.8.13+mc1.21.1.jar  (declares client-only dependencies)
  both     lithium-neoforge-0.15.4+mc1.21.1.jar
```

For anything the heuristic misses — a mod that declares `BOTH` and then crashes the server
anyway — name it by filename fragment:

```
dev_mods_server_exclude=some-mod,another
```

This asymmetry is correct and matches how a real server pack is built: the client gets the
renderer, the server does not.

#### Checking the standalone case

```bash
./gradlew :modules:atmosphere:runServer -PnoDevMods
```

(A module's own run, deliberately — this is the one case where loading less is the point.)

Loads no third-party mods at all, which is how to confirm a Tier 1 module works alone
(ADR-0003 rule 6). A client with those mods installed will refuse to connect, which is the
point: that configuration is for verifying the module loads, not for playing.

**None of this weakens rule 6.** What enforces "atmosphere works with only `core` present" is
the *compile* classpath, and none of these are on it — Tier 1 cannot reference a class it cannot
see, and a jar present at dev runtime cannot create a compile dependency.

### Profiling a run

```bash
./gradlew runDevServer -Pjfr
```

Adds a Java Flight Recorder recording to the run, written to `run/server/ascension-server.jfr`
when the server exits **cleanly**. Needs no mod: JFR is in the JDK the build already pins. Full
instructions, including how to read the file, are in
[`performance-log.md`](performance-log.md#profiling-with-jfr).

### Debug commands

```
/ascension atmosphere query      breathability, drain, lung reserve, valve state
/ascension atmosphere why        every provider that claimed this position, and which won
/ascension atmosphere volumes    emitter count, pressurised blocks, whether your head is inside one
/ascension atmosphere debug vacuum | clear
/ascension atmosphere debug oxygen <units>
/ascension atmosphere debug tank <units>    charge of the tank in your main hand
```

`why` and `volumes` exist because two separate bugs were invisible without them: a provider
losing to a higher band looks exactly like a provider that does not work.

```
/ascension worlds weather                       this planet's storm clock
/ascension worlds weather force                 (op) flip it next tick
/ascension worlds gateway goto <planet>         (op) teleport in front of that planet's gateway:
                                                the attuned one, else the pre-generated one
                                                (generates its chunk if nobody has been there)
/ascension worlds gateway build [active]        (op) a complete frame 2 blocks in front of you,
                                                dormant, or attuned and lit
/ascension worlds gateway travel <planet>       what the destination screen does; non-ops need
                                                to have just used a lit portal
/ascension worlds gateway light <id>            (op) mark a gate lit in the network, no frame
                                                needed; any id (ascension_worlds:test_3...) steps
                                                the mark through stages that have no world yet
/ascension worlds gateway unlight <id>          (op) put it out again; the mark steps back down
/ascension worlds archive tablet <entry>        (op) a tablet carrying that archive entry
/ascension worlds archive plinth <entry>        (op) point the plinth you're looking at at an entry
/ascension worlds debug chunks                  (permission 3) vanilla's debug report per dimension; see "What is holding chunks in memory"
/ascension sable carry <dimension> <x y z>      (op, compat-sable) carry the ship you stand on, crew
                                                and all, with its centre at that point
/ascension sable carry ship <ship> <dim> <xyz>  (op) the same for a ship named by a Sable selector
                                                (@l latest made, @n nearest, @e all); works on RCON
```

The lore text itself lives in `tools/archive-lore.py`; run it after editing to regenerate the
entry files and the lore lang file.

Items that are otherwise behind a boss or a Warden:

```
/give @s ascension_worlds:warden_tendril
/give @s ascension_worlds:gateway_key[ascension_worlds:gateway_key_for_planet="ascension_worlds:moon"]
```

The gateway key needs that component: a key without it opens nothing (`GatewayRules.keyOpens`).

### Checking ship travel without a client

How the airship road (ADR-0015) was checked on 2026-10-11, over RCON on a throwaway world:

```
forceload add 0 0
execute in ascension_worlds:space run forceload add -16 596 16 628     # where Earth's ships come out
execute in ascension_worlds:moon run forceload add 0 0                 # where the Moon receives them
fill 0 200 0 4 200 4 minecraft:oak_planks
setblock 2 201 2 minecraft:chest{Items:[{Slot:0b,id:"minecraft:diamond",count:5}]}
sable assemble area 0 200 0 4 201 4
sable teleport @l 2 330 2                                              # past the line: goes to space
execute in ascension_worlds:space run sable info @e
execute in ascension_worlds:space run data get block 20481034 129 20481034 Items   # the chest, in the ship's slot
```

- **Load the destination first.** A ship teleported into an unloaded part of space is parked by
  Sable, invisible to `sable info` and to `ShipVoyages`, until its chunk loads. In play the crew
  keeps those chunks loaded.
- The chest's address is in the ship's storage slot, not the world; the first ship made on a fresh
  world has slot 0, so the address above holds.
- `sable teleport @e 4000 0 0` in space puts a ship inside the Moon's body, which sends it down.

### Checking generated structures without a client

How the Moon gateway's generation was verified before anyone flew there. Needs nothing beyond
the tools in `tools/worldgen-diagnostics/`, and never touches the real dev world:

1. Make sure no dev server is running (`session.lock` allows one per world).
2. In `run/server/server.properties`: `level-name=<throwaway>`, `enable-rcon=true`, a
   `rcon.password`.
3. `./gradlew runDevServer` in the background; wait for `RCON running` in its log. Since
   2026-10-10 nothing pre-generates on its own (the server pack ships Chunky's
   `continueOnRestart: false`, and `ascension-compat-chunky`'s background pre-generation is off),
   so a throwaway world is left alone. **If either is ever switched back on, run `chunky pause`
   the moment `RCON running` appears, and keep pausing until `chunky progress` says nothing runs.**
   - Chunky's tasks live in `run/server/config/chunky/tasks/`, which is shared by every world, not
     saved per world. A resumed task spends the dev world's progress (`chunks=` in each task file)
     on the throwaway world, and the dev world later skips that strip. Note the `chunks=` values
     before and put them back after; too low only re-checks existing chunks, too high leaves a gap.
   - Edits to `run/server/config/chunky/config.json` do not stick: `syncServerPackConfig` copies
     `serverpack/config` over `run/server/config` on every launch. Change the server pack's copy.
   - Late is not good enough. On 2026-10-10 a pause a minute late let both saved tasks run at once
     on a fresh world, and with no player online the heap filled and the watchdog killed the
     server (the Chunky bug in `plans/m3-vertical-slice.md`).
4. Force-generate the area: `RCON_PASSWORD=... python tools/worldgen-diagnostics/rcon_client.py
   execute in ascension_worlds:moon run forceload add <x1> <z1> <x2> <z2>`, wait a few seconds,
   then `save-all flush`.
5. `python tools/worldgen-diagnostics/structure_starts.py
   run/server/<throwaway>/dimensions/ascension_worlds/moon/region gateway` shows the start, its
   origin and facing. `/locate` cannot find `fixed_position` structures; this can.
6. Find a block wherever a structure put it: `python tools/worldgen-diagnostics/find_blocks.py
   run/server/<throwaway>/dimensions/ascension_worlds/moon/region ascension_worlds:mark_glyph`
   lists each one with its properties (after `save-all flush`). That is how the pylon's glyphs
   were checked lighting with the gates.
7. Spot-check blocks while the area is still force-loaded: `rcon_client.py execute in
   ascension_worlds:moon if block <x> <y> <z> <block>[state]` answers `Test passed` / `failed`.
   (Once `forceload remove` runs the chunks unload and every check reads "not loaded".)
8. `rcon_client.py stop`, put `level-name`, `enable-rcon` and `rcon.password` back, restore
   Chunky's `chunks=` values (step 3), delete the throwaway world.

Terrain shape (holes, heights) has its own scanner, `scan_region.py`; see
[`moon-terrain-tuning.md`](moon-terrain-tuning.md).

### What is holding chunks in memory

`/ascension worlds debug chunks` (permission 3, works over RCON) writes vanilla's own debug
report for every dimension to `run/server/debug/ascension-<time>/<namespace>/<dimension>/`: a
client gets the same from F3 + L, but a dedicated server has no command for it. `chunks.csv` lists
every loaded chunk with its level, status and the ticket holding it (`no_ticket` at level 45 means
marked for unload but not unloaded yet), plus its block and fluid tick counts. Pair it with a heap
histogram for the totals: `jcmd <pid> GC.class_histogram`, counting `LevelChunk`, `ProtoChunk`,
`ChunkHolder` and `ScheduledTick`.

### Leaving a mod out of the dev server

`-Pdev_mods_server_exclude=<name>,<name>` drops every jar whose file name contains one of those
from `run/server/mods`, without touching the CurseForge instance. **It replaces `gradle.properties`'
list rather than adding to it**, and that list keeps `immersivedamageindicators` off the server,
which crashes it. So always include it: `./gradlew runDevServer
-Pdev_mods_server_exclude=immersivedamageindicators,modernfix`. This is how the Chunky bug was
narrowed down.

## Testing expectations

Per [ADR-0008](../decisions/0008-build-and-test-cadence.md), compiling is not evidence.
Every increment is observed in a running client; anything touching world state or sync is also
verified on a dedicated server and across a world unload/reload.

Per [ADR-0007](../decisions/0007-performance-contract.md) rule 12, every milestone ends with a
measurement recorded in `docs/technical/performance-log.md`.
