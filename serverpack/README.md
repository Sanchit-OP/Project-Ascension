# Server pack configuration

Configuration for a **real multiplayer server**, kept in git rather than in `run/server`, which
is gitignored and thrown away.

`config/` here is copied over the dev server's config by `prepareServerRun`, so the dev server
runs the same configuration we ship. Files not listed here are left alone — Chunky's
`config/chunky/tasks/` in particular holds live task state and must never be clobbered.

## Pre-generating the spawn area

**Chunky resumes its own task across restarts.** That is the answer to "start generating when
the server starts": there is no startup hook to write, because Chunky already persists task
state to `config/chunky/tasks/` and picks it up again on boot.

`config/chunky/config.json` sets it:

```json
"continueOnRestart": true
```

Then start the task **once**, as an operator:

```
/chunky world minecraft:overworld
/chunky center 0 0
/chunky shape square
/chunky radius 4000
/chunky start
```

From then on every restart continues it automatically until it completes. `/chunky progress`
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
| `allow-nether` | `false` | Dimension access is progression, and progression is not built yet. A stopgap — see below. |
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

`allow-nether=false` closes nether portals. **The End has no equivalent server property**, and
is currently gated only by needing a stronghold and twelve ender eyes.

This is a stopgap and should be recorded as one. **Dimension access is the spine of this
project's progression**, so gating it belongs in `ascension-progression` (M3) as a designed
mechanic, not in a server property. Anything built on top of the property will have to be undone.
