# Known issues: Project Ascension 0.1.0-alpha.1

The alpha rule is **no dead ends**: if something that should work does not, there is a way around it. Each line says what is wrong
and what to do. Anything that says "not seen" has been built and checked by machine and never watched in a game.

## If you get stuck

| Problem | What to do |
|---|---|
| **A ship will not reach space** | Rising on foot is closed while the ship mod is installed. In the world's `serverconfig/ascension_worlds-server.toml` (or `config/ascension_worlds-server.toml` on a server) set `vehicleOnly = false`: you can then rise past the build height on foot (a tower, fireworks) and be carried to space. On the public server an operator can set it. |
| **A ship does not hold air** | Carry tanks. Your lungs and two tanks last 620 seconds; two helium tanks last 1,220. The Moon needs about 7 blocks a second to reach on that, the Nether 29 (15 with helium), the End 52 (26 with helium). Landing and refilling at each world cuts every leg. |
| **You cannot give air to another player** | Each player carries their own tanks. |
| **The gate or its key will not work** | Gates are shortcuts, not requirements: a ship goes to any world and back. An operator can light one with `/ascension worlds gateway light <planet>`. |
| **A quest cannot be finished** | The book never blocks anything. Skip it. |
| **A legendary item cannot be made** | Items reward, they do not unlock. Vanilla and netherite gear, growth points and runes still carry you. |
| **You fall out of the bottom of the End** | You drift down slowly and are carried into space beside the planet. Fly back down. |

## Things that are known to be wrong or missing

- **Shaders.** Iris is installed but **no shader pack is**. With one on, a shader draws its own clouds in space, our planets are drawn
  with toothed edges, and Create Aeronautics warns of its own problems with Iris. Shaders are not supported in space. Turn them off
  there.
- **Ships and air (Air Dispenser aboard a ship): not seen.** It works on the ground. Aboard a moving ship it rests on three
  assumptions about Create Aeronautics that nobody has tested.
- **A ship built from survival parts has never flown the whole road.** Every crossing so far was built by command.
- **Two players at once: not seen.** Parties, sharing air, friendly fire and a late joiner claiming past rewards are all built and
  unproven. A player who joins a party that is ahead gets the worlds but not the gear to survive them.
- **The End.** Its sky is still vanilla's. Flying fast over the outer islands makes the game generate new land and can stutter. The
  Elytra on an End city ship has not been seen in a real city.
- **Slots.** The oxygen tank works from your inventory (right-click it to open its valve) and Zonia's Hourglass and Sterak's
  Harness work in the **off hand**. Curios slots for them are planned, not built.
- **Storage.** Sophisticated Storage, Sophisticated Backpacks and Tom's Simple Storage are installed and have not been played with.
  Very many Sophisticated chests in one place can cost frames: use fewer, bigger ones.
- **Waystones.** They carry you within a world and are set to refuse any trip between worlds. The refusal has not been seen.
- **Balance.** The Cataclysm weapons are untouched and out of step with ours. Boss health was cut by a third on 2026-10-19 and has not
  been fought at every difficulty.
- **Other launchers.** Three mods (Create Aeronautics, Entity Culling and Keybind Atlas) do not allow other programs to download them.
  The CurseForge app handles them; another launcher may ask you to fetch them by hand.
- **Old worlds.** Start a new world. A world keeps the dimensions it was made with.

## How to report

See the page's "Reporting a bug". Attach `logs/latest.log` and the newest `crash-reports/` file.
