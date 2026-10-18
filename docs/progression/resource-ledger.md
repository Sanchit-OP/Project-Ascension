# Resource ledger

> **Generated** by `tools/resource-ledger.py` from every module's data. Do not edit it by
> hand: change the data, or `tools/resource-ledger-rules.json` for what the data cannot say,
> then run the script. The build runs it with `--check`, which fails if this page is stale,
> if one of our items has no source, or if a material has no use
> (`resource-economy.md`, principle 6).

## Known gaps

Excused by the rules, with the reason. This list should only get shorter.


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
- Used: Tank Upgrade into Helium Oxygen Tank

**Helium Oxygen Tank** · `ascension_atmosphere:oxygen_tank_helium`
- From: Tank Upgrade: Oxygen Tank + anything in `#c:raw_materials/helium`

### Blocks

**Air Dispenser** · `ascension_atmosphere:air_dispenser`
- From: Crafting: Iron Ingot + Glass + Hopper

**Oxygen Emitter** · `ascension_atmosphere:oxygen_emitter`
- From: Crafting: Oxygen Tank + Redstone Block + Iron Ingot

**Oxygen Refill Station** · `ascension_atmosphere:oxygen_refill_station`
- From: Crafting: Oxygen Tank + Piston + Iron Ingot

## ascension-combat

### Materials

**Boots** · `ascension_combat:basic_boots`
- From: Crafting: Leather + anything in `#c:ingots/titanium`
- Used: Crafting into Berserker's Greaves, Mercury Treads, Ninja Tabi, Swifty, Tasted Blood, Wizard's Agony

**B. F. Sword** · `ascension_combat:bf_sword`
- From: Crafting: Long Sword + Netherite Ingot
- Used: Crafting into Darken Blade, Divine Sunderer, Endless Hunger

**Caulfield's Warhammer** · `ascension_combat:caulfields_warhammer`
- From: Crafting: Long Sword + anything in `#c:raw_materials/helium`
- Used: Crafting into Darius's Axe, Divine Sunderer

**Chain Vest** · `ascension_combat:chain_vest`
- From: Crafting: Cloth Armor + anything in `#c:raw_materials/helium`
- Used: Crafting into Gargoyle Stoneplate, Guardian's Wing, The Protean Mask

**Cloth Armor** · `ascension_combat:cloth_armor`
- From: Crafting: Leather
- Used: Crafting into Chain Vest, Collector's Pants, Gargoyle Stoneplate, Giant's Belt, Ninja Tabi, Seeker's Armguard

**Dagger** · `ascension_combat:dagger`
- From: Crafting: anything in `#c:ingots/titanium` + Stick
- Used: Crafting into Berserker's Greaves, Edge of Night, Executioner's Calling, Hearthbound Axe, Last Whisper, Noonquiver, Pickaxe, Ragefang, Recurve Bow, Seeker's Armguard, Serrated Dirk, Spear of Shojin, Swifty, Wraithblade

**Heart of the Ender Golem** · `ascension_combat:ender_golem_heart`
- From: Dropped by the Ender Golem: 1, killed by a player
- Used: Crafting into Sterak's Harness

**Heart of the Ender Guardian** · `ascension_combat:ender_guardian_heart`
- From: Dropped by the Ender Guardian: 1, killed by a player
- Used: Crafting into Guardian's Wing

**Executioner's Calling** · `ascension_combat:executioners_calling`
- From: Crafting: Long Sword + Dagger + Wither Skeleton Skull
- Used: Crafting into Collector's Pants, Mortal Reminder

**Giant's Belt** · `ascension_combat:giants_belt`
- From: Crafting: Ruby Crystal + Cloth Armor + anything in `#c:raw_materials/helium`
- Used: Crafting into Endless Hunger, Rocketbelt, Sterak's Harness, Unending Despair

**Hearthbound Axe** · `ascension_combat:hearthbound_axe`
- From: Crafting: Long Sword + Dagger + Blaze Rod
- Used: Crafting into Spear of Shojin

**Heart of Ignis** · `ascension_combat:ignis_heart`
- From: Dropped by the Ignis: 1, killed by a player
- Used: Crafting into Divine Sunderer

**Kindlegem** · `ascension_combat:kindlegem`
- From: Crafting: Ruby Crystal + Magma Cream
- Used: Crafting into Galeforce, Rocketbelt, Zonia's Hourglass

**Last Whisper** · `ascension_combat:last_whisper`
- From: Crafting: Long Sword + Dagger + anything in `#c:raw_materials/helium`
- Used: Crafting into Mortal Reminder, Spear of Shojin, Wraithblade

**Long Sword** · `ascension_combat:long_sword`
- From: Crafting: anything in `#c:ingots/titanium` + Stick
- Used: Crafting into B. F. Sword, Caulfield's Warhammer, Executioner's Calling, Hearthbound Axe, Last Whisper, Phage, Pickaxe, Vampiric Scepter, Veigo's Blade

**Negatron Cloak** · `ascension_combat:negatron_cloak`
- From: Crafting: Null-Magic Mantle + Glowstone Dust
- Used: Crafting into Edge of Night, Gargoyle Stoneplate

**Heart of the Netherite Monstrosity** · `ascension_combat:netherite_monstrosity_heart`
- From: Dropped by the Netherite Monstrosity: 1, killed by a player
- Used: Crafting into Zonia's Hourglass

**Noonquiver** · `ascension_combat:noonquiver`
- From: Crafting: Dagger + String + anything in `#c:raw_materials/helium`
- Used: Crafting into Galeforce, Heartwood of Pallas

**Null-Magic Mantle** · `ascension_combat:null_magic_mantle`
- From: Crafting: Amethyst Shard + Echo Shard
- Used: Crafting into Mercury Treads, Negatron Cloak, Spectre's Cowl

**Phage** · `ascension_combat:phage`
- From: Crafting: Long Sword + Ruby Crystal + Blaze Powder
- Used: Crafting into Darius's Axe, Ragefang

**Pickaxe** · `ascension_combat:pickaxe`
- From: Crafting: Long Sword + Dagger + anything in `#c:raw_materials/helium`
- Used: Crafting into Darken Blade, Ragefang, Veigo's Blade

**Recurve Bow** · `ascension_combat:recurve_bow`
- From: Crafting: Bow + Dagger + anything in `#c:raw_materials/helium`
- Used: Crafting into Galeforce, Heartwood of Pallas, Mortal Reminder

**Ruby Crystal** · `ascension_combat:ruby_crystal`
- From: Crafting: Diamond + Redstone
- Used: Crafting into Giant's Belt, Kindlegem, Phage, The Protean Mask, Rocketbelt, Spectre's Cowl

**Seeker's Armguard** · `ascension_combat:seekers_armguard`
- From: Crafting: Cloth Armor + Dagger + anything in `#c:raw_materials/helium`
- Used: Crafting into Collector's Pants, Edge of Night, Zonia's Hourglass

**Serrated Dirk** · `ascension_combat:serrated_dirk`
- From: Crafting: Dagger + Quartz
- Used: Crafting into Veigo's Blade, Wraithblade

**Spectre's Cowl** · `ascension_combat:spectres_cowl`
- From: Crafting: Null-Magic Mantle + Ruby Crystal + Nether Wart
- Used: Crafting into Guardian's Wing, Heartwood of Pallas, The Protean Mask, Unending Despair

**Vampiric Scepter** · `ascension_combat:vampiric_scepter`
- From: Crafting: Long Sword + Ghast Tear
- Used: Crafting into Darken Blade, Endless Hunger, Sterak's Harness

**Heart of the Warden** · `ascension_combat:warden_heart`
- From: Dropped by the Warden: 1, killed by a player
- Used: Crafting into Endless Hunger

**Heart of the Wither** · `ascension_combat:wither_heart`
- From: Dropped by the Wither: 1, killed by a player
- Used: Crafting into Unending Despair

### Items

**Berserker's Greaves** · `ascension_combat:berserkers_greaves`
- From: Crafting: Boots + Dagger

**Collector's Pants** · `ascension_combat:collectors_pants`
- From: Crafting: Executioner's Calling + Seeker's Armguard + Cloth Armor

**Darius's Axe** · `ascension_combat:darius_axe`
- From: Crafting: Phage + Caulfield's Warhammer

**Darken Blade** · `ascension_combat:darken_blade`
- From: Crafting: B. F. Sword + Vampiric Scepter + Pickaxe

**Divine Sunderer** · `ascension_combat:divine_sunderer`
- From: Crafting: B. F. Sword + Caulfield's Warhammer + Heart of Ignis

**Edge of Night** · `ascension_combat:edge_of_night`
- From: Crafting: Negatron Cloak + Seeker's Armguard + Dagger

**Endless Hunger** · `ascension_combat:endless_hunger`
- From: Crafting: B. F. Sword + Giant's Belt + Vampiric Scepter + Heart of the Warden

**Galeforce** · `ascension_combat:galeforce`
- From: Crafting: Noonquiver + Recurve Bow + Kindlegem

**Gargoyle Stoneplate** · `ascension_combat:gargoyle_stoneplate`
- From: Crafting: Chain Vest + Negatron Cloak + Cloth Armor

**Guardian's Wing** · `ascension_combat:guardian_wing`
- From: Crafting: Chain Vest + Spectre's Cowl + Heart of the Ender Guardian

**Heartwood of Pallas** · `ascension_combat:heartwood_of_pallas`
- From: Crafting: Recurve Bow + Noonquiver + Spectre's Cowl

**Mercury Treads** · `ascension_combat:mercury_treads`
- From: Crafting: Boots + Null-Magic Mantle

**Mortal Reminder** · `ascension_combat:mortal_reminder`
- From: Crafting: Last Whisper + Recurve Bow + Executioner's Calling

**Ninja Tabi** · `ascension_combat:ninja_tabi`
- From: Crafting: Boots + Cloth Armor

**The Protean Mask** · `ascension_combat:protean_mask`
- From: Crafting: Spectre's Cowl + Chain Vest + Ruby Crystal

**Ragefang** · `ascension_combat:ragefang`
- From: Crafting: Phage + Pickaxe + Dagger

**Rocketbelt** · `ascension_combat:rocketbelt`
- From: Crafting: Giant's Belt + Kindlegem + Ruby Crystal

**Rune Chalk** · `ascension_combat:rune_chalk`
- From: Crafting: Amethyst Shard + Calcite
- Used: Spent, with experience levels, by a rune page save that removes or replaces a rune (RuneChangeCost)

**Spear of Shojin** · `ascension_combat:spear_of_shojin`
- From: Crafting: Last Whisper + Hearthbound Axe + Dagger

**Sterak's Harness** · `ascension_combat:steraks_harness`
- From: Crafting: Giant's Belt + Vampiric Scepter + Heart of the Ender Golem

**Swifty** · `ascension_combat:swifty`
- From: Crafting: Boots + Dagger

**Tasted Blood** · `ascension_combat:tasted_blood`
- From: Crafting: Boots + Ghast Tear

**Test Magic Blade [DEBUG]** · `ascension_combat:test_magic_blade`
- From: *nothing yet*

**Tome of Extraction** · `ascension_combat:tome_of_extraction`
- From: Crafting: Book + Lapis Lazuli + Amethyst Shard
- Used: Used up in a crafting grid to move an item's enchantments onto a book (ExtractionRecipe)

**Tome of Scrapping** · `ascension_combat:tome_of_scrapping`
- From: Crafting: Book + Lapis Lazuli + Flint
- Used: Used up at an anvil to wipe an item's enchantments (TomeAnvil)

**Unending Despair** · `ascension_combat:unending_despair`
- From: Crafting: Giant's Belt + Spectre's Cowl + Heart of the Wither

**Veigo's Blade** · `ascension_combat:veigos_blade`
- From: Crafting: Pickaxe + Serrated Dirk + Long Sword

**Wizard's Agony** · `ascension_combat:wizards_agony`
- From: Crafting: Boots + anything in `#c:raw_materials/helium`

**Wraithblade** · `ascension_combat:wraithblade`
- From: Crafting: Serrated Dirk + Last Whisper + Dagger

**Zonia's Hourglass** · `ascension_combat:zonias_hourglass`
- From: Crafting: Seeker's Armguard + Kindlegem + Heart of the Netherite Monstrosity

## ascension-worlds

### Materials

**Raw Helium Crystal** · `ascension_worlds:raw_helium`
- From: Chests in the Lunar Archive (the Moon): 1 to 4, weight 3 of 11, 3 to 5 rolls
- From: Mined from Lunar Helium Ore: The Moon's rock, y −64 to 24, needs an iron pickaxe or better
- Used: Tank Upgrade into Helium Oxygen Tank
- Used: Crafting into Caulfield's Warhammer, Chain Vest, Giant's Belt, Last Whisper, Noonquiver, Pickaxe, Recurve Bow, Seeker's Armguard, Wizard's Agony

**Raw Titanium** · `ascension_worlds:raw_titanium`
- From: Chests in the Lunar Archive (the Moon): 2 to 5, weight 3 of 11, 3 to 5 rolls
- From: Mined from Titanium Ore: Earth's rock, y −64 to 0, needs a diamond pickaxe or better
- Used: Blast furnace into Titanium Ingot
- Used: Furnace into Titanium Ingot

**Titanium Ingot** · `ascension_worlds:titanium_ingot`
- From: Blast furnace: Raw Titanium; or Titanium Ore
- From: Furnace: Raw Titanium; or Titanium Ore
- Used: Crafting into Boots, Dagger, Long Sword, Propeller Bearing (with aeronautics installed)

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

## ascension-compat-create

### Blocks

**Air Compressor** · `ascension_compat_create:air_compressor`
- From: Crafting: Iron Sheet + Shaft + Fluid Pipe + Andesite Casing + Propeller
