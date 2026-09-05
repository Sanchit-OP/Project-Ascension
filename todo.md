# Todo

Open questions only. **Settled decisions live in [`docs/decisions/`](docs/decisions/)** — check
there before reopening anything.

Last reviewed: 2026-09-05.

---

## Resolved on 2026-09-05

Moved out of this list — see the register for reasoning:

- ~~Define what requires a custom mod~~ → ADR-0002
- ~~Target Minecraft and loader version~~ → ADR-0001
- ~~Planet framework approach~~ → ADR-0004
- ~~v1 scope~~ → ADR-0005
- ~~Whether oxygen is custom or mod-based~~ → ADR-0003 / M1

## Blocking the next milestones

These need answers during the milestone that touches them, not before.

**M1.1 (atmosphere API design) — draft written, awaiting review**

See [`docs/technical/atmosphere-api.md`](docs/technical/atmosphere-api.md). Answered there:

- ~~Does sprinting or combat increase consumption?~~ Yes, via a short named `activityMultiplier` list.
- ~~Sealed bases: zones or refill points?~~ Zones — same mechanism ships need later.
- ~~Can players create temporary field refills?~~ Yes, but not in v0.1.

Still open, and blocking the API being frozen:

- How visible should oxygen information be in the UI? (bar only, or bar + hazard readout)
- How do multiplayer rescue and recovery work? Decides whether `OxygenSource` needs a transfer
  operation in its first version.
- Ship `drainMultiplier` in v0.1, or add it when `worlds` lands? Changing a record's shape after
  third parties depend on it is a breaking change.
- Oxygen units: integer units, or seconds of remaining air?

**Before M2 (`ascension-worlds`) — optimisation mods.** *Decided 2026-09-05 to happen at this
boundary, not sooner.*

M2 is the right gate: custom dimensions and chunk generation are where the real performance
cliffs are, and knowing early whether our worldgen fights a chunk-optimisation mod, or a custom
sky renderer fights a rendering one, is compatibility information we want *before* building
dimensions rather than after.

Two things must not be confused when we do it:

1. **The ADR-0007 rule 12 measurement stays on a clean instance.** Optimisation mods change
   exactly the numbers the baseline tracks — tick time, heap, allocation rate. Measuring our own
   code against a Canary/FerriteCore-modified instance would make "no regression vs. M0.5"
   unfalsifiable: we could never tell our inefficiency from something else's optimisation
   covering for it. And per ADR-0003 our modules have to be fast *standalone*, because that is
   how most adopters will run them.
2. **So: two instances.** `Ascension Dev` stays clean and remains the measurement reference.
   A duplicate carries the optimisation mods and exists to catch compatibility breakage.

### Candidates, checked against NeoForge 1.21.1 on 2026-09-05

Verified through the Modrinth API — a project listing "neoforge" and "1.21.1" separately does
not mean the pairing exists, so each was checked per-version.

| Mod | Verdict |
|---|---|
| **Sodium** `0.8.13-neoforge` | Yes. Upstream Sodium has a real NeoForge 1.21.1 build now, which is the answer to the renderer question — multithreaded chunk meshing and batched draws are what actually gate render distance. |
| **Distant Horizons** `3.2.0-b-1.21.1` | Yes, and it is the mechanism for high render distance at low RAM: an LOD database instead of loaded chunks. Compatibility with our custom dimensions is untested — see the M2 question above. |
| **Lithium** `0.15.4-neoforge` | Yes. General server-side wins. |
| **FerriteCore** `7.0.3-neoforge` | Yes. Memory reduction, and directly relevant to holding more chunks. |
| **ModernFix** `5.27.24` | Yes. Memory and startup. |
| **Chunky** `1.4.23` | Yes, and it is the practical substitute for C2ME's benefit in a *travel* pack: pre-generate, and flying becomes disk I/O instead of worldgen. |
| **Noisium** `2.3.0` | Yes. Worldgen speed. Server-side only, so it is also the one most likely to interact with `ascension-worlds`. |
| **EntityCulling**, **MoreCulling**, **BadOptimizations** | Yes. Client-side, uncontroversial. |
| **Embeddium** `1.0.15` | Available, but **do not install alongside Sodium** — it is a Sodium fork and they conflict. Pick one; Sodium is upstream. |
| **VulkanMod** | **No NeoForge build.** Also replaces the whole renderer and breaks most rendering mods. See the note below — it costs less than it looks. |
| **C2ME**, **VMP**, **Krypton** | **No NeoForge build.** |
| **Dynamic FPS** | Works, but **keep it out of the dev instance.** It throttles the game when unfocused, and `pauseOnLostFocus=false` is seeded in our dev runs specifically so alt-tabbing to an editor does not invalidate a timing observation. |
| **Fabric API**, **Mod Menu**, **Placeholder API** | Fabric plumbing, not optimisation. |

**On VulkanMod and OpenGL.** The bottleneck on render distance is not the graphics API. It is
chunk mesh building on the CPU, draw-call count, and chunk data in RAM. Sodium attacks all three
— threaded meshing, batched draws, a compact vertex format. Vulkan would further reduce
draw-call overhead, but Sodium's batching has already collapsed that count. So having no Vulkan
renderer on NeoForge costs much less than it appears to.

**On C2ME.** It parallelises chunk *generation and I/O on the server*, which is a different
problem from render distance. For a pack about travelling, Chunky gets most of the same
practical benefit by removing generation from the hot path entirely.

**Spark is different and should go in now, not at M2.** It is a profiler, not an optimiser.
`docs/technical/performance-log.md` currently tells you to read numbers off F3 by hand, which is
why the M0.5 baseline is still missing its sawtooth low point and its three reload cycles. Spark
gives per-call-site allocation and tick distribution, which is what M1.9 actually needs to sign
off honestly.

**Before M2 (`ascension-worlds`):**
- **Is orbit a separate dimension or a high-Y band of the surface dimension?**
  (Deferred deliberately by ADR-0004 — real memory and chunk cost either way.)

  > **New constraint, 2026-09-05.** Sanchit: high render distance is *necessary* for space
  > travel to read well, not a nice-to-have. That is a design input to this question, and it
  > pushes hard toward **separate dimensions**.
  >
  > A high-Y band means the planet's surface chunks are loaded while you are in orbit. At the
  > render distance this requirement implies, that is the most expensive configuration
  > available — and it is expensive in exactly the resource ADR-0007 rule 1 is about.
  >
  > It also reframes what "see far" means. From orbit you do not need *terrain*; you need a
  > planet, which is one rendered body, not thousands of loaded chunks. Distance on the
  > **surface** is a chunk problem. Distance in **orbit** should be a rendering problem with
  > almost no chunks behind it. Conflating them would buy the worst of both.
- **Does a custom dimension work with Distant Horizons' LOD generation?** DH is the mechanism
  that makes high surface render distance affordable, so this is a compatibility question worth
  answering before dimension design is finished rather than after.
- What is the exact order and identity of the seven mandatory off-world steps?
- Where do ancient gateways physically exist?
- How are failed landings handled?

## Design questions, not yet blocking

**Progression:**
- Define mandatory tech families on the critical path versus optional optimisation
- Define late-game planets after the gun unlock step
- Guns currently land at planet 6 of 7 — only one world left to use them in. Move earlier, or
  make planets 7+ substantial?
- Define the repair and gateway lore arc
- Define gun-world identity beyond industrialisation
- Define the exact scope of advanced suit modules

**Combat:**
- Define late-game boss anti-cheese rules in implementable terms
- Which boss pressure types are mandatory across the campaign?
- Should some planet completions use non-boss victory conditions?

**Systems gaps identified 2026-09-05:**
- **Nether removal has an uncosted recipe debt.** Cutting Nether access means re-sourcing
  blaze, nether quartz, netherite and ancient debris wherever our progression needs them. This
  is concrete authoring work, not a policy line.
- **The late-joiner gap.** "Joins at the team's current tier" collides with "unique planetary
  resources gate progression" — they get the dimension unlocked and no gear to survive it.
  Needs a catch-up mechanism, not just a permission flag.
- **Fallback ship experience without Sable** — created by ADR-0006. What do players who do not
  install the optional physics jar actually get?

## Pack-level polish

- **First-launch defaults for players.** The dev build seeds `options.txt` via
  `seedDevGameOptions`, but that only covers our run directories. The shipped pack should
  ship the same defaults so players never see the accessibility onboarding prompt before the
  main menu. Decide whether this is a bundled `options.txt`, a config mod, or our own module.

## Atmosphere follow-ups

Captured during M1.6 testing; none block v0.1.

- **Emitter becomes a three-block Create chain** — generator (rotation or addon electricity)
  feeds a buffer tank, tank feeds a pressuriser that holds the volume. Direct generator-to-
  pressuriser is the cheap early setup. Belongs in `ascension-compat-create` (Tier 2); the
  standalone emitter stays so the module works with no Create installed. Design recorded in
  `docs/technical/atmosphere-api.md` section 7b.
- **Airlocks need solid blocks.** Doors, trapdoors, slabs and panes are not full collision cubes,
  so they leak by the current sealing rule. Either accept it, or add a sealing door block.
- **No general block-change event** means external world edits are not seen until something else
  near the volume changes. Fails safe, but worth revisiting if it bites.

## Missing documentation

- Quest and onboarding design — FTB Quests appears in the mod list with no design behind it. In
  a campaign this authored, the quest book is effectively the game's UI.
- Multiplayer and server operations
- Resource ledger — seven gating materials with no single page tracking what each is and which
  recipes consume it
- Testing and playtest plan
- `docs/technical/performance-log.md` — created in M0.5
