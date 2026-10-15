# Session prompt 1: one stat system for every item

Paste everything below the line into a new Claude Code session opened on this repository.

---

You are joining Project-Ascension: a Minecraft 1.21.1 NeoForge campaign built from our own mods,
by Sanchit (player name Devil0701). Your job is **one unified stat system for every item that
should carry combat stats**: vanilla's, ours, and other mods'.

## Sanchit's words (2026-10-15)

> we need make the items available in the game also have the stats. we cant really only have our
> items have stats. we need to do this for all items which needs it either it comes from diff mod
> or not doesnt matter i need a unified system.

## Read first

- `CLAUDE.md`, then `docs/decisions/`. These ADRs bind you:
  - **0003:** Tier 1 never hard-depends on a third-party mod, and mixins never appear in Tier 1.
  - **0007:** performance. No static collections holding levels or entities; no per-tick scans.
  - **0008:** see it in a client, check world state on a dedicated server, and a refactor pass every
    third increment.
  - **0011:** Tier 1 modules share contracts through `core`.
  - **0012:** the combat module.
  - **0018:** no world is required.
- `docs/technical/combat-api.md`, especially §3: the stat model, the defence curve, and mob tiers.
- `docs/progression/player-power.md`: the audit this job comes from, and its proposals.
- `docs/progression/resource-economy.md`: what materials are for, and the build paths waiting on
  your work.

## What is true today

- **The stats:** HP, Attack Damage, Attack Speed and Move Speed are vanilla attributes. Armor,
  Magic Resist, Omnivamp, Lethality, and Armor and Magic Penetration % are our own, in
  `modules/combat/.../internal/CombatAttributes.java`.
- **The 26 combat items** (`CombatContent.java`) are `SwordItem`, `BowItem` and `ArmorItem`
  instances. They borrow `Tiers.DIAMOND` and `ArmorMaterials.DIAMOND` as placeholder stats, and
  each has an `*Effects` class for its mechanics.
- **Vanilla armour protects nothing while combat is loaded.** `PhysicalDamage` throws away
  vanilla's armour result and applies combat's own Armor through `DefenseFormula`, where every 100
  Armor halves damage.
  - Vanilla armour never feeds that stat, so netherite counts for nothing.
  - Only Ninja Tabi (15), Gargoyle Stoneplate (2) and the Protean Mask (up to 8) give combat Armor.
  - Bosses have 100.
- **Mobs get Armor and Magic Resist by tier** in `internal/mobs/MobStats.java`.

## The job

1. **Research first.** Find how NeoForge 1.21.1 lets one system give stats to any item, vanilla or
   modded, with no mixins. Start with:
   - **Data maps** (`DataMapType`, `RegisterDataMapTypesEvent`): per-item data from datapacks,
     tag-aware, overridable by modpacks.
   - **`ItemAttributeModifierEvent`:** adds attribute modifiers to any item stack.
   - How tooltips show attribute modifiers.

   Write down what works and what doesn't before you build.
2. **Design it, in `docs/technical/item-stats.md`.** Then write an ADR for the structural choice.
   The design should cover:
   - **One data format** assigning stats to an item, an item tag, or a whole class of items. For
     example, every sword in `minecraft:swords` or `c:tools/melee_weapon` gets a baseline, overridden per item.
   - **Vanilla items first:** swords, axes, bows, crossbows, tridents, the maces, and leather to
     netherite armour. Vanilla armour counting again is the urgent part.
   - **Our 26 items** move from the diamond placeholders into the same data.
   - **Other mods' items:** Cataclysm's weapons and armour, Create's tools, and anything later.
     That means data that only loads when the mod is present (`neoforge:mod_loaded` conditions), or
     files in the Tier 2 `ascension-compat-*` jar for that mod.
   - **Base versus bonus.** Many League items scale off *bonus* Attack Damage: "deal X% of your
     bonus AD". Separate base, bonus and total, behind an API in `com.ascension.combat.api`. The
     rune session and the growth session will read stats only through it, so make it the one
     place stats are read.
   - **Tooltips** that show our stats the way League does, readably.
   - **The character sheet** (`CharacterSheetScreen`) showing where each stat comes from, if it
     fits.
   - **Possibly mobs:** moving `MobStats` into the same data system, keyed by entity type.
3. **Ask Sanchit the decisions**, each as a plain question with your recommendation first. Then
   build in increments.

## Working beside other sessions

Two other sessions start at the same time: **runes** (prompt 2) and **player growth** (prompt 3).
Both build on your stat model.

- **You own the stat definitions and the stat-reading API.** If another session needs a new stat,
  it asks through Sanchit, or in a "Requested stats" list you keep in `combat-api.md` §3. Answer
  requests quickly: get the API's shape into the doc early, before the full build.
- **Work in your own git worktree and branch**, for example `combat-stats`. If the app did not
  make one, run `git worktree add`. Merge into `main` only when Sanchit says.
- **Keep edits to shared files small:** `AscensionCombat.java`, the lang file, `CombatContent.java`.
  Put new code in your own package.
- **Only one dev server can run at a time** (same ports). Check that nothing is on 25565 or 25575
  before `./gradlew runDevServer`.
- **The original combat session's work stays as it is:** the 3D weapon models, Better Combat
  animations and art scripts. Don't redo it.

## How Sanchit works

- **Commit each step.** Never push unless he asks.
- **ADR-0008:** a feature is done only when seen in his client; check world state and sync on a
  dedicated server (`runDevServer`, RCON helpers in `tools/worldgen-diagnostics/`). A refactor pass
  every third increment.
- **Every build runs `tools/resource-ledger.py --check`** (Python on PATH). It fails when one of our
  items has no source, or a material has no use.
- **Original art only** (`docs/technical/art-guide.md`). Combat items are drawn in League likeness.
- **Plain language.** Keep docs handoff-ready, and send docs to his phone as `.md` (SendUserFile).
  When he must choose, ask a clear question with your recommendation first.
