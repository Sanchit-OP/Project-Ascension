# Session prompt 8: Oxygen v2 (air as a fluid, a powered station, a dispenser, a better tank, air aboard ships)

Paste everything below the line into a new Claude Code session opened on this repository.

---

You are joining Project-Ascension: a Minecraft 1.21.1 NeoForge campaign built from our own mods,
by Sanchit (player name Devil0701). Your job is **Oxygen v2**: the second version of
`ascension-atmosphere`'s machines and tank. Version 1 is finished and works (tank with a valve,
refill station, sealed rooms, share-air), but its blocks are placeholders, and they were always
meant to be replaced (`docs/technical/atmosphere-api.md` section 7b).

## Sanchit's words (2026-10-19, and 2026-10-18 for the ship part)

> what i want in v2 is. i want the tank and the oxygen station to be redesigned it looks shit.
> then what i need is helium to be used as an upgrading material for lvl 2 of a tank so it holds more.
> and i want the oxygen center to work as a machine. so it needs the rotation energy from the create
> and more the energy more area it can keep oxygenated and all.
> and i want a way for a tank to store the oxygen too. so it works like we create a new block which
> is the dispenser. so we have create tanks right, in that now we can store air as well. so in ship it
> should be like we create this station (which can fill tanks using the right click but it no longer
> oxygenates the room, that is done by the air dispenser, which can be connected to the create fluid
> motor [the mechanical pump] which is connected to the tank having air) to add the oxygen in a closed
> space like how the block does it.

> (2026-10-18) dont we have an item which can fill air in a closed space. a ship can be and will be a
> closed space. so we can say that it will work in it.

In one picture (this is **my reading** of his words; section "Questions" asks him to confirm it):

```
 Create rotation ──► OXYGEN STATION ──► air (a fluid) ──► Create fluid tank (stores air)
  (stress, speed)    (a machine)             │                    │
                          │                  │                    └─► Create mechanical pump ─► pipes
                          └ right-click      └─ (or straight out)                                  │
                            fills a held                                                           ▼
                            portable tank                                              AIR DISPENSER (new block)
                                                                       holds a closed room breathable: a base, or a ship's cabin
```

Five jobs: **(1)** redraw the tank, the station and the room block, **(2)** a level-2 tank
upgraded with helium, **(3)** the station becomes a Create-powered machine, **(4)** air becomes a
fluid that Create's tanks and pipes carry, with a new Air Dispenser that pressurises a room from
that fluid, **(5)** all of it working aboard a Create Aeronautics ship.

## Read first

- `CLAUDE.md`, then `docs/decisions/`. These bind you:
  - **0003:** no Tier-1 module hard-depends on a third-party mod. Foreign integration lives in
    optional Tier-2 `ascension-compat-*` jars. Mixins only in Tier 2, with an ADR.
  - **0006 and 0016:** Sable is optional; read both before touching ships. **0011:** Tier-1 modules
    never depend on each other, they share contracts through `core`.
  - **0007:** performance. No per-tick scans, nothing static holding a `Level`/`Player`/`BlockEntity`,
    delta sync, no per-tick packets. **0008:** nothing is done until seen in a running client; world
    state on a dedicated server; anything saved, across a reload; a refactor pass every third increment.
  - **0009:** the tank's valve rules and the two-tanks-carried limit. **0018:** no world is required,
    materials reward rather than unlock. **0025:** helium is also rocket fuel (a Create data entry),
    so its supply is not unlimited demand.
- `docs/technical/atmosphere-api.md` end to end (especially 7, 7b and the sections on the valve
  and sources), `docs/gameplay/oxygen.md` (the "authored simulation" rule: **no gas physics**, if a
  rule cannot be said in one plain sentence it is overdesigned), `docs/progression/journey.md`
  ("Decided by Sanchit" and "What is true about air on a ship today"), `todo.md` ("Oxygen v2" and
  "Airlocks need solid blocks").
- `docs/technical/art-guide.md` before you draw anything. `docs/technical/dev-environment.md` for
  running the dev server and client. `docs/technical/pack-extras.md` for the mods in the pack.

## What is true today (checked in the code, 2026-10-19)

- **Tank** (`internal/supply/OxygenTankItem.java`): an iron-and-glass item holding **1,200 units**
  (4 units a second for 5 minutes), stored in the `oxygen_units` data component; the valve is the
  `tank_open` component; one is open at a time and a player may carry two (`AtmosphereTuning`).
  **`OxygenTankItem.capacity(ItemStack)` already takes the stack**, written so that "larger tanks
  later are a new item and not a change to every call site". Everything reads a tank through
  `TankOxygenSource`. The HUD's tank ring reads it through the public view, so do not change that API.
- **Refill station** (`OxygenRefillStationBlock`): right-click with a tank fills it, instantly and
  free, but **only where the player's head is in breathable air**. It never pressurised a room.
- **Oxygen Emitter** (`internal/sealed/`): one block, free, no power. A bounded flood fill (4,096
  blocks, 24 out, a full collision cube seals) from the emitter makes the room breathable.
  `SealedVolumeIndex` is a per-level attachment keyed by block position; the block entity only
  registers itself on chunk load; recompute happens on place, break and explosion events, deferred
  a tick (`SealedVolumeEvents`). It does not tick.
- **Art:** the module has **no texture of its own**. The emitter and station use vanilla's
  `iron_block` and `copper_block` (`models/block/*.json`) and the tank is two crossed boxes in
  `iron_block`. That is why they look bad, and it also breaks the original-art rule
  (`art-guide.md` rule 1). Everything new is drawn by a seeded script in `tools/`.
- **Ships:** Sable keeps a ship's blocks in a numbered "plot" of the *same* level (ADR-0016), so a
  block on a ship has an ordinary position in that level and an emitter there is indexed like any
  other. What is missing is the translation of *a player's world position* into the plot, which is
  the `AnchorSpace` seam in `api/AtmosphereAnchor.java` / `AnchorSpace.java`. **Nothing registers
  it**, and `ascension-compat-sable` has no atmosphere code. It has never been seen to work aboard
  a moving ship. I have **not** verified how Sable reports block edits inside a plot, or what
  happens to the index when a ship is assembled (blocks move in bulk, with no events) or
  disassembled. Find out before you trust the design.
- **`ascension-compat-create` is a datapack only** (no Java, no compile dependency on Create): it
  disables Create's backtank and diving helmet, and makes raw helium a superheated blaze-burner
  fuel. Create 6.0.10 and Aeronautics 1.3.2 are in the dev instance. The station in step 3 needs
  Create's kinetic classes, so **this jar will get its first Java and its first compile
  dependency**; that needs an ADR and a way to get Create's jar at compile time (see how
  `compat/sable` and `compat/jade` do theirs). `create_version_range` is already in `gradle.properties`.
- **Helium** is `ascension_worlds:raw_helium`, mined on the Moon. `atmosphere` may not depend on
  `worlds` (ADR-0011), so the tank recipe must name helium through an **item tag** (an empty tag
  just makes the recipe uncraftable, where a missing item breaks the recipe). Decide where the tag lives.
- **Things that name the three items and must keep working:** the quest book
  (`pack/quests/first_days.py`, `planets.py`, `road_up.py` lines about the station and emitter,
  `the_moon.py`, `together.py`), `tools/resource-ledger-rules.json` and `resource-ledger.md`. The
  build **fails** if the book names an item that does not exist, and if a registered item has no
  source or use. Never rename a shipped quest slug; changing what a quest *says* is fine.

## The job

1. **Design it first, in a doc, and ask.** Write `docs/technical/oxygen-v2.md` and an ADR (next
   free number, 0026 at the time of writing) for the structural choices: air as a fluid, which
   block lives in which tier, and the standalone story. Put the questions below to Sanchit, each
   with your recommendation first, and wait for his answers before building. Send him the doc
   (`SendUserFile`).
2. **Increments**, each small enough to see. Suggested order:
   - **A. The look.** New art for the tank, the station and the room block (and a level-2 tank
     skin, a fluid texture for air). No behaviour change, so Sanchit can see it alone. A new
     `tools/generate-atmosphere-art.py`, seeded; models with proper elements and our own PNGs; keep
     item ids. Palette: `art-guide.md` reserves teal for the gateways and **amber-cream for
     helium**, so level 2 can wear an amber-cream band that says "helium" at a glance. The station
     must show a shaft face once it is Create-powered.
   - **B. Air as a fluid, and the dispenser (Tier 1).** Register an `air` fluid type (no bucket, no
     world block). The **Air Dispenser** exposes the standard NeoForge fluid capability
     (`Capabilities.FluidHandler.BLOCK`), accepting only air, so Create's pipes and tanks connect
     to it with **no Create code in Tier 1**, and so do other mods' pipes. It holds a buffer and
     pressurises the sealed room around it while it has air; when the air runs out, the room stops
     being held (the index already supports that, the same path as a broken wall). Fluid units
     against tank units is a decision to make and write down.
   - **C. The powered station (Tier 2, `ascension-compat-create`).** A Create kinetic machine:
     rotation in, air out (into an internal buffer that a fluid tank or pump can drain, and that
     a right-click can fill a held portable tank from). Rate follows speed and stress in a way a
     player can predict with goggles on; "more energy, more area" has to become one plain rule.
     Create's stress display and goggles must say what the machine is doing.
   - **D. Tank level 2.** Helium as the upgrade material; holds more. Keeps charge and valve state
     through the upgrade. Form of the upgrade (a second item, or a level on the same item) is
     yours to propose; remember `MAX_TANKS_CARRIED`, the valve rules and the HUD's rings.
   - **E. Air aboard a ship (Tier 2, `ascension-compat-sable`).** Register the Sable side of
     `AnchorSpace`, so a player standing in a ship's cabin is asked about the ship's own blocks.
     The cabin must stay valid as the ship's blocks change and as the ship moves and turns, and
     a ship must stay a closed room across assembly, disassembly, a carry to space (ADR-0015),
     and a world reload. "Airlocks need solid blocks" (todo.md) is part of this: decide with
     Sanchit whether a sealing door block is needed.
   - **F. Everything that names the old blocks:** the quest book (the station and emitter quests
     on The Road Up, the Moon's air lesson, the flight pages' "A long flight" and "Share your
     air" lines), the resource ledger and its rules file, `journey.md`, `atmosphere-api.md` 7b,
     `docs/progression/quests.md`, `CLAUDE.md`'s current-state paragraph.
3. **Prove it, ADR-0008 style.** Add a server-side check in the pattern of `QuestChecks` /
   `TeamChecks` (a class that runs on `./gradlew runDevServer` and writes a pass/fail file; read
   `docs/technical/dev-environment.md`). It should cover at least: a dispenser with air holds a
   room and an empty one does not; the room recovers when air returns; a station's output follows
   its rotation; a tank upgrade keeps its charge; a ship cabin holds air after the ship has moved;
   a reload keeps all of it. World-state parts also need the dedicated server, and Sanchit's own
   eyes in a client for the art and the feel (see "How Sanchit works").
4. **Performance, measured** (`docs/technical/performance-log.md`). The dispenser and the station
   may tick, but only with arithmetic, never a scan; the flood fill stays on block changes only
   (ADR-0007). Record the heap and tick cost in the log.

## Constraints and traps

- **Tier split.** Fluid, dispenser, tank and level-2 tank are Tier 1 and use only NeoForge APIs.
  Anything touching Create's classes (the kinetic station) is `ascension-compat-create`. Anything
  touching Sable is `ascension-compat-sable`. Tier 1 must still load and be useful with only `core`
  present: **what is the standalone way to make air with no Create?** That is the first question below.
- **Do not break the ids the quest book uses** (`oxygen_tank`, `oxygen_refill_station`,
  `oxygen_emitter`) without updating the book in the same commit. Removing a registered block also
  deletes it from existing worlds; if the old emitter must go, think about a hidden legacy block.
- **Name a data folder anything but `build`.** `.gitignore` skips every folder called `build/`;
  that silently dropped 49 recipes last week (`docs/progression/build-paths.md` has the story).
- **Minecraft ignores a data field it does not know.** A misspelt key in a recipe, a tag or a
  model makes a condition empty with nothing in the log. `tools/check-data.py` runs in every build
  and catches the mistakes found so far; add a rule when you meet a new kind.
- Fluids in Create tanks are drawn from the fluid's client extension: give air a texture that
  reads as a faint pale gas and check it in a tank, in a pipe and in a pump.
- Hot air in an Aeronautics balloon is a different thing and must not be confused with this air.

## Working beside other sessions

- **The HUD** (`ascension-hud`) shows lungs and tank rings from the public oxygen view. Keep that
  API stable; tell Sanchit if a level-2 tank needs a new field.
- **The logic session** (the one that wrote this) owns the quest book and the pack mods. Small,
  honest edits to `pack/quests/*.py` for the renamed lines are yours; regenerate with
  `python tools/generate-quests.py`; the build runs it with `--check`. A bigger rewrite of the
  flight pages belongs to that session; ask Sanchit.
- **Build paths** use helium too (`docs/progression/build-paths.md`). Do not take items from that
  session's recipes; helium for the tank must come from the same tag.
- **Work in your own git worktree and branch** (for example `atmosphere-v2`). If the app did not
  make one, run `git worktree add`. Merge into `main` only when Sanchit says. Other worktrees exist
  (`combat-build-paths`, `combat-tooltips`).
- **Keep edits to shared files small:** `AscensionAtmosphere.java`, `AtmosphereContent.java`,
  `en_us.json`. Only one dev server can run at a time (ports 25565 and 25575); check before
  `./gradlew runDevServer` and never stop another session's java.

## How Sanchit works

- **Commit each step,** ending the message with `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`.
  **Never push** unless he asks. Stage explicit paths only, never `git add -A`.
- **Few game launches.** Build several increments, check them headless and on the dedicated
  server, then one combined client session with a short checklist for him. Do not start the game
  per tweak.
- **Original art only** (`art-guide.md`): seeded scripts, never copied from vanilla, Create or any pack.
- **Plain language.** Keep docs handoff-ready and send them to his phone (`SendUserFile`). When he
  must choose, ask a clear question with your recommendation first.
- Every build runs `tools/resource-ledger.py --check`, `tools/check-data.py` and the quest
  generator's check (Python on PATH). If this shell's guard rejects a complex command, write a
  Python script to your scratchpad and run it.

## Questions to put to Sanchit (do not decide these yourself)

1. **Is my picture right?** One producer machine (the Oxygen Station: rotation in, air out, right-click
   fills a held tank) plus the new Air Dispenser, which **replaces the Oxygen Emitter's job of holding
   the room**. Today's refill station never pressurised anything; the emitter did.
2. **The standalone story.** With no Create, how does Tier 1 make air? Recommendation: keep today's
   free emitter and station as the Tier-1 "basic" versions, and let `ascension-compat-create`
   switch off their recipes (as it already does for Create's backtank), so the pack uses only the
   Create chain while a player who installs only our atmosphere jar still has working air.
3. **"More energy, more area."** Recommendation: the station makes air at a rate that follows
   its speed times stress; a dispenser spends air in proportion to the size of the room it holds
   (so a bigger room needs more production), and a room whose supply falls short goes unsealed
   with the existing "pressure lost" message, never half-sealed. Is that the feel he wants?
4. **The old Oxygen Emitter.** Remove it, keep it hidden for existing worlds, or keep it as the
   free basic version (question 2)?
5. **Level 2.** How much more air (recommendation: about 2 times, 600 seconds), how much helium, and
   whether the upgrade is a second item or a level on the same item. Is there ever a level 3?
6. **Fluid scale.** How many millibuckets are a tank's worth of air, and should a tank filled
   from a Create tank and a tank filled by right-click be the same thing? (Recommendation: yes,
   1 unit = 1 mB.)
7. **Doors.** Should a ship get a sealing door/hatch block, or do players keep to full blocks?
8. **Share-air and the valve** are unchanged by this unless he says otherwise.
