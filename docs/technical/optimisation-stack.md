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
| **Distant Horizons** | `2.4.5-b-1.21.1` | Renders far terrain from an LOD database instead of loaded chunks. Back in the stack 2026-09-06 after being dropped and then un-dropped the same day — see "Distant Horizons: dropped, then fixed" below for why this specific version and this specific config. |
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

## Distant Horizons: dropped, then fixed, same day

It was the one mod in this stack whose whole job was decoupling apparent view distance from
loaded-chunk memory, and losing it was a real loss, not a shrug — Sanchit's call 2026-09-06 after
testing against `ascension_worlds`' own custom worldgen: **already-generated chunks kept getting
visibly corrupted, repeatedly, not as a one-off.**

**Checked, not assumed: this was not `ascension-compat-chunky` racing DH's own LOD builder.**
That was the first suspicion — DH warns that Chunky generating chunks faster than DH can turn
them into LODs produces exactly this symptom. **Ruled out**: the corruption was already happening
back when Chunky was installed but its pre-generation had nothing active yet, before
`ChunkySequentialPregen` existed to race anything.

**The real cause, found after reinstalling to test a different build: `distantGeneratorMode`.**
DH has four settings for how it fills in terrain beyond real chunk data, and the build we'd had
installed was configured to `FEATURES` — which carries DH's own in-config warning, verbatim:
*"may cause world generator bugs or instability when paired with certain world generator mods."*
`ascension_worlds` leans hard on non-vanilla worldgen (a custom noise router, craters, a
checkerboard biome source) for a small pack — exactly the case that warning names, and a much
more specific explanation than "DH's own bug" was the first time this section was written.

**The fix, applied 2026-09-06:** `distantGeneratorMode = "PRE_EXISTING_ONLY"` in both
`config/DistantHorizons.toml` (client) and the dedicated server's copy. DH now only ever builds
LOD terrain from chunks that actually exist — Chunky's pre-generated ones, or anything a player
has visited — and shows a plain gap rather than a guess for anything beyond that, closing exactly
the moment real terrain generates there (already how DH's LOD system works; this setting just
stops it from ever inventing terrain of its own against a generator it wasn't built to guess at).
Also picked up a newer build in the process — `2.4.5-b` (Dec 2025, 612K downloads, the most
field-tested 1.21.1 release that exists) instead of the `3.2.0-b` beta that had been current —
confirmed via a live Spark profile to cost 0.37% of server-thread time, in the same negligible
company as every Ascension module.

**What this means for Chunky.** With DH never generating its own guesses, Chunky's pre-generation
radius *is* DH's effective long-range view distance now — the two systems are directly coupled in
a way they weren't before. Widening Chunky's reach later is now also a rendering-distance lever,
not just an arrival-lag one.

**DH's own generator was never a substitute for Chunky, regardless of this fix.** Its "distant
generation" produces approximate LOD data for rendering far-away terrain — it never creates real,
minable, structure-bearing chunks. The moment a player actually gets close, real worldgen still
has to run. Chunky (real chunks, so arrival isn't laggy) and DH (rendering far beyond that) solve
different problems; `PRE_EXISTING_ONLY` just means DH no longer tries to do a version of Chunky's
job badly on the side.

**No replacement — genuinely nothing else does this job on NeoForge 1.21.1 today.** Every
lighter-weight alternative already in "Do not install" above (C2ME, VMP, Krypton) lacks a NeoForge
build entirely, and none of them do what DH does (long-range LOD terrain) even on Fabric — they
solve chunk *generation* speed, not chunk *rendering* range. The honest consequence: apparent view
distance is capped by ordinary server `view-distance`/`simulation-distance` again, same as any
unmodified server. Chunky pre-generation (below) still removes worldgen from the travel hot path,
which is the cost DH was never solving anyway — but the "see a planet's terrain from far away
before you land" polish DH offered is gone until something more stable appears. Revisit if a
NeoForge LOD mod with a real track record shows up later; nothing here rules that back in.

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

**Server view distance, on its own now that DH is gone.** Raising it is the expensive way to see
further — loaded chunks cost memory and ticking on both sides, and the cost is quadratic in the
radius. With no LOD mod covering the distance beyond it, this setting is the actual, hard ceiling
on how far a player can see rather than one half of a DH/view-distance trade-off. Raise it
deliberately and re-measure (`performance-log.md`), not as a reflexive fix for "space feels too
close."

**Chunky pre-generation is per dimension**, so it is an M2-and-later activity for our own worlds.
Install it now; use it once dimensions exist. With DH gone, Chunky is no longer sharing the
worldgen-vs-LOD-generation balancing act described in the previous version of this section — it
just removes worldgen from the travel hot path, plainly, with nothing else racing it.

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
