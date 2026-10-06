# Lunar Archive design session — handoff (paused 2026-10-07)

The design session was stopped partway through its last step (writing up and handing back). This
note says exactly where it stopped, so a new session can finish it. Read it, then
`docs/technical/lunar-archive-design.md` (the design record) and the "Design's handover" section of
`docs/technical/lunar-archive-format.md` (the contract).

## Where the work is

- **Worktree:** `E:\code\Project-Ascension-archive`, branch **`lunar-archive-design`**. The main
  checkout `E:\code\Project-Ascension` belongs to the logic session, so don't work there.
- **Rebased onto `main` at `eb468a4`** (2026-10-07). It should still fast-forward unless `main` has
  moved since.
- Commits on the branch: first pass (templates, murals, looks), the headless placement and its
  fixes, the plinth-placement checks, and this handoff (design note plus contract handover).
- Nothing is pushed (the prompt says don't push).

## Done

- `tools/generate-lunar-archive.py` writes `entrance.nbt` (start, 19 × 47 × 48, **surface_y 21**)
  and `vault.nbt` (48 × 21 × 48), one jigsaw between them. Before writing, it checks the contract:
  the eight entries once each, one orrery with its clear volume, chest loot, jigsaw pairing, plinths
  free-standing and facing a reader, light on every floor cell a mob could stand on. It then writes
  the previews.
- `tools/generate-archive-art.py`: 121 mural tiles (8 murals), the plinth, orrery and tablet looks,
  the stair models, and the mural models and blockstate. It also checks `ArchiveMuralBlock.TILES`.
- `tools/render-structure-preview.py` reads `.nbt` and draws plans, cut-aways, a section,
  perspective views and textured block models (`render_block_model`).
- Java: `internal/archive/ArchiveDecorBlocks.java` and `ArchiveMuralBlock.java`, plus one line in
  `AscensionWorlds`. Data: loot, tags (`archive_decor`, `#minecraft:stairs`), recipes, lang names.
- Both generators are byte-for-byte reproducible (350 files checked). All models resolve. The stairs
  blockstate matches vanilla's 40 variants. `./gradlew :modules:worlds:build` passed before the
  rebase.
- Headless placement on a dev server at four sites matched the templates block for block. The
  scripts are in `design/lunar-archive/headless-check/` (throwaway, hard-coded to this machine's
  paths).

## Left to do, in order

1. **Re-run the headless check once** on the final templates. The rotunda's diagonal pilasters and
   the moved rubble landed after the four-site run. Cell counts are unchanged (22,395); confirm 0
   differences.
   - Run `python design/lunar-archive/headless-check/setup_test_server.py`.
   - Start `./gradlew runDevServer` in the worktree, in the background, and wait for "RCON running".
   - Over RCON (`RCON_PASSWORD=archive-test-local`): `execute in ascension_worlds:moon run forceload
     add ...`, then `place structure archive_test:archive <x> 64 <z>`, then `save-all flush`.
   - Run `python design/lunar-archive/headless-check/verify_sites.py <out-dir>`, then `stop`.
   - Afterwards delete `run/server/archive-test`; `run/` is gitignored. Note that
     `run/server/server.properties` in the worktree has RCON on, but it is the worktree's own,
     gitignored copy.
2. **Art guide additions** (`docs/technical/art-guide.md`; the edit was interrupted, nothing written):
   - Templates (`.nbt`) carry block states, so real stairs are fine there.
   - An up face lays its texture's top along the north edge (the plinth tablet's flipped uv).
   - Tools table rows: `generate-archive-art.py`, `generate-lunar-archive.py`,
     `render_block_model`, the `.nbt` previews.
   - Palettes table: `ASH`, `ICE`, `FORGE`, `EARTH`, `GREY`, `SUN` inlay stones in the archive
     script.
   - Failures table: the 3-px diamond and the hollow block diamond both read as crosses (fixed by
     the almond eye and solid lozenges); white-on-white relief figures vanished (fixed by
     black-figure builders); the tablet read upside down (fixed by the uv flip, caught by
     `render_block_model`).
3. Re-run both generators and `./gradlew :modules:worlds:build`. `git status` must be clean apart
   from intended changes.
4. Rebase onto `main` if it moved, confirm the branch fast-forwards, and tell Sanchit it is ready to
   merge.

## Open, not design's call (flagged in the design note)

- `docs/lore/lore-spread-proposal.md` proposes moving *The Last Order*, *The Sealing* and
  *Unfinished* off the Moon. The sealed room and the dead gate are staged for *Unfinished*.
  Re-pointing a plinth is one line in the generator. The choice is Sanchit's.
- Logic should add `.noOcclusion()` and shapes to the plinth and orrery (boxes are in the contract),
  and use `terrain_adaptation: "none"` and `start_height -21` (reasoning in the contract).
