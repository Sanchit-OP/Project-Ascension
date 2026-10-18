"""The Planets page's geometry: one place that decides where every world is on the picture.

The picture is a night-sky chart with Earth, the origin of space, in the middle. A world's distance from
Earth is drawn on a square-root curve (so the near worlds are not crushed against the middle) and its
bearing is true. North is up and east is right, as on Minecraft's own maps.

Everything that needs a position uses this module: the art script draws the planets here, the generator
places each planet's quest here, and `chart_layout.json` (written by `layout()`) hands the same numbers to
the game, whose own projection (`ChartMath.java`) is checked against the sample points in that file.
"""

import json
import math
import os

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))

PLANET_DIR = os.path.join(ROOT, 'modules', 'worlds', 'src', 'main', 'resources', 'data',
                          'ascension_worlds', 'ascension_worlds', 'planet')

SIZE = 1024
CENTER = (512.0, 512.0)
RADIUS = 440.0            # pixels from the middle to the edge of the chart (the frame sits outside it)
RANGE_BLOCKS = 60000.0    # the distance that lands on the edge; room for all seven worlds (worlds-api.md)
UNITS_ACROSS = 24.0       # how wide the picture is in the quest book's own units
SPACE = 'ascension_worlds:space'

UNIT_PX = SIZE / UNITS_ACROSS

# Sample points for the game's own projection to be tested against. Round numbers, every quadrant, inside
# the range and beyond it.
GOLDEN_POINTS = [
    (0, 0), (1, 0), (0, 1), (-100, 50), (4000, 0), (10324, 14745), (-30000, 20000), (60000, 0),
    (0, -60000), (75000, 75000), (250, -9000), (-18000, -18000), (-4000, 0), (0, 15000),
]


class Planet:
    def __init__(self, planet_id, surface, x, z, color, order):
        self.id = planet_id
        self.surface = surface
        self.x = x
        self.z = z
        self.color = color
        self.order = order

    @property
    def name_key(self):
        return f'planet.ascension_worlds.{self.id}'

    @property
    def pixel(self):
        return project(self.x, self.z)

    @property
    def units(self):
        return to_units(*self.pixel)


def project(x, z):
    """Blocks from Earth (east, south) to pixels on the picture."""
    distance = math.hypot(x, z)
    if distance < 1e-9:
        return CENTER
    r = RADIUS * math.sqrt(min(distance, RANGE_BLOCKS) / RANGE_BLOCKS)
    return (CENTER[0] + r * x / distance, CENTER[1] + r * z / distance)


def to_units(px, py):
    """Pixels on the picture to the quest book's units, with the picture centred on (0, 0)."""
    return ((px - CENTER[0]) / UNIT_PX, (py - CENTER[1]) / UNIT_PX)


def load_planets():
    """Every planet file, in the order the game lists them."""
    planets = []
    for name in sorted(os.listdir(PLANET_DIR)):
        if not name.endswith('.json'):
            continue
        with open(os.path.join(PLANET_DIR, name), encoding='utf-8') as handle:
            data = json.load(handle)
        x, z = data['space']['position']
        planets.append(Planet(name[:-5], data['surface'], float(x), float(z),
                              int(data.get('color', 0x808080)), int(data.get('order', 0))))
    planets.sort(key=lambda p: (p.order, p.id))
    return planets


def layout():
    """The contents of chart_layout.json."""
    planets = load_planets()
    return {
        'size': SIZE,
        'center': list(CENTER),
        'radius': RADIUS,
        'range_blocks': RANGE_BLOCKS,
        'units_across': UNITS_ACROSS,
        'space': SPACE,
        'planets': [
            {
                'surface': p.surface,
                'position': [p.x, p.z],
                'pixel': [round(p.pixel[0], 6), round(p.pixel[1], 6)],
            }
            for p in planets
        ],
        'golden': [[x, z, round(project(x, z)[0], 6), round(project(x, z)[1], 6)] for x, z in GOLDEN_POINTS],
    }


def layout_text():
    return json.dumps(layout(), indent=2) + '\n'
