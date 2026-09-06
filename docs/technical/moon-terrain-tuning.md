# Moon terrain tuning — session handoff

**Status as of 2026-09-06: both outstanding issues from the previous session are fixed and
verified headlessly.** Sanchit flagged two problems after testing the previous session's build:
large craters still looking cut off, and mountains generating as ugly vertical pillars with flat
tops instead of climbable peaks. Both are root-caused and fixed below. **Not yet confirmed by a
human standing in a live client** — see "What's still worth checking in a live client" at the
end before calling this fully closed per ADR-0008.

## Where this picks up from

M2.3 (real Moon terrain) landed in `f240759`. Two follow-up fixes landed after that:

- `0a2c00c` — fixed shallow deepslate / swiss-cheese terrain caused by the density function's
  gradient band.
- `f2a222a` — the actual root cause of "patchy, swiss-cheese" terrain: overlapping craters.
- (uncommitted, this session) — crater density halved/quartered, hills waviness increased, a
  first attempt at mountains landed but was untested against a fresh world.

Sanchit then tested that build and reported two problems, verbatim: large craters still clipped,
and mountains "are horrible... small areas suddenly 120 blocks up... instead of being climbable
mountains they are straight pillar like... if we can fix them by actually adding a way to create
slope... and the top of the mountain should not also be flat." He was explicitly willing to drop
mountains entirely if they couldn't be fixed properly, but wanted the crater clipping fixed
regardless.

## Issue 1: large craters clipping — fixed by converting them to a Structure

### Root cause, found by reading the actual game engine source, not guessing

Extracted `WorldGenRegion.java`, `ChunkPyramid.java` and `StructureStart.java` from the real
NeoForge 21.1.249 build (via `neoformruntime`'s cached intermediate jars in
`~/.gradle/caches/neoformruntime/intermediate_results/sourcesAndCompiledWithNeoForge_*.jar` — this
has the actual joined Minecraft+NeoForge source for the exact version this project pins, which is
worth remembering as a technique next time a "why does vanilla do X" question needs a real
answer instead of a guess).

Confirmed: the `FEATURES` chunk generation step has a hardcoded `blockStateWriteRadius` of **1
chunk** (`ChunkPyramid.java`). Any `setBlock` call landing outside the currently-generating
chunk's immediate neighbours is silently dropped — no exception, no partial write, just gone
(`WorldGenRegion.ensureCanWrite`, which is what logs the "Detected setBlock in a far chunk"
error). `CraterFeature`'s large-crater configuration samples radius 25–45, giving a search radius
up to ~52 blocks from a single random origin — routinely 2–3× past the safe envelope (worst case
as low as 16 blocks depending on where in the chunk the origin lands). Small craters (radius
5–10, search radius ≤12) never approached this limit, which is exactly why only large craters
clipped.

### The fix

Converting a `Feature` to a `Structure` was the correct fix, not a smaller radius cap — confirmed
against `StructureStart.placeInChunk`: the game calls `StructurePiece.postProcess` once for
**every chunk a piece's bounding box overlaps**, handing over that specific chunk's own safe
write area each time. A crater spanning several chunks now gets written completely, one
chunk-sized slice per call, instead of once from a single origin — this is the exact mechanism
vanilla itself uses for anything bigger than a chunk (ocean monuments, mansions).

New/changed files:

- `CraterStructure.java` (new) — decides where a large crater starts (jittered a few blocks
  within its chunk, cosmetic only — unlike the old `Feature`, placement no longer needs to be
  biased toward the chunk centre for safety).
- `CraterPiece.java` (new) — the actual carve/rim algorithm, moved from `CraterFeature.place()`
  essentially unchanged. The only real difference: each `postProcess` call clips its scan to the
  slice of the crater that falls inside the chunk it was called for (via `chunkPos`'s own min/max
  block bounds), rather than rescanning the whole footprint every time.
- `CraterFeature.java` / `CraterConfiguration.java` — now small-craters-only; javadoc updated to
  say so and point at why.
- `WorldsContent.java` — registers `CRATER_STRUCTURE_TYPE` (`Registries.STRUCTURE_TYPE`) and
  `CRATER_PIECE` (`Registries.STRUCTURE_PIECE`), alongside the existing `Feature` registration.
- `worldgen/structure/large_crater.json` (new), `worldgen/structure_set/large_crater.json` (new)
  — replace `configured_feature/large_crater.json` and `placed_feature/large_crater.json`
  (deleted). Placement is `minecraft:random_spread` with `spacing: 7, separation: 3` (~49 chunks
  per candidate cell), chosen to land close to the old `rarity_filter chance: 50` density —
  not an exact statistical match (spread placement grids+jitters rather than rolling
  independently per chunk), close enough and verified by the scan below to not be egregiously
  more or less frequent.
- `worldgen/biome/moon_highlands.json` — `ascension_worlds:large_crater` removed from the
  features list (structures aren't referenced there; `moon_mare` never generated large craters
  and still doesn't).

Small craters deliberately stay a `Feature` — their radius never gets close to the one-chunk
limit, so there's nothing to gain from the extra machinery.

### Verification (headless, via the existing RCON + region-scan tooling)

Ran the exact recipe `moon-terrain-tuning.md` already documented (see below) against a fresh
world:

- Force-generated ~300 chunks across four quadrants around spawn plus a targeted area around a
  located `ascension_worlds:large_crater`. **Zero "far chunk" errors in the server log**, across
  the whole session (previously this was frequent enough to be noticed during ordinary testing).
- **Reload test** (required by ADR-0008 — this introduced new persistent NBT data via
  `CraterPiece.addAdditionalSaveData`/its NBT constructor, which had never been exercised): located
  a fresh crater, force-generated only its western half, stopped the server, restarted it, then
  force-generated the eastern half. No exceptions on load, and the scanned cross-section through
  the crater's exact centre — the single most sensitive point for this test — came out as a
  clean, symmetric bowl: baseline ~24 → rim rising to ~31 → floor down to ~8 at centre → rim
  ~33 on the far side → back to baseline. The restart boundary is invisible in the data.

## Issue 2: mountains as vertical pillars with flat tops — fixed

### Root cause

The mountain noise term was being **added** to `final_density` alongside the existing baseline
`y_clamped_gradient(from_y=0, from_value=1.5, to_y=40, to_value=-1.5)`. That gradient is what
makes a column stop being solid as `y` increases — but it's clamped: above `y=40` it flatlines at
a constant `-1.5` and never decreases further. The hills term (amplitude ±1.6) was small enough
to never expose this. The mountain term was not: once its offset exceeds `1.5`, density stays
strongly positive **indefinitely** above `y=40` — there is no further term that ever brings it
back down. The column is solid all the way up until something else stops it (world height, or
noise-cell sampling granularity), which is a vertical pillar with an arbitrary, flat cutoff — not
a tapering peak. This also explains the "120 blocks up" report: the previous session's own
calibration table (multiplier 180, "tallest peak seen: surface_y 30") was almost certainly
measured against a stale/non-fresh world region, exactly the trap the table's own surrounding
text warned about ("the multiplier value at handoff was itself untested against a fresh world").
The real, fresh-world behaviour at multiplier 180 was never this session's problem to reproduce —
it's already established to have been wrong.

### The fix

Two changes, both to the *same* `y_clamped_gradient` node (there are two identical copies, in
`final_density` and `initial_density_without_jaggedness` — both updated identically, as before):

1. **Extend the gradient instead of letting it saturate.** Changed `to_y: 40, to_value: -1.5` to
   `to_y: 320, to_value: -22.5`. This preserves the *exact same slope*
   (`(to_value - from_value) / (to_y - from_y) = -0.075/block`) — so baseline terrain (hills,
   craters) is byte-for-byte unchanged in the height range they actually use (roughly y=-1 to
   y=41) — but now a mountain's offset keeps translating into height linearly instead of hitting
   a wall at y=40. A column's final height is no longer capped by an arbitrary flat ceiling; it's
   wherever the (now-uninterrupted) linear relationship actually crosses zero. This is also what
   fixes the flat-top complaint: neighbouring columns with slightly different noise magnitudes
   now resolve to smoothly different heights instead of all hitting the same shared ceiling.
2. **Rescale the multiplier down from 180 to 25.** With the gradient's slope fixed at `0.075`,
   *any* multiplier translates a magnitude into `magnitude / 0.075` blocks of height — at 180,
   even the empirically-observed noise ceiling (~0.65, giving max excess ~0.15) would have
   produced a ~360-block spike once the saturation bug was fixed, i.e. still absurd, just sloped
   instead of instant. 25 was chosen to target common moderate excess (~0.05) giving modest
   ~15–20 block rises, and rare near-ceiling excess (~0.15) giving ~50-block peaks — "not very
   common but still seeable in all directions... not too much," per Sanchit's original ask.
   Threshold stays at 0.5 (unchanged) — the previous session's own table already established that
   as the right *rarity*, independent of multiplier, and the project's own stated lesson is not
   to change two variables in the same test.

### Verification (headless)

Region-scan tool (`scan_region.py`) had its own surface-search range extended from `y=60` to
`y=200` first — the old cap would have silently under-reported any peak above 60, hiding exactly
the thing being tested. After force-generating ~300 chunks:

```
surface_y range: 0 to 66, avg 32.2
surface_y distribution:
     0..   9: 69       40..  49: 1357
    10..  19: 649       50..  59: 371
    20..  29: 2849      60..  69: 70
    30..  39: 2507
```

No column anywhere near the world height cap (confirms no pillar/runaway), a smooth distribution
rather than a spike concentrated at one ceiling value (consistent with "no more shared flat top"),
and a max-to-baseline delta (~66 vs a pre-mountain baseline of ~17-20) in the intended 40-50 block
range.

## What's still worth checking in a live client

The scan above proves the *numbers* are sane — no pillars, no runaway height, no far-chunk drops,
a plausible rarity and height distribution. It does not prove the mountains *feel* climbable or
that the peak silhouettes read as "mountain" rather than "big rounded hill" — that is a visual/
movement judgement a region-file scanner cannot make. Per ADR-0008, this needs an actual client
session before it counts as done:

- Fly or walk to a mountain (the scan found several in the 50-66 surface_y range near spawn) and
  check the slope is actually walkable/climbable, not just "less vertical than before."
- Check the peak silhouette doesn't still read as flat from a distance — the fix removes the
  *artificial* flat ceiling, but whether the underlying 2-octave noise's natural peak shape looks
  interesting enough is a separate, softer question the numbers can't answer.
- Visit a large crater in person (e.g. spawn area or the ones this session found) and confirm it
  reads as a believable bowl+rim now that it's not missing its outer edge.
- If mountains still don't look right after this, the next lever (per the design intentionally
  *not* pulled this session, to avoid changing two things at once) is the raw noise's amplitude in
  `noise/moon_mountains.json` — currently `[1.0, 1.0]` — rather than the multiplier again.

## Verification tooling

`tools/worldgen-diagnostics/` — unchanged from the previous session except `scan_region.py`'s
surface-search range, extended from `y=60` to `y=200` (see above; the 60 cap predates mountains
and would have hidden the exact thing this session needed to measure).

- **`rcon_client.py`** — ~40-line Source RCON client. Lets a script run server commands —
  critically, `forceload add <x1> <z1> <x2> <z2>` — without a player ever connecting.
- **`scan_region.py`** — reads `.mca` region files and their NBT chunk data directly, reporting
  per sampled column: surface height, and non-solid ("hole") blocks below that surface.

### The recipe, end to end

1. Confirm nothing else has the world open: `Get-CimInstance Win32_Process -Filter
   "Name='java.exe'"` and check for `devlaunch`/`forgeserverdev` in the command line.
2. In `run/server/server.properties`, set `enable-rcon=true` and a `rcon.password` — both are
   blank/disabled by default and **must be reverted after**, since RCON has no other
   authentication.
3. Delete `run/server/ascension-dev` for a truly fresh world if the test needs to observe
   generation from scratch (dimension generator settings freeze into `level.dat` at world
   creation — editing `noise_settings/moon.json` does nothing for an existing world).
4. `./gradlew :modules:worlds:runServer` (background it; wait for `Done (...)!` in its log).
5. `RCON_PASSWORD=<yours> python tools/worldgen-diagnostics/rcon_client.py execute in
   ascension_worlds:moon run forceload add <x1> <z1> <x2> <z2>` — max 256 chunks per call.
   `execute positioned <x> <y> <z> in ascension_worlds:moon run locate structure
   ascension_worlds:large_crater` finds one to target specifically.
6. `python tools/worldgen-diagnostics/scan_region.py
   run/server/ascension-dev/dimensions/ascension_worlds/moon/region` and read the surface-height
   distribution and hole count.
7. When done: `rcon_client.py stop`, revert `enable-rcon`/`rcon.password` to blank/false.

This session additionally used the reload-test pattern (generate half a structure's footprint,
restart the server, generate the other half, confirm the seam is invisible) — worth reusing any
time a change touches something with persistent NBT state, per ADR-0008.

## Files touched this session

- `modules/worlds/src/main/java/com/ascension/worlds/internal/terrain/CraterStructure.java` (new)
- `modules/worlds/src/main/java/com/ascension/worlds/internal/terrain/CraterPiece.java` (new)
- `modules/worlds/src/main/java/com/ascension/worlds/internal/terrain/CraterFeature.java` (javadoc)
- `modules/worlds/src/main/java/com/ascension/worlds/internal/terrain/CraterConfiguration.java` (javadoc)
- `modules/worlds/src/main/java/com/ascension/worlds/internal/WorldsContent.java` (registrations)
- `modules/worlds/src/main/resources/data/ascension_worlds/worldgen/structure/large_crater.json` (new)
- `modules/worlds/src/main/resources/data/ascension_worlds/worldgen/structure_set/large_crater.json` (new)
- `modules/worlds/src/main/resources/data/ascension_worlds/worldgen/configured_feature/large_crater.json` (deleted)
- `modules/worlds/src/main/resources/data/ascension_worlds/worldgen/placed_feature/large_crater.json` (deleted)
- `modules/worlds/src/main/resources/data/ascension_worlds/worldgen/biome/moon_highlands.json`
- `modules/worlds/src/main/resources/data/ascension_worlds/worldgen/noise_settings/moon.json`
- `tools/worldgen-diagnostics/scan_region.py` (surface-search range)

The `run/server/ascension-dev` world currently on disk is the fresh world used for this session's
verification — it already has the new terrain generated around spawn and around the tested
crater/mountain coordinates, so it's usable as-is for a live-client check rather than needing
another regeneration.

## Follow-up after Sanchit's live-client check

Tested in game: crater clipping and mountain pillars both confirmed fixed. Mountains specifically
weren't spotted at all during play, which Sanchit was fine leaving as-is ("it looked good overall")
— **the mountain multiplier/gradient from this session is intentionally untouched.** Three more
changes requested, accepted without a further verification pass (his call, explicitly) and applied
directly:

1. **Large craters reduced to 1/5 of their (already-reduced) current frequency.** `structure_set/
   large_crater.json`: `spacing` 7→16, `separation` 3→7 (scaled proportionally). `random_spread`
   density is roughly `1/spacing²`, so `(16/7)² ≈ 5.2` — close enough to 5x given the doc's own
   earlier note that this placement type isn't an exact statistical match for the old
   `rarity_filter` chance model anyway.
2. **Small craters reduced to 1/6 of current frequency.** `placed_feature/small_crater.json`:
   `rarity_filter chance` 16→96 (chance is inverse-frequency, so ×6 frequency reduction = ×6 on
   the chance number).
3. **Craters must not destroy other structures.** This world will get more structures later, and
   nothing stopped a crater from carving straight through one placed nearby. Rather than teaching
   every future structure about craters (a coupling someone would have to remember to add each
   time), `CraterPiece` now checks *outward*: before carving or piling rim material onto any
   block, it asks `StructureManager.getStructureWithPieceAt` whether that exact position already
   belongs to some other generated structure's piece, and stops (same as hitting existing air)
   if so. Excludes `CraterStructure` itself, or a crater's own piece would block its own carve.
   This needed no config and needs no updates when new structures are added — it's a general
   check against "any structure that isn't a crater," not a list of specific structures to avoid.

The world was deleted again after these changes (structure_set/placed_feature changes need a
fresh world, same as before) and not re-verified via RCON — Sanchit's call, since these are small,
low-risk parameter/config changes riding on top of an already-verified mechanism.
