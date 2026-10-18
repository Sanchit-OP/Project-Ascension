# Session prompt 4: the combat items' build paths

**Start this one later**, once prompts 1 and 3 have something to measure against: the stat data
format, and the first growth numbers. Build paths set the price of power, so they come after the
power.

Paste everything below the line into a new Claude Code session opened on this repository.

---

You are joining Project-Ascension: a Minecraft 1.21.1 NeoForge campaign built from our own mods,
by Sanchit (player name Devil0701). Your job is **the build paths for the 26 combat items**:
League-style recipes that make every item obtainable in survival, and hard to get.

## Read first

- `CLAUDE.md`, then `docs/decisions/`, including ADR-0018: no world is required, so every path
  is open, just costly.
- `docs/progression/resource-economy.md` §5: what Sanchit decided on 2026-10-12.
  - League-style build paths, with materials from different worlds.
  - One weapon must be very hard to make.
  - Strong items, mostly the ones that heal, need a boss's heart. Boots never do.
  - One heart per boss.
  - The items must feel strong.
  - Balance, and how enchantments interact with the items, come with it.
- `docs/progression/player-power.md`, and whatever the stats and growth sessions have since written
  (`docs/technical/item-stats.md`, `docs/progression/growth.md`). Your stat budgets are set
  against them.
- `docs/progression/resource-ledger.md` and `tools/resource-ledger-rules.json`. The build fails
  until every new component and heart has a source and a use. Remove each item from
  `no_source_yet` as its recipe lands.

## A draft to start from

Drafted 2026-10-12, never shown to Sanchit. Put it to him before building.

**Tiers:**
- **Six basic components,** each made from Earth materials:
  - Long Sword (Attack Damage; titanium);
  - Dagger (Attack Speed; titanium);
  - Cloth Armor (Armor; leather);
  - Null-Magic Mantle (Magic Resist; amethyst and an echo shard);
  - Ruby Crystal (HP; diamond and redstone);
  - Boots (Move Speed).
- **Seventeen epic components,** each adding a Moon or Nether material to basics:
  - B.F. Sword, netherite;
  - Pickaxe, Caulfield's Warhammer, Giant's Belt, Recurve Bow, Chain Vest, Seeker's Armguard, Last
    Whisper and Noonquiver, helium;
  - Phage, blaze powder;
  - Kindlegem, magma cream;
  - Vampiric Scepter, ghast tears;
  - Serrated Dirk, quartz;
  - Negatron Cloak, glowstone;
  - Spectre's Cowl, nether wart;
  - Hearthbound Axe, blaze rods;
  - Executioner's Calling, a wither skeleton skull.
- **The 20 non-boot legendaries:** two or three epics each, and every one needs all three worlds.
  - For example, Darius's Axe is Phage plus Caulfield's Warhammer: 9 titanium, a diamond, 4
    redstone, 4 blaze powder and 6 helium.
- **Boots:** Boots plus one basic or one material. Some can be made on Earth alone.

**Hearts:** one per boss, each finishing one item.
- The Warden: Endless Hunger.
- The Ender Guardian: Guardian's Wing.
- The Ender Golem: Sterak's Harness.
- The Netherite Monstrosity: Zonia's Hourglass.
- Ignis: Divine Sunderer.
- The Wither: Unending Despair.

**How to build it:**
- **Recipes:** shapeless, at a crafting table, so JEI shows every step. Ask for other modules'
  materials by tag (`#c:ingots/titanium`, `#c:raw_materials/helium`), never by id.
- **Components:** ingredients only, with no stats of their own (a proposal).
- **Heart drops:**
  - The Warden's and the Wither's: the combat module's own `add_table` loot modifiers.
  - The Cataclysm bosses' hearts: `ascension-compat-cataclysm`, which already depends on combat as
    an optional dependency. Load them under a `neoforge:mod_loaded` condition, and check that the
    condition works on loot modifiers.
- **JER:** Just Enough Resources is in the pack, but it won't show drops added by loot modifiers.

## What changed beside this since it was drafted (2026-10-18)

- **The quest book exists** (`docs/progression/quests.md`, `docs/progression/journey.md`). Its Gear and Growth page
  (`pack/quests/gear_and_growth.py`) can only teach what exists today. **When your recipes land, give it a forge chain**: one
  quest per component and legendary, each an `item(...)` task (a quest never takes the item from the player). Run
  `python tools/generate-quests.py`; the build refuses an item id that does not exist. Where the path forks, the book wants a
  **choice of one reward from three**, which needs FTB reward tables; the generator does not write them yet (the format is in
  `journey.md` 5.8), so adding that to `tools/quests/emit.py` is part of your hand-off.
- **The Wither already has a quest** (The Rot page, optional), and so does every boss named above except the Ender Guardian
  and Golem, which are on The Moon page. Your hearts are their natural rewards to point at, never a requirement.
- **Helium** has sources (ore, Archive chests) and, in the draft above, eight epic components as its first real use. Sanchit
  is also considering a helium tank or a fuel; keep the supply enough for both, and tell him how much helium the draft asks for.
- **Our five enchantments are already in the enchanting table, villagers and loot** (`docs/progression/enchantments.md`),
  and Sanchit is adding Lootr, so chests give every player their own loot. The per-item enchantment choice for the 26 items
  (enchantments.md section 8) belongs here.
- **He will do one client run at the very end** of the quest book and the build paths together. Test on a dedicated server as you
  go, and record anything only a client can show in your hand-off instead of asking for a launch.

## Working beside other sessions

- **Ask the stats session for the API** your items read stats through. Never read stats around it.
- **Work in your own git worktree and branch.** Merge only when Sanchit says.
- **Only one dev server can run at a time.**

## How Sanchit works

- **Commit each step.** Never push unless he asks.
- **ADR-0008:** a feature is done only when seen in his client; check world state on a dedicated
  server. A refactor pass every third increment.
- **Original art only:** components in League likeness, the hearts our own design.
- **Plain language.** Send docs to his phone as `.md`, and ask decisions with a recommendation
  first.
