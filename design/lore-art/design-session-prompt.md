# Prompt for the lore art design session

Paste everything below the line into a new Claude Code session opened on `E:\code\Project-Ascension`.

---

You are the **design session** for the lore art in Project-Ascension, a Minecraft 1.21.1 NeoForge
mod project. Another session (the "logic session") is working in the main checkout at the same time,
building the behaviour that will use your art. So **work in your own git worktree**:

```
git worktree add -b lore-art-design ../Project-Ascension-lore-art main
```

Do all your work there. Commit as you go. Do not push.

**Why this exists.** The project's story was settled with Sanchit (the owner) over 2026-10-07 to
10-10. An ancient people opened portals and then gates to other worlds, wore a hole into the void,
and woke the **Observers**. These are beings who had never been seen, and cannot bear it. A voice
now leads the player through a flattering, false story. The truth survives only in pictures. A
**mark** grows on the player's hand with every gate they light, and the voice calls it an honour.
It is really the Observers counting the seal's broken locks. Your art is what makes that story
visible: the mark, the glyphs, the pillar that lights in step with your hand, and the pictures that
contradict the words.

**Read first, in this order:**

1. `docs/technical/lore-art-format.md`. This is your contract with the logic session. It sets what
   you own and what you must not touch, every id, path and format, and the handover checklist. If
   you need something it cannot express, propose a change to that document rather than working
   around it.
2. `docs/technical/art-guide.md`. How art is made here. The hard rules:
   - **Original art only.** Never copy, trace or sample vanilla, another mod or a pack. This is
     Sanchit's legal line.
   - **Art is code.** Every PNG, model and `.nbt` comes from a seeded script in `tools/`, and
     re-running it reproduces the committed files byte for byte.
   - Light comes from the top-left.
   - **Teal is the network's colour**, and the **diamond eye** is its motif.
   - No accidental symbols.
3. The story, for tone and meaning:
   - `docs/lore/what-the-warden-keeps.md` (the whole story).
   - `docs/lore/the-mark.md` (your main subject).
   - `docs/lore/the-moon.md` (every Moon piece, and what the murals must show).
4. What already exists, which your work extends:
   - `docs/technical/lunar-archive-design.md` (the Archive as built: its rooms, murals, materials
     and lighting).
   - `tools/generate-lunar-archive.py` and `tools/generate-archive-art.py` (its two generators).
   - `tools/generate-gateway-art.py`: `rune_pixels()` is the script's existing look, and your
     glyphs must be its family.

**What to build:** everything in the contract's nine sections:

1. The script: seven glyphs and an eye in eight stages.
2. The script as a bitmap font.
3. The mark on the player's right arm, stages 1 to 7, for wide and slim arms.
4. The illagers' complete mark.
5. The `mark_glyph` block and the pylon's glyph band.
6. The revised Archive murals: cats, Earth's side made consistent, and the Road's gouge.
7. The Sealers' Record: a new picture mural, with an empty frame in the sealed room.
8. The keeper shrine template.
9. The light on the Moon.

Start with the script (section 1). Everything else is drawn from it. Show Sanchit the glyph sheet
before going further, because the glyphs are the one thing every other piece depends on.

**How to work:**

- Make previews and **look at them yourself** before calling anything done. Expect several rounds of
  critique.
- The mark is the hardest piece. A 4 × 12 arm face may be too small for seven glyphs and an eye. If
  it is, use the contract's Format B and write the change into it first.
- Preview the mark in first person, the way the player will actually see it. The combat session
  built `tools/render-item-model.py` with first-person views, so see whether it helps.
- Keep `ArchiveMuralBlock.TILES` and the art script's tile count in step. The script refuses to run
  if they differ.
- Keep the Archive's rules: deterministic NBT (sorted palette, fixed block order, gzip mtime 0,
  DataVersion 3955), and no unlit floor a mob could stand on. The generator already checks both.
- Don't touch logic's files: Java behaviour outside `ArchiveDecorBlocks` and `ArchiveMuralBlock`,
  `WorldsContent`, `ArchiveContent`, worldgen JSON, loot tables, client rendering code, and the lore
  text in `tools/archive-lore.py`. Lang names for your own blocks are fine.
- `./gradlew :modules:worlds:build` must pass before each commit.

**Hand back** when the contract's checklist is done:

- a design note, `docs/technical/lore-art-design.md`, ending in a live checklist;
- previews in `design/lore-art/`;
- the Sealers' Record's tile range and layout, and the shrine's size, in the note;
- the branch rebased onto `main`, so it fast-forwards.

Then tell Sanchit it is ready to merge. If you are paused partway, leave `design/lore-art/HANDOFF.md`
saying what is done and what is next.
