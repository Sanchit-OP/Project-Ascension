# Known issues: Project Ascension 0.1.0-alpha.3

The alpha rule is **no dead ends**: if something that should work does not, there is a way around it. Each line says what is wrong
and what to do. Anything that says "not seen" has been built and checked by machine and never watched in a game.

## If you get stuck

| Problem | What to do |
|---|---|
| **A ship will not reach space** | Rising on foot is closed while the ship mod is installed. In the world's `serverconfig/ascension_worlds-server.toml` (or `config/ascension_worlds-server.toml` on a server) set `vehicleOnly = false`: you can then rise past the build height on foot (a tower, fireworks) and the same trip works. |
| **A ship is hard to steer** | The book's **Wings** page builds the controls in order: a helm, a gyroscope that keeps the ship level, and a control stand that takes a lever, a joystick, a wheel and a throttle. |
| **A ship does not hold air** | Carry tanks. Your lungs and two tanks last 620 seconds; two helium tanks last 1,220. The Moon needs about 7 blocks a second to reach on that, the Nether 29 (15 with helium), the End 52 (26 with helium). Landing and refilling at each world cuts every leg. |
| **You cannot find oil** | Oil is in the ground of **deserts, swamps, jungles and deep oceans** only, and a pumpjack must stand in one of those biomes. Swamp and jungle oil is already desalted, so it skips the electrolyzer. The other worlds have none. |
| **The oil chain wants a Blaze Burner, quartz or tin** | The Brass page of the book (*Three burners*) gives you three Blaze Burners: one for the basin, two for the tower's base. A burner mined with a pickaxe comes back whole, so move it rather than lose it; there is no recipe for more on Earth, and in the Nether an empty burner used on a blaze makes one. Tin: two zinc nuggets and an iron nugget. Quartz for the electron tube: crush diorite, or wash crushed raw gold or soul sand in a fan's water. |
| **You cannot find the Ancient City** | A cartographer villager (apprentice level or above) may sell a map to the nearest one for 14 emeralds and a compass; not every cartographer has it, so try another or level one up. The city is in the deep dark, near the bottom of the world. |
| **You cannot give air to another player** | Each player carries their own tanks. |
| **The gate or its key will not work** | Gates are shortcuts, not requirements: a ship goes to any world and back. An operator can light one with `/ascension worlds gateway light <planet>`. |
| **A quest cannot be finished** | The book never blocks anything. Skip it. |
| **A legendary item cannot be made** | Items reward, they do not unlock. Vanilla and netherite gear, growth points and runes still carry you. |
| **You fall out of the bottom of the End** | You drift down slowly and are carried into space beside the planet. Fly back down. |

## What alpha.3 changed, and has not been played

Alpha.3 is built and was checked on a dedicated server; a client has started with it once, for a minute or two, in an old world. Everything below was
**never played**, so expect to be the first.

- **The quest book's nine projects.** The blueprint pictures behind the pages, the lines and the quests drawn over them, the title blocks and the nine round icons have not been seen.
  If a page's quests float on a plain background, the picture did not load: tell us the page.
- **The new ship parts.** Create Aeroworks' gyroscope, joystick and control stand, and Create Propulsion's solid, liquid and ion thrusters, have never flown a ship.
  Nobody has flown a survival-built ship the whole way yet.
- **Oil.** The pumpjack, electrolyzer and distillation tower, the three given Blaze Burners heating a basin or the tower, and kerosene or gasoline running a thruster, have not been seen.
  How the five fuels compare is in the repository's `docs/progression/oil-and-fuels.md`; gasoline gives the most push from a barrel and kerosene the most per tank.
- **Skill points from projects.** Six projects each give your whole team a point when you hold their finale; whether the advancement fires on the right item is checked by machine only.
- **Paragliders, Traveler's Backpack and its sleeping bag, Inventory Profiles Next, Xaero's maps and Farmer's Delight** have not been played in this pack.
- **Aviator's Goggles in the head Curios slot, closed doors, slabs and glass panes holding air, breaking the air machines, and the Heavy Core's rate**: fixed from your alpha.2 reports, checked on a server, not seen.

## Things that are known to be wrong or missing

- **Shaders.** Complementary Shaders (Reimagined and Unbound) come with the pack and are **off**; pick one in Video Settings > Shader Packs. Their clouds are off. With one on,
  our planets may be drawn with toothed edges and the sky in space has not been looked at; ships under shaders have not been looked at either. If anything looks wrong, turn shaders off.
- **No clouds, on purpose.** The pack turns clouds off everywhere (the game's setting, held off, and Distant Horizons' own clouds). If you
  see one, tell us which mod drew it.
- **A small purple arrow on each hotbar slot** is Inventory Profiles Next's Auto Refill marker (a stack that runs out is topped up from your inventory). To hide it, open
  its config (Mods > Inventory Profiles Next > Config), then Auto Refill, and switch off "Also Show Icons in the Hotbar".
- **The map is on M and is Xaero's.** FTB Chunks stays for claiming and protecting chunks, but its map is off the M key and its minimap is off: to claim a chunk,
  bind **Claim Manager** under FTB Chunks in Controls. Xaero's shows players near you, not a whole party at any distance.
- **Ships and air (Air Dispenser aboard a ship): not seen.** It works on the ground and on a ship in a server check. Aboard a moving ship it rests on assumptions about Create Aeronautics that nobody has tested.
- **Two players at once: not seen.** Parties, sharing air, friendly fire and a late joiner claiming past rewards are all built and
  unproven. A player who joins a party that is ahead gets the worlds but not the gear to survive them.
- **The End.** Its sky is still vanilla's. Flying fast over the outer islands makes the game generate new land and can stutter. The
  Elytra on an End city ship has not been seen in a real city.
- **Slots.** The oxygen tank and Zonia's Hourglass or Sterak's Harness each have a **Curios slot** now (the tank still works from your
  inventory, and the two items still work in the off hand). The slots have been checked on a server and **not seen in a game**.
- **Storage.** Sophisticated Storage and Tom's Simple Storage are installed and have not been played with. Very many Sophisticated chests in one place can cost frames: use fewer, bigger ones.
- **Waystones.** They carry you within a world and are set to refuse any trip between worlds. The refusal has not been seen.
- **Balance.** The legendary weapons, the skill tree and the runes were all raised and **have not been fought against a real boss**: expect the numbers to move.
  Cataclysm's weapons and armour were rebalanced against netherite in alpha.3 and are not played either.
- **The HUD's level ring and hunger badge** used to vanish at random. A mod that batches the HUD's drawing (ImmediatelyFast) was removed as the
  likeliest cause; if it still happens, press F3+T and tell us whether it is a single-player world or a server.
- **Other launchers.** A few mods (Create Aeronautics, Entity Culling and Keybind Atlas, at least) do not allow other programs to download them.
  The CurseForge app handles them; another launcher may ask you to fetch them by hand.

## Updating from alpha.2

- **An alpha.2 world loads. Back it up first.** Three things in it change:
  - **Sophisticated Backpacks and Gliders are gone** (Traveler's Backpack and Paragliders replace them). Backpacks and gliders in the world disappear, and **what is inside a backpack is lost**:
    empty your backpacks into chests before you update.
  - **Create Simulated's thrusters are gone** (Create Propulsion's replace them): a ship built on them loses those blocks.
  - **The quest book was recut.** Quests you finished stay finished. If you had spent skill points that came from an Ancient City, Trial Chambers, a Woodland Mansion, an Ocean Monument,
    a Nether Fortress or a Bastion (those no longer give a point; the project finales do), your skill tree is given back to spend again and you are told.
- **Alpha.2 and alpha.3 cannot share a server** (the network protocol changed): update the server and every player together.
- **Keys changed.** Z, X, mouse 4 and mouse 5 are the four ability keys; an old install picks them up for any key it had not changed.

## How to report

See the page's "Reporting a bug". Attach `logs/latest.log` and the newest `crash-reports/` file.
