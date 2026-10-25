# ADR-0033: The quest book is nine projects, and a project's finale pays a skill point

- **Status:** Accepted, 2026-10-25 (Sanchit: "go with B and make the blueprint style pages"; points "maybe we can give but we don't want to increase the total number of points given, so be careful")
- **Date:** 2026-10-25
- **Builds on:** [ADR-0018](0018-no-world-is-required.md) (the book never gates a path), [ADR-0021](0021-player-growth.md) (growth: points from firsts, a finite total),
  [ADR-0023](0023-teams-are-ftb-teams-parties.md) (shared milestones go to the whole team), [ADR-0030](0030-rocket-parts-are-create-propulsion-and-aeroworks.md),
  [ADR-0032](0032-oil-and-the-first-ship-without-the-nether.md) (oil before the first ship)
- **Research and options:** `docs/progression/dr-stone-roadmap.md`

## Context

Sanchit wanted the quest book to feel like the roadmaps in *Dr. Stone*: a slow climb from a stone age to the Moon, one named project at a time, each with a chart of what it needs
and a finale that is a capability. The series' method was read from its fan wiki and written up in our own words; nothing of its names, characters or pictures is used. Of the three builds on offer
(a cover page, nine projects, one campaign) he chose **B, the nine projects, with blueprint-style pages**.

Before this the book taught parts: five pages (The Road Up, Create: Power, Create: Machines, Aeronautics: A Ship, Aeronautics: In Flight) listed what Create and Aeronautics do, in the order of the mods' menus,
not of a goal. Oil sat at the end of a flight page.

## Decision

1. **Nine projects, each a page and a finale** (the order is a map, not a lock):

   | # | Project | Finale | Pays |
   |---|---|---|---|
   | 1 | First Light | Earth's gate lit | 2 (the gate milestone) |
   | 2 | Spin | A working line: press, mixer and fan | 1 |
   | 3 | Brass | A precision mechanism | 1 |
   | 4 | Current | An accumulator | 0 |
   | 5 | Crude | A bucket of kerosene | 1 |
   | 6 | Wings | A gyroscope, a control stand and a thruster, solid or liquid | 1 |
   | 7 | Breath | An air compressor and a dispenser | 1 |
   | 8 | Skyline | Being in space | 1 |
   | 9 | The Moon | The Moon's gate lit | 2 (the gate milestone) |

   The pages that are not on the road stay as they were: Runes and Skill Points, Build the Gate, Planets, The Rot, The Belt, Gear and Growth and Together. **16 pages, 185 quests.**
2. **Every page is a blueprint sheet**: a generated picture in quest coordinates (`tools/generate-blueprint-art.py`, our own seeded art) with a frame, step numbers, a heading, a line drawing for the project, and a title
   block that says the finale and what it pays. The quests are laid out by `tools/quests/layout.py` as a dependency chart (needs on top, each as low as it can sit, the finale last, side notes under their parents).
   Dependency lines never lock anything.
3. **Moved quests keep their ids.** A quest's id is a hash of its page and slug, so moving it would lose everyone's progress; `Quest.legacy` names the page it was first written on, and the id comes from there.
   Five old pages dissolved into the projects this way. Three duplicates were dropped (Lift, Thrust, Know your height).
4. **A finale pays one skill point to the whole team, taken from somewhere else so the total stays 41.** Six projects pay: Spin, Brass, Crude, Wings, Breath and Skyline, as shared growth milestones
   (`data/*/ascension_combat/growth_milestone/project/*.json`). Current pays none, because its finale is the middle of the climb, not an arrival. Six discovery points stop paying: an Ancient City, Trial Chambers, a Woodland
   Mansion, an Ocean Monument, a Nether Fortress and a Bastion. Their advancements stay, so they still show in the Advancements screen, and can pay again if a design needs them. The total is 21 for bosses,
   8 for gates, 6 for the places that remain and 6 for projects: **41**, as the server logs (`Growth: 23 milestones worth 41 points, 56 tree nodes`).
5. **A finale is a plain advancement**: holding the finale item or items (an inventory advancement), or, for Skyline, being in the space dimension. Crude and Wings need Petrochem, Aeroworks and Propulsion and carry `mod_loaded`
   conditions, so a pack without them loses the milestone, not the world.
6. **Oil comes before the first ship** (Crude precedes Wings), by three Earth recipes (ADR-0032).
7. **The test flight is a lesson, not a mechanic.** There is no unmanned-rocket rule in the game and no way for the book to detect one; Skyline's "A first hop" (a checkmark) tells the player to take the ship up a few hundred blocks, watch
   the altitude sensor and the fuel, bring it back and mend what they saw before the long climb.
8. **Platinum keeps Propulsion's own meaning.** The mod put platinum in the world for its fluid tanks, casing and a few parts; the book adds one optional quest, "A bigger tank" (Crude: a platinum fluid tank holds twice
   what a copper one does), and nothing that makes platinum a refinery catalyst. If a mod has a use for an item, the book teaches that use (Sanchit: "if a mod has it for something we should keep that mod's essence").

## What was proved, and what was not

`tools/generate-quests.py --check` (run by every build): ids unchanged for every moved quest, every item, advancement and structure exists, no guarded story word, no dependency loop, every project's finale is reached by every
need, the sheet is the shape of its picture, and **the points a sheet advertises are what the growth milestone pays** (49 unit tests). The real FTB Quests on a dedicated server loaded 16 chapters (`quest-checks` 10/0,
609 ids, 248 items, 76 advancements). **Not seen in a client**: how the sheets sit under the quests and lines (the order of drawing is assumed from the Planets page), the dependency lines, the icons and the title blocks.

## Consequences

- Growth's source table is now bosses 21, gates 8, places 6, projects 6 (`docs/progression/growth.md`). Places are personal; a project is the team's.
- A new project is a Python module, a motif in the art script, a finale and (if it pays) two data files. A new world is still data.
- The Runes and Skill Points page's text about where points come from is updated to match.
- A project whose mod the pack lacks should lose its milestone with a warning in the log, not a crash (the advancement carries the condition, the milestone file does not); this was only tried with the mods present.
- The points do not depend on the book: a finale is an advancement, so a player who never opens the book still earns a project's point by holding its finale.
