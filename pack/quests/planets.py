"""Planets: the star chart. Earth in the middle, every other world where it really is, and a marker for you.

The page is laid out from the planet files (tools/quests/chart.py), so a world's place on the picture,
its quest, its ring and its halo all come from one number. Each world has three states, all plain FTB
Quests visibility and no code:

    rumour   a faint ring and nothing else          always, until the world is known
    known    the named sphere, one click from a page   once the quest it depends on is done
    visited  a halo around the sphere               once the player has been there

The "you are here" marker is a texture our own code draws on the client (ChartOverlay); it is the last
image in the list and lies over the chart.
"""

import math

from quests import chart
from quests.model import (Chapter, Image, Quest, dimension, give, picture)

# The one thing that reveals each world. Earth is always known.
KNOWN_AFTER = {
    'moon': 'first_days.first_breath',    # once you can carry your own air
    'nether': 'moon',                     # once you have stood on the Moon
}

# What each world's page says, in practical words and no more. Distances come from the planet files.
BLURB = {
    'earth': {
        'subtitle': 'Home',
        'text': [
            'Where you start. Everything on this chart is measured from here.',
            'The marker follows you: on the ground, and between the worlds when you are in space.',
        ],
        'rewards': [],
    },
    'moon': {
        'text': [
            'A pale world with no air. Reach it by airship: up past the top of the sky, then {direction}.',
            'Bring a tank, and a way home.',
            'Something lies buried under its surface.',
        ],
        'rewards': [give('ascension_atmosphere:oxygen_tank', 1), give('minecraft:cooked_beef', 12)],
    },
    'nether': {
        'text': [
            'A world of fire with no roof over it, far out to the {direction}.',
            'Something in the air there harms you. Gold armour turns it away: wear it.',
            'Plan a long flight.',
        ],
        'rewards': [give('minecraft:golden_helmet', 1), give('minecraft:golden_boots', 1),
                    give('minecraft:golden_apple', 2)],
    },
}

# Matches planet.ascension_worlds.<id> in worlds' lang file.
TITLES = {'earth': 'Earth', 'moon': 'The Moon', 'nether': 'The Nether'}

_COMPASS = ['east', 'south-east', 'south', 'south-west', 'west', 'north-west', 'north', 'north-east']


def _direction(planet):
    """The compass word for a world's bearing, north up and east right (so +z is south)."""
    angle = math.degrees(math.atan2(planet.z, planet.x)) % 360.0
    return _COMPASS[int((angle + 22.5) // 45) % 8]


def _blocks(planet):
    """A world's distance from Earth as the player would say it: rounded, with a thousands comma."""
    distance = math.hypot(planet.x, planet.z)
    rounded = int(round(distance / 100.0) * 100)
    return f'{rounded:,}'


def _build():
    planets = chart.load_planets()
    quests, images = [], []

    images.append(Image('chart', 'textures/quests/planets/chart.png', 0, 0,
                        chart.UNITS_ACROSS, chart.UNITS_ACROSS, order=0))

    for planet in planets:
        ux, uy = planet.units
        ux, uy = round(ux, 3), round(uy, 3)
        key = planet.id

        # The rumour ring is always drawn; the sphere (the quest) covers it once the world is known.
        if key != 'earth':
            images.append(Image(f'ring_{key}', 'textures/quests/planets/ring.png', ux, uy, 2.4, 2.4, order=1))

        text = [line.format(direction=_direction(planet)) for line in BLURB[key]['text']]
        title = TITLES[key]
        subtitle = BLURB[key]['subtitle'] if key == 'earth' else f'{_blocks(planet)} blocks {_direction(planet)}'

        deps = []
        hide = None
        if key in KNOWN_AFTER:
            deps = [KNOWN_AFTER[key]]
            hide = 'complete'

        quests.append(Quest(
            slug=key, x=ux, y=uy,
            title=title, subtitle=subtitle, text=text,
            icon=picture(f'textures/quests/planets/{key}.png'),
            shape='none', size=1.7,
            deps=deps, hide_until=hide,
            tasks=[dimension(planet.surface)],
            rewards=BLURB[key]['rewards'],
        ))

        # Visited: a halo that only appears once the world's quest is done.
        if key != 'earth':
            images.append(Image(f'halo_{key}', 'textures/quests/planets/halo.png', ux, uy, 3.0, 3.0,
                                order=2, shown_after=key))

    # Last, and above the chart: the marker our own code draws.
    images.append(Image('you_are_here', 'textures/quests/planets/you_are_here.png', 0, 0,
                        chart.UNITS_ACROSS, chart.UNITS_ACROSS, order=9))

    return Chapter(
        slug='planets',
        title='Planets',
        subtitle='Where you are, and what you have found',
        icon=picture('textures/quests/icons/planets.png'),
        quests=quests,
        images=images,
        shape='none',
    )


CHAPTER = _build()
