"""The quest book as plain data, and the little language the pages in pack/quests/ are written in.

A page is a `Chapter` holding `Quest`s and `Image`s. A quest has tasks (what the player does) and
rewards (what they get). Everything is named by a slug, never by a number: the slug is turned into
FTB Quests' 16-hex-digit id by `object_id`, so an id never changes unless someone renames a slug, and
a renamed slug is the one thing that can strand a player's progress. Do not rename a slug that has
shipped; add a new quest instead.

Pages are written like this (see pack/quests/first_days.py):

    quest('open_ledger', 0, 0, 'Open the Ledger', ['text', ...],
          tasks=[checkmark()], rewards=[give('minecraft:bread', 4)])
"""

import hashlib
from dataclasses import dataclass, field
from typing import List, Optional

# The three kinds of quest, each drawn in its own shape so a page reads at a glance (quests.md section 3).
LESSON = 'ascension_star'        # teaches one thing; one easy task
GOAL = 'ascension_nova'          # a real piece of play: build, craft, reach
LANDMARK = 'ascension_eye'       # a place or a boss; the network's motif, kept to those

# ----------------------------------------------------------------------------------------------
# Icons
# ----------------------------------------------------------------------------------------------


@dataclass(frozen=True)
class Icon:
    """What a chapter or quest is drawn with: an item, or one of our own pictures."""
    item: Optional[str] = None
    texture: Optional[str] = None


def item_icon(item_id):
    return Icon(item=item_id)


def picture(path):
    """One of our pictures, as `textures/quests/...png` inside the compat jar (the namespace is added)."""
    return Icon(texture=path)


# ----------------------------------------------------------------------------------------------
# Tasks and rewards
# ----------------------------------------------------------------------------------------------


@dataclass(frozen=True)
class Task:
    kind: str                      # FTB's task type: checkmark, item, advancement, dimension, structure, kill
    args: dict = field(default_factory=dict)


def checkmark():
    """Done when the player presses it: for a lesson there is nothing to detect."""
    return Task('checkmark')


def item(item_id, count=1):
    """Have this many of an item. Not taken from the player."""
    return Task('item', {'item': item_id, 'count': count})


def advancement(advancement_id):
    """Done when the player has this advancement (ours, or vanilla's)."""
    return Task('advancement', {'advancement': advancement_id})


def dimension(dimension_id):
    """Done when the player is in this dimension."""
    return Task('dimension', {'dimension': dimension_id})


def structure(structure_id):
    """Done when the player is standing in this structure."""
    return Task('structure', {'structure': structure_id})


@dataclass(frozen=True)
class Reward:
    kind: str                      # FTB's reward type: item, xp_levels, toast
    args: dict = field(default_factory=dict)


def give(item_id, count=1):
    return Reward('item', {'item': item_id, 'count': count})


def levels(count):
    return Reward('xp_levels', {'xp_levels': count})


# ----------------------------------------------------------------------------------------------
# Quests, images, chapters
# ----------------------------------------------------------------------------------------------


@dataclass
class Quest:
    slug: str
    x: float = 0.0                         # a page with a `blueprint` is laid out by tools/quests/layout.py; the numbers here are then overwritten
    y: float = 0.0
    title: str = ''
    text: List[str] = field(default_factory=list)
    subtitle: str = ''
    icon: Optional[Icon] = None
    shape: Optional[str] = None            # None: the chapter's default
    size: float = 1.0
    deps: List[str] = field(default_factory=list)    # 'slug' in this chapter or 'chapter.slug'
    tasks: List[Task] = field(default_factory=list)
    rewards: List[Reward] = field(default_factory=list)
    hide_until: Optional[str] = None       # None, 'visible' (deps seen) or 'complete' (deps done)
    requirement: Optional[str] = None      # how many deps count: None (all done), 'one_completed', 'all_started' or 'one_started'; FTB Quests calls a
                                           # quest started once any of its tasks has progress, even when the quest cannot complete yet
    optional: bool = False
    secret: bool = False                   # invisible until it is completed
    legacy: Optional[str] = None           # the chapter this quest was first written in: its ids (quest, tasks, rewards) keep that chapter's name,
                                           # so moving a quest to another page strands nobody's progress


@dataclass
class Image:
    slug: str
    texture: str                           # path inside the compat jar, e.g. 'textures/quests/planets/chart.png'
    x: float
    y: float
    width: float
    height: float
    order: int = 0
    shown_after: Optional[str] = None      # a quest ('slug' or 'chapter.slug'): drawn only once that is done
    alpha: int = 255
    click_quest: Optional[str] = None      # opens this quest when clicked


@dataclass
class Blueprint:
    """A project page drawn as a blueprint (docs/progression/dr-stone-roadmap.md): laid out by dependency, on a generated sheet.

    `number` and `of` are the sheet number in the title block, `motif` names the line drawing in its corner, `finale` is the quest the page
    exists for (drawn larger, at the bottom). `box` is filled by tools/quests/layout.py: (left, top, right, bottom) in position units."""
    number: int
    of: int
    motif: str
    finale: str
    strict: bool = True                    # every quest that is not optional leads to the finale (a hand-placed page, like First Light, need not)
    points: int = 0                        # skill points the finale pays the team; checked against the growth milestone file
    box: Optional[tuple] = None


@dataclass
class Chapter:
    slug: str
    title: str
    subtitle: str
    icon: Icon
    quests: List[Quest] = field(default_factory=list)
    images: List[Image] = field(default_factory=list)
    shape: str = 'ascension_star'          # the quest shape used where a quest names none
    blueprint: Optional[Blueprint] = None


@dataclass
class Book:
    chapters: List[Chapter]


# ----------------------------------------------------------------------------------------------
# Ids
# ----------------------------------------------------------------------------------------------

# Quests keeps a few small ids for itself; ours are all far above them.
_FLOOR = 1 << 40


def object_id(kind, slug):
    """A stable 16-hex-digit id for (kind, slug): the first eight bytes of its SHA-256, kept positive."""
    digest = hashlib.sha256(f'ascension:{kind}:{slug}'.encode('utf-8')).digest()
    value = int.from_bytes(digest[:8], 'big') & 0x7FFFFFFFFFFFFFFF
    if value < _FLOOR:
        value += _FLOOR
    return f'{value:016X}'
