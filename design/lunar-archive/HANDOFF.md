# Lunar Archive design session — handoff (complete 2026-10-07)

**The design session is finished and the branch is ready to merge.** It was paused once, partway
through handing back, and a second session finished the list below. Start from
`docs/technical/lunar-archive-design.md` (the design record) and the "Design's handover" section of
`docs/technical/lunar-archive-format.md` (the contract). Nothing has been seen in a running client
yet; the design note ends in the live checklist for that (ADR-0008).

## Where the work is

- **Worktree:** `E:\code\Project-Ascension-archive`, branch **`lunar-archive-design`**. The main
  checkout `E:\code\Project-Ascension` belongs to the logic session.
- **Rebased onto `main` at `77e5eeb`** (2026-10-07) and fast-forwards from it. If `main` moves
  again before the merge, rebase, re-run both generators and the build, and check again.
- Commits on the branch: first pass (templates, murals, looks), the headless placement and its
  fixes, the plinth-placement checks, the design note and contract handover, the final headless
  re-check, the art-guide additions, and this note.
- Nothing is pushed.

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
- Both generators are byte-for-byte reproducible. All models resolve. The stairs blockstate matches
  vanilla's 40 variants. `./gradlew :modules:worlds:build` passes on the rebased branch.
- **Headless placement, twice.** First at four sites, then, on the final templates, at four new
  sites in a fresh world, including the real one (x −160, z −256). Both times all 22,395 template
  cells matched the world at every site, block state for block state, with all eight plinths in
  place. The second run covered three start rotations; the court's rim met the ground within −2 to
  +3. One site of the second run landed in a crater that had already generated, which exposed the
  vault's roof. That is a `/place` artefact, but it led to the crater request below. The scripts
  are in `design/lunar-archive/headless-check/` (throwaway, hard-coded to this machine's paths);
  the test world has been deleted.
- **Art guide** (`docs/technical/art-guide.md`): templates carry block states; which way is up on an
  up face; tool rows for both archive scripts, the `.nbt` previews and `render_block_model`; the six
  inlay palettes; three failures and their fixes.

## If you re-run the generators

`git status` may list about 140 JSON files under `assets/ascension_worlds/` as modified straight
after a run. They are not: the checkout wrote them CRLF (`core.autocrlf=true`) and the generator
writes LF, which is exactly what is committed. `git diff` shows nothing for them, and
`git hash-object --no-filters <file>` equals `git rev-parse HEAD:<file>`. `git add` on them stages
nothing and clears the listing.

## Open, not design's call (flagged in the design note)

- **The lore spread.** `docs/lore/lore-spread-proposal.md` proposes moving *The Last Order*,
  *The Sealing* and *Unfinished* off the Moon. The sealed room and the dead gate are staged for
  *Unfinished*. Re-pointing a plinth is one line in the generator, but the sealed room would want
  new text that earns its staging. The choice is Sanchit's.
- **For logic,** all in the contract's "Design's handover":
  - `.noOcclusion()` and shapes on the plinth and orrery (boxes given).
  - `terrain_adaptation: "none"` and `start_height -21`, with the two pools named there.
  - **New:** an `exclusion_zone` on `structure_set/large_crater.json` against the Archive's
    structure set (`chunk_count` 10), so a crater never hollows the ground round the Archive and
    leaves its piece boxes standing in the bowl. Nothing protects the gateway ruin from craters
    either.
