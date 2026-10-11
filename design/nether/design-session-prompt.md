# Prompt for the Planet 3 (Nether) design session

Paste everything below the line into a new Claude Code session opened on `E:\code\Project-Ascension`.

---

You are the **design session** for Planet 3 in Project-Ascension, a Minecraft 1.21.1 NeoForge mod
project. Planet 3 is the real Nether, opened to the sky: players reach it by airship from space,
must wear gold against the rot in its air, and fight the Netherite Monstrosity for its gateway key.
The logic is built and working with placeholders; your job is how it looks. Another session (the
"logic session") may be working in the main checkout, so **work in your own git worktree**:

```
git worktree add -b nether-art-design ../Project-Ascension-nether main
```

and do all your work there. Commit as you go. Do not push.

**Read first, in this order:**
1. `docs/technical/nether-art-format.md`: your contract with the logic session. It says exactly what
   you own, the two placeholder files that become yours, the block ids, the seven tablet ids and
   where each should sit, the template rules, and the handover checklist. If you need something it
   cannot express, propose a change to that document rather than working around it.
2. `docs/technical/art-guide.md`: how art is made here. The hard rules: **original art only**
   (never copy, trace or sample vanilla, another mod or a pack), **art is code** (every PNG and every
   `.nbt` comes from a seeded script in `tools/`, and re-running it reproduces the committed files
   byte for byte), light from the top-left, **teal is the network's colour**, the **diamond eye** is
   its motif, no accidental symbols. Building structures out of vanilla blocks is fine.
3. `docs/lore/the-nether.md`: the story this world tells. The ancients' first and richest colony,
   where the voice first spoke; the piglins are the stranded colonists, who wear gold because the
   rot cannot hold it; the warped forest grew teal around the dead gate; the soul sand valleys are
   the last war's battlefield. Tone: a burnt, rich, haunted place, and a quiet horror about the gift
   of coming back from death.
4. `docs/technical/gateway-frame-format.md` (the site format and `frame_palette`),
   `docs/technical/lunar-archive-format.md` ("The template files") and
   `docs/technical/lunar-archive-design.md`: how the Moon's gate ruin and the Archive were made, and
   the generator and preview tools already written for them. Reuse them.
5. `docs/technical/worlds-api.md`, sections "Sky bodies", "Haze", "Space bodies" and "Planet 3":
   the formats for the sky and the look from space.

**What to build** (the contract's sections 1 to 5): the Nether's face from space; its haze and sky;
the gate ruin in the warped forest around the shared gateway frame, in Nether stone and gold; the
colony plinth's look; three or more colonists' ruins holding the seven tablets between them; and
the Sealers' carving for the soul sand valleys.

**How to work:**
- Write NBT deterministically (sorted palette, fixed block order, gzip with a fixed mtime), so
  regenerating is a clean `git status`. DataVersion 3955.
- Make previews and **look at them yourself** before calling anything done: the planet lit and up
  close, the views down from y 216 and up from a plateau, each ruin from the front, above and 64
  blocks. Expect several rounds of critique.
- To see the real terrain your ruins sit on, the test world's notes are in the contract (the gate
  stood at about (24, 107, 328) on a plateau, lava pools beside it) and `dev-environment.md` says
  how to check worldgen without a client.
- Don't touch logic's files: Java behaviour, `WorldsContent`/`ArchiveContent`, worldgen JSON, loot
  tables, the lore text (`tools/archive-lore.py` and the `archive.*` lang keys). Your lang names
  for blocks and items are fine.
- `./gradlew :modules:worlds:build` must pass before each commit.

**Hand back** when the contract's checklist is done: a design note
`docs/technical/nether-art-design.md` ending in a live checklist, previews in `design/nether/`, the
piece names and `surface_y` of every template for the logic session, and the branch rebased onto
`main` so it fast-forwards. Then tell Sanchit it is ready to merge.
