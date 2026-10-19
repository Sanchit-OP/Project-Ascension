# Prompt for the Planet 4 (the End) design session

Paste everything below the line into a new Claude Code session opened on `E:\code\Project-Ascension`.

---

You are the **design session** for Planet 4 in Project-Ascension, a Minecraft 1.21.1 NeoForge mod project. Planet 4 is the
vanilla End as an asteroid belt: players reach it by airship, kill the Ender Dragon, and its exit portal becomes the End's
gate (ADR-0028). Fall out of the bottom of the belt and you are handed back to space. The logic is built and working with
placeholders; your job is how it looks. Another session may be working in the main checkout, so **work in your own git
worktree**:

```
git worktree add -b end-art-design ../Project-Ascension-end-art main
```

and do all your work there. Commit as you go. Do not push.

**Read first, in this order:**
1. `docs/technical/end-art-format.md`: your contract with the logic session. It says what you own, what the formats can and
   cannot do, and the handover checklist. If you need something it cannot express, propose a change to that document.
2. `docs/technical/art-guide.md`: **original art only** (never copy, trace or sample vanilla, another mod or a pack), **art is
   code** (every PNG from a seeded script in `tools/`, reproducible byte for byte), light from the top-left, teal is the
   network's colour, the diamond eye is its motif.
3. `docs/lore/the-end.md` (an outline) and `docs/decisions/0028-planet-4-is-the-end.md`: the islands are what is left of a
   world taken apart on purpose; the Endermen are the Observers showing through; the Dragon is the last keeper.
4. `docs/technical/worlds-api.md`: "Sky bodies", "Haze", "Space bodies"; and `docs/technical/nether-art-design.md` for how a
   design session did the same for the Nether, with its generators and preview tools to reuse.

**What to build** (the contract's sections 1, 2 and 4): the End's face from space (a belt of pale rock, not a plain sphere,
within what the space-body format can draw, or a proposal for one more field); its haze and any sky bodies; the End's style
for the quest-book planet sprite in `tools/generate-quest-art.py`.

**How to work:**
- Make previews and **look at them yourself** before calling anything done: the planet lit and up close, the views down from
  y 216 and out from the arena. Expect several rounds of critique.
- Don't touch logic's files: Java, worldgen JSON, loot tables, lore text, the Dragon's balance.
- `./gradlew build` must pass before each commit.

**Hand back** when the contract's checklist is done: a design note `docs/technical/end-art-design.md` ending in a live
checklist, previews in `design/end/`, and the branch rebased onto `main` so it fast-forwards. Then tell Sanchit it is ready to
merge.
