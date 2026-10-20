# Server pack configuration

Configuration for a **real multiplayer server**, kept in git rather than in `run/server`, which
is gitignored and thrown away.

`config/` here is copied over the dev server's config by `prepareServerRun`, so the dev server
runs the same configuration we ship. **Settings that a single-player world needs too live in `pack/config/`, not here** (Cataclysm's boss multipliers moved there on 2026-10-21, with the Waystones rule), so the server and a player's own world agree. The real server must carry both folders.

Files not listed here are left alone — Chunky's
`config/chunky/tasks/` in particular holds live task state and must never be clobbered.

## Putting it on a host

`python tools/build-server-pack.py` writes `build/server/ProjectAscension-server-<version>.zip` (about 195 MB): `mods/` (the same 50 third-party
jars the dev server loads, minus anything client-only, plus our 17), `config/` (this folder and `pack/config`), a `server.properties`
with the settings below, `user_jvm_args.txt` and a `SERVER-README.md` with the steps. It carries three mods that forbid redistribution
(Create Aeronautics, Entity Culling, Keybind Atlas), so it is for our own server only and is never published. The EULA is accepted
on the host by whoever owns the server. **Not yet booted from the built jars on a clean server directory** (the dev server loads our code
from source); see `plans/alpha-checklist.md` A6.

## The End (Planet 4)

The End is `minecraft:the_end` itself (ADR-0028). The central island and the Dragon's arena load with the first player.
The outer islands and the End cities (where the Elytra is) lie about 1,000 blocks or more out and generate as players
fly there at Elytra speed, which is the costly moment. Advice, **not yet measured** (a `spark` reading is owed, see
`docs/technical/worlds-api.md`, "Planet 4"):

```
/chunky world minecraft:the_end
/chunky center 0 0
/chunky shape square
/chunky radius 1600
/chunky start
```

Run it by hand with nobody flying, watch memory, and `/chunky pause` if it climbs (the same crash as above). A
smaller ring (radius 1200) covers the first End cities. Nothing pre-generates the End on its own.

## Pre-generating the spawn area

> **Off by default since 2026-10-10.** A pre-generation on a fresh server crashed it with
> nobody online: generated chunks were not unloaded while Chunky ran, and the 6 GB heap filled
> in about six minutes (`plans/m3-vertical-slice.md`, the Chunky bug). Until that is found,
> nothing pre-generates on its own:
>
> - `config/chunky/config.json` ships `"continueOnRestart": false`, so a restart does not
>   resume an interrupted task. On the dev server this also stops the dev world's two saved
>   tasks (Earth and the Moon) starting together at every boot, which is what crashed it first.
> - `ascension-compat-chunky`'s own background pre-generation of every planet is off:
>   `config/ascension_compat_chunky-server.toml`, `[pregeneration] background = false`.
>
> Pre-generating by hand still works, below. Watch memory while it runs, and `/chunky pause` if
> the heap climbs: pausing lets the held chunks save and unload.

**Chunky can resume its own task across restarts** (`"continueOnRestart": true`). That was the
answer to "start generating when the server starts": there is no startup hook to write, because
Chunky already persists task state to `config/chunky/tasks/` and picks it up again on boot. It
is off for now, as above; with it off, `/chunky continue` resumes a task by hand.

Start the task **once**, as an operator:

```
/chunky world minecraft:overworld
/chunky center 0 0
/chunky shape square
/chunky radius 4000
/chunky start
```

With `continueOnRestart` on, every restart continues it automatically until it completes. `/chunky progress`
reports where it is; `/chunky pause` and `/chunky continue` are manual overrides.

### What radius 4000 actually costs

A square of radius 4000 is 8000 × 8000 blocks — **500 × 500 = 250,000 chunks**.

| | |
|---|---|
| Disk | roughly **2–3 GB** of region data for the overworld |
| Time | **30–90 minutes**, once, depending on CPU |
| Then | Distant Horizons wants to build LODs over all of it, which is a second and longer pass |

Worth doing for a multiplayer pack: the spawn area is the terrain everybody crosses, so
generating it once removes the worst source of tick spikes from the place it is felt most.

**Run Chunky before turning DH loose, not alongside it.** DH warns about this itself: Chunky
generates faster than DH converts to LODs and the LODs come out with holes.

## `server.properties` that matter

Not shipped as a file — it carries per-server values like `level-name` — but these are the
settings with a reason behind them:

| Setting | Value | Why |
|---|---|---|
| `online-mode` | **`true`** | **The dev server runs `false` and a real one must not.** Offline mode authenticates nobody: anyone can connect as any username, including an operator's. |
| `white-list` | `true` | With `online-mode=true` this is belt and braces; without it, it is the only thing standing between the server and the internet. |
| `view-distance` | `8` | Deliberately modest. Distant Horizons covers distance visually; raising this pays twice for one result. See [`optimisation-stack.md`](../docs/technical/optimisation-stack.md). |
| `simulation-distance` | `6` | Ticking radius, which is the expensive one. |
| `allow-nether` | either | Only stops portal travel into the Nether, which `ascension-worlds` already makes dormant (ADR-0014). The Nether is Planet 3 (ADR-0017) and still loads either way; ships and gateways reach it. See below. |
| `max-players` | as needed | The dev server ships `4`. |
| `difficulty` | `normal` | Suffocation damage is tuned against vanilla drowning. |

### JVM arguments

```
-XX:+UseZGC -XX:+ZGenerational
```

Both flags together — generational ZGC is opt-in on Java 21, and the non-generational collector
is markedly worse here. Same collector as the dev runs (`jvm_gc` in `gradle.properties`) so the
two are comparable.

## Locking the Nether and the End

Superseded: `ascension-worlds` makes vanilla's Nether and End portals dormant on its own
(ADR-0014, server config `vanilla.disableVanillaPortals`), in single-player too. And the Nether is
no longer locked at all: it is **Planet 3** (ADR-0017), opened to the sky and reached by airship
from space. `allow-nether` is read in exactly one place in vanilla, its portal code, so either
value leaves the Nether loaded and reachable by ship. Leaving it `false` changes nothing.

