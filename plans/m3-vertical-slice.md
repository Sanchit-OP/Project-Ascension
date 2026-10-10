# M3 — closing the v1 vertical slice: a boss, a gateway, Planet 3

**Goal:** meet the "finished" bar ADR-0005 actually set for v1, which M2's closeout did not
claim to meet. M2 built the *framework* — travel, survival, real terrain, weather. It did not
build the boss, the gateway, or the third world ADR-0005 lists as part of "finished."

**Exit criteria (restated from ADR-0005):** one authored boss unlock, one restored gateway /
repeat-travel shortcut, and a third world proving the framework is not Moon-shaped, on top of
everything M2 already delivered.

## Where M3 stands — start here (2026-10-06)

Written as a handoff: a fresh session should be able to pick up from this section alone. All of
it is committed and pushed. Details live in the sections below and the linked docs.

**Built, and how far each is verified:**

| What | Verified |
|---|---|
| M3.1 Moon boss chain (Cataclysm citadel on the Moon, Golem → Guardian → gateway key; ADR-0013 mixins) | Live 2026-10-05: bosses spawn and fight, 2.5x health applies. The Armor/MR fix (`CataclysmBossStats`) not yet seen. |
| Moon content: rare wild citadels | Confirmed from the world's structure data. |
| Gateways: walk-through multiblock frames, Moon pre-generated in a ruin site, Earth player-built, rune ring lights on attunement, destination screen, debug `goto` / `build` | **Live 2026-10-06: Sanchit's client test worked.** He has a few follow-ups he'll raise later. |
| Vanilla Nether/End portals dormant (ADR-0014) | **Live 2026-10-06: confirmed dead.** |
| Warden tendril drop | Not yet seen. |
| End Islands Endermen on the Moon | **Live 2026-10-06: spawning.** |
| Earth gateway recipes from the Deep Dark (keystone: diamonds, echo shards, sculk catalysts, compass; gatestone: sculk) | Loaded on a dedicated server with no errors; not yet crafted in a client. |
| Share-air rescue | Needs two clients; rides on the two-client test. |

**Settled 2026-10-06:** the keystone recipe (Deep Dark materials, one Warden tendril per gate,
nothing from the Nether); Planet 3 is the **Nether world** (roofless, netherite, a boss; M3.3
below); the lore premise (an interplanetary civilisation that fell to something unknown, the Moon
as the lore anchor; `docs/lore/world.md`); Voxy evaluated and not adopted
(`optimisation-stack.md`).

**Decisions waiting on Sanchit:**

1. **Planet 3's remaining questions**: survival rule, traversal wrinkle, ambient life (M3.3).
   Decided: the Netherite Monstrosity, no power generation there, no ash storms.
2. **The gateway follow-ups** from his live test, when he raises them.
3. **The armor/MR check** on the Cataclysm bosses: Sanchit will look himself.

**In progress (2026-10-06):** Moon round two.
- **Earthrise:** built (`SkyBodyRenderer`; format in `worlds-api.md`, "Sky bodies"). Not yet seen
  in a client.
- **The Lunar Archive**, split through [`lunar-archive-format.md`](../docs/technical/lunar-archive-format.md):
  - Design: a separate session, from `design/lunar-archive/design-session-prompt.md`.
  - Logic increment 1 done (`84914c8`): the `archive_entry` lore registry, plinth, tablet, book
    reader, eight drafted Moon inscriptions (`tools/archive-lore.py`, text for Sanchit to edit),
    stores loot, op commands. Verified on a dedicated server; not yet seen in a client.
  - Logic still to do: the orrery hologram (needs gate state synced to clients), then the
    worldgen plumbing once the design's templates land.

**Next:** the **lore spread** across all 7-8 planets. The first prose draft ("It was us",
`docs/lore/the-road-home.md`) was rejected 2026-10-07 as disconnected from Minecraft. The second,
built from Sanchit's direction (vanilla mobs explained, a planted false story, horror twist,
branching endings), is [`docs/lore/what-the-warden-keeps.md`](../docs/lore/what-the-warden-keeps.md).
Its companions are `the-mark.md`, `the-moon.md`, `the-nether.md` (Planet 3's lore, which also proposes
its survival rule: wear gold) and `the-ending-and-names.md`. Settled: the Archivist is Tacita, and her
name, signed by the voice on every tablet in the wrong order, is the way to close the seal. **The
Moon's eight tablets are rewritten as the voice (2026-10-10)**, with entry ids unchanged. They were
checked by `ArchiveLoreTest` but have not yet been read in a client. Still open: which endings, the
faction names, and wear gold for Planet 3.

**2026-10-10: the Archive is merged into main** (rebased onto main, both generators re-run
byte-identical, `:modules:worlds:build` green with 80 tests). **The lore art went to a design
session** through [`docs/technical/lore-art-format.md`](../docs/technical/lore-art-format.md) and
`design/lore-art/design-session-prompt.md` (worktree `../Project-Ascension-lore-art`, branch
`lore-art-design`). It covers the mark, the script glyphs and font, the pylon band, revised murals,
the Sealers' Record, the keeper shrine and the moon signal. **Merged into main and pushed 2026-10-10**
(`888af41`; generators byte-identical, build green). The design record is
`docs/technical/lore-art-design.md`, and its "For the logic session" list is what step 2 builds
against. The mark became Format B, a 24 × 24 decal per stage. The Sealers' Record is tiles 121–174,
and `TILES` = 193. The Archive templates changed in this merge, so the headless block comparison of
step 1 should be re-run on them.

**Next logic build, picked by Sanchit 2026-10-10: the Archive in the world, then gate progress.**

**Live 2026-10-10: steps 1 and 2 seen working in Sanchit's client** on a dedicated server, fresh
world: the Archive, the eight tablets as the voice with Tacita's signature, Earthrise, and the
mark through its stages, dark in Darkness and a dust storm, on illagers, and kept across a relog.
"It looks and feels amazing." Checklist and result: [`live-test-2026-10-10.md`](live-test-2026-10-10.md).
What the walk-through did not cover is unticked in `lunar-archive-design.md`'s live checklist.
The restart check is deferred; its baseline is in that file.

**Also live 2026-10-10, asked for by Sanchit after the first round:** no clouds on the Moon or in
space (`ascension-compat-distanthorizons`: DH 2.4.5 drew its own clouds everywhere without a
ceiling), and our Moon over Earth in place of vanilla's (a sky body riding vanilla's moon and its
phases; vanilla's moon texture shipped blank). Both seen in his client.

1. **Done 2026-10-10, seen live.** Archive worldgen from the design
   handover: structure, two pools, the fixed structure set at x −160, z −256, plinth and orrery
   shapes with `noOcclusion`. Craters and wild citadels are now kept clear of both the Archive and
   the gateway ruin by a new placement type, `random_spread_keep_clear`. Results are in
   `lunar-archive-format.md`.
2. **Done 2026-10-10, with its first reader, the mark. Seen live.**
   - `GateProgressPayload` carries the lit planets. It is sent at login and whenever a gate lights
     or goes out, never per tick, and the client copy is cleared on logout.
   - The mark is drawn on the right hand in first and third person (`PlayerMarkLayer`, `MarkDecal`,
     `MarkClientEvents`), and the illagers' full mark on their crossed arms, exactly where the art
     contract places it. It is full-bright, dimmed in a dust storm or the Deep Dark.
   - Every player on the server shows the same stage, because the gates are shared.
   - In first person it shows only with an empty hand: vanilla draws no arm while an item is held.
   - Debug commands `gateway light|unlight <id>` step through all seven stages.
   - Checked on a dedicated server (clean boot, commands answer) and by 86 tests.
3. **Now: the refactor pass, which is due** (below). Its first job, the Chunky bug, is fixed in
   part and parked safely (below); the pass continues with the rest of its list.
4. Then the rest of gate progress's readers, from `lore-art-design.md`'s "For the logic session"
   list: the pylon's glyphs lighting with the moon signal in Earth's sky (one story beat), the
   orrery hologram, cats shying from a marked player, Tacita's signature as a glyph row in the
   reader, Endermen carrying Sealers' Record tiles, the keeper shrine. Then Planet 3 once its
   questions are answered. Deferred to the alpha release: the two-client test (also covers
   share-air) and the M1.9 measurement.

(The tablet rewrite once listed here is done: the voice's eight tablets kept their slots and ids,
`lunar-archive-format.md`, 2026-10-10 note.)

**The Chunky bug, found 2026-10-10. Parked safely; the root cause is still open.**

What happened: a fresh dedicated server crashed twice with nobody online, both times while
Chunky pre-generated Earth, once with the heap full at 35,331 loaded Overworld chunks.

- **Fixed: a pause was taken for a finished task.** Chunky's public `onGenerationComplete` fires
  at the end of every task, paused or cancelled ones included, so pausing Earth started the Moon.
  `ChunkySequentialPregen` now listens to Chunky's `GenerationTaskFinishEvent` and moves on only
  when the task's chunk iterator is empty. Verified on a dedicated server: pausing Earth starts
  nothing.
- **Fixed in passing:** the hook did level work on Chunky's own thread; now on the server thread.
- **Added, but not the fix:** `PregenGovernor` pauses our task while ticks are over 40 ms and
  resumes under 25 ms. It cannot catch this crash: ticks stayed at 3 to 10 ms until the heap was
  full. It stays, so a pre-generation never lags players who are online.
- **Root cause, not found.** While Chunky runs, almost every generated chunk stays in memory:
  4,814 held of 4,853 generated at 90 s. Vanilla's own chunk dump (the new
  `/ascension worlds debug chunks`) shows 8,446 of 8,498 holders with **no ticket**, most at level
  45 (marked for unload), so nothing pins them: they are simply never unloaded while generation
  runs. Pausing then unloads them all at once; saving 20,000 chunks in one go filled the heap and
  one tick took 60 s.

  | Server mods left out (`-Pdev_mods_server_exclude`) | Chunks held / generated at 90 s | Pending block ticks |
  |---|---|---|
  | none | 4,814 / 4,853 | about 600,000 |
  | ModernFix | 4,676 / 4,558 | 653,476 |
  | Lithium | 2,924 / 2,897 | 281,205 |
  | Distant Horizons + Sable + Create Aeronautics | 4,743 / 4,636 | **32** |

  None of those is the cause. Separately, one of DH, Sable and Aeronautics schedules the huge
  number of block ticks that make each held chunk heavy (about 120 a chunk) and its save slow.
- **Parked safely (Sanchit's call, 2026-10-10):** nothing pre-generates on its own any more.
  `ascension-compat-chunky`'s background pre-generation is off by default
  (`config/ascension_compat_chunky-server.toml`, `[pregeneration] background`), and the server
  pack ships Chunky's `continueOnRestart: false`, so the dev world's saved Earth and Moon tasks no
  longer start together at every boot. Verified on a dedicated server, fresh world: nothing runs,
  the saved tasks are untouched. `/chunky` by hand still works.
- **Next, when it is picked up again:** trial our mods plus Chunky alone; if chunks are still held,
  try vanilla plus Chunky, which would make it Chunky's or vanilla's behaviour on 1.21.1. Then
  halve the remaining mods. Separately, find which of DH, Sable and Aeronautics schedules the
  block ticks. Crash reports: `run/server/crash-reports/crash-2026-10-10_21.36.23-server.txt` and
  `crash-2026-10-10_23.33.26-server.txt`.

**Cadence:** the last refactor pass was 2026-10-06 evening (`e8119d6`). Since then: the Archive's
lore system, the tablet rewrite, the Archive's worldgen, gate progress with the mark. The next
increment is the refactor pass (ADR-0008).

**Where things are:**
- Gateway format contract: [`docs/technical/gateway-frame-format.md`](../docs/technical/gateway-frame-format.md);
  design and its live checklist: [`docs/technical/gateway-portal-design.md`](../docs/technical/gateway-portal-design.md)
  (art regenerates from `tools/generate-gateway-art.py`).
- Debug commands, test items, and how to check worldgen without a client:
  [`docs/technical/dev-environment.md`](../docs/technical/dev-environment.md).
- How design and logic were split across two sessions: a written contract doc first, then a
  prompt for the design session in its own git worktree, merged back. Worked well; reuse it for
  Planet 3's art.

## Sequencing decision

**The Moon's boss and gateway first, then Planet 3.** Not arbitrary — `planets.md`'s own
original per-planet notes assign the Moon "oxygen progression step, small boss, first repaired
return connection." M2 shipped the oxygen step and the travel framework, but not the boss or the
gateway. Building those now finishes what the Moon was always supposed to carry, rather than
skipping straight to Planet 3 and leaving the Moon permanently half-built.

It also means Planet 3 gets to *reuse* a boss pattern and a gateway pattern already proven live,
instead of inventing three new systems (a world, a boss, a gateway) at once.

**Revised sequencing, 2026-09-21: Moon content density before the gateway.** M3.1 (boss)
landed; M3.2 (gateway) turned out to be a bigger, network-shaped system than first drafted (see
its section below) — but Sanchit's explicit call is to fill out the Moon itself first (passive
mobs, more than one findable structure, general content) before building the gateway network on
top of a world that's still mostly empty. See "Moon content backlog" below for the concrete list.

## Sourcing bosses: corrected 2026-09-21

First draft of this plan proposed a fully custom boss, reading `bosses.md`'s Cataclysm mention as
stale pre-ADR-0002 content. **Sanchit's correction: bosses are sourced from other mods, same as
`bosses.md` always said** — "Bosses can be sourced from curated combat mods, but must be
recontextualized around Project-Ascension progression" and "Existing boss kits should be
modified or rebalanced rather than accepted as-is" were never wrong; `mod-list.md`'s objection to
Cataclysm was narrower than it read — its bosses' fixed AI can't be retuned into the *adaptive*
boss `combat.md` wants, which is a reason not to lean on Cataclysm's AI framework, not a reason
to reject Cataclysm's structures and mobs as raw material entirely.

**And this has to scale.** One sourced mod will not cover every planet's boss — different
locations want different fits, so this project will end up depending on several boss-source mods
over time, each recontextualized the same way. The recipe below is written to be reused, not
Cataclysm-specific:

1. **Relocate the structure.** A third-party boss's dungeon almost never generates where a
   planet's design wants it. Referencing the source mod's own structure id in our own
   `structure_set` override handles the biome restriction (so it can *only* ever generate on the
   target planet); pinning it to a specific spot needs `ascension_worlds:fixed_position`, a new
   structure placement type built for exactly this (see `FixedPositionStructurePlacement`'s
   javadoc) — neither of vanilla's own two placement types (`random_spread`,
   `concentric_rings`) can put a structure at a coordinate we actually choose, both fold the
   world seed into where an instance lands. Every future `ascension-compat-<bossmod>` jar
   reuses this same placement type by id; none of them need new placement Java.
2. **Redirect the drop.** Override the boss's loot table so it drops this project's own reward
   instead of the source mod's — `ascension_worlds:gateway_key`, one item id for every planet
   (a `gateway_key_for_planet` data component names which one, so adding a planet never means
   adding an item, per ADR-0007 rule 10). This is what ties a sourced boss into *this* campaign's
   story rather than leaving it a detour into someone else's progression.
3. **Rebalance the stats, where it actually matters.** Mostly free: per ADR-0012, generic
   melee/mob damage already reads as physical against `ascension-combat`'s armor system with zero
   coupling in either direction, so a sourced boss's ordinary hits already interact correctly with
   no work. The real gap is anything the source mod flavors as magic or otherwise special (an
   ability that should read against magic resist, not armor) — that needs a damage-event listener
   in the same compat jar, reclassifying specific `DamageSource`s. Not yet built for M3.1; the
   Ender Golem's kit is melee-only, so nothing forced this yet.

None of this needs mixins — every piece so far is a datapack override (structure, structure_set,
loot table) plus one new, genuinely reusable piece of worldgen infrastructure in
`ascension-worlds` itself. `bosses.md` needs its "Current Boss Sources" list updated to say
Cataclysm is confirmed and active, not just a candidate — done alongside this plan doc.

**And it should not be easy.** Sanchit's own framing: this gates real progression (the gateway,
the return route), so the encounter should be a genuine wall, not a formality — worth remembering
when M3.1's stats get tuned from a live fight rather than left at Cataclysm's own out-of-the-box
numbers.

## M3.1 — The Moon's boss: L_Ender's Cataclysm's Ender Golem

**Picked over Cataclysm's other bosses because it's explicitly the small one** (150 HP, vs. 300
for the Ender Guardian that normally caps the same dungeon, 450–500 for Ignis/the Netherite
Monstrosity) — `planets.md` calls for a "small boss" specifically on the Moon, saving the bigger
kits for later worlds. Its home structure, the Ruined Citadel, already reads as ancient-ruins
architecture, which fits the gateway-civilization lore for free.

**First pass (2026-09-21) was floating and End-stone colored** — live-tested by Sanchit, not
guessed at. Real problems found, all fixed the same day (`ascension-compat-cataclysm`, Tier 2,
requires both `ascension_worlds` and `cataclysm`):

- **Placement height was hardcoded, not data-driven.** Decompiling `RuinedCitadelStructure`
  showed `generatePieces` places every piece at a literal `y = 53`, completely independent of
  the real surface height vanilla's own `onTopOfChunkCenter` computes one line above it —
  that computed height is used only for the structure's reported generation-stub position, never
  passed into `generatePieces`. Coincidentally tolerable in the End; on the Moon's real terrain
  it floated the whole citadel above the ground. **No datapack field reaches this** — fixed via
  this project's first mixin (ADR-0013), `@ModifyConstant` replacing the `53` with a real
  `getFirstOccupiedHeight` query, the same one `CraterStructure` already uses.
- **Block palette had no JSON hook either.** `Piece.makeSettings` hardcodes its own two
  processors in Java with nothing reading structure/structure_set JSON for a third. Fixed by the
  same ADR-0013 mixin pass: a `RuleProcessor` (`MoonRockRuleProcessor`) swapping End Stone/End
  Stone Bricks for the Moon's own anorthosite, added via `@Inject` at `RETURN` on
  `makeSettings`.
- **Loot table mistake, corrected:** the first pass fully *replaced* the Ender Golem's loot table
  with just the gateway key — Sanchit's correction: additive only, never a wholesale swap. Also
  moved the key entirely: **the Ender Guardian drops it, not the Golem** — the Golem reverted to
  Cataclysm's own untouched loot, and the Guardian's own two original drops
  (`gauntlet_of_guard`, its music disc) stay, with the gateway key added as a third pool.
- **Nothing spawns the Guardian at all.** Checked exhaustively: no structure data marker, no
  biome spawn rule, no config-driven natural spawn anywhere in Cataclysm's own data or Java for
  `ender_guardian`. A mandatory gate can't rely on an undiscovered trigger, so
  `GuardianAwakening` (plain event listener, no mixin) spawns the Guardian where the Golem died —
  matches the boss text's own framing of the Golem as guarding the citadel the Guardian is the
  true final boss of.
- **Stats were Cataclysm's out-of-the-box numbers** (Golem 150 HP/12 armor, Guardian 300 HP) —
  too easy per Sanchit's call. Cataclysm ships a genuine config-driven difficulty knob for
  exactly this (`[mobs.ender_golem.combat_config]` / `[mobs.ender_guardian.combat_config]`,
  `health_multiplier`/`attack_multiplier`) — no mixin needed. First pass: both bosses at 2.5x
  health, 1.5x attack, shipped in `serverpack/config/cataclysm-common.toml` (a partial file;
  NeoForge's config spec fills in every other Cataclysm default on load). Starting point to tune
  from after a live fight, not a final number.

**Boot-verified 2026-09-21 with all of the above in place:** clean boot, no exceptions, no mixin
apply failures (`SpongePowered MIXIN Subsystem` loads on `ModLauncher`/`SERVER`, and
`ascension_compat_cataclysm` logs successfully). **Not yet re-verified live in a client** — the
mixins' actual visual effect (grounded, reskinned structure) and the Guardian-spawn trigger both
need an actual playthrough to confirm, same as everything else in this project per ADR-0008.

**Still open:**

- No damage-type reclassification yet (see point 3 in "Sourcing bosses" above) — neither boss's
  kit forced it.
- **Failure condition:** dying resets the encounter with the usual death penalty; `bosses.md`
  asks for a real failure condition but nothing harsher has an obvious fit yet.
- The 2.5x/1.5x stat multipliers are a first guess, not a tuned number.

## Grievous Wounds fixed project-wide, not just for Cataclysm

Found while testing M3.1: Mortal Reminder's Grievous Wounds (blocks all healing) did nothing
against the Ender Golem, **and against the vanilla Wither** — not a Cataclysm-specific bug.
Root cause: `LivingEntity.addEffect` is a no-op on both (`WitherBoss.addEffect` unconditionally
returns `false`; every Cataclysm boss's shared base class only admits effects tagged
`cataclysm:effective_for_bosses`) — a common, reasonable "bosses ignore arbitrary debuffs"
convention that happens to make a vanilla `MobEffect` the wrong tool for a mechanic that
specifically exists to matter against tough, healing enemies. Fixed in `ascension-combat` itself
(not a compat-jar patch, since it affects a vanilla mob too): Grievous Wounds' real gate is now
`CombatAttachments.GRIEVOUS_WOUNDS_STATE`, a `DurationState` attachment that bypasses
`addEffect` entirely. The vanilla `MobEffectInstance` still gets applied alongside it, best-effort,
for the status icon on targets that do accept it.

## M3.2 — The gateway network (redesigned 2026-09-21, supersedes the single-shortcut draft below)

> **Current state, 2026-10-06 — read increment 3 below first.** Gateways are now walk-through
> multiblock frames (`docs/technical/gateway-frame-format.md`, design in
> `docs/technical/gateway-portal-design.md`). Increment 1's single-block portal, its Earth copy,
> the Relay Coil and the later obsidian pillar are all gone; the sections describing them are kept
> as history.

**Sanchit's correction: this is not an Earth&harr;Moon shortcut, it's a network.** The same
portal structure exists on every planet (Earth included). Built/found on Earth first — trying to
use it there does nothing, it's inert. The **same structure** on the Moon is the one that
activates (tied to M3.1's boss/gateway-key chain). Using an *active* portal opens a destination
picker; **only planets whose own portal is already active appear as choices.** Clearing more
planets over time grows the list every existing active portal can reach — this is a network that
gets more useful as the campaign progresses, not a fixed pair.

This is a materially bigger system than the original draft below and raises real open questions,
recorded here rather than guessed at:

- **One structure type, reused per-planet**, presumably via the same `fixed_position`
  placement pattern M3.1 already proved, once per planet's datapack. Earth's copy needs
  *somewhere* to sit — Earth is vanilla's own dimension (`TITANIUM_ORE`'s biome-modifier
  precedent is the relevant technique, not a structure_set override the Moon used).
- **What makes a portal "active" is presumably planet-specific** (the Moon's is the boss/key
  chain M3.1 already built) — Earth's own activation condition is undefined. Worth checking
  whether Earth's portal activates for free (it's the start), or has its own requirement.
- **Network state (which planets are active) is shared, not per-player** — a physical structure
  anyone can walk up to strongly implies team/server-wide state, not `PlanetArrivalMemory`'s
  existing per-player scope. This resolves M3.2's old "per-player or per-team?" open question in
  favor of per-team/server, but *where that state lives* is still open — `ascension-worlds`
  itself, or deferred to `ascension-progression` once it exists (per ADR-0011, Tier 1 modules
  don't reach for a module that doesn't exist yet, which argues for a `core` contract now and the
  real ownership moving later, the same pattern already used for combat state).
- **The destination-picker UI** is new surface area this project hasn't needed before — a screen,
  not a command or an item interaction like everything built so far.
- Landing at the destination still very plausibly reuses `PlanetArrivalMemory` /
  `SpaceMechanics.resolveLandingSpot`'s safe-landing resolution once a target is picked — that
  part of the original draft below likely still holds regardless of how the network/UI shakes
  out.

**Increment 1 built 2026-09-26, in `ascension-worlds` itself (Tier 1 — this is original content,
not sourced from a third-party mod like M3.1's boss).** Resolves the open questions above:

- **Earth's activation condition, decided (Sanchit, 2026-09-26): a small Earth-side task, not
  free and not a boss.** A new item, `Relay Coil` (`ascension_worlds:relay_coil`, a plain
  survival crafting recipe — redstone/iron/copper, no new base resource), consumed on the
  portal's core block to activate Earth's copy. The Moon's copy still activates by right-clicking
  with that planet's own `gateway_key` (M3.1's boss/key chain, unchanged).
- **One structure type, reused per-planet, resolved.** `GatewayPortalStructure`/
  `GatewayPortalPiece` (mirroring `CraterStructure`/`CraterPiece`'s own "no NBT asset, place
  blocks in Java" pattern, but trivially — one block, so none of `CraterPiece`'s multi-chunk
  `postProcess` handling is needed) is the one Java pair; `worldgen/structure/gateway_portal_earth.json`
  and `gateway_portal_moon.json` are separate JSON instances of it, each with its own `biomes`
  restriction and `ascension_worlds:fixed_position` coordinate (`[300,0,0]` on Earth, `[-800,0,0]`
  on the Moon, clear of M3.1's citadel and its exclusion zone). **Correction to the note this
  superseded:** structures take a `biomes` field directly (`#minecraft:is_overworld` works fine)
  — no `TITANIUM_ORE`-style biome modifier needed; that technique is for features/ores, not
  structures.
- **Network state, resolved:** `GatewayNetworkState` (a `Set<ResourceLocation>` of active
  planets' surface dimension ids), attached to a `Level` the same way `PlanetWeatherState`
  already is — no new `SavedData` plumbing — but read/written only via the Overworld's own
  instance, since it is a fact about the whole network rather than about any one planet's
  surface. Lives in `ascension-worlds` per ADR-0011, exactly as flagged; moving it to
  `ascension-progression` later is a relocation, not a redesign.
- **The destination-picker UI, deliberately NOT a real screen yet.** Built as a clickable chat
  message instead (`GatewayDestinations`) — each line runs a new
  `/ascension worlds gateway travel <planet>` command via a `ClickEvent`, which calls a new
  `SpaceMechanics.travelViaGateway` (an instant version of the same `descendTo` landing
  resolution the approach/descent flow already uses, skipping the flight). This is a placeholder
  on purpose, the same posture `OxygenRefillStationBlock`'s free instant fill has: a real
  `Screen`/`AbstractContainerMenu` picker is genuinely new client-side surface area this project
  has never needed, and is worth its own focused pass once the underlying mechanism (activation,
  shared state, instant travel) is proven live rather than before. **Next thing to build once
  this is live-verified**, not a loose end being ignored.

**Boot-verified 2026-09-26**: fresh world, clean boot, `Done (5.929s)`, no structure/registry
resolution errors. **Not yet confirmed live** — whether either portal actually generates at its
fixed coordinate, whether activation/consumption feels right, and whether the chat picker's click
event actually fires a working teleport all need a real playthrough, same as everything else in
this plan.

**Increment 2, 2026-10-05: the real destination screen.** Sanchit's call to keep building while
live testing is deferred, so this landed ahead of increment 1's live check rather than after it.
The chat list is gone: an active portal sends `GatewayDestinationsPayload` (the reachable list,
computed server-side) and the client opens `GatewayDestinationScreen`, one button per planet; a
click sends `GatewayTravelPayload` back. A plain `Screen`, not the `AbstractContainerMenu` the
note above anticipated — there is no inventory to sync, just a list in and a choice out. Both the
screen and the typed command go through `GatewayDestinations.tryTravel`, the single place the
picker-session and `canTravel` rules from the refactor pass are applied; the client is never
trusted. Non-pausing in single-player, because the session it rides on expires in game time.
Payload protocol version 1 → 2. Not yet seen in a client.

**First live test, 2026-10-05 (fresh world, dedicated server).** Confirmed: both bosses spawn
and fight, the 2.5x health multiplier applies (Golem 375 HP), the destination screen opens and
travels. Found and fixed the same day:

- **Bosses had 0 Armor / 0 Magic Resist.** `ascension-combat` only assigns its two attributes to
  vanilla mob types. `CataclysmBossStats` (compat jar, optional dependency on combat's api, loads
  AFTER it) gives the Golem HEAVY (50/20) and the Guardian BOSS (100/40) values.
- **Gateway travel landed at 0,0**, the generic first-arrival point, not at the destination's
  portal. `GatewayNetworkState` now remembers each portal's position at activation (from the
  clicked block, so a hand-placed portal works too), and travel lands one block south of it,
  facing it. Old saves without positions still load and fall back to the ordinary landing.
- **The Moon portal was unfindable.** A single block 800 blocks from the arrival point, and its
  chunk had never even generated (Chunky was stopped before reaching it). Now a 3&times;3 obsidian
  pillar 10 tall with the portal set in its south face (Sanchit's design), and the Moon's copy
  moved to x=-160 so it is in view of where players first land. Needs a fresh world to see.
- **The citadel at 0,0 was a wild copy**, confirmed by reading the world's structure starts: the
  story citadel generated correctly at chunk (50,0); three wild ones generated within ~1,000
  blocks. Working as designed.

**Increment 3, 2026-10-05/06: walk-through multiblock gateways.** Sanchit's call after the live
test: the pillar "doesn't even feel like a portal" — he wanted something in the league of the
Nether and End portals. Split into two sessions against one written contract,
`docs/technical/gateway-frame-format.md`: a design session authored the frame, the Moon ruin
site, ten decorative Moon-stone blocks and all the art (`gateway-portal-design.md`); this side
built the logic.

- **The frame is data** (`ascension_worlds/gateway_frame/gateway.json`, a 15&times;20&times;7
  crescent arch with a 5&times;7 opening), read by the pure, unit-tested `GatewayFrame`.
- **Earth: player-built**, attuned by right-clicking the keystone with a **Warden tendril** (new
  item, Warden drop via a loot modifier). **Moon: pre-generated** inside its ruin site at x=-160
  (in view of the arrival point), attuned with the boss's gateway key. No Earth pre-generation,
  no Relay Coil.
- **Attuning** checks the frame in all four facings, fills the opening with `gateway_field`, lights
  every frame block with a `lit` property (the Inscribed Gatestone ring), and records the arrival
  spot network-wide. Attuned keystones re-light a collapsed field for free; breaking one darkens
  the ring.
- **Walking into the field opens the destination screen** (once per entry), Sanchit's pick over a
  per-portal tuned destination. The field collapses like a Nether portal when its border breaks.
- **Arrivals** land in front of the destination's field, never inside one.
- **Vanilla Nether and End portals are dormant** (ADR-0014) — those worlds become planets.
- **Debug:** `/ascension worlds gateway goto <planet>` and `build [active]`, operator-only.

Verified on a throwaway dedicated-server world: the Moon gateway generates with its full site,
keystone dormant, opening clear, ring in place, arrival spot clear. **Not yet seen in a client.**

**Still open / explicitly deferred:**
- Lore: why the gateways exist at all (`docs/lore/world.md`).
- The format proposals in `gateway-portal-design.md` that are not done: block states in key
  entries (real stairs), "must be air" cells, and clearing terrain above the Moon plaza.

<details>
<summary>Original single-shortcut draft (2026-09-21, superseded by the network redesign above)</summary>

No prior design existed for this beyond two lore lines (`world.md`: "why are ancient gateways
present?", still an open question; `worlds-api.md`: a restored gateway "saves a 50,000 block
flight").

Mechanically, a gateway is not a new travel system. `ascension-worlds` already has
`PlanetArrivalMemory` — per-player, per-dimension, serialized memory of exactly where and which
way a player last ascended from (built for M2.6's return-to-orbit flow). A gateway is an
alternate *trigger* for that same landing resolution (`SpaceMechanics.resolveLandingSpot` /
`findSafeLandingSpot`), reached by interacting with a repaired gateway structure instead of by
flying the full orbit round-trip.

- A dormant gateway structure, physically present on the Moon — repairing an existing ruin, not
  building a new machine.
- Repair consumes the boss's unique drop plus a small additional crafted cost, via a right-click
  interaction sequence on the structure's core block.
- Once repaired: a permanent, instant two-way link between the gateway and Earth.
- Lore framing, proposed rather than settled: the "ancient" gateways were not built by whoever is
  native to these worlds — they're remnants of the same civilization whose worldgen artifacts
  (structures, the boss itself) are already being placed.

</details>

## Moon content backlog (2026-09-21) — do this before M3.2

Sanchit's explicit sequencing: **before** building the gateway network, the Moon itself needs to
stop being one weather system, one ore, and a single fixed boss structure floating in otherwise
empty terrain.

- **Ambient population — done 2026-09-24, `ascension-compat-endermanoverhaul`.** Sourced the
  same way M3.1 sourced its boss (see "Sourcing bosses" above, generalized to non-boss content):
  no installed mod had a clean thematic fit for Moon ambient life, so this is the project's
  second promoted-from-landscape-survey mod. Picked Enderman Overhaul's **End Islands Enderman**
  specifically because the Moon's boss chain is already Ender-lore (Ender Golem/Ender Guardian
  from Cataclysm) — an End-adjacent creature wandering the same ancient ruins reads as
  intentional, and its "Ancient Pearl" drop (summons a pet enderman) fits the gateway-civilization
  framing for free, same as the Ruined Citadel's architecture did. Sanchit's own steer: not
  everything needs to be passive — Endermen are vanilla-neutral (hostile only when provoked),
  which is real ambient presence without forcing a fight, and the recipe scales to future
  ambient-population mods the same way boss-sourcing does.
  - **Zero mixins, zero compile dependency** — the entire integration is one NeoForge
    `neoforge:add_spawns` biome modifier (`data/ascension_compat_endermanoverhaul/neoforge/biome_modifier/moon_end_islands_enderman.json`)
    referencing Enderman Overhaul's entity by registry id, targeting a new, genuinely reusable
    `ascension_worlds:moon_biomes` tag added to `ascension-worlds` itself (not the compat jar) so
    any future Moon-content compat jar can reuse it instead of re-declaring the two biome ids.
  - Requires Enderman Overhaul 2.0.3 (confirmed NeoForge 1.21.1) plus its own mandatory
    dependencies — ResourcefulLib, ResourcefulConfig, GeckoLib — all confirmed present in the dev
    instance's mods folder.
  - **Boot-verified 2026-09-24**: clean boot, `Done (12.721s)`, no registry/tag resolution
    errors, `ascension_compat_endermanoverhaul` and `endermanoverhaul` both load. **Not yet
    confirmed live** — whether the End Islands Enderman actually spawns on the Moon at a
    reasonable rate needs an actual playthrough, same as M3.1's mixin fixes.
  - First-pass spawn weight/cost copied directly from Enderman Overhaul's own End Islands
    numbers (weight 1, minCount 1, maxCount 2, charge 0.5, energy_budget 0.08) — a starting point
    to tune from after a live look, not a final number.

- **Rare/findable structure copies — done 2026-09-24, `ascension-compat-cataclysm`.** Sanchit's
  own words: "the strute we added is a single strucutre we need multiple of these" — more copies
  of the *same* Ruined Citadel, not different content, placed randomly and rare-but-findable.
  Added a second, brand-new `structure_set`
  (`data/ascension_compat_cataclysm/worldgen/structure_set/ruined_citadel_wild.json`)
  referencing the same `cataclysm:ruined_citadel` structure the mandatory one uses, with
  `minecraft:random_spread` placement instead of `fixed_position` — the same vanilla mechanism
  Cataclysm's own dungeons use for their default (pre-relocation) placement, reused here for
  variety and rarity rather than pinning to one spot.
  - Spacing 48 / separation 16 chunks, `triangular` spread (avoids a visibly grid-aligned feel),
    a fresh salt distinct from the mandatory copy's. An `exclusion_zone` (10 chunks around
    `cataclysm:ruined_citadel`, the mandatory structure_set's own id) keeps wild copies from ever
    generating on top of or near the story-critical one — the same `super_exclusion_zone` pattern
    Cataclysm's own `abandoned_structures.json` uses to keep its own dungeon families apart.
  - **Everything else about the citadel is shared automatically, no new code:** ADR-0013's two
    mixins (grounded placement, Moon-rock palette) apply to every instance of the structure
    regardless of which structure_set placed it; the Golem/Guardian boss chain and the gateway
    key drop are keyed off entity type and loot table, not structure instance, so every wild copy
    is a full, real encounter — not a watered-down side version. That is intentional: "rare but
    findable side content" describes how often you find one, not how hard it is once you do,
    consistent with Sanchit's "I wont make it easy for anyone to conquer."
  - Spacing/separation/exclusion numbers are a first pass, not tuned against how many actually
    land inside a planet's Chunky-pregenerated radius — check after a live look, same as
    everything else in this section.
  - **Boot-verified 2026-09-24**: clean boot, `Done (11.533s)`, no structure_set/registry
    resolution errors. **Not yet confirmed live** — whether any wild copies actually generate
    within reach, and how many, needs a real playthrough.
- **Only one Ruined Citadel exists (the fixed, mandatory one from M3.1).** Sanchit wants more —
  additional structure instances (same citadel and/or other content), placed to spawn in random
  locations across the Moon, **rare but still findable** — i.e. genuine optional/rare side
  content, not more mandatory gates. This is a different placement need than M3.1's
  `fixed_position` type: ordinary `minecraft:random_spread` with wide spacing is the right tool
  here, the same vanilla mechanism already used for the mandatory citadel's *original*
  (pre-relocation) Cataclysm placement — reused for variety and rarity instead of pinning to one
  spot.
- **General content density** — Sanchit's own words: "we need to add more content here for
  sure," without a more specific spec yet. Not a checklist item so much as a standing signal that
  the Moon is still thin relative to what a finished world should feel like.

## Moon content, round two (proposals, 2026-10-06)

Sanchit: "for moon we will need something more, idk what". These are proposals waiting for his
pick, not commitments. Each says what it costs and what it ties into.

1. **The Lunar Archive: the lore anchor.** One buried vault per Moon, placed like the gateway
   (`fixed_position`), cut into a crater wall. Inside: carved murals of the gateway network
   spanning many worlds, a star-map room marking every planet's gate (it could light a world's
   marker as the player attunes that world's gate: a progress board that is also story), and
   **Archive Tablets**, readable fragments of the builders' record. Stray tablets also turn up as
   loot in the wild citadels and the gateway ruin. This is the "lore heavy anchor" for the
   premise in `docs/lore/world.md`. Cost: one structure (art to a design session), one readable
   item, loot tables. Mostly data.
2. **Earthrise.** Earth hanging in the Moon's black sky, rendered by `MoonSpecialEffects`.
   Cheap, client-only, and the single biggest "I am on the Moon" moment we can buy. Natural
   follow-up: other planets' skies show their neighbours.
3. **Lava tubes.** Huge collapsed tunnels under the maria, a reason to go underground. Dark,
   echoing, and the home for whatever comes next. Cost: a worldgen carver or noise cave tuned in
   `moon-terrain-tuning.md`'s style.
4. **Polar ice in shadowed craters.** Ice deposits that process into water and oxygen: a
   self-sufficient Moon base loop. Ties straight into `atmosphere` (the emitter and the
   refill station). Cost: a block, a feature, one recipe chain.
5. **Meteor showers.** A hazard event on the Moon's own timer, reusing the dust-storm machinery:
   falling rocks, fresh craters, meteoric iron to salvage afterwards. Risk and reward in one.
6. **Derelict outposts.** Small wrecked builders' stations scattered rare-but-findable (the wild
   citadel pattern), with spare tanks, gatestone and tablets as loot.
7. **A native threat.** Something that lives in the dust and ambushes (regolith burrowers).
   Sourced from a mod first (the "Sourcing bosses" rule), custom only if nothing fits.

**Picked 2026-10-06: 1 + 2, the Archive and Earthrise.** Earthrise is built (`SkyBodyRenderer`,
format in `worlds-api.md`, "Sky bodies"). The Archive is split design/logic through
[`lunar-archive-format.md`](../docs/technical/lunar-archive-format.md).

Original suggestion: 1 + 2. The Archive answers the lore ask directly, and Earthrise changes
how every minute on the Moon feels for very little code. Then 3 + 4, which give the Moon a
reason to stay rather than pass through.

## M3 refactor pass (2026-10-05)

ADR-0008's scheduled pass: M3.1, the Moon content backlog and M3.2 increment 1 had all landed
since M2.7's. Same method as M2.4/M2.7 — pull the pure decisions out of event and block code into
classes that take ids in and give answers back, then test those. Found and fixed two real bugs
doing it:

- **The gateway travel command was a free teleport.** `/ascension worlds gateway travel <planet>`
  had no permission and no checks — its javadoc reasoned that the picker only lists active
  planets, but anyone could type the command directly, from anywhere, to any planet, skipping the
  entire spaceflight loop. Now a non-operator needs a `GatewayPickerSession` (opened only by
  clicking an active portal, tied to that dimension, one minute, spent by one trip) and a
  destination `GatewayNetworkState.canTravel` allows. Operators skip the session as a debug tool.
- **Two copies of "what opens this portal" that disagreed.** `satisfies` hardcoded the Moon and
  refused every other dimension; `describeRequirement` told those same dimensions to bring a key.
  Both now read `GatewayRules`: Earth takes a Relay Coil, every other planet its own key, matched
  on the key's `gateway_key_for_planet` component. That also means Planet 3's portal needs no new
  branch — its boss's key naming it is enough — which supersedes M3.2's "Planet 3 needs a third
  branch" note above.

Smaller: `GuardianAwakening` now listens at `LOWEST` so a Golem death another mod cancels never
leaves a Guardian behind; the picker's hover text is translatable; and earlier the same day, the
gateway key got the item model it shipped without.

**Tests: `worlds` 18 → 34** (`GatewayNetworkStateTest`, `GatewayPickerSessionTest`,
`GatewayRulesTest`). Build and `tools/check-dist-separation.sh` clean. Like all of M3, not yet
confirmed in a live client — the session gate in particular should be exercised by clicking a
picker line, waiting past a minute, and typing the command by hand as a non-op.

## Refactor pass, 2026-10-06

ADR-0008's pass after ten increments (share-air rescue through the rune glow). No behaviour
change:

- **One grid parser.** `GatewayFrame` and `GatewaySite` each carried a near-identical copy of the
  key/layers parser and its size checks; now `PatternGrid`, with the same error messages.
- **Placement maths in the model.** Centring a frame, putting its arrival spot on a chosen block,
  and its world bounds were computed in the worldgen structure, the piece and the debug command
  respectively; now `GatewayFrame.placeCentredAt` / `placeArrivalAt` / `Placement.bounds`, pure and
  tested.
- **`GatewayPortalActivation`**: the two copies of "a frame becomes a working gate" are `wake()`;
  the keystone-removed lookup is `findFrame`'s `keystoneGone`.
- **Docs**: this plan, `CLAUDE.md`, `architecture.md` and `todo.md` brought up to date; the
  vanilla-portal decision recorded as ADR-0014.

**Tests: `worlds` 58 → 68** (and 34 → 58 across the increments themselves). Build and
`tools/check-dist-separation.sh` clean — though that check is known to miss client references
written as method references; a fix is queued as its own task.

## Refactor pass, 2026-10-06 (evening)

ADR-0008's pass after three increments: the Deep Dark recipes, Earthrise, the Archive's
inscriptions. No behaviour change (`e8119d6`):

- **One archive lookup.** The reader, the tablet's tooltip and the debug commands each resolved an
  entry id their own way; now `ArchiveEntry.find`.
- **`ArchiveDebug`**, shaped like `GatewayDebug`: the archive op commands' logic out of
  `WorldsCommands`.
- **`WorldsNetwork.toClient`**: each server-to-client payload had two registrations (client
  handler, server no-op); now one call. The client handler sits in a `Supplier` a dedicated server
  never calls. Booted on a dedicated server, dist check clean.
- **The sky disc as a record.** `SkyBodyRenderer`'s face and halo repeated the disc-to-world maths
  through 11- and 12-parameter methods; now a small `Disc` record. `SkyBodyGeometry.daylight` split
  out of `brightness`, so the halo no longer inverts the face's formula (tested).
- **Earlier the same day,** the dist check learned to see method references (`fb6f368`), which is
  why the payload helper could be checked at all.

**Tests: `worlds` 79 → 80.**

## M3.3 — Planet 3: the Nether world

**Direction (Sanchit, 2026-10-06)**, replacing `planets.md`'s "radiated power world":

- **Mostly Nether in feel, not an exact copy.** Its own dimension (ADR-0004), not vanilla's
  Nether, which stays dormant (ADR-0014).
- **No roof.** Open sky all the way up, so it connects to the shared space dimension and is
  reached the same way as the Moon: ascend, orbit, descend. The vanilla Nether's bedrock ceiling
  is exactly what this drops.
- **Netherite is the unique resource.** That doubles as the payoff for ADR-0014's recipe debt:
  ancient debris, quartz, blaze rods, glowstone all come back once this world exists.
- **A boss, yes.** Which one is open.

**What makes this cheap (to confirm while building, not assumed):** a custom dimension can use
vanilla's Nether *biomes* with our own noise settings. If it does, vanilla's Nether structures
(fortresses, bastions) and ores (ancient debris, quartz, gold) follow the biomes through their
existing tags, so the whole Nether loot economy arrives without our writing it. The new
engineering is the terrain: the vanilla Nether's noise router builds a cave world under a
ceiling, so a roofless version needs its own `noise_settings` (open surface, lava seas, basalt
and soul-sand valleys rising toward the sky).

**Boss candidates.** Cataclysm (already integrated, `ascension-compat-cataclysm`) has two Nether
bosses, both bigger than the Moon's on purpose (M3.1 "saving the bigger kits for later worlds"):
**Ignis** (Burning Arena) and **the Netherite Monstrosity** (Soul Black Smith). The Monstrosity
fits a netherite planet by name. Same pattern as the Moon: relocate the structure with
`fixed_position`, its boss drops this planet's gateway key, and `GatewayRules` needs no new code
(the key names its planet).

**Decided 2026-10-06:** the boss is **the Netherite Monstrosity**. **No power generation** here
("radiated power" may land on a different planet). **No ash storms** for now.

**Open, needs Sanchit before building:**
1. ~~Which boss~~ (the Netherite Monstrosity).
2. **The survival rule** (`planets.md`: every mandatory world introduces one). Natural fits: heat
   (a suit or cooling mechanic, ties to `atmosphere`), a toxic/sulphur atmosphere (oxygen
   already works here for free, the planet just declares an unbreathable atmosphere), or both.
3. **The traversal wrinkle** (also required): lava seas to cross, sky-high basalt pillars? (Not
   ash storms, for now.)
4. ~~Radiated power~~ (not here; maybe another planet).
5. Ambient life: vanilla Nether mobs come with the biomes; anything extra?

**Art** goes to a separate design session through a contract doc, the same way the gateway did.

## Explicitly out of scope for M3

- Team-shared gateway state (see M3.2's open question) — resolve when `ascension-progression`
  exists to own team capability state; forcing it into `worlds` now would be exactly the
  Tier-1-depends-on-a-module-that-doesn't-exist-yet mistake ADR-0011 exists to prevent.
- Boss balance/anti-cheese tuning beyond a first playable pass — `todo.md`'s "Combat:" open
  questions stay open until there's a live fight to tune against.
- Gravity-per-planet — still blocked on `ascension-gear` existing, per `todo.md`.
