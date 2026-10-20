"""The book: its pages, in the order the player sees them. Each page is one module in this folder.

Adding a page is a new module with a CHAPTER, one line here, and `python tools/generate-quests.py`.
The generator turns this into FTB Quests' files (pack/config/ftbquests/quests/) and the chart's layout
file; the build fails if those are out of date or if anything in here is wrong.
See docs/progression/quests.md.

The order is the order of the player's journey: the ground (First Days) and the ring you build on it (Build the
Gate), the signpost (Planets), the way up (The Road Up) with the four pages that teach it (Create: Power, Create:
Machines, Aeronautics: A Ship, Aeronautics: In Flight), the three worlds that exist (The Moon, The Rot, The Belt), and then the
two pages that are not a place (Gear and Growth, Together). A page's order index is not part of any id, so it can move freely; only a slug
can never be renamed.
"""

import create_craft
import create_power
import first_days
import gear_and_growth
import planets
import road_up
import ship_build
import ship_fly
import the_belt
import the_gate
import the_moon
import the_rot
import together
from quests.model import Book

BOOK = Book(chapters=[
    first_days.CHAPTER,
    the_gate.CHAPTER,
    planets.CHAPTER,
    road_up.CHAPTER,
    create_power.CHAPTER,
    create_craft.CHAPTER,
    ship_build.CHAPTER,
    ship_fly.CHAPTER,
    the_moon.CHAPTER,
    the_rot.CHAPTER,
    the_belt.CHAPTER,
    gear_and_growth.CHAPTER,
    together.CHAPTER,
])
