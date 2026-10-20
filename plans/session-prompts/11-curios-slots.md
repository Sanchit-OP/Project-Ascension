# Session prompt 11: Curios slots (a tank slot, a relic slot, a backpack slot)

Paste everything below the line into a new Claude Code session opened on this repository. **Not a blocker for the alpha**
(`plans/alpha-checklist.md`): a tank already works in the inventory and the two relics already work in the off hand. This
makes them nicer, and it is the answer to ADR-0009 section 4, which has waited since September.

---

You are joining Project-Ascension: a Minecraft 1.21.1 NeoForge campaign built from our own mods, by Sanchit (player name
Devil0701). Your job is **`ascension-compat-curios`**: three Curios slots, built so that nothing in Tier 1 ever needs Curios.

## Sanchit's words (2026-10-21)

> this is one other is to have curios slot added for the tank. for a backpack (also add a backpack mod). then the item slot
> which can take zonia or the sterak instead of the left hand that will be nice. and think if we need some other slot, of
> this much is fine.

His reason, from the same conversation: group play needs storage (the pack is adding Sophisticated Storage and Sophisticated
Backpacks, a separate piece of pack work), and the left hand should not be the only place Zonia's Hourglass or Sterak's Harness
can live.

## The three slots

| Slot | Holds | Behaviour |
|---|---|---|
| `ascension_tank` | an oxygen tank or a helium tank | Equipping **opens the valve**, unequipping **closes it** (ADR-0009 section 4). One slot, so one open tank, and the spare stays in the inventory. |
| `ascension_relic` | Zonia's Hourglass, Sterak's Harness | Works **exactly as the off hand does today**: the ability key, the passive, the stats. The off hand keeps working too; either place counts, never both at once. |
| `back` (Curios' own) | a backpack | The pack's backpack mod (Sophisticated Backpacks) is worn here. We assign the slot to the player and check it works; we write nothing to restrict what a backpack may hold (ADR-0009 forbids it). |

Do not add a fourth slot unless something forces it; Sanchit said this much is fine.

## Read first

- `CLAUDE.md`, then `docs/decisions/`. These bind you:
  - **0003:** Tier 1 never hard-depends on a third-party mod; foreign integration is an optional Tier-2 jar; mixins only in
    Tier 2 with an ADR; extension is through **provider registries**. **0009:** carry limits live on the player; **Curios is
    an optional front-end, never the mechanism**; section 4 is this job. **0011:** Tier-1 modules never depend on each other.
    **0007:** performance (no per-tick scans; nothing static holding a `Level`, `Player`, `Entity`, `BlockEntity`).
    **0008:** nothing is done until seen in a client; world state on a dedicated server; anything saved, across a reload.
    **0019:** item stats are data.
- `docs/technical/atmosphere-api.md` (collectors and the valve), `docs/technical/item-stats.md` and `combat-api.md`
  section 6 (the shared active key), `docs/technical/architecture.md` (the table lists `ascension-compat-curios` as "designed,
  not built"), `docs/technical/controls.md` (Curios is on G), `docs/technical/dev-environment.md` (the dev server must carry
  the same mods as the client, and both directions bite: Curios is the example).
- `docs/technical/art-guide.md`: **every texture is original and made by a seeded script in `tools/`.** The slot icons are
  yours to draw (16 x 16, brass and teal, the network's colours), not Curios' and not vanilla's.

## What is true today (checked 2026-10-21)

- **Curios API 9.5.1+1.21.1 is in the pack.** Its jar defines ten slots as data (`data/curios/curios/slots/*.json`: back, belt,
  body, bracelet, charm, curio, hands, head, necklace, ring) and **assigns none to the player**; a mod or datapack does that
  with `data/<ns>/curios/entities/*.json`. Its slot files use `"validators": ["curios:tag"]`, so what fits a slot is an item tag
  (`data/curios/tags/item/<slot>.json`). **Check the exact formats against the jar and its source before writing any file.**
- **A third party adds an oxygen source with `AtmosphereRegistry.register(id, OxygenSourceCollector)`.** Tier 1's own
  collector (`PlayerTankCollector`) reads only the 41 inventory slots and only the **open** tank, through `TankRules.openTank`;
  `TankRules` also enforces one open tank at a time (`closeAllExcept`) and the cap of two carried (`MAX_TANKS_CARRIED`).
  There is **no public API to open or close a tank's valve**; you will need to add one (`ascension-atmosphere`, `api`, a small
  addition noted in the API's own docs).
- **Zonia's Hourglass and Sterak's Harness are off-hand items in three places**: `ZoniasHourglassEffects.tryActivate`,
  `SterakHarnessEffects` (twice), plus the item data (`item_stats.json`: `"slot": "offhand"`) and
  `ItemStatModifiers`, which attaches their stats under `EquipmentSlotGroup.OFFHAND`. There is **no way for another module to say
  "this stack counts as worn"**; you will need to add one (`ascension-combat`, `api`, a provider registry in the style of
  `OxygenSourceCollector`, Tier 1, no Curios in it).
- **Stats from a curio:** Curios applies a curio's attribute modifiers through its own path, not through the
  `EquipmentSlotGroup`s our stat system fills. How a stack in a curio slot shows on the stat sheet (`StatSources`,
  `StatBreakdown`) and in the tooltip has to be settled: read `docs/technical/item-stats.md` first.
- The pack's list of mods and what each is for: `docs/technical/pack-extras.md`. The Sophisticated mods are not installed yet.

## Increments (ADR-0008: a refactor pass, no new features, after every third)

1. **The two extension points in Tier 1, no Curios yet.** A public valve API in atmosphere (open, close, "is this stack
   open") that `TankRules` itself uses; a public "extra worn stacks" registry in combat that the three off-hand checks and the
   stat system read through, so the off hand is just the first provider. Unit tests for the pure parts; the existing
   `damage-checks` and the combat client checks must still pass unchanged.
2. **The module.** `modules/compat/curios` (copy a small existing compat module's layout and its `neoforge.mods.toml`
   dependency style: Curios optional, `type="optional"`), registered in `settings.gradle.kts` and listed in
   `docs/technical/architecture.md`. The three slots as data, the tags, the entity assignment, the icons from a new
   `tools/generate-curios-slot-art.py` (seeded; re-running writes the same bytes), the two providers.
3. **Behaviour:** equipping a tank in `ascension_tank` opens its valve and closes any other (server side, on Curios' change
   event), unequipping closes it, the pressurise delay (ADR-0009 section 2) still applies, and a tank in the slot counts in the
   carried limit or does not: decide, say why, and write it in ADR-0009's follow-up. The relic slot: the ability key, the stats
   and the tooltip all work for a stack in the slot. Both survive death, respawn, a relog and a world reload.
4. **Proof.** A dedicated-server check file in the repo's style (a stand-in player with a Curios inventory: equip, open, close,
   activate Zonia's from the slot, the stat source named), then **one batched client list for Sanchit**: the three slots visible,
   equipping and unequipping each, the HUD's air bar with a tank in the slot, Zonia's and Sterak's from the slot, a backpack worn
   on the back (once the pack has one), a reload. Do not launch the game client for each tweak; he wants one combined run.

## How to work

- **Work in your own git worktree** and merge only when Sanchit says:
  `git worktree add -b curios-slots ../Project-Ascension-curios main`. Commit each step with explicit paths (never
  `git add -A`); do not push.
- Java is Temurin JDK 21 (system `java` is Java 8). A full `./gradlew build` must pass before you hand back.
- The CurseForge instance (`C:/Users/sanch/curseforge/minecraft/Instances/Ascension Dev/mods`) is shared with other sessions:
  **compare before overwriting any jar there**.
- If you disagree with a locked decision, supersede it with an ADR. Do not work around it.
- Do not touch: the quest pages, the Planet 4 files, build paths' recipes, Oxygen v2's machines.

## Hand back with

The ADR note (the carried-limit decision), the API additions listed, the dedicated-server results (numbers), the open items you
could not close, and the batched client checklist. Say plainly what was only checked headless.
