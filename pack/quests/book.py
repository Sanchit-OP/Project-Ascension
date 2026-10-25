"""The book: its pages, in the order the player sees them. Each page is one module in this folder.

Adding a page is a new module with a CHAPTER, one line here, and `python tools/generate-quests.py`.
The generator turns this into FTB Quests' files (pack/config/ftbquests/quests/) and the chart's layout
file; the build fails if those are out of date or if anything in here is wrong.
See docs/progression/quests.md.

The order is the order of the player's journey, drawn as nine projects the way a roadmap is drawn (docs/progression/dr-stone-roadmap.md): each
project page is a blueprint sheet laid out by tools/quests/layout.py, with a finale that is the thing the project is for. They are, in order: First
Light (First Days: the ground, the dark below, Earth's gate), Spin, Brass, Current, Crude, Wings, Breath, Skyline and The Moon. Between the first
and the second sit the guide to the two screens that the first points and runes lead to (Runes and Skill Points), the ring you build (Build the
Gate) and the signpost (Planets). After The Moon come the two worlds that are not on the road, The Rot and The Belt, and the two pages that are not
a place (Gear and Growth, Together). A page's order index is not part of any id, so it can move freely; only a slug can never be renamed, and a
quest moved to another page keeps the id of the page it was first written on (`legacy`).
"""

import brass
import breath
import crude
import current
import first_days
import gear_and_growth
import planets
import runes_and_skills
import skyline
import spin
import the_belt
import the_gate
import the_moon
import the_rot
import together
import wings
from quests.model import Book

BOOK = Book(chapters=[
    first_days.CHAPTER,
    runes_and_skills.CHAPTER,
    the_gate.CHAPTER,
    planets.CHAPTER,
    spin.CHAPTER,
    brass.CHAPTER,
    current.CHAPTER,
    crude.CHAPTER,
    wings.CHAPTER,
    breath.CHAPTER,
    skyline.CHAPTER,
    the_moon.CHAPTER,
    the_rot.CHAPTER,
    the_belt.CHAPTER,
    gear_and_growth.CHAPTER,
    together.CHAPTER,
])
