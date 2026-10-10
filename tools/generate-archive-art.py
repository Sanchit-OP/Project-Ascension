"""Authoring helper for the Lunar Archive's art. No build or runtime dependency.

Run from the repository root:  python tools/generate-archive-art.py
Needs Pillow. Rewrites every PNG, model and blockstate it owns under
modules/worlds/src/main/resources/assets/ascension_worlds/, and previews under
design/lunar-archive/, so hand edits to those files are lost on the next run -- edit this file.

Owns: the carved murals (archive_mural, one block, one tile per state), the look of the three
blocks and items the logic session gives behaviour to (archive_plinth, archive_orrery,
archive_tablet), and the models and blockstates of the Archive's two stair blocks.
tools/generate-lunar-archive.py reads MURALS from here to lay the murals into the templates.

Palettes and helpers come from the gateway and Moon materials scripts (art-guide: import, never
copy), so the Archive is visibly cut from the same stone and written in the same teal script.

Everything is original: shapes are drawn by code or placed by hand in pixel maps in this file.
Nothing is traced or sampled from vanilla, another mod or a pack. Design record:
docs/technical/lunar-archive-design.md.
"""
import importlib.util
import json
import math
import os
import random
import re

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))


def _load(name, filename):
    spec = importlib.util.spec_from_file_location(name, os.path.join(HERE, filename))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


art = _load('gateway_art', 'generate-gateway-art.py')
moon = _load('moon_art', 'generate-moon-materials-art.py')

BASALT, ANORTH, FLECK, TEAL, DULL = art.BASALT, art.ANORTH, art.FLECK, art.TEAL, art.DULL
REGOLITH = moon.REGOLITH
mix, shade, new = art.mix, art.shade, art.new
ROOT = art.ROOT
ASSETS = os.path.join(ROOT, 'modules/worlds/src/main/resources/assets/ascension_worlds')
TEX = os.path.join(ASSETS, 'textures')
DESIGN = os.path.join(ROOT, 'design/lunar-archive')
JAVA_MURAL = os.path.join(ROOT, 'modules/worlds/src/main/java/com/ascension/worlds/internal/archive/ArchiveMuralBlock.java')
NS = 'ascension_worlds'

# --- the mural catalogue ---------------------------------------------------------------------

# Every mural, in the order its tiles are numbered. Tile 0 of archive_mural is bare plaster; each
# mural's tiles follow on, row by row from its top-left. Appending is safe; reordering renumbers
# every tile after the change and breaks the templates until they are regenerated.
_ORDER = [
    ('stonecutters', (5, 3)),
    ('the_step', (5, 3)),
    ('the_road', (5, 3)),
    ('ring_ash', (5, 3)),
    ('ring_ice', (5, 3)),
    ('ring_forge', (5, 3)),
    ('keeper', (3, 5)),
    ('the_quiet', (5, 3)),
    ('sealers_record', (9, 6)),          # lore art, section 7: six panels of 3 x 3
    ('record_gouged', (3, 1)),           # the record's empty frame: three torn-out sockets
    ('keeper_shrine', (5, 3)),           # lore art, section 8
]
MURALS = {}
_next = 1
for _name, _size in _ORDER:
    MURALS[_name] = {'size': _size, 'first': _next}
    _next += _size[0] * _size[1]
MURAL_TILES = _next

# Inlay stones for the worlds the murals show. Muted, because they are coloured stone set into
# plaster, not paint; teal stays the network's alone.
ASH = [(36, 22, 22), (64, 34, 30), (96, 50, 40), (132, 74, 56)]
ICE = [(120, 146, 164), (156, 180, 196), (192, 210, 220), (226, 236, 240)]
FORGE = [(40, 34, 34), (66, 56, 54), (196, 92, 40), (246, 156, 72), (255, 214, 150)]
EARTH = [(40, 70, 130), (70, 110, 170), (120, 160, 200)]
GREY = [(110, 110, 112), (140, 140, 142), (170, 170, 172)]
SUN = [(214, 206, 170), (236, 230, 200), (250, 248, 232)]


# --- the mural canvas --------------------------------------------------------------------------

class Canvas:
    """A mural as carved plaster: every pixel has a relief height (-1 incised, 0 the plaster face,
    1 raised, 2 raised higher) and optionally an inlay colour, which may glow. render() lights it
    from the top-left: raised work gets a lit upper-left edge and casts a shadow down-right; an
    incision is shadowed on its near (upper-left) wall and lit on its far rim."""

    def __init__(self, w, h, seed):
        self.w, self.h = w, h
        self.rng = random.Random(seed)
        self.height = [[0] * w for _ in range(h)]
        self.inlay = [[None] * w for _ in range(h)]
        self.glow = [[False] * w for _ in range(h)]
        self.stain = [[0.0] * w for _ in range(h)]          # darkening of the bare plaster, 0..1
        self.group = [[None] * w for _ in range(h)]         # which inlay a pixel belongs to

    def ok(self, x, y):
        return 0 <= x < self.w and 0 <= y < self.h

    def px(self, x, y, height=None, inlay=None, glow=False, group=None):
        """Set one pixel. An inlay's edges are shaded where it meets anything not in its `group`
        (default: its own colour), so a shaded disc of several tones still reads as one stone."""
        x, y = int(round(x)), int(round(y))
        if not self.ok(x, y):
            return
        if height is not None:
            self.height[y][x] = height
        if inlay is not None:
            self.inlay[y][x] = inlay
            self.glow[y][x] = glow
            self.group[y][x] = group if group is not None else inlay

    def rect(self, x0, y0, x1, y1, **kw):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.px(x, y, **kw)

    def disc(self, cx, cy, r, **kw):
        for y in range(int(cy - r) - 1, int(cy + r) + 2):
            for x in range(int(cx - r) - 1, int(cx + r) + 2):
                if math.hypot(x + 0.5 - cx, y + 0.5 - cy) <= r:
                    self.px(x, y, **kw)

    def ring(self, cx, cy, r, width=1.0, a0=0, a1=360, **kw):
        for y in range(int(cy - r) - 2, int(cy + r) + 3):
            for x in range(int(cx - r) - 2, int(cx + r) + 3):
                d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
                if abs(d - r) <= width / 2:
                    a = math.degrees(math.atan2(-(y + 0.5 - cy), x + 0.5 - cx)) % 360
                    if (a0 <= a <= a1) if a0 <= a1 else (a >= a0 or a <= a1):
                        self.px(x, y, **kw)

    def line(self, x0, y0, x1, y1, every=1, **kw):
        n = max(abs(x1 - x0), abs(y1 - y0), 1)
        for i in range(0, int(n) + 1):
            if i % every == 0:
                self.px(x0 + (x1 - x0) * i / n, y0 + (y1 - y0) * i / n, **kw)

    def sprite(self, rows, x0, y0, key, flip=False):
        """Stamp a pixel map: key maps a symbol to px() keyword arguments."""
        for dy, row in enumerate(rows):
            if flip:
                row = row[::-1]
            for dx, ch in enumerate(row):
                if ch in key:
                    self.px(x0 + dx, y0 + dy, **key[ch])

    def render(self):
        w, h = self.w, self.h
        base = Image.new('RGBA', (w, h))
        glow = Image.new('RGBA', (w, h), (0, 0, 0, 0))
        bp, gp = base.load(), glow.load()
        # plaster: a low wash plus grain, so a wall of tiles reads as one dressed surface
        cells = 6
        lattice = [[self.rng.random() for _ in range(w // cells + 2)] for _ in range(h // cells + 2)]

        def wash(x, y):
            fx, fy = x / cells, y / cells
            x0, y0 = int(fx), int(fy)
            tx, ty = fx - x0, fy - y0
            a, b = lattice[y0][x0], lattice[y0][x0 + 1]
            c, d = lattice[y0 + 1][x0], lattice[y0 + 1][x0 + 1]
            return (a * (1 - tx) + b * tx) * (1 - ty) + (c * (1 - tx) + d * tx) * ty

        H = self.height

        def ht(x, y):
            return H[y][x] if self.ok(x, y) else 0

        for y in range(h):
            for x in range(w):
                v = 0.60 + (wash(x, y) - 0.5) * 0.10 + self.rng.uniform(-0.025, 0.025)
                c = moon.ramp(ANORTH, v)
                me = ht(x, y)
                up, left, down, right = ht(x, y - 1), ht(x - 1, y), ht(x, y + 1), ht(x + 1, y)
                if me > 0:
                    c = moon.ramp(ANORTH, 0.72 + 0.06 * (me - 1) + self.rng.uniform(-0.02, 0.02))
                    if up < me or left < me:
                        c = ANORTH[5]
                    elif down < me or right < me:
                        c = ANORTH[2]
                elif me < 0:
                    c = ANORTH[2]
                    if up > me or left > me:
                        c = ANORTH[1]
                    elif down > me or right > me:
                        c = ANORTH[4]
                else:
                    if ht(x - 1, y - 1) > 0 or up > 0:
                        c = mix(c, ANORTH[1], 0.55)                 # shadow cast down-right
                    if self.stain[y][x] > 0:
                        c = mix(c, mix(ANORTH[0], BASALT[3], 0.5), self.stain[y][x])
                ink = self.inlay[y][x]
                if ink is not None:
                    c = ink

                    grp = self.group[y][x]

                    def other(xx, yy):
                        return not self.ok(xx, yy) or self.group[yy][xx] != grp
                    if not self.glow[y][x]:
                        if other(x, y - 1) or other(x - 1, y):
                            c = mix(c, (255, 255, 255), 0.14)       # lit upper-left edge
                        elif other(x, y + 1) or other(x + 1, y):
                            c = mix(c, (0, 0, 0), 0.22)             # shadowed lower-right edge
                    else:
                        gp[x, y] = ink + (255,)
                bp[x, y] = c + (255,)
        # a carved frame groove one pixel in, and a dark arris at the very edge
        for x in range(w):
            for y in (0, h - 1):
                bp[x, y] = mix(bp[x, y][:3], ANORTH[1], 0.6) + (255,)
        for y in range(h):
            for x in (0, w - 1):
                bp[x, y] = mix(bp[x, y][:3], ANORTH[1], 0.6) + (255,)
        return base, glow


# --- the shared pictures --------------------------------------------------------------------------

# The gateway in miniature, as the builders carved it: crescent blades (B, the local stone), the
# ring (R, gatestone; r a rune in it), its field (F), the lintel (P) and the floating eye (E, e).
GATE = [
    "........E........",
    ".......E.E.......",
    "......E.e.E......",
    ".......E.E.......",
    "........E........",
    ".B.............B.",
    ".BB...........BB.",
    "..BB.........BB..",
    "..BBPPPPPPPPPBB..",
    "..BBRRrRRRrRRBB..",
    "..BBRFFFFFFFRBB..",
    "..BBrFFFFFFFRBB..",
    "..BBRFFFFFFFrBB..",
    "..BBRFFFFFFFRBB..",
    "..BBRFFFFFFFRBB..",
    "..BBrFFFFFFFRBB..",
    "..BBRFFFFFFFRBB..",
    "..BBRFFFFFFFrBB..",
    "..BBRFFFFFFFRBB..",
    "..BBRFFFFFFFRBB..",
    "..BBrFFFFFFFRBB..",
    "..BBRFFFFFFFRBB..",
    "..BBRFFFFFFFrBB..",
    "..BBRFFFFFFFRBB..",
    "..BBrFFFFFFFRBB..",
    "..BBRFFFFFFFRBB..",
    ".BBBRRRrRRRRRBBB.",
    "BBBBBBBBBBBBBBBBB",
]
RING_STONE = BASALT[2]


def gate(c, x0, ground_y, body, lit=True, field_dark=None):
    """Stamp the gate so its base sits on ground_y. `body` is the local stone (the blades); the
    ring is gatestone on every world."""
    y0 = ground_y - len(GATE)
    field = TEAL[3] if lit else (field_dark or BASALT[1])
    key = {
        'B': dict(height=1, inlay=body),
        'R': dict(height=1, inlay=RING_STONE),
        'r': dict(height=1, inlay=TEAL[3] if lit else DULL[2], glow=lit),
        'F': dict(height=0, inlay=field, glow=lit),
        'P': dict(height=2),
        'E': dict(height=1),
        'e': dict(height=1, inlay=TEAL[4] if lit else DULL[1], glow=lit),
    }
    c.sprite(GATE, x0, y0, key)
    if lit:                                       # the field is never flat: rising glints
        for y in range(y0 + 10, y0 + 26):
            for x in range(x0 + 5, x0 + 12):
                if (x * 3 + y * 2) % 11 == 0:
                    c.px(x, y, inlay=TEAL[4], glow=True)


# The builders, in profile, facing right. Tall, robed, no faces. W walking, C carrying a block on
# the shoulder, K kneeling at work with a chisel, S standing.
FIGURES = {
    'W': [
        "...###..",
        "..#####.",
        "..#####.",
        "...###..",
        "....#...",
        "..####..",
        ".######.",
        ".#######",
        ".####..#",
        ".####...",
        "..####..",
        "..####..",
        "..#####.",
        "..##.##.",
        ".##...##",
        ".#.....#",
    ],
    'C': [
        "........",
        "###.....",
        "###.###.",
        "###.###.",
        ".#..###.",
        ".#...#..",
        ".######.",
        ".######.",
        "..#####.",
        "..####..",
        "..####..",
        "..####..",
        "..#####.",
        "..##.##.",
        ".##...##",
        ".#.....#",
    ],
    'K': [
        "........",
        "........",
        "........",
        "........",
        "..###...",
        ".#####..",
        ".#####..",
        "..###...",
        "..###...",
        ".######.",
        ".#######",
        ".####..#",
        ".#####..",
        "..######",
        "..##.###",
        ".###....",
    ],
    'S': [
        "...###..",
        "..#####.",
        "..#####.",
        "...###..",
        "....#...",
        "..####..",
        ".######.",
        ".######.",
        ".######.",
        ".######.",
        "..####..",
        "..####..",
        "..####..",
        "..####..",
        "..#..#..",
        ".##..##.",
    ],
}
FIGURE_INK = BASALT[1]          # black-figure: the builders drawn in their own dark stone


def figure(c, kind, x0, ground_y, flip=False, height=1, inlay=FIGURE_INK):
    rows = FIGURES[kind]
    key = {'#': dict(height=height, inlay=inlay)}
    c.sprite(rows, x0, ground_y - len(rows), key, flip=flip)


def lying(kind):
    """A builder's figure laid on its back, head to the left: the standing sprite turned a quarter."""
    rows = FIGURES[kind]
    h, w = len(rows), len(rows[0])
    return [''.join(rows[h - 1 - y][x] for y in range(h))[::-1] for x in range(w)][::-1]


# Cats, the only creatures that can see the Observers; the builders kept them in every gate hall
# (docs/lore/the-moon.md). Black-figure like the builders, in profile. S sits facing right, tail
# curled round; A arches and spits, facing left, at something the builders cannot see.
CATS = {
    'S': [
        ".....#.#",
        ".....###",
        ".....###",
        "#...###.",
        "#..####.",
        ".#.####.",
        "..#####.",
        "...##.##",
    ],
    'A': [
        "#.#......#",
        "###....#..",
        "###..##...",
        ".######...",
        ".#######..",
        ".##...##..",
        ".#.....#..",
    ],
}


def cat(c, kind, x0, ground_y, flip=False):
    c.sprite(CATS[kind], x0, ground_y - len(CATS[kind]), {'#': dict(height=1, inlay=FIGURE_INK)}, flip=flip)


def ground(c, y0, colour=None, height=1):
    """The world the scene stands on: a raised band to the bottom edge, one line cut along it."""
    for y in range(y0, c.h):
        for x in range(c.w):
            c.px(x, y, height=height, inlay=colour[min(len(colour) - 1, 1 + (y - y0) // 3)] if colour else None)
    if not colour:
        for x in range(2, c.w - 2):
            c.px(x, y0 + 3, height=0)


def sun(c, cx, cy, r):
    """Concentric rings, never rays: a rayed disc turns into a cross or a star at this size."""
    c.disc(cx, cy, r, height=1, inlay=SUN[1])
    c.disc(cx, cy, r * 0.62, height=2, inlay=SUN[2])
    c.ring(cx, cy, r + 2, 1.0, height=-1)


def planet(c, cx, cy, r, pal, band=None, glow=False):
    """A world as an inlaid disc, lit from the upper-left: its far side in shadow."""
    for y in range(int(cy - r) - 1, int(cy + r) + 2):
        for x in range(int(cx - r) - 1, int(cx + r) + 2):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            d = math.hypot(dx, dy)
            if d > r:
                continue
            lit = -(dx + dy) / (r * 1.6)
            i = max(0, min(len(pal) - 1, int(round((len(pal) - 1) * (0.5 + lit)))))
            col = pal[i]
            if band and int(dy + r) % band == 0:
                col = mix(col, (0, 0, 0), 0.2)
            c.px(x, y, height=1, inlay=col, glow=glow, group=('planet', cx, cy))


# --- the murals ------------------------------------------------------------------------------------

def m_stonecutters(c):
    """We were stonecutters first: a quarry, the builders cutting and carrying, a ring rising."""
    ground(c, 41)
    sun(c, 8, 8, 4)
    # the quarry: stepped faces of cut stone at the left, seams incised
    steps = [(1, 19, 9), (1, 26, 14), (1, 33, 19)]
    for x0, y0, x1 in steps:
        c.rect(x0, y0, x1, 40, height=1)
    for x0, y0, x1 in steps:
        for y in range(y0 + 3, 41, 4):
            c.line(x0 + 1, y, x1, y, height=-1)
        for y in range(y0, 41, 4):
            for x in range(x0 + (2 if (y // 4) % 2 else 5), x1, 6):
                c.line(x, y, x, min(y + 2, 40), height=-1)
    figure(c, 'K', 21, 41, flip=True)                       # cutting at the face
    c.line(20, 35, 21, 34, height=1, inlay=FIGURE_INK)      # the chisel
    figure(c, 'C', 30, 41)                                  # carrying a stone away
    figure(c, 'W', 40, 41)
    # the ring, half raised: its left side and part of the lintel stand, the rest lies ready
    gx = 52
    part = [row[:10] for row in GATE[8:]]
    key = {'B': dict(height=1, inlay=BASALT[3]), 'R': dict(height=1, inlay=RING_STONE),
           'r': dict(height=1, inlay=TEAL[3], glow=True), 'P': dict(height=2), 'F': dict(height=0)}
    c.sprite(part, gx, 41 - len(part), key)
    for (x, y) in [(66, 37), (70, 37), (74, 37), (68, 33), (72, 33)]:
        c.rect(x, y, x + 3, y + 3, height=1, inlay=RING_STONE)
        c.px(x + 1, y + 1, inlay=TEAL[2], glow=True)        # each stone already cut with script
    c.line(63, 12, 63, 40, every=2, height=-1)              # the scaffold, scratched in
    c.line(56, 12, 78, 12, every=2, height=-1)
    c.line(63, 12, 70, 30, every=3, height=-1)              # a rope down to the next stone
    cat(c, 'S', 71, 33, flip=True)                           # on the cut stones, watching the work


def m_the_step(c):
    """One step, and a new sky: the same ring, one world on either side of it. The near side is the
    blue world's stone, on the left, where the blue world sits in every mural."""
    ground(c, 41)
    for y in range(41, c.h):                               # home is the blue world's stone ...
        for x in range(0, 39):
            c.px(x, y, height=1, inlay=EARTH[min(2, (y - 41) // 3)], group='earth')
    for y in range(41, c.h):                               # ... and the far world is ash: the ring
        for x in range(39, c.w):                           # stands on the seam between them
            c.px(x, y, height=1, inlay=ASH[min(3, 1 + (y - 41) // 3)], group='ash')
    sun(c, 9, 9, 4)
    planet(c, 68, 11, 7, ASH[1:], band=3)                   # a stranger sky over the far side
    gate(c, 31, 41, BASALT[3])
    cat(c, 'S', 1, 41)                                      # sitting where the walkers set out
    figure(c, 'W', 10, 41)
    figure(c, 'W', 20, 41)
    figure(c, 'S', 35, 39, height=2)                        # in the ring, between worlds
    figure(c, 'W', 54, 41)
    for (x, y) in [(60, 30), (73, 26), (64, 34), (76, 32), (52, 22), (70, 36)]:
        c.px(x, y, height=1, inlay=ASH[3])                  # ash on the wind


def m_the_road(c):
    """Count the lights on the orrery: every world they walked, joined by the road out from the
    Moon. The road to the blue world was laid like the rest and then prised out: a gouged channel,
    one sliver of its teal still caught in the cut. The Sealers cut Earth off on purpose."""
    cx, cy = -10, 24
    sun(c, cx, cy, 15)
    orbits = [22, 33, 44, 55, 66, 77]
    for r in orbits:
        c.ring(cx, cy, r, 1.0, a0=290, a1=70, height=-1)
    # (orbit, angle, radius, palette, glows, band)
    worlds = [
        (0, -14, 4.0, EARTH, False, None),    # the blue world
        (1, 20, 3.5, ASH, False, None),       # ash plains
        (2, -12, 4.0, ICE, False, None),      # seas under ice
        (3, 16, 5.0, FORGE[1:4], True, None), # the forge
        (4, -10, 3.5, GREY, False, 2),
        (5, 12, 3.0, GREY, False, None),      # the farthest
    ]
    pts = []
    for orbit, ang, rad, pal, gl, band in worlds:
        r = orbits[orbit]
        x, y = cx + r * math.cos(math.radians(ang)), cy - r * math.sin(math.radians(ang))
        pts.append((x, y, rad))
        planet(c, x, y, rad, pal, band=band, glow=gl)
    ex, ey, er = pts[0]
    mx, my = ex + 12, ey - 9                                 # the Moon, out from the blue world
    planet(c, mx, my, 2.0, GREY)
    road = [(mx, my, 2.0)] + pts[1:]
    for (x0, y0, r0), (x1, y1, r1) in zip(road, road[1:]):
        d = math.hypot(x1 - x0, y1 - y0)
        ux, uy = (x1 - x0) / d, (y1 - y0) / d
        c.line(x0 + ux * (r0 + 1), y0 + uy * (r0 + 1), x1 - ux * (r1 + 1), y1 - uy * (r1 + 1),
               inlay=TEAL[2], glow=True)
    # to the blue world: the channel cut like every other road, its inlay hacked out of it. What
    # shows is the rough dark bed the teal was set in, bitten at the edges by the chisel, and one
    # sliver the Sealers missed.
    x0, y0, x1, y1 = mx - 2, my + 2, ex + 3, ey - 3
    rng = random.Random(461)
    n = int(max(abs(x1 - x0), abs(y1 - y0)))
    bed = [mix(BASALT[2], ANORTH[1], 0.35), mix(BASALT[3], ANORTH[1], 0.5), BASALT[2]]
    for i in range(n + 1):
        t = i / n
        gx, gy = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t
        for ox in (0, 1):
            c.px(gx + ox, gy, height=-1, inlay=rng.choice(bed), group='gouge')
        if 0 < i < n and rng.random() < 0.6:
            c.px(gx + rng.choice((-1, 2)), gy + rng.choice((-1, 0, 1)), height=-1)
    t = 0.35
    c.px(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, inlay=TEAL[3], glow=True)
    for x, y, rad in road:
        c.px(x, y - rad - 2, height=1, inlay=TEAL[4], glow=True)  # each world's gate, lit


def world_panel(c, body, ground_pal, sky):
    ground(c, 41, ground_pal)
    sky(c)
    gate(c, 31, 41, body)


def m_ring_ash(c):
    """The ring on the ash plains: dark local stone, spires of black rock, ash on the wind."""
    def sky(c):
        c.disc(66, 9, 5, height=1, inlay=ASH[2])           # a dim red sun
        c.disc(66, 9, 3, height=1, inlay=ASH[3])
        for x0, h in [(4, 15), (10, 9), (16, 20), (23, 7), (55, 10), (61, 17), (69, 23), (75, 12)]:
            for y in range(41 - h, 41):
                half = max(0, (y - (41 - h)) // 6)
                c.rect(x0 - half, y, x0 + 1 + half, y, height=1, inlay=ASH[0], group='spire')
        for (x, y) in [(22, 12), (30, 6), (51, 17), (56, 22), (26, 25), (49, 28), (77, 31), (38, 4)]:
            c.px(x, y, height=1)
    world_panel(c, ASH[1], ASH, sky)


def m_ring_ice(c):
    """The ring on a world of seas under ice: pale local stone, a cracked frozen sea, a ringed sky."""
    def sky(c):
        planet(c, 13, 12, 6, ICE)
        c.ring(13, 12, 9.5, 1.0, a0=160, a1=20, height=1, inlay=ICE[3])
        for x0, w, h in [(55, 6, 10), (64, 10, 16), (74, 6, 8)]:
            for y in range(41 - h, 41):
                k = (y - (41 - h)) * w // max(h, 1)
                c.rect(x0 - k // 2, y, x0 + k // 2, y, height=1, inlay=ICE[1], group='berg')
            c.line(x0, 41 - h, x0 + w // 2, 40, inlay=ICE[3], group='berg_lit')
        for (x0, y0, x1, y1) in [(3, 44, 15, 46), (19, 43, 27, 46), (50, 44, 61, 42), (66, 45, 77, 43)]:
            c.line(x0, y0, x1, y1, inlay=ICE[0])
    world_panel(c, ICE[2], ICE, sky)


def m_ring_forge(c):
    """The ring on the forge world: iron-dark mountains seamed with fire, smoke rising off them."""
    def sky(c):
        peaks = [(0, 27), (7, 15), (14, 24), (21, 18), (29, 31), (50, 31), (57, 17), (64, 23), (71, 13), (79, 25)]
        for (xa, ya), (xb, yb) in zip(peaks, peaks[1:]):
            for x in range(xa, xb + 1):
                yy = int(ya + (yb - ya) * (x - xa) / max(1, xb - xa))
                for y in range(yy, 41):
                    c.px(x, y, height=1, inlay=FORGE[1], group='range')
        for x0, y0, length in [(7, 17, 10), (57, 20, 9), (71, 15, 12), (21, 21, 8)]:
            for i in range(length):
                c.px(x0 + (1 if (i // 3) % 2 else 0), y0 + i, inlay=FORGE[3], glow=True)
        for x0, y0 in [(7, 13), (71, 11)]:                  # smoke, rising in loose strands
            for i in range(9):
                c.px(x0 + int(2 * math.sin(i * 0.9)), y0 - i, height=-1)
                c.px(x0 + 3 + int(2 * math.sin(i * 0.9 + 1.5)), y0 - i - 1, height=-1)
    world_panel(c, FORGE[0], FORGE[:3], sky)


def m_keeper(c):
    """A keeper: stone that walks and remembers one order, standing in the ring it guards, its staff
    held level across the opening like a bar across a door. It keeps the ring shut."""
    for y in range(4, 77):
        for x in range(4, 44):
            edge = x in (4, 5, 42, 43) or y in (4, 5, 75, 76)
            if edge:
                c.px(x, y, height=1, inlay=RING_STONE, group='ring')
                if (x * 3 + y) % 9 == 0:
                    c.px(x, y, inlay=TEAL[3], glow=True)
            else:
                c.px(x, y, height=0, inlay=BASALT[1], group='field')
    body, limb, dark = BASALT[4], BASALT[3], BASALT[2]

    def part(x0, y0, x1, y1, col, grp='keeper'):
        c.rect(x0, y0, x1, y1, height=1, inlay=col, group=grp)
    part(20, 8, 27, 8, body)                                         # brow ridge
    part(19, 9, 28, 19, body)                                        # head, a heavy block
    for dx, dy in [(0, -2), (1, -2), (-1, -1), (2, -1), (-2, 0), (3, 0), (-1, 1), (2, 1), (0, 2), (1, 2)]:
        c.px(23 + dx, 14 + dy, inlay=TEAL[3], glow=True)            # the eye, a diamond
    for (x, y) in [(23, 13), (24, 13), (23, 14), (24, 14), (23, 15), (24, 15)]:
        c.px(x, y, inlay=TEAL[5], glow=True)
    part(21, 20, 26, 21, dark)                                       # neck
    part(14, 22, 33, 23, limb)                                       # shoulders, sloping out
    part(11, 24, 36, 29, limb)
    for y in range(30, 47):                                          # chest tapering to the waist
        half = 9 - (y - 30) // 4
        part(int(23.5 - half), y, int(23.5 + half), y, body)
    part(10, 30, 13, 51, limb, 'larm')                               # arms hang apart from it
    part(34, 30, 37, 51, limb, 'rarm')
    part(9, 52, 14, 56, dark, 'lhand')
    part(33, 52, 38, 56, dark, 'rhand')
    part(17, 47, 22, 70, body, 'lleg')                               # legs, two pillars
    part(25, 47, 30, 70, body, 'rleg')
    part(15, 71, 23, 73, dark, 'lfoot')
    part(24, 71, 32, 73, dark, 'rfoot')
    for y in (27, 34, 40):
        c.line(15, y, 32, y, height=-1)                              # courses: built, not born
    for y in (55, 61, 66):
        c.line(18, y, 21, y, height=-1)
        c.line(26, y, 29, y, height=-1)
    c.line(23, 30, 23, 45, height=-1)
    # the staff, pale, held in both hands and laid across the opening from post to post: the
    # network's eye at its end rests against the ring
    c.rect(6, 54, 38, 55, height=2, inlay=ANORTH[4], group='staff')
    for dx, dy in [(0, -3), (-1, -2), (1, -2), (-2, -1), (2, -1), (-1, 0), (1, 0), (0, 1)]:
        c.px(39 + dx, 54 + dy + 1, height=2, inlay=ANORTH[5], group='staffeye')
    c.px(39, 54, inlay=TEAL[3], glow=True)
    part(9, 52, 14, 53, dark, 'lhand')                               # the hands close over it
    part(33, 52, 38, 53, dark, 'rhand')
    part(9, 56, 14, 57, dark, 'lhand')
    part(33, 56, 38, 57, dark, 'rhand')


def m_the_quiet(c):
    """Four rings along the road, told in the order it happened: the dark comes out of the blue world
    at the left (the side it sits on in every mural), puts out the nearest rings first and spreads
    right. The voice says it came from the far worlds; the carving says otherwise. The fifth ring is
    only scratched in: the carving stops."""
    ground(c, 41)
    planet(c, 7, 9, 5, EARTH)                               # the blue world, where it began
    c.line(12, 13, 15, 22, every=2, height=-1)              # the road down from it to the first ring
    mini = [row[3:14] for row in GATE[8:]]
    xs = [3, 18, 33, 48, 63]
    for i, x in enumerate(xs[:4]):
        lit = i >= 2
        dark = mix(BASALT[0], ASH[0], 0.3)
        key = {'B': dict(height=1, inlay=BASALT[3] if lit else BASALT[1]),
               'R': dict(height=1, inlay=RING_STONE),
               'r': dict(height=1, inlay=TEAL[3] if lit else DULL[1], glow=lit),
               'F': dict(height=0, inlay=TEAL[3] if lit else dark, glow=lit),
               'P': dict(height=2)}
        c.sprite(mini, x, 41 - len(mini), key)
    for i in range(3):                                      # the road between them
        x0, x1 = xs[i] + 11, xs[i + 1] - 1
        if i >= 1:
            c.line(x0, 38, x1, 38, inlay=TEAL[2], glow=True)
        else:
            c.line(x0, 38, x1, 38, every=3, height=-1)
    gx = xs[4]                                              # the last ring: guide lines, then nothing
    for y in range(41 - 18, 41, 2):
        c.px(gx + 1, y, height=-1)
        c.px(gx + 9, y, height=-1)
    c.line(gx + 1, 22, gx + 9, 22, every=2, height=-1)
    c.line(xs[3] + 11, 38, gx, 38, every=4, height=-1)
    # the dark spreading out of the blue world: the plaster itself stained, densest round it
    for y in range(1, 41):
        for x in range(1, c.w - 1):
            d = math.hypot(x - 7, (y - 9) * 1.3)
            s = 1.15 - d / 34.0 + (math.sin(x * 0.45 + y * 0.3) + math.sin(y * 0.7 - x * 0.2)) * 0.08
            c.stain[y][x] = max(0.0, min(0.7, s * 0.8))


# --- the Sealers' Record and the keeper shrine (docs/technical/lore-art-format.md, sections 7, 8) ------
#
# The record is the truth of the Moon told only in pictures: six panels of 3 x 3 tiles on one canvas,
# read left to right, top row then bottom. The porters carried it out of the sealed room tile by tile;
# the template leaves its frame empty (RECORD_GOUGED) and logic gives its tiles to Endermen. The blue
# world sits at the left of every panel that shows it, as in every other mural.

PANEL = 48                       # each panel is 3 x 3 tiles
MINI_RING = [row[3:14] for row in GATE[8:]]          # 11 x 20: the ring without blades or eye


def mini_ring(c, x0, ground_y, lit, bricked=0, socket=False):
    """A small ring standing on ground_y. bricked: how many rows of its field, from the bottom, are
    walled up in stone courses. socket: the keystone (its bottom middle) prised out."""
    y0 = ground_y - len(MINI_RING)
    dark = mix(BASALT[0], ASH[0], 0.3)
    key = {'R': dict(height=1, inlay=RING_STONE),
           'r': dict(height=1, inlay=TEAL[3] if lit else DULL[1], glow=lit),
           'F': dict(height=0, inlay=TEAL[3] if lit else dark, glow=lit),
           'P': dict(height=2), 'B': dict(height=1, inlay=BASALT[3])}
    c.sprite(MINI_RING, x0, y0, key)
    if lit:
        for y in range(y0 + 2, ground_y - 1):
            for x in range(x0 + 2, x0 + 9):
                if (x * 3 + y * 2) % 11 == 0:
                    c.px(x, y, inlay=TEAL[4], glow=True)
    for i in range(bricked):                         # stone courses, laid from the bottom up
        y = ground_y - 2 - i
        for x in range(x0 + 2, x0 + 9):
            joint = (x - x0 + (i // 2) % 2 * 2) % 4 == 0
            c.px(x, y, height=1 if not joint else 0,
                 inlay=BASALT[2] if i % 2 else BASALT[3], group=('course', i))
    if socket:
        for x in (x0 + 4, x0 + 5, x0 + 6):
            c.px(x, ground_y - 1, height=-1, inlay=BASALT[0], group='socket')


def key_sprite(c, x0, y0):
    """The gate's key, as the keepers hold it: a pale bow with the network's eye, teal in its pupil."""
    rows = [".#.", "#o#", ".#.", ".#.", ".##", ".#.", ".##"]
    c.sprite(rows, x0, y0, {'#': dict(height=2, inlay=ANORTH[5], group='key'),
                            'o': dict(height=2, inlay=TEAL[4], glow=True)})


def keeper_small(c, x0, ground_y, eye=True, rough=0):
    """A keeper at panel scale, 18 wide and 34 tall, built in courses like The Keeper's. rough:
    how many rows from its feet are still uncut stone."""
    top = ground_y - 34
    body, limb, darkc = BASALT[4], BASALT[3], BASALT[2]

    def part(a, b, cc, d, col, grp):
        c.rect(x0 + a, top + b, x0 + cc, top + d, height=1, inlay=col, group=grp)
    part(5, 0, 12, 6, body, 'k_head')
    if eye:
        for dx, dy in ((7, 2), (8, 2), (9, 2), (6, 3), (10, 3), (7, 4), (8, 4), (9, 4)):
            c.px(x0 + dx - 1, top + dy, inlay=TEAL[3], glow=True)
        c.px(x0 + 7, top + 3, inlay=TEAL[5], glow=True)
    else:
        c.line(x0 + 5, top + 3, x0 + 10, top + 3, inlay=BASALT[1])        # the eye shut
    part(6, 7, 11, 7, darkc, 'k_neck')
    part(1, 8, 16, 11, limb, 'k_shoulders')
    for y in range(12, 21):
        half = 6 - (y - 12) // 3
        part(8 - half, y, 9 + half, y, body, 'k_body')
    part(0, 12, 2, 23, limb, 'k_larm')
    part(15, 12, 17, 23, limb, 'k_rarm')
    part(4, 21, 7, 33, body, 'k_lleg')
    part(10, 21, 13, 33, body, 'k_rleg')
    for y in (14, 18):
        c.line(x0 + 3, top + y, x0 + 14, top + y, height=-1)
    for y in range(34 - rough, 34):                   # the uncut block the carvers have not reached
        for x in range(2, 16):
            c.px(x0 + x, top + y, height=1, inlay=BASALT[3] if (x + y) % 5 else BASALT[2], group='k_rough')


def blue_world(c, cx=8, cy=8, r=5):
    planet(c, cx, cy, r, EARTH)


def p_breathing(c):
    """1. The colony breathing: the ring lit, its air drifting out to the people beside it."""
    ground(c, 40)
    blue_world(c)
    mini_ring(c, 27, 40, lit=True)
    figure(c, 'S', 6, 40)
    figure(c, 'S', 15, 40)
    for i, x in enumerate(range(26, 9, -3)):          # the air, drifting from the field to them
        c.px(x, 24 + round(2 * math.sin(i * 1.3)), inlay=TEAL[4], glow=True)
    for i, x in enumerate(range(25, 13, -4)):
        c.px(x, 30 + round(1.5 * math.cos(i * 1.1)), inlay=TEAL[3], glow=True)
    cat(c, 'S', 39, 40, flip=True)                    # every gate hall kept a cat


def p_dark_spreads(c):
    """2. The dark comes out of the blue world and along the road, ring by ring."""
    planet(c, 9, 25, 7, EARTH)
    worlds = [(22, 17, 2.5, GREY), (32, 29, 3, ASH), (42, 14, 3, ICE)]
    planet(c, *worlds[0][:3], worlds[0][3])
    for (xa, ya, ra, _), (xb, yb, rb, pal) in zip(worlds, worlds[1:]):
        planet(c, xb, yb, rb, pal)
    pts = [(9, 25, 7)] + [w[:3] for w in worlds]
    for i, ((xa, ya, ra), (xb, yb, rb)) in enumerate(zip(pts, pts[1:])):
        d = math.hypot(xb - xa, yb - ya)
        ux, uy = (xb - xa) / d, (yb - ya) / d
        a = (xa + ux * (ra + 1), ya + uy * (ra + 1))
        b = (xb - ux * (rb + 1), yb - uy * (rb + 1))
        if i < 2:
            c.line(*a, *b, height=-1)                 # put out: the inlay gone dark
        else:
            c.line(*a, *b, inlay=TEAL[2], glow=True)
    for x, y, r, _ in worlds[2:]:
        c.px(x, y - r - 2, height=1, inlay=TEAL[4], glow=True)    # the farthest gate still lit
    for y in range(1, PANEL - 1):                     # the stain, poured out of the blue world
        for x in range(1, PANEL - 1):
            d = math.hypot(x - 9, (y - 25) * 1.4)
            s = 1.2 - d / 26.0 + (math.sin(x * 0.5 + y * 0.35) + math.sin(y * 0.8 - x * 0.25)) * 0.08
            c.stain[y][x] = max(0.0, min(0.75, s * 0.85))


def p_walling(c):
    """3. The Sealers wall up their own ring and prise its keystone out."""
    ground(c, 40)
    mini_ring(c, 18, 40, lit=False, bricked=9, socket=True)
    figure(c, 'C', 3, 40)                             # bringing another stone
    figure(c, 'K', 28, 40, flip=True)                 # at the socket, with the chisel
    c.line(27, 37, 26, 38, height=1, inlay=FIGURE_INK)
    c.rect(37, 37, 40, 39, height=1, inlay=RING_STONE, group='keystone')   # the keystone, out
    c.px(38, 38, inlay=DULL[2])
    for x, y in ((42, 37), (42, 34), (45, 37)):       # the next courses, ready
        c.rect(x, y, x + 2, y + 2, height=1, inlay=BASALT[3], group=('stone', x, y))


def p_keepers(c):
    """4. A keeper carved from the stone, and the key put in its hand."""
    ground(c, 40)
    keeper_small(c, 27, 40, eye=True, rough=5)
    figure(c, 'K', 17, 40)                            # carving its foot
    c.line(25, 37, 26, 36, height=1, inlay=FIGURE_INK)
    c.rect(2, 33, 11, 39, height=1)                   # a step, to reach its hand
    figure(c, 'S', 3, 33)
    c.line(9, 21, 17, 17, height=1, inlay=FIGURE_INK)     # the arm, raised to the keeper
    key_sprite(c, 18, 13)                             # the key, passing from hand to hand


def p_stillness(c):
    """5. After the gate went dark: the people lying still beside it. The cat is still there."""
    ground(c, 40)
    blue_world(c)
    mini_ring(c, 36, 40, lit=False, bricked=17)
    lie = lying('S')
    c.sprite(lie, 2, 40 - len(lie), {'#': dict(height=1, inlay=FIGURE_INK, group='near')})
    c.sprite(lie, 9, 40 - 2 * len(lie) - 2,                       # one further back, higher up
             {'#': dict(height=1, inlay=mix(FIGURE_INK, ANORTH[1], 0.3), group='far')})
    cat(c, 'S', 24, 40, flip=True)


def p_last_ship(c):
    """6. One figure leaves, on a ship rising on fire toward the blue world. That is Tacita."""
    ground(c, 40)
    blue_world(c, 9, 9, 6)
    # the ship: a blunt hull rising up and to the left, toward the blue world, lit on its upper side
    tail, nose = (33.0, 27.0), (21.0, 15.0)
    ax, ay = nose[0] - tail[0], nose[1] - tail[1]
    length = math.hypot(ax, ay)
    ux, uy = ax / length, ay / length
    for y in range(8, 34):
        for x in range(13, 40):
            px_, py_ = x + 0.5 - tail[0], y + 0.5 - tail[1]
            u = px_ * ux + py_ * uy
            v = -px_ * uy + py_ * ux                  # signed distance across the hull
            if not 0 <= u <= length:
                continue
            half = 2.6 if u < length - 3 else 2.6 - (u - (length - 3)) * 0.8
            if abs(v) <= half:
                c.px(x, y, height=2, inlay=ANORTH[4] if v > 0.4 else ANORTH[2], group='ship')
    c.px(25, 20, height=2, inlay=BASALT[1], group='window')          # one figure, at the window
    c.px(25, 19, height=2, inlay=BASALT[1], group='window')
    rng = random.Random(66)                                           # the fire under it
    for i in range(1, 15):
        x, y = 33.5 + i * 0.7, 28.5 + i * 0.7
        w = 1 + i // 5
        for k in range(-w, w + 1):
            if rng.random() < 0.95 - i * 0.045:
                col = FORGE[4] if i < 4 else FORGE[3] if i < 9 else FORGE[2]
                c.px(x + k * 0.7, y - k * 0.7, inlay=col, glow=True)
    for x in range(40, 47):                                          # scorched ground under it
        c.px(x, 40, height=-1)


RECORD_PANELS = [p_breathing, p_dark_spreads, p_walling, p_keepers, p_stillness, p_last_ship]


def m_sealers_record(c):
    """The six panels on one canvas, 3 across and 2 down, each in its own raised border."""
    for i, draw in enumerate(RECORD_PANELS):
        sub = Canvas(PANEL, PANEL, seed=700 + i)
        draw(sub)
        ox, oy = (i % 3) * PANEL, (i // 3) * PANEL
        for y in range(PANEL):
            for x in range(PANEL):
                c.height[oy + y][ox + x] = sub.height[y][x]
                c.inlay[oy + y][ox + x] = sub.inlay[y][x]
                c.glow[oy + y][ox + x] = sub.glow[y][x]
                c.stain[oy + y][ox + x] = sub.stain[y][x]
                c.group[oy + y][ox + x] = None if sub.group[y][x] is None else (i, sub.group[y][x])
    rib = [(x, y) for k in (1, 2) for x in (k * PANEL - 1, k * PANEL) for y in range(c.h)]
    rib += [(x, y) for y in (PANEL - 1, PANEL) for x in range(c.w)]
    for x, y in rib:                                  # the raised ribs between the panels
        c.height[y][x], c.inlay[y][x], c.glow[y][x], c.stain[y][x] = 1, None, False, 0.0


def m_record_gouged(c):
    """Where the record's tiles were torn out of the sealed room's wall: three sockets of hacked
    plaster, the rougher bed showing, chisel bites round the rim. The template fills the record's
    frame with these; a recovered tile goes back in place of one."""
    rng = random.Random(901)
    bed = [mix(ANORTH[1], BASALT[4], 0.25), mix(ANORTH[1], BASALT[4], 0.4), mix(ANORTH[2], BASALT[4], 0.3)]
    for t in range(3):
        ox = t * 16
        for y in range(2, 14):
            for x in range(2, 14):
                ragged = (x in (2, 13) or y in (2, 13)) and rng.random() < 0.35
                if not ragged:
                    c.px(ox + x, y, height=-1, inlay=rng.choice(bed), group=('bed', t))
        for _ in range(7):                            # bites into the plaster left at the rim
            x, y = rng.choice([(rng.randrange(2, 14), rng.choice((1, 14))), (rng.choice((1, 14)), rng.randrange(2, 14))])
            c.px(ox + x, y, height=-1)
        for _ in range(4):                            # gouges across the bed
            x, y = rng.randrange(3, 10), rng.randrange(3, 12)
            for i in range(rng.randrange(2, 5)):
                c.px(ox + x + i, y + (i if t % 2 else 0), height=-1, inlay=mix(ANORTH[0], BASALT[3], 0.5),
                     group=('gouge', t))


def m_keeper_shrine(c):
    """The Sealers making the Moon's keepers, a pair: the sleeper, its eye shut, takes the key into
    its hand from a Sealer on a step; the guard, its eye open, is still being cut from the rock. One
    to sleep until it is needed and one to stand, so one is always awake. It hangs behind the Ender
    Guardian, and is found only after the fight."""
    ground(c, 41)
    keeper_small(c, 6, 41, eye=False)                  # the sleeper
    c.rect(27, 35, 36, 40, height=1)                   # a step, to reach its hand
    figure(c, 'S', 28, 35, flip=True)               # facing the sleeper
    c.line(30, 23, 25, 27, height=1, inlay=FIGURE_INK)     # the arm, reaching up to the sleeper's hand
    key_sprite(c, 26, 18)                              # the key, going into its hand
    keeper_small(c, 54, 41, eye=True, rough=7)         # the guard, its legs still in the rock
    figure(c, 'K', 44, 41)
    c.line(52, 38, 53, 37, height=1, inlay=FIGURE_INK)
    c.line(75, 5, 75, 40, every=2, height=-1)          # the scaffold round it, scratched in
    c.line(51, 5, 77, 5, every=2, height=-1)
    c.line(73, 21, 77, 21, height=1)                   # a plank, and a carver on it at its head
    figure(c, 'W', 71, 21, flip=True)


MURAL_FUNCS = {
    'stonecutters': m_stonecutters, 'the_step': m_the_step, 'the_road': m_the_road,
    'ring_ash': m_ring_ash, 'ring_ice': m_ring_ice, 'ring_forge': m_ring_forge,
    'keeper': m_keeper, 'the_quiet': m_the_quiet,
    'sealers_record': m_sealers_record, 'record_gouged': m_record_gouged, 'keeper_shrine': m_keeper_shrine,
}


def plaster(rng):
    """Tile 0: bare plaster, tileable, so a run of it reads as one smooth dressed wall."""
    img = new()
    p = img.load()
    low = moon.tile_noise(rng, 4)
    for y in range(16):
        for x in range(16):
            v = 0.60 + (low[y][x] - 0.5) * 0.10 + rng.uniform(-0.025, 0.025)
            p[x, y] = moon.ramp(ANORTH, v) + (255,)
    return img


# --- the plinth, the orrery, the tablet --------------------------------------------------------------

# The inscription's face, as the reader sees it on the plinth: a pale tablet with a diamond eye
# at its head and three lines of the builders' script under it. The tablet element shows the
# middle 12 x 10 of this texture (uv 2..14, 3..13).
TABLET_FACE = [
    "................",
    "................",
    "................",
    "..dddddddddddd..",
    "..dbbbbeeebbbd..",
    "..dbbbe.o.ebbd..",
    "..dbbbbeeebbbd..",
    "..dbbbbbbbbbbd..",
    "..db-r-rr--rbd..",
    "..dbbbbbbbbbbd..",
    "..dbr-rr-r-bbd..",
    "..dbbbbbbbbbbd..",
    "..db-rr-rr--bd..",
    "..dddddddddddd..",
    "................",
    "................",
]


def tablet_face(lit):
    img = new()
    p = img.load()
    rng = random.Random(61)
    for y, row in enumerate(TABLET_FACE):
        for x, ch in enumerate(row):
            if ch == '.':
                p[x, y] = ANORTH[3] + (255,)
            elif ch == 'd':
                p[x, y] = (ANORTH[4] if (x < 3 or y < 4) else ANORTH[1]) + (255,)
            elif ch == 'b':
                p[x, y] = shade(ANORTH[4], 1 + rng.uniform(-0.03, 0.03)) + (255,)
            elif ch == '-':
                p[x, y] = ANORTH[2] + (255,)                   # a cut stroke
            elif ch in 'r':
                p[x, y] = (TEAL[3] if lit else DULL[2]) + (255,)
            elif ch == 'e':
                p[x, y] = ANORTH[1] + (255,)
            elif ch == 'o':
                p[x, y] = (TEAL[4] if lit else DULL[2]) + (255,)
    return img


def tablet_glow():
    img = new()
    p = img.load()
    for y, row in enumerate(TABLET_FACE):
        for x, ch in enumerate(row):
            if ch == 'r':
                p[x, y] = TEAL[3] + (255,)
            elif ch == 'o':
                p[x, y] = TEAL[4] + (255,)
    return img


def orrery_top(lit):
    """The star map's face: the sun at the centre in concentric rings, worlds on their orbits.
    The hologram stands on this; it is what the projection comes out of."""
    img = art.polished(BASALT, random.Random(71), base_idx=1)
    p = img.load()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 1.6:
                p[x, y] = SUN[2] + (255,)
            elif d < 2.4:
                p[x, y] = SUN[0] + (255,)
            elif abs(d - 4.0) < 0.45 or abs(d - 6.2) < 0.45:
                p[x, y] = (TEAL[2] if lit else DULL[1]) + (255,)
            elif d > 7.3:
                p[x, y] = BASALT[3] + (255,)
    for (x, y) in [(11, 7), (4, 4), (6, 13), (13, 10)]:
        p[x, y] = (TEAL[4] if lit else DULL[2]) + (255,)
    return img


def orrery_glow():
    full = orrery_top(True)
    img = new()
    p, q = img.load(), full.load()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 2.4 or abs(d - 4.0) < 0.45 or abs(d - 6.2) < 0.45 or q[x, y][:3] == TEAL[4]:
                p[x, y] = q[x, y]
    return img


# The portable copy: a pale stone tablet, eye at its head, script below, in the outline the item
# guide asks for (tinted toward the stone, not black).
TABLET_ITEM = [
    "................",
    "....kkkkkkkk....",
    "...kLLLLLLLLk...",
    "..kLllleeelllk..",
    "..kLlle.o.ellk..",
    "..kLllleeelllk..",
    "..kLllllllllsk..",
    "..kLl-r-rr-lsk..",
    "..kLllllllllsk..",
    "..kLlr-rr-r-sk..",
    "..kLllllllllsk..",
    "..kLl-rr-r-lsk..",
    "..kLllllllllsk..",
    "..kssssssssssk..",
    "...kkkkkkkkkk...",
    "................",
]


def tablet_item():
    img = new()
    p = img.load()
    pal = {'k': (60, 58, 54), 'L': ANORTH[5], 'l': ANORTH[3], 's': ANORTH[1], 'e': ANORTH[1],
           '-': ANORTH[2], 'r': TEAL[3], 'o': TEAL[4]}
    for y, row in enumerate(TABLET_ITEM):
        for x, ch in enumerate(row):
            if ch in pal:
                p[x, y] = pal[ch] + (255,)
    return img


# --- models and blockstates -------------------------------------------------------------------------

def write_json(rel, obj):
    path = os.path.join(ASSETS, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


def face_set(texture, faces=('down', 'up', 'north', 'south', 'west', 'east'), cull=True, uv=None):
    out = {}
    for f in faces:
        e = {'texture': texture}
        if uv:
            e['uv'] = uv[f] if isinstance(uv, dict) else uv
        if cull:
            e['cullface'] = f
        out[f] = e
    return out


GLOW = {'block_light': 15, 'sky_light': 15, 'ambient_occlusion': False}


def mural_models(tiles_with_glow):
    variants = {}
    for t in range(MURAL_TILES):
        tex = f'{NS}:block/archive_mural/tile_{t}'
        rel = f'models/block/archive_mural/tile_{t}.json'
        if t in tiles_with_glow:
            write_json(rel, {
                'parent': 'minecraft:block/block',
                'render_type': 'minecraft:cutout',
                'textures': {'particle': tex, 'side': tex, 'end': f'{NS}:block/archive_mural/tile_0',
                             'glow': tex + '_glow'},
                'elements': [
                    {'from': [0, 0, 0], 'to': [16, 16, 16],
                     'faces': {**face_set('#end', ('down', 'up')), **face_set('#side', ('north', 'south', 'west', 'east'))}},
                    {'from': [0, 0, 0], 'to': [16, 16, 16], 'shade': False, 'neoforge_data': GLOW,
                     'faces': face_set('#glow', ('north', 'south', 'west', 'east'))},
                ],
            })
        else:
            write_json(rel, {'parent': 'minecraft:block/cube_column',
                             'textures': {'end': f'{NS}:block/archive_mural/tile_0', 'side': tex}})
        variants[f'tile={t}'] = {'model': f'{NS}:block/archive_mural/tile_{t}'}
    write_json('blockstates/archive_mural.json', {'variants': variants})
    write_json('models/item/archive_mural.json', {'parent': f'{NS}:block/archive_mural/tile_0'})


def stairs_models(kind, texture):
    for suffix, parent in (('', 'stairs'), ('_inner', 'inner_stairs'), ('_outer', 'outer_stairs')):
        write_json(f'models/block/{kind}_stairs{suffix}.json', {
            'parent': f'minecraft:block/{parent}',
            'textures': {'bottom': texture, 'top': texture, 'side': texture}})
    base_y = {'east': 0, 'south': 90, 'west': 180, 'north': 270}
    variants = {}
    for facing, by in base_y.items():
        for half in ('bottom', 'top'):
            for shape in ('straight', 'inner_left', 'inner_right', 'outer_left', 'outer_right'):
                model = f'{NS}:block/{kind}_stairs' + ('_inner' if shape.startswith('inner') else
                                                       '_outer' if shape.startswith('outer') else '')
                y = by
                if half == 'bottom' and shape.endswith('_left'):
                    y = by - 90
                if half == 'top' and shape.endswith('_right'):
                    y = by + 90
                y %= 360
                v = {'model': model}
                if half == 'top':
                    v['x'] = 180
                if y:
                    v['y'] = y
                if half == 'top' or y:
                    v['uvlock'] = True
                variants[f'facing={facing},half={half},shape={shape}'] = v
    write_json(f'blockstates/{kind}_stairs.json', {'variants': variants})
    write_json(f'models/item/{kind}_stairs.json', {'parent': f'{NS}:block/{kind}_stairs'})


def plinth_model():
    """A tablet resting on a pedestal, front (the tablet's face) to the north; the blockstate turns
    it to face its reader. The base covers the whole footprint: the block is a full cube to the
    renderer until logic gives it noOcclusion and a shape, and a narrower base would let the floor
    under it be culled into a see-through gap."""
    b, s = f'{NS}:block/', 'minecraft:block/'
    tex = {
        'particle': b + 'anorthosite_bricks',
        'base': b + 'polished_moon_basalt', 'foot': b + 'polished_moon_basalt',
        'column': b + 'anorthosite_pillar', 'column_top': b + 'anorthosite_pillar_top',
        'desk': b + 'anorthosite_bricks', 'face': b + 'archive_tablet_face', 'edge': b + 'chiseled_anorthosite',
        'glow': b + 'archive_tablet_face_glow',
    }
    del s

    def box(frm, to, faces):
        return {'from': frm, 'to': to, 'faces': faces}

    def all_faces(t, x0, y0, z0, x1, y1, z1, cull_down=False):
        f = {}
        for name, uv in (('north', [x0, 16 - y1, x1, 16 - y0]), ('south', [x0, 16 - y1, x1, 16 - y0]),
                         ('west', [z0, 16 - y1, z1, 16 - y0]), ('east', [z0, 16 - y1, z1, 16 - y0]),
                         ('up', [x0, z0, x1, z1]), ('down', [x0, z0, x1, z1])):
            f[name] = {'texture': t, 'uv': uv}
        if cull_down:
            f['down']['cullface'] = 'down'
        return f

    tilt = {'origin': [8, 13, 8], 'axis': 'x', 'angle': -22.5}      # front edge down, toward the reader
    # An up face lays its texture's top along the north edge -- the edge nearest a reader standing
    # north -- so the inscription is turned half round (uv flipped both ways) to read upright.
    READ_UV = [14, 13, 2, 3]
    elements = [
        box([0, 0, 0], [16, 2, 16], all_faces('#base', 0, 0, 0, 16, 2, 16, cull_down=True)),
        box([2, 2, 2], [14, 4, 14], all_faces('#foot', 2, 2, 2, 14, 4, 14)),
        box([4, 4, 4], [12, 11, 12], {**all_faces('#column', 4, 4, 4, 12, 11, 12)}),
        box([2, 11, 3], [14, 13, 13], all_faces('#desk', 2, 11, 3, 14, 13, 13)),
        {'from': [2, 13, 3], 'to': [14, 14.5, 13], 'rotation': tilt,
         'faces': {'up': {'texture': '#face', 'uv': READ_UV},
                   'north': {'texture': '#edge', 'uv': [2, 6, 14, 7.5]},
                   'south': {'texture': '#edge', 'uv': [2, 6, 14, 7.5]},
                   'west': {'texture': '#edge', 'uv': [3, 6, 13, 7.5]},
                   'east': {'texture': '#edge', 'uv': [3, 6, 13, 7.5]}}},
        {'from': [2, 13, 3], 'to': [14, 14.5, 13], 'rotation': tilt, 'shade': False, 'neoforge_data': GLOW,
         'faces': {'up': {'texture': '#glow', 'uv': READ_UV}}},
    ]
    elements[2]['faces']['up']['texture'] = '#column_top'
    write_json('models/block/archive_plinth.json', {
        'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout', 'textures': tex,
        'elements': elements})
    write_json('blockstates/archive_plinth.json', {'variants': {
        'facing=north': {'model': f'{NS}:block/archive_plinth'},
        'facing=east': {'model': f'{NS}:block/archive_plinth', 'y': 90},
        'facing=south': {'model': f'{NS}:block/archive_plinth', 'y': 180},
        'facing=west': {'model': f'{NS}:block/archive_plinth', 'y': 270},
    }})
    write_json('models/item/archive_plinth.json', {'parent': f'{NS}:block/archive_plinth'})


def orrery_model():
    """A squat drum on a full-footprint base (same reason as the plinth), its sides cut with the
    gatestone script and its top the star map. Only the top glows: the hologram logic draws above
    it is the thing to look at, and a glowing drum competed with it."""
    b = f'{NS}:block/'
    tex = {'particle': b + 'polished_moon_basalt', 'base': b + 'polished_moon_basalt',
           'drum': b + 'inscribed_gatestone', 'rim': b + 'anorthosite_bricks', 'top': b + 'archive_orrery_top',
           'glow': b + 'archive_orrery_top_glow'}

    def faces(t, x0, y0, z0, x1, y1, z1, top=None, cull_down=False):
        f = {}
        for name in ('north', 'south'):
            f[name] = {'texture': t, 'uv': [x0, 16 - y1, x1, 16 - y0]}
        for name in ('west', 'east'):
            f[name] = {'texture': t, 'uv': [z0, 16 - y1, z1, 16 - y0]}
        f['up'] = {'texture': top or t, 'uv': [x0, z0, x1, z1]}
        f['down'] = {'texture': t, 'uv': [x0, z0, x1, z1]}
        if cull_down:
            f['down']['cullface'] = 'down'
        return f

    elements = [
        {'from': [0, 0, 0], 'to': [16, 3, 16], 'faces': faces('#base', 0, 0, 0, 16, 3, 16, cull_down=True)},
        {'from': [2, 3, 2], 'to': [14, 11, 14], 'faces': {k: v for k, v in faces('#drum', 2, 3, 2, 14, 11, 14).items()}},
        {'from': [1, 11, 1], 'to': [15, 13, 15], 'faces': faces('#rim', 1, 11, 1, 15, 13, 15, top='#top')},
        {'from': [1, 11, 1], 'to': [15, 13, 15], 'shade': False, 'neoforge_data': GLOW,
         'faces': {'up': {'texture': '#glow', 'uv': [1, 1, 15, 15]}}},
    ]
    # the drum's sides show the middle of the gatestone's three lines of script
    for n in ('north', 'south', 'west', 'east'):
        elements[1]['faces'][n]['uv'] = [2, 5, 14, 13]
    write_json('models/block/archive_orrery.json', {
        'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout', 'textures': tex,
        'elements': elements})
    write_json('blockstates/archive_orrery.json', {'variants': {'': {'model': f'{NS}:block/archive_orrery'}}})
    write_json('models/item/archive_orrery.json', {'parent': f'{NS}:block/archive_orrery'})


# --- previews ------------------------------------------------------------------------------------------

def mural_sheet(images, path, scale=4):
    """Every mural at `scale`, with its tile grid ticked along the edges, on a basalt-dark ground."""
    pad = 18
    rows = []
    for name, img in images:
        big = img.resize((img.width * scale, img.height * scale), Image.NEAREST)
        rows.append((name, big))
    W = max(b.width for _, b in rows[:6]) * 2 + pad * 3
    out = Image.new('RGBA', (W, 10), BASALT[1] + (255,))
    # columns: the six hall murals; keeper and the quiet; the record, its sockets and the shrine
    cols = [rows[0:3], rows[3:6], rows[6:8], rows[8:11]]
    heights = [sum(b.height + pad for _, b in col) for col in cols]
    widths = [max(b.width for _, b in col) for col in cols]
    out = Image.new('RGBA', (sum(widths) + pad * (len(cols) + 1), max(heights) + pad), BASALT[1] + (255,))
    x = pad
    for col, cw in zip(cols, widths):
        y = pad
        for name, big in col:
            out.paste(big, (x, y))
            y += big.height + pad
        x += cw + pad
    out.save(path)


def in_wall(murals, path, scale=3):
    """The murals hall as a visitor sees it: each wall's three bays between pale pillars, dark
    basalt dado below, a pale band above. Only this file's art and the gateway's."""
    tex = lambda rel: Image.open(os.path.join(TEX, rel)).convert('RGBA').crop((0, 0, 16, 16))
    pillar = tex('block/anorthosite_pillar.png')
    band = tex('block/anorthosite_bricks.png')
    dado = tex('block/polished_moon_basalt.png')
    brick = tex('block/moon_basalt_bricks.png')
    lit = tex('block/inscribed_gatestone.png')
    walls = []
    for names in (['stonecutters', 'the_step', 'the_road'], ['ring_ash', 'ring_ice', 'ring_forge']):
        W, H = (1 + 3 * 6) * 16, 7 * 16
        wall = Image.new('RGBA', (W, H))
        for bx in range(W // 16):
            for by in range(H // 16):
                wall.paste(brick, (bx * 16, by * 16))
        for i in range(4):
            for by in range(7):
                wall.paste(pillar, (i * 6 * 16, by * 16))
        for i, name in enumerate(names):
            x0 = (1 + i * 6) * 16
            for bx in range(5):
                wall.paste(dado, (x0 + bx * 16, 6 * 16))
                wall.paste(dado, (x0 + bx * 16, 5 * 16))
                wall.paste(band, (x0 + bx * 16, 1 * 16))
            wall.paste(lit, (x0 + 2 * 16, 5 * 16))
            wall.paste(murals[name], (x0, 2 * 16), murals[name])
        walls.append(wall.resize((W * scale, H * scale), Image.NEAREST))
    out = Image.new('RGBA', (walls[0].width, sum(w.height for w in walls) + 12), (14, 15, 20, 255))
    out.paste(walls[0], (0, 0))
    out.paste(walls[1], (0, walls[0].height + 12))
    out.save(path)


def blocks_preview(path):
    """The plinth from its reader's side and from behind, and the orrery, drawn from their own
    model files by the structure preview tool."""
    rsp = _load('render_structure_preview', 'render-structure-preview.py')
    views = [rsp.render_block_model(f'{NS}:block/archive_plinth', view=(0.8, -0.75, 1.25)),
             rsp.render_block_model(f'{NS}:block/archive_plinth', view=(-1, -0.9, -1.25)),
             rsp.render_block_model(f'{NS}:block/archive_orrery', view=(-1, -0.9, -1.25))]
    crop = [v.crop((v.width // 8, v.height // 5, v.width * 7 // 8, v.height * 7 // 8)) for v in views]
    out = Image.new('RGBA', (sum(c.width for c in crop), crop[0].height), (40, 42, 50, 255))
    x = 0
    for c in crop:
        out.paste(c, (x, 0))
        x += c.width
    out.save(path)


def check_java():
    src = open(JAVA_MURAL, encoding='utf-8').read()
    m = re.search(r'TILES\s*=\s*(\d+)\s*;', src)
    if not m or int(m.group(1)) != MURAL_TILES:
        raise SystemExit(f'ArchiveMuralBlock.TILES is {m and m.group(1)}, the catalogue has {MURAL_TILES}: '
                         f'update the Java constant')


def main():
    check_java()
    made = {}

    def put(rel, img):
        art.save(img, rel)
        made[rel] = img

    put('block/archive_mural/tile_0.png', plaster(random.Random(80)))
    glowing = set()
    full = {}
    for i, (name, (tw, th)) in enumerate(_ORDER):
        c = Canvas(tw * 16, th * 16, seed=100 + i)
        MURAL_FUNCS[name](c)
        base, glow = c.render()
        full[name] = Image.alpha_composite(base, glow)
        first = MURALS[name]['first']
        for row in range(th):
            for col in range(tw):
                t = first + row * tw + col
                box = (col * 16, row * 16, col * 16 + 16, row * 16 + 16)
                art.save(base.crop(box), f'block/archive_mural/tile_{t}.png')
                g = glow.crop(box)
                path = os.path.join(TEX, f'block/archive_mural/tile_{t}_glow.png')
                if g.getbbox():
                    art.save(g, f'block/archive_mural/tile_{t}_glow.png')
                    glowing.add(t)
                elif os.path.exists(path):
                    os.remove(path)
    put('block/archive_tablet_face.png', tablet_face(lit=True))
    put('block/archive_tablet_face_glow.png', tablet_glow())
    put('block/archive_orrery_top.png', orrery_top(lit=True))
    put('block/archive_orrery_top_glow.png', orrery_glow())
    put('item/archive_tablet.png', tablet_item())

    mural_models(glowing)
    stairs_models('moon_basalt_brick', f'{NS}:block/moon_basalt_bricks')
    stairs_models('anorthosite_brick', f'{NS}:block/anorthosite_bricks')
    plinth_model()
    orrery_model()
    write_json('models/item/archive_tablet.json', {'parent': 'minecraft:item/generated',
                                                   'textures': {'layer0': f'{NS}:item/archive_tablet'}})

    os.makedirs(DESIGN, exist_ok=True)
    art.sheet(list(made.items()), os.path.join(DESIGN, 'textures.png'))
    mural_sheet(list(full.items()), os.path.join(DESIGN, 'murals.png'))
    in_wall(full, os.path.join(DESIGN, 'murals-in-the-hall.png'))
    blocks_preview(os.path.join(DESIGN, 'plinth-and-orrery.png'))
    item = made['item/archive_tablet.png'].resize((192, 192), Image.NEAREST)
    bg = Image.new('RGBA', item.size, (110, 110, 112, 255))
    Image.alpha_composite(bg, item).save(os.path.join(DESIGN, 'tablet-item-12x.png'))
    print(f'wrote {MURAL_TILES} mural tiles ({len(glowing)} with glow), {len(made)} other textures, models')


if __name__ == '__main__':
    main()
