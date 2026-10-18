# Resource ledger

> **Generated** by `tools/resource-ledger.py` from every module's data. Do not edit it by
> hand: change the data, or `tools/resource-ledger-rules.json` for what the data cannot say,
> then run the script. The build runs it with `--check`, which fails if this page is stale,
> if one of our items has no source, or if a material has no use
> (`resource-economy.md`, principle 6).

## Known gaps

Excused by the rules, with the reason. This list should only get shorter.

- **No source** (26): Berserker's Greaves, Collector's Pants, Darius's Axe, Darken Blade, Divine Sunderer, Edge of Night, Endless Hunger, Galeforce, Gargoyle Stoneplate, Guardian's Wing, Heartwood of Pallas, Mercury Treads, Mortal Reminder, Ninja Tabi, The Protean Mask, Ragefang, Rocketbelt, Spear of Shojin, Sterak's Harness, Swifty, Tasted Blood, Unending Despair, Veigo's Blade, Wizard's Agony, Wraithblade, Zonia's Hourglass. League-style build paths, to be built by the combat session (resource-economy.md §5).
- **No use** (1): Raw Helium Crystal. Helium's jobs: the Moon's material in the combat build paths, and the helium lamp (resource-economy.md §4, §5).

## Creative only, by design

No survival source, on purpose. The rules say why.

- **Test Magic Blade [DEBUG]** (`ascension_combat:test_magic_blade`): The combat session's test item for magic damage, not a player item.
- **Archive Orrery** (`ascension_worlds:archive_orrery`): Unbreakable: the Archive's orrery, its centrepiece.
- **Archive Plinth** (`ascension_worlds:archive_plinth`): Unbreakable: the Lunar Archive holds its tablets on these, and the story reads them in place.
- **Colony Plinth** (`ascension_worlds:colony_plinth`): Unbreakable: the colonists' halls' plinth, as the Archive's.
- **Carved Glyph** (`ascension_worlds:mark_glyph`): Unbreakable and drops nothing: a glyph of the script cut into the pylon, read in place.

## ascension-atmosphere

### Items

**Oxygen Tank** · `ascension_atmosphere:oxygen_tank`
- From: Crafting: Iron Ingot + Glass
- Used: Crafting into Oxygen Emitter, Oxygen Refill Station

### Blocks

**Oxygen Emitter** · `ascension_atmosphere:oxygen_emitter`
- From: Crafting: Oxygen Tank + Redstone Block + Iron Ingot

**Oxygen Refill Station** · `ascension_atmosphere:oxygen_refill_station`
- From: Crafting: Oxygen Tank + Piston + Iron Ingot

## ascension-combat

### Items

**Berserker's Greaves** · `ascension_combat:berserkers_greaves`
- From: *nothing yet*

**Collector's Pants** · `ascension_combat:collectors_pants`
- From: *nothing yet*

**Darius's Axe** · `ascension_combat:darius_axe`
- From: *nothing yet*

**Darken Blade** · `ascension_combat:darken_blade`
- From: *nothing yet*

**Divine Sunderer** · `ascension_combat:divine_sunderer`
- From: *nothing yet*

**Edge of Night** · `ascension_combat:edge_of_night`
- From: *nothing yet*

**Endless Hunger** · `ascension_combat:endless_hunger`
- From: *nothing yet*

**Galeforce** · `ascension_combat:galeforce`
- From: *nothing yet*

**Gargoyle Stoneplate** · `ascension_combat:gargoyle_stoneplate`
- From: *nothing yet*

**Guardian's Wing** · `ascension_combat:guardian_wing`
- From: *nothing yet*

**Heartwood of Pallas** · `ascension_combat:heartwood_of_pallas`
- From: *nothing yet*

**Mercury Treads** · `ascension_combat:mercury_treads`
- From: *nothing yet*

**Mortal Reminder** · `ascension_combat:mortal_reminder`
- From: *nothing yet*

**Ninja Tabi** · `ascension_combat:ninja_tabi`
- From: *nothing yet*

**The Protean Mask** · `ascension_combat:protean_mask`
- From: *nothing yet*

**Ragefang** · `ascension_combat:ragefang`
- From: *nothing yet*

**Rocketbelt** · `ascension_combat:rocketbelt`
- From: *nothing yet*

**Rune Chalk** · `ascension_combat:rune_chalk`
- From: Crafting: Amethyst Shard + Calcite
- Used: Spent, with experience levels, by a rune page save that removes or replaces a rune (RuneChangeCost)

**Spear of Shojin** · `ascension_combat:spear_of_shojin`
- From: *nothing yet*

**Sterak's Harness** · `ascension_combat:steraks_harness`
- From: *nothing yet*

**Swifty** · `ascension_combat:swifty`
- From: *nothing yet*

**Tasted Blood** · `ascension_combat:tasted_blood`
- From: *nothing yet*

**Test Magic Blade [DEBUG]** · `ascension_combat:test_magic_blade`
- From: *nothing yet*

**Tome of Extraction** · `ascension_combat:tome_of_extraction`
- From: Crafting: Book + Lapis Lazuli + Amethyst Shard
- Used: Used up in a crafting grid to move an item's enchantments onto a book (ExtractionRecipe)

**Tome of Scrapping** · `ascension_combat:tome_of_scrapping`
- From: Crafting: Book + Lapis Lazuli + Flint
- Used: Used up at an anvil to wipe an item's enchantments (TomeAnvil)

**Unending Despair** · `ascension_combat:unending_despair`
- From: *nothing yet*

**Veigo's Blade** · `ascension_combat:veigos_blade`
- From: *nothing yet*

**Wizard's Agony** · `ascension_combat:wizards_agony`
- From: *nothing yet*

**Wraithblade** · `ascension_combat:wraithblade`
- From: *nothing yet*

**Zonia's Hourglass** · `ascension_combat:zonias_hourglass`
- From: *nothing yet*

## ascension-worlds

### Materials

**Raw Helium Crystal** · `ascension_worlds:raw_helium`
- From: Chests in the Lunar Archive (the Moon): 1 to 4, weight 3 of 11, 3 to 5 rolls
- From: Mined from Lunar Helium Ore: The Moon's rock, y −64 to 24, needs an iron pickaxe or better
- Used: *nothing yet*

**Raw Titanium** · `ascension_worlds:raw_titanium`
- From: Chests in the Lunar Archive (the Moon): 2 to 5, weight 3 of 11, 3 to 5 rolls
- From: Mined from Titanium Ore: Earth's rock, y −64 to 0, needs a diamond pickaxe or better
- Used: Blast furnace into Titanium Ingot
- Used: Furnace into Titanium Ingot

**Titanium Ingot** · `ascension_worlds:titanium_ingot`
- From: Blast furnace: Raw Titanium; or Titanium Ore
- From: Furnace: Raw Titanium; or Titanium Ore
- Used: Crafting into Propeller Bearing (with aeronautics installed)

### Items

**Archive Tablet** · `ascension_worlds:archive_tablet`
- From: Dropped by the Netherite Monstrosity: 1, tablet: The Forge Record
- From: Chests in the Lunar Archive (the Moon): 1, tablet: The Road, weight 1 of 2
- From: Chests in the Lunar Archive (the Moon): 1, tablet: Many Worlds, weight 1 of 2
- From: Chests in the colonists' halls (the Nether): 1, tablet: The Colony, weight 1 of 2
- From: Chests in the colonists' halls (the Nether): 1, tablet: The Ward, weight 1 of 2

**Gateway Key** · `ascension_worlds:gateway_key`
- From: Dropped by the Netherite Monstrosity: 1
- From: Dropped by the Ender Guardian: 1
- Used: Attunes its own planet's gate (GatewayAttunement)

**Warden Tendril** · `ascension_worlds:warden_tendril`
- From: Dropped by the Warden: 1, killed by a player
- Used: Wakes Earth's player-built gate, one tendril per gate (GatewayPortalActivation)

### Blocks

**Anorthosite** · `ascension_worlds:anorthosite`
- From: Mined: The Moon's terrain
- From: Mined: the Moon's gate ruin, built into it
- Used: Stonecutter into Anorthosite Brick Slab, Anorthosite Brick Stairs, Anorthosite Bricks, Anorthosite Pillar, Archive Mural, Chiseled Anorthosite
- Used: Crafting into Anorthosite Bricks
- Used: Builds a gateway frame (Gateway)

**Anorthosite Brick Slab** · `ascension_worlds:anorthosite_brick_slab`
- From: Crafting: Anorthosite Bricks
- From: Stonecutter: Anorthosite Bricks; or Anorthosite
- From: Mined: the Lunar Archive (the Moon), built into it
- From: Mined: the Moon's gate ruin, built into it
- Used: Crafting into Chiseled Anorthosite
- Used: Builds a gateway frame (Gateway)

**Anorthosite Brick Stairs** · `ascension_worlds:anorthosite_brick_stairs`
- From: Crafting: Anorthosite Bricks
- From: Stonecutter: Anorthosite Bricks; or Anorthosite
- From: Mined: the Lunar Archive (the Moon), built into it

**Anorthosite Bricks** · `ascension_worlds:anorthosite_bricks`
- From: Crafting: Anorthosite
- From: Stonecutter: Anorthosite
- From: Mined: the Lunar Archive (the Moon), built into it
- From: Mined: the Moon's gate ruin, built into it
- Used: Crafting into Anorthosite Brick Slab, Anorthosite Brick Stairs, Anorthosite Pillar
- Used: Stonecutter into Anorthosite Brick Slab, Anorthosite Brick Stairs
- Used: Furnace into Cracked Anorthosite Bricks
- Used: Builds a gateway frame (Gateway)

**Anorthosite Pillar** · `ascension_worlds:anorthosite_pillar`
- From: Crafting: Anorthosite Bricks
- From: Stonecutter: Anorthosite
- From: Mined: the Lunar Archive (the Moon), built into it
- From: Mined: the Moon's gate ruin, built into it
- Used: Builds a gateway frame (Gateway)

**Archive Mural** · `ascension_worlds:archive_mural`
- From: Stonecutter: Anorthosite
- From: Mined: the Lunar Archive (the Moon), built into it

**Archive Orrery** · `ascension_worlds:archive_orrery`
- From: *nothing yet*

**Archive Plinth** · `ascension_worlds:archive_plinth`
- From: *nothing yet*

**Chiseled Anorthosite** · `ascension_worlds:chiseled_anorthosite`
- From: Crafting: Anorthosite Brick Slab
- From: Stonecutter: Anorthosite
- From: Mined: the Lunar Archive (the Moon), built into it
- From: Mined: the Moon's gate ruin, built into it
- Used: Builds a gateway frame (Gateway)

**Colony Mural** · `ascension_worlds:colony_mural`
- From: Mined: the colonists' halls (the Nether), built into it

**Colony Plinth** · `ascension_worlds:colony_plinth`
- From: *nothing yet*

**Cracked Anorthosite Bricks** · `ascension_worlds:cracked_anorthosite_bricks`
- From: Furnace: Anorthosite Bricks
- From: Mined: the Lunar Archive (the Moon), built into it
- From: Mined: the Moon's gate ruin, built into it

**Cracked Lunar Basalt Bricks** · `ascension_worlds:cracked_moon_basalt_bricks`
- From: Furnace: Lunar Basalt Bricks
- From: Mined: the Lunar Archive (the Moon), built into it
- From: Mined: the Moon's gate ruin, built into it

**Gateway Keystone** · `ascension_worlds:gateway_keystone`
- From: Crafting: Diamond + Echo Shard + Sculk Catalyst + Compass
- Used: Builds a gateway frame (Gateway)

**Inscribed Gatestone** · `ascension_worlds:inscribed_gatestone`
- From: Crafting: Polished Deepslate or Polished Lunar Basalt + Sculk or Echo Shard
- From: Chests in the Lunar Archive (the Moon): 2 to 6, weight 3 of 11, 3 to 5 rolls
- From: Mined: the Lunar Archive (the Moon), built into it
- Used: Builds a gateway frame (Gateway)

**Lunar Helium Ore** · `ascension_worlds:lunar_helium_ore`
- From: Mined: The Moon's rock, y −64 to 24, needs an iron pickaxe or better; with Silk Touch

**Carved Glyph** · `ascension_worlds:mark_glyph`
- From: *nothing yet*

**Lunar Basalt** · `ascension_worlds:moon_basalt`
- From: Mined: The Moon's terrain
- From: Mined: the Lunar Archive (the Moon), built into it
- From: Mined: the Moon's gate ruin, built into it
- Used: Stonecutter into Lunar Basalt Brick Slab, Lunar Basalt Brick Stairs, Lunar Basalt Bricks, Polished Lunar Basalt
- Used: Crafting into Polished Lunar Basalt
- Used: Builds a gateway frame (Gateway)

**Lunar Basalt Brick Slab** · `ascension_worlds:moon_basalt_brick_slab`
- From: Crafting: Lunar Basalt Bricks
- From: Stonecutter: Lunar Basalt Bricks; or Lunar Basalt
- From: Mined: the Lunar Archive (the Moon), built into it
- From: Mined: the Moon's gate ruin, built into it
- Used: Builds a gateway frame (Gateway)

**Lunar Basalt Brick Stairs** · `ascension_worlds:moon_basalt_brick_stairs`
- From: Crafting: Lunar Basalt Bricks
- From: Stonecutter: Lunar Basalt Bricks; or Lunar Basalt
- From: Mined: the Lunar Archive (the Moon), built into it

**Lunar Basalt Bricks** · `ascension_worlds:moon_basalt_bricks`
- From: Crafting: Polished Lunar Basalt
- From: Stonecutter: Lunar Basalt; or Polished Lunar Basalt
- From: Mined: the Lunar Archive (the Moon), built into it
- From: Mined: the Moon's gate ruin, built into it
- Used: Furnace into Cracked Lunar Basalt Bricks
- Used: Crafting into Lunar Basalt Brick Slab, Lunar Basalt Brick Stairs
- Used: Stonecutter into Lunar Basalt Brick Slab, Lunar Basalt Brick Stairs
- Used: Builds a gateway frame (Gateway)

**Lunar Regolith** · `ascension_worlds:moon_regolith`
- From: Mined: The Moon's terrain
- From: Mined: the Lunar Archive (the Moon), built into it
- From: Mined: the Moon's gate ruin, built into it

**Polished Lunar Basalt** · `ascension_worlds:polished_moon_basalt`
- From: Crafting: Lunar Basalt
- From: Stonecutter: Lunar Basalt
- From: Mined: the Lunar Archive (the Moon), built into it
- From: Mined: the Moon's gate ruin, built into it
- Used: Crafting into Inscribed Gatestone, Lunar Basalt Bricks
- Used: Stonecutter into Lunar Basalt Bricks
- Used: Builds a gateway frame (Gateway)

**Sealers' Carving** · `ascension_worlds:sealer_carving`
- From: Mined: the Sealer carving (the Nether), built into it

**Titanium Ore** · `ascension_worlds:titanium_ore`
- From: Mined: Earth's rock, y −64 to 0, needs a diamond pickaxe or better; with Silk Touch
- Used: Blast furnace into Titanium Ingot
- Used: Furnace into Titanium Ingot
