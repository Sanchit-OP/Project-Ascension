# Changelog

## 0.1.0-alpha.3 (2026-10-25)

What changed after friends tested alpha.2: the ships were too hard to fly, so this alpha adds the parts that make a ship steerable and a rocket fuel to burn, recuts the quest book into
projects, and fixes what the test found. Minecraft 1.21.1, NeoForge 21.1.249. **An alpha.2 world still loads, but read "Updating from alpha.2" in the known issues first**: backpacks and gliders
from alpha.2 are gone from the pack, and what is inside a backpack is lost.

**Fixes from the alpha.2 test**
- **The server pack has no whitelist** any more (it blocked friends from joining). Names are still checked with Mojang.
- **Distant Horizons no longer asks for an update on the main menu.** The pack is updated as a whole.
- **Brass can be crafted by hand**: 3 copper ingots and 3 zinc ingots make 2 brass, no Nether needed. Create's heated mixer is still far cheaper.
- **Air machines can be broken and picked up.** The Air Dispenser, Air Compressor, Oxygen Emitter and Refill Station needed a tool but no tool
  could mine them, so they broke almost never and dropped nothing. A pickaxe now works and they drop themselves; Create's wrench lifts the
  Air Dispenser (sneak and use).
- **Aeronautics' Aviator's Goggles go in the Curios head slot** and count as goggles there (they are not drawn on your head while in the slot).
- **The Heavy Core (the mace's head) drops more often** from the Trial Chambers' ominous vaults: about 12% a vault instead of 7.5%.
- **A shut door or trapdoor seals a room** (wooden, iron, copper, Create's glass and sliding doors). Opening one lets the air out and shutting it
  holds the air again, at once. **Slabs and glass panes hold air too**, and so does the hot air balloon's adjustable burner; stairs, iron bars and fences do not.
  A datapack can add blocks to `ascension_atmosphere:airtight`.

**Ships and flight** (the alpha.2 test: nobody reached the Moon, "the controls were too hard")
- **Create: Aeroworks** is in: a **gyroscope** that keeps a ship level, a **joystick**, and a **control stand** that takes lever, joystick, wheel, yoke and
  throttle modules. **Create Propulsion** replaces Create Simulated Thrusters: a solid fuel thruster that burns coal, wood or raw helium, a liquid
  thruster, an ion thruster that runs on power. **Create: Petrochem** puts crude oil in the ground of Earth (desert, swamp, jungle, deep ocean) with a pumpjack, an electrolyzer and a distillation tower; its kerosene, diesel, gasoline, LPG and fuel oil are the liquid Thruster's fuels (gasoline gives the most push from a barrel, kerosene the most per tank). **Create Crafts & Additions** (with its Sable addon, so wires work on a ship) gives a ship stored electricity.
  The flight quest page teaches them in that order.
- **Brass can be crafted by hand**, which the new thruster needs.
- **Oil and the first ship no longer need the Nether.** Petrochem's refinery needs heat and tin, and each used to come from the Nether. The book's **Brass page gives you three Blaze Burners**
  (one for the basin, two under the tower's base; a burner mined with a pickaxe comes back whole, so move it rather than lose it) and an Earth recipe makes a **tin nugget** (two zinc nuggets and an iron nugget).
  No burner is made on Earth, and the Nether still makes more. (Quartz for the electron tube needs no recipe: Create makes it by crushing diorite or washing crushed raw gold or soul sand.)

**The quest book is nine projects.** Like a roadmap, each is a page with a blueprint behind it and a finale: First Light, Spin, Brass, Current, Crude, Wings, Breath, Skyline and The Moon. Crude (oil) comes before Wings (the first ship).
The old Create and Aeronautics teaching pages are folded into them (16 pages, 185 quests); anything you had finished stays finished. **Finishing a project pays a skill point to your whole team** (Spin, Brass, Crude, Wings, Breath
and Skyline), and six places stopped paying a point so the total is still 41: an Ancient City, Trial Chambers, a Woodland Mansion, an Ocean Monument, a Nether Fortress and a Bastion (they still tick in your advancements).

**Finding things**
- **A cartographer sells a map to the nearest Ancient City** (Earth's Overworld; 14 emeralds and a compass, six uses). A cartographer offers it from apprentice level up and never twice; the first time the game
  works out where the city is it may pause for a few seconds, as it does for vanilla's explorer maps. The Earth gate's key needs sculk from the city, and the city is under the deep dark, which is hard to find without this.
- **The Planets chart shows a world once you have landed there**, even if you skipped steps of the book before it. Before, a world stayed a bare ring until the step that pointed the way had been completed.

**Quality of life**
- **Paragliders** replaces Gliders: a glider you hold, not wear, with no durability. Its Spirit Orbs, Heart Containers, Stamina Vessels and statues are off.
- **Complementary Shaders (Reimagined and Unbound)** come with the pack and are **off** until you pick one in Video Settings > Shader Packs; their clouds are
  turned off in both, so there are none in space. Planets and ships under shaders have not been looked at yet: if one looks wrong, turn shaders off.
- **Traveler's Backpack** replaces Sophisticated Backpacks, and has a sleeping bag in it. **Inventory Profiles Next** sorts your inventory and chests.
  **Xaero's Minimap and World Map** are in. **Farmer's Delight** adds food.

**Combat**
- **Cataclysm's gear was rebalanced.** Ignitium, Cursium and the Monstrous Helm are smithed from a netherite piece, and several came out worse than the
  piece they used up: each is now at least that netherite piece in Armor, Magic Resist and Health (a full Ignitium set is 84 / 60 / 40, Cursium 76 / 53 / 42,
  netherite 69 / 49 / 40). Its boss weapons hit harder, about as hard as a rare trident, and still well under any legendary. Three more of their
  abilities (Ceraunus' waves, Tidal Claws' tentacle, the Ancient Spear's sandstorm) now deal magic damage.
- **Bows and crossbows add damage:** the bow +5 Attack Damage and the crossbow +4, so an arrow is the size of a sword hit. Hitting with a bow in your hand is slow
  now, which closes a way round the swords.
- **One key for each ability.** The single "activate armor ability" key fired every worn active at once. There are four, one for each place: **Z** the legs
  (Rocketbelt), **X** the chest (Gargoyle Stoneplate), **mouse button 4** the off hand and relic slot (Zonia's Hourglass), **mouse button 5** the item in
  your hand (Galeforce). Rebind them in Controls; an item's tooltip says which it uses. The old key is gone, and an old install picks up the four new ones
  with these defaults.

**Maps and keys**
- **M opens Xaero's World Map and nothing else.** FTB Chunks' own map key is unbound and its minimap is off (it stays for claims; bind **Claim Manager** in Controls to claim a chunk).
- A small purple arrow on each hotbar slot is Inventory Profiles Next's Auto Refill marker; switch it off in its config if you do not want it (known issues).

**Notes for the release**
- The network protocol changed (the ability keys), so an alpha.2 client cannot join an alpha.3 server, and the reverse. Update both together.
- Nothing in this alpha has been played in a client beyond a minute's start-up. Known issues says what is unseen.

## 0.1.0-alpha.2

The player was far too weak against the bosses, so this alpha is mostly about power, and about the gear and screens that make a character.
Minecraft 1.21.1, NeoForge 21.1.249. **A world made on alpha.1 still loads** (a saved rune page that held Silent Step or Cat's Eye is moved to the
keystone that replaced it), but the numbers below apply to it at once.

**Combat and power**
- **The 11 legendary weapons are stronger**, in base damage and in what their abilities scale with: a weapon kills the standard boss in about
  two thirds of the old time and the Wither in about half. The flat-damage weapons (Darius's Axe, Wraithblade, Mortal Reminder, Endless Hunger)
  rose furthest. Both dual-wield weapons (Ragefang, Endless Hunger) were checked as a pair.
- **The skill tree gives a lot more.** A small node is now +1 Attack Damage (it was +0.25), +3 Health (was +1), +5 Armor (was +4) and +3 Lethality (was +2), and the notables and capstones were raised too.
- **Enchantments are bigger, and the legendary items can be enchanted** (they could not). Sharpness and Power +1.5 Attack Damage a level, Smite and
  Bane +1 Attack Damage a level on top of their bonus, Sweeping Edge adds magic damage to every extra target, Collector's Pants add a real Looting level.
- **Three magic enchantments:** Arcane Edge (melee), Aether Tip (bows) and Spellpiercing, so magic damage has sources beyond a few items. Keen Edge goes with
  Sharpness; an item takes at most 6 enchantments (7 on a legendary); the enchanting table can add more; a new **Enchantment Library** block stores books.
- **The runes were audited and reworked.** Flat numbers became percentages of your stats. Ambush is a primed strike that grows with Lethality; Momentum
  heals from damage before Armor; Arc stays charged until the fight ends and throws four strikes a hit; Starfall is far bigger; Vein Breaker takes whole
  veins; **Predator** (Lethality is damage) replaced Silent Step and **Overheal** (healing becomes a shield) replaced Cat's Eye. Growing runes cap at 20.
- A fix: strikes that landed just after a sword hit (stars, lightning, shockwaves) were mostly swallowed by the game's invulnerability frames.

**Loot**
- **Every chest on the Moon, the Nether and the End now pays out.** Wheat, seeds, bones and rotten flesh are taken out of them, and each gets a
  bonus that grows with the planet: raw titanium and helium, diamonds, experience bottles, netherite, enchanted gear and books, our five
  enchantment books, and the build paths' components (the epics are rare finds). The Ender Golem's arena on the Moon is the example.
  It works for every chest from every mod, and with Lootr each player gets their own. Earth's chests only lose their seeds and rotten flesh; mob drops and boss drops are as they were.

**Slots and quality of life**
- **Curios slots:** one for an oxygen tank (a worn tank is an open source, no valve) and one for Zonia's Hourglass or Sterak's Harness, which no
  longer need the off hand. Cataclysm's six accessory slots were removed.
- **Also in the pack:** FallingTree (a tree comes down in one go), Gliders, Double Doors (with Collective) and GraveStone Mod.
- **No clouds, anywhere.** The Moon, space and the End were getting Distant Horizons' clouds; the pack now turns clouds off in every world,
  the game's own and Distant Horizons'.
- **The Moon's dust storms obey the `doWeatherCycle` game rule:** `/gamerule doWeatherCycle false` ends a storm and stops new ones.
- **ImmediatelyFast was removed:** it is the likeliest cause of the level ring and hunger badge vanishing from the HUD. If they still vanish, tell us.

**The quest book** has a new page, **Runes and Skill Points**, a guide to the two screens (K and R): where the 41 points come from and what everything does.
Fourteen pages in all. The Create fan quest now also hands you a block of soul sand, so you can light soul fire and try haunting.

**Updating:** a world made on alpha.1 needs no wipe. Back it up, then update the pack (or, on a server, delete the old `mods` folder contents and
upload the new ones; `SERVER-README.md` in the server zip has the steps).

## 0.1.0-alpha.1

The first alpha. Minecraft 1.21.1, NeoForge 21.1.249.

- Earth, the Moon, the Nether (Planet 3, opened to the sky) and the End (Planet 4, an asteroid belt with the Ender Dragon and the
  Elytra), reached by Create Aeronautics ship, lit by gates.
- The atmosphere: air as a fluid, the Air Compressor and Air Dispenser, tanks (helium tanks hold twice), sealed rooms.
- 26 legendary items with stats, build paths and boss hearts; 45 runes; a 56-node growth web; physical and magic damage; six
  rebalanced bosses and the Dragon.
- The quest book: thirteen pages, including the Planets chart with a "you are here" marker.
- Teams, a map that shows your team, per-player loot, storage and backpacks, and waystones inside a world.
- Not in this alpha: shader packs (see KNOWN_ISSUES.md), Curios slots, planets past the End.
