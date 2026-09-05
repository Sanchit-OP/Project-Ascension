# Optimisation stack

Third-party performance mods for Project-Ascension. Every entry below was checked
**per-version** against NeoForge 1.21.1 through the Modrinth API on 2026-09-05 — a project
listing `neoforge` and `1.21.1` separately does not mean the pairing exists, and four popular
candidates fail exactly that way.

## This does not contradict ADR-0002

[ADR-0002](../decisions/0002-custom-mods-not-curated-modpack.md) says we build our own mods
rather than curating someone else's. That is about **gameplay**: progression, oxygen, worlds,
machines. These are **infrastructure** — they change how fast Minecraft runs, not what the game
is. Nothing here is a dependency of any Ascension module, and
[`mod-list.md`](mod-list.md) remains a landscape survey rather than a dependency list.

## The set

Install order matters only where noted.

### Client rendering

| Mod | Version | Why |
|---|---|---|
| **Sodium** | `0.8.13-neoforge` | The one that actually raises usable render distance: threaded chunk meshing, batched draws, compact vertex format. |
| **Distant Horizons** | `3.2.0-b-1.21.1` | Renders far terrain from an LOD database instead of loaded chunks. The only thing here that decouples apparent view distance from memory. |
| **EntityCulling** | | Skips entities the camera cannot see. |
| **MoreCulling** | | Skips hidden block faces. |
| **ImmediatelyFast** | `1.6.13+1.21.1-neoforge` | Batches immediate-mode rendering — GUIs, text, item entities. Pays for itself the moment a JEI screen or a full base of item frames is on screen. |
| **BadOptimizations** | `2.4.1` | Assorted client micro-optimisations. |

### Server / game logic

| Mod | Version | Why |
|---|---|---|
| **Lithium** | `0.15.4-neoforge` | General game-logic optimisation. **No conflict with Sodium** — same authors, deliberately split: Sodium is rendering, Lithium is logic. |
| **FerriteCore** | `7.0.3-neoforge` | Cuts memory used by block states and chunk data. Directly buys headroom for holding more chunks. |
| **ModernFix** | `5.27.24+mc1.21.1` | Memory and startup. Its dynamic-resources option is a large win on a pack this size. |
| **Chunky** | `1.4.23` | Pre-generates terrain. For a pack about travelling, this is the practical substitute for C2ME: it removes worldgen from the hot path rather than making it faster. |
| **Alternate Current** | `neoforge-mc1.21-1.9.0` | Faster redstone. Worth having once Create-driven machinery is common. |

### Measurement

| Mod | Version | Why |
|---|---|---|
| **Spark** | `1.10.124-neoforge-1.21.1` | Profiler. `/spark health`, `/spark profiler`, `/spark heapsummary` for a retained-object breakdown without leaving the game. The in-game counterpart to JFR on the dev server. |

## Do not install

| Mod | Reason |
|---|---|
| **Embeddium** | A fork of Sodium. Installing both means two copies of the same renderer. Pick one; Sodium is upstream. |
| **VulkanMod** | No NeoForge build. Also replaces the entire renderer and breaks most rendering mods. See below — it costs less than it looks. |
| **C2ME**, **VMP**, **Krypton** | No NeoForge build. |
| **Dynamic FPS** | Works on NeoForge, but **keep it out of the dev instance.** It throttles the game when unfocused, and `pauseOnLostFocus=false` is seeded into our dev runs specifically so alt-tabbing to an editor cannot invalidate a timing observation. Fine in the shipped pack. |
| Any "memory leak fix" mod | These paper over leaks — including ours. That is the last thing we want while our own code is the thing being measured (ADR-0007 rule 1). |
| **Fabric API**, **Mod Menu**, **Placeholder API** | Fabric plumbing, not optimisation. |
| **Noisium** | **Dropped 2026-09-05** after being recommended. Its only NeoForge 1.21.1 build is from August 2024 and it is not published to CurseForge, which is why searching there turns up third-party forks instead. See below. |

## Why no Vulkan renderer is not the loss it appears to be

The bottleneck on render distance is not the graphics API. It is three things: chunk mesh
building on the CPU, draw-call count, and chunk data in RAM. Sodium attacks all three. Vulkan's
main advantage is lower draw-call overhead, and Sodium's batching has already collapsed that
count.

## Why Noisium was dropped

It was on the recommended list and should not have been. Four reasons, and the last is the one
that decides it:

1. Its only NeoForge 1.21.1 build is `2.3.0`, published **August 2024** — over a year stale on
   this loader, while remaining current on Fabric.
2. It is not on CurseForge under that name. What a CurseForge search turns up are third-party
   forks — an unmaintained fork of a stale build is strictly worse than neither.
3. **Chunky already covers the need.** Noisium makes worldgen faster; Chunky removes it from the
   hot path. For a pack about travelling long distances, pre-generating once is the better
   answer, and it makes a worldgen optimiser close to redundant.
4. It patches vanilla worldgen internals, and `ascension-worlds` is about to introduce our own.
   That is the highest-risk overlap in this entire stack, taken on for a benefit we already
   have.

If worldgen shows up as a real cost in a profile later, the thing to reach for is more Chunky
pre-generation, not this.

## Garbage collector: generational ZGC, not G1

Distant Horizons warns about G1 on sight, and it is right to. G1's pauses run to tens of
milliseconds; against a 50 ms tick budget that is a hitch you can see, and on the client it is
the frame stutter DH is complaining about.

**Dev runs use generational ZGC by default** — `jvm_gc` in `gradle.properties`, applied to the
client and server runs. Generational is opt-in on Java 21 and the non-generational collector is
markedly worse for this workload, so both flags go together:

```
-XX:+UseZGC -XX:+ZGenerational
```

Put the same two on the CurseForge instance (Settings → Java → JVM Arguments) so the client and
the dev server are not running different collectors.

`jvm_gc=g1` switches back, which is how to A/B it.

**One consequence for measurement.** ZGC does not sawtooth the way G1 does, so the "watch the
low point of the sawtooth" method in
[`performance-log.md`](performance-log.md) stops applying. No loss: leak detection had already
moved to JFR and `/spark heapsummary`, both of which report the live set directly and neither of
which cares which collector produced it.

## Configuration that actually matters

Two mods here can be configured into doing nothing useful.

**Server view distance versus Distant Horizons.** Raising server view distance is the expensive
way to see further: loaded chunks cost memory and ticking on both sides, and the cost is
quadratic. The point of Distant Horizons is that you *do not* raise it — keep server view
distance modest and let DH cover everything beyond. Setting both high pays twice for one result.

**Chunky pre-generation is per dimension**, so it is an M2-and-later activity for our own worlds.
Install it now; use it once dimensions exist.

**Do not run Chunky and DH's LOD generation flat out at the same time.** DH warns about exactly
this: Chunky generates chunks faster than DH can turn them into LODs, and the LODs come out with
holes. Either raise DH's CPU thread count first, or pre-generate with Chunky and let DH build
LODs over already-generated terrain afterwards. The second is the calmer order.

## Known gap: no lighting engine optimisation

**There is no Starlight build for NeoForge 1.21.1**, and no equivalent. Lithium includes some
lighting work but not the full light-engine rewrite. Lighting is a real cost at high render
distance, so this is an unaddressed expense rather than a solved problem — worth remembering if
light updates show up in a Spark or JFR profile and there is nothing obvious of ours behind them.

## Measuring with these installed

See [`performance-log.md`](performance-log.md) — specifically *What a baseline can be compared
to*. Two rules carry over:

1. **The A/B is the signal.** Our module on versus off, same session, same stack. It survives
   every mod update in this list, because both halves move together.
2. **The mod set is part of any absolute baseline.** Adding or updating anything here
   invalidates the recorded absolute numbers and means retaking them. It does not invalidate an
   A/B.

A clean reading with none of these installed is still taken once per module release, because that
is the number someone adopting `ascension-atmosphere` into their own pack actually cares about
(ADR-0003).
