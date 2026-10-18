"""The book: its pages, in the order the player sees them. Each page is one module in this folder.

Adding a page is a new module with a CHAPTER, one line here, and `python tools/generate-quests.py`.
The generator turns this into FTB Quests' files (pack/config/ftbquests/quests/) and the chart's layout
file; the build fails if those are out of date or if anything in here is wrong.
See docs/progression/quests.md.
"""

import first_days
import planets
from quests.model import Book

BOOK = Book(chapters=[
    first_days.CHAPTER,
    planets.CHAPTER,
])
