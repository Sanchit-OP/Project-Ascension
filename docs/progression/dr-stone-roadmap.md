# A book that works like Dr. Stone's roadmaps (research and proposal, 2026-10-25)

**Status: option B built 2026-10-25** (commits `7a6fba2` tooling, `756f64f` the book, `d9eefc5` growth; ADR-0033; the answers to the six questions are in section 5). Not seen in a client.
Sections 1 to 4 are the research and proposal as written before the build. Sanchit's idea, once the oil work was finished: make the quest book feel like *Dr. Stone*, the manga and
anime where one scientist rebuilds civilisation from a stone age, project by project, up to a rocket to the Moon. "Slowly gain and create items,
industrialisation, advancing to the Moon", and "their roadmap parts were fun and good." This page is what the series does (read from its fan
wiki, summarised here in our own words), what already matches our game, three ways to build it, and the questions that decide which.

**Inspiration, not a copy.** The series is someone else's work. We borrow its *method*, not its names, characters, pictures or plot. Our projects,
titles and pictures are our own (the art rule in `docs/technical/art-guide.md` already says so), and the lore stays ours (`docs/lore/`).

## 1. What the series does

### The projects, in the order the story makes them

The wiki's inventions list is grouped by story arc. Each arc is one big **project** with a named goal; the list below is the project, a few of the
things made or found on the way (as the wiki lists them), and what the project opened up.

| # | Project (the goal) | On the way | What it opened |
|---|---|---|---|
| 0 | **Wake people up** (revival fluid) | stone tools, rope, bow drill, fire, pottery, salt, nitric acid from bat droppings, wine, distilling it to ethanol, then lime, soap, bronze, sulfur, saltpetre, gunpowder | fire, tools, chemistry, the first allies |
| 1 | **Cure a sick girl** (the sulfa drug) | magnet and iron sand, a bellows-blown furnace (level 1, then 2), iron, copper, copper wire, a hand-cranked dynamo, glass, lab glassware, sulfuric and hydrochloric acid, a water condenser, dozens of intermediate chemicals | iron, glass, a lab, the first electricity |
| 2 | **Build a phone** | a water wheel and a hydroelectric plant, a lead-acid battery, a vacuum pump, a bulb with a tungsten filament, a vacuum tube, a microphone and speaker, a land line | power at scale, light, radio |
| 3 | **Win the Stone Wars** | a lathe, a steam furnace, casting moulds, a steam engine, an automobile, then explosives | engines, machine tools |
| 4 | **Cross the ocean** (a sailing ship) | hemp and a loom, a hot-air balloon, crude oil, an oil pump, a Stirling engine, a motor boat, **gasoline, asphalt and a distillation tower**, rails, radar, sonar, GPS | oil, fuel, the sea |
| 5 | **Treasure Island** | a motor, platinum and a nitric acid setup, an air pump and oxygen tank, propeller blades and ball bearings, drones | catalysts, air supply, flying machines |
| 6 | **North America** | a drill, carbide and acetylene, an aircraft carrier, aircraft | heavy industry |
| 7 | **South America** | rubber and tyres, a motorcycle, **cryogenic air separation (oxygen, argon)**, stainless steel, nickel by the Mond process, an induction furnace, a **Fischer-Tropsch reactor, a first rocket engine**, inconel | alloys, synthetic fuel, a rocket engine |
| 8 | **Round the world** | a better ship, a fluorite telescope, a test rocket, the parts of a computer, a rocket launch pad | a computer, a launch pad |
| 9 | **The Moon** | television, a finished computer, selenium and solar panels, a launch site, **unmanned rockets numbered 1, 2, 3 and 7**, a "level 99" engine, a satellite, the internet, space suits, a Moon rocket, a rover | the Moon |

### What makes it work (the patterns worth stealing)

1. **Roadmap diagrams.** Before each project the hero draws a chart: the goal at the top, everything it needs below, each need a thing to *make* or a
   thing to *find*, arrows between. The wiki keeps about twenty of them (antibiotics, cell phone, automobile, hot-air balloon, Perseus, "To the Moon", ...).
   The reader always knows the whole route and which node is next. This is the thing Sanchit liked, and it is what a quest book's dependency lines already are.
2. **A project ends in a capability, not an item.** Finish the drug and a village is saved; finish the dam and the base has light; finish the engine
   and a ship moves without wind. The reward is "you can now ...", and the next project needs it.
3. **Tools that make tools.** A lathe is built to build the parts of the next machine. Early projects are mostly about the means of production.
4. **Raw materials are expeditions.** A hot spring for sulfur, a cave for the droppings, a far island for platinum, a coast for rubber. The map matters.
5. **Test, fail, scale.** The Moon rocket is not built once: small numbered rockets come first, then a satellite, then the real one.
6. **Chemistry as a ladder.** Each substance is the step before another (salt, acid, ethanol, then the fluid). Nobody skips a rung, and nobody needs to be told why.
7. **A mystery as the engine.** The whole climb exists to find out what turned everyone to stone, and the answer is on the Moon.

That last one is already ours. The Warden, the mark, the Observers and the trap (`docs/lore/what-the-warden-keeps.md`) are a question, and the Lunar Archive is
on the Moon. We do not need their plot; we have the same shape.

## 2. What already matches our game

| Their project | Our nearest page and parts | Gap |
|---|---|---|
| 0 Wake people up | **First Days**, **Build the Gate**: the Warden, the keystone, a gate that lights | The page is a list of tasks; no single named goal with a chart under it |
| 1 Iron, glass, a lab | **The Road Up**, **Create: Power**, **Create: Machines**: andesite alloy, casing, the press, brass by hand (new) | Teaches parts, not a goal |
| 2 Power and light | **Create: Power**, and the **power aboard** lane (Crafts & Additions: alternator, accumulator) | Two places, no shared finale |
| 3 Engines | Create's stress and engines; Petrochem's engines | Not taught as a step |
| 4 Oil, fuel, a ship | The **oil lane** on *Aeronautics: In Flight* (pumpjack, electrolyzer, tower, kerosene, ADR-0031); **Aeronautics: A Ship** | Oil needs a Blaze Burner (the Nether), which the book places late |
| 5 Platinum, air, propellers | Propulsion's **platinum ore** (no job yet in our ledger), the **Air Compressor and tanks** (Oxygen v2), Aeronautics' propeller | Platinum has no purpose here yet |
| 7 Air separation, rocket engine | The compressor (air), helium, the Thruster and Propulsion's fuels | Rocket engine is a Thruster; no "test rocket" step |
| 8 Telescope, computer, pad | **Planets** (the chart), the ship's gyroscope and control stand | No launch step |
| 9 The Moon | **The Moon** page; *In Flight*'s "A rocket" | No numbered tests, no satellite |

The book already says what and where and never why, and never blocks a path (`docs/progression/quests.md`, ADR-0018). **A roadmap does not break that:** it
is a drawing of what leads to what, not a lock. Dependency lines can stay visible and unlocked.

## 3. Three ways to build it

**A. A cover page (small).** One new first page, *To the Moon*: the goal at the top, about ten project nodes below it with the dependency lines of the
whole game, each node opening the existing page it stands for. No other page changes. Cost: one page and a picture. Gain: the whole route on one screen.

**B. Projects (medium, recommended).** Re-cut the book into about nine **projects**, each a named goal with its own roadmap page in the series' manner:
the goal at the top, its needs below, split into *find* nodes (a place or a material) and *make* nodes (a part), and a last node, the finale, whose reward is
a capability and a fixed payout (a batch of growth points, `docs/progression/growth.md`). Existing quests become the nodes, so most of the 178 stay as they
are; a few are added (test flights, a satellite) and the text gets shorter. A project's finale also shows on the cover page of A. The pages teach
by building, as the series does: a lathe-like quest first ("make the thing that makes the thing").

**C. The whole book as one campaign (large).** Every project soft-locks the next, a narrator's voice, a story the player follows. It is the most faithful and the
most work, and it fights two rules we have set (ADR-0018 "no world is required", the book "never gates"). Not recommended for the alpha.

### A first cut of the projects for B (our names, our order; the order is a map, not a lock)

1. **First Light**: tools, titanium, a tank, the Warden, the keystone, the gate lit. *(their 0)*
2. **Iron and Spin**: andesite, casing, the press, brass by hand, a working Create base. *(their 1)*
3. **A Lit Base**: water or steam to stress, an alternator, an accumulator, light. *(their 2)*
4. **Wings**: the first ship that flies and steers: propeller, envelope, gyroscope, control stand. *(their 3 and 4)*
5. **Breath**: the compressor, tanks, a ship that holds air. *(their 5 and 7's air)*
6. **The Rot**: gold, the Nether, blazes, a Blaze Burner. *(an expedition for a rare material, their 5 to 7)*
7. **Crude**: steel, bronze, pumpjack, electrolyzer, tower, kerosene. *(their 4 and 7)*
8. **Test Flights**: small rockets first, a satellite or a probe, a launch pad. *(their 8 and 9's SENKU numbers)*
9. **The Moon**: the rocket, the landing, the Archive. *(their 9)*

(The Belt, the End, Gear and Growth, Runes and Skills and Together stay as they are: they are not on the road to the Moon.)

## 4. Questions for Sanchit

1. **A, B or C?** (Recommended: B, with A as its cover.)
2. **Roadmap pictures.** Should each project page look like a chart drawn on paper, with *find* nodes and *make* nodes in different shapes (the theme already has gear,
   diamond, hexagon and our own pictures)? It means a generated blueprint background per page, original art by our scripts.
3. **Does finishing a project pay growth points?** Fits the growth web (56 nodes fed by points) and gives each finale weight. How many?
4. **Test flights.** Is a "small rocket first" step wanted (an uncrewed ship that reaches space and comes back), as the series does? It is one or two new quests; it needs a check that a ship
   can be tested without a crew (`ascension-compat-sable`).
5. **Oil's place.** Oil needs a Blaze Burner (the Nether). In the cut above it is after The Rot. Keep that, or add a Nether-free steel recipe so it can come before?
6. **Platinum.** Propulsion put platinum in the world. Give it a job in the chain (the series puts platinum right before a nitric acid setup, as a catalyst, so a refinery catalyst would echo it), or leave it?

## 5. What he answered, and what was built (2026-10-25)

Sanchit's answers, in his words, and what each became:

| # | Question | His answer | Built |
|---|---|---|---|
| 1 | A, B or C | "go with B" | Nine projects (ADR-0033) |
| 2 | Roadmap pictures | "make the blueprint style pages" | A generated blueprint sheet behind every project page, in quest coordinates |
| 3 | Points | "maybe we can give, but we don't want to increase the total number of points given, so be careful" | Six finales pay 1 point; six discovery points stopped paying; the total is still 41 (`growth.md` section 3) |
| 4 | Test flights | "idk how it will fit here though" | A lesson, not a mechanic: Skyline's *A first hop* (the game has no unmanned-ship rule and the book cannot detect one) |
| 5 | Oil's place | "oil needs to come earlier as we need it in the first ship part" | Crude comes before Wings, and the Nether is off its path: a Blaze Burner and tin can be made on Earth (ADR-0032; the burner is questioned, see `oil-and-fuels.md` section 5) |
| 6 | Platinum | "if a mod has it for something we should keep that mod's essence" | Platinum keeps Propulsion's own job (a platinum tank holds twice as much); not made a catalyst |

He also asked how Petrochem's fuels differ; that is [`oil-and-fuels.md`](oil-and-fuels.md).

### How the first cut changed

| First cut (section 3) | Built |
|---|---|
| 1 First Light | **First Light** (the page was First Days, retitled) |
| 2 Iron and Spin | **Spin**, with **Brass** split off as its own project |
| 3 A Lit Base | **Current** |
| 4 Wings | **Wings** (after Crude) |
| 5 Breath | **Breath** |
| 6 The Rot | not a project: with a burner made on Earth the Nether is no longer on the road to the Moon; The Rot stays as a world page |
| 7 Crude | **Crude**, moved before Wings |
| 8 Test Flights | **Skyline**: the climb out of the atmosphere, with the first hop as a lesson |
| 9 The Moon | **The Moon**, which now begins with the three ways a ship gets there |

### The series' patterns, and where each landed

| Pattern | Where |
|---|---|
| Roadmap diagrams | Every project page is one; the finale is last and every need leads to it (the validator checks it) |
| A project ends in a capability | Each finale names what you can now do (a line, a precision mechanism, a ship that flies, a room that holds air) |
| Tools that make tools | Spin first: press, mixer and fan, which make the parts of every later project |
| Raw materials are expeditions | Platinum, helium, oil in a chosen biome (desert, ocean, swamp, jungle) |
| Test, fail, scale | *A first hop*; the Moon's three approaches |
| Chemistry as a ladder | The refinery: desalt, flash, atmospheric, vacuum, each cut the step before another |
| A mystery as the engine | Already ours (`docs/lore/`), untouched |

## Sources (read 2026-10-25)

Dr. Stone Wiki (Fandom), pages: *Inventions and Discoveries* and its per-arc lists (Prologue, Sulfa Drug, Cellphone, Automobile, Sailboat, Treasure Island,
North America, South America, Globetrotting, Moon Mission), *Roadmap Diagrams*, *Story Arcs*, *Moon Mission Arc*, *South America Arc*, *Globetrotting Arc*.
<https://dr-stone.fandom.com/wiki/Inventions_and_Discoveries>, <https://dr-stone.fandom.com/wiki/Roadmap_Diagrams>, <https://dr-stone.fandom.com/wiki/Story_Arcs>
