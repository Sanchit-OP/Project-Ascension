#!/usr/bin/env python3
"""Smooth rune glyphs for the HUD chips (docs/technical/hud.md). Original vector shapes, drawn at 4x
and scaled down, white so the game tints each by its path. One sheet, one cell per rune.

    python tools/generate-hud-rune-glyphs.py

Writes modules/combat/src/main/resources/assets/ascension_combat/textures/gui/hud/rune_glyphs.png
and a `blur` mcmeta beside it, and design/hud/rune-glyphs-preview.png to look at. Deterministic.

The orrery keeps its pixel glyphs; these are only for the HUD, where everything else is smooth.
Only runes with running state have a chip, so only those have a glyph here. Add a name to NAMES and
a function below to add one; the order of NAMES is the order on the sheet and is read by
`RuneGlyphs` in combat's client code.
"""
import json
import math
import os

from PIL import Image, ImageDraw

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
OUT = os.path.join(ROOT, 'modules', 'combat', 'src', 'main', 'resources', 'assets', 'ascension_combat',
                   'textures', 'gui', 'hud')
PREVIEW = os.path.join(ROOT, 'design', 'hud', 'rune-glyphs-preview.png')

CELL = 64
SS = 4
W = (255, 255, 255, 255)
CLEAR = (0, 0, 0, 0)

NAMES = ['conquerors_resolve', 'relentless_strikes', 'momentum', 'cadence', 'arc', 'second_wind',
         'pack_leader', 'starfall', 'brace', 'drop_in', 'riposte', 'killing_surge', 'vanish', 'plating']


def scale(pts):
    return [(x * SS, y * SS) for x, y in pts]


def rot(pts, deg, cx=32, cy=32):
    a = math.radians(deg)
    out = []
    for x, y in pts:
        dx, dy = x - cx, y - cy
        out.append((cx + dx * math.cos(a) - dy * math.sin(a), cy + dx * math.sin(a) + dy * math.cos(a)))
    return out


def poly(d, pts, fill=W):
    d.polygon(scale(pts), fill=fill)


def disc(d, cx, cy, r, fill=W):
    d.ellipse([(cx - r) * SS, (cy - r) * SS, (cx + r) * SS, (cy + r) * SS], fill=fill)


def line(d, a, b, width, fill=W):
    d.line([(a[0] * SS, a[1] * SS), (b[0] * SS, b[1] * SS)], fill=fill, width=int(width * SS))
    disc(d, a[0], a[1], width / 2, fill)
    disc(d, b[0], b[1], width / 2, fill)


def star(cx, cy, outer, inner, points=5, start=-90):
    pts = []
    for i in range(points * 2):
        r = outer if i % 2 == 0 else inner
        a = math.radians(start + i * 180 / points)
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return pts


# --- the glyphs, each drawn in a 64 x 64 box --------------------------------------------------

def conquerors_resolve(d):
    # a crown: a band and three points with jewels cut out
    poly(d, [(10, 46), (10, 22), (22, 34), (32, 14), (42, 34), (54, 22), (54, 46)])
    poly(d, [(10, 50), (54, 50), (54, 56), (10, 56)])
    for x in (22, 32, 42):
        disc(d, x, 42, 2.6, CLEAR)


def relentless_strikes(d):
    # three claw slashes, each longer than the last
    for i, x in enumerate((18, 30, 42)):
        poly(d, rot([(x, 8), (x + 6, 8), (x + 8 + i * 2, 56), (x + 1 + i * 2, 56)], 18, x + 4, 32))


def momentum(d):
    # an arrow driving forward with trailing lines
    poly(d, [(20, 18), (40, 18), (58, 32), (40, 46), (20, 46), (34, 32)])
    line(d, (4, 24), (22, 24), 4)
    line(d, (4, 40), (22, 40), 4)
    line(d, (10, 32), (22, 32), 4)


def cadence(d):
    # three beats, each taller than the last, on a baseline
    for i, x in enumerate((10, 26, 42)):
        h = 18 + i * 12
        poly(d, [(x, 54 - h), (x + 12, 54 - h), (x + 12, 54), (x, 54)])
    poly(d, [(6, 56), (58, 56), (58, 60), (6, 60)])


def arc(d):
    # a lightning bolt with a notch
    poly(d, [(38, 4), (14, 36), (28, 36), (22, 60), (50, 24), (34, 24)])


def second_wind(d):
    # a heart with a breath of wind across it
    heart = []
    for i in range(0, 361, 4):
        t = math.radians(i)
        x = 16 * math.sin(t) ** 3
        y = 13 * math.cos(t) - 5 * math.cos(2 * t) - 2 * math.cos(3 * t) - math.cos(4 * t)
        heart.append((32 + x * 1.7, 30 - y * 1.7))
    poly(d, heart)
    for y, x1, x2 in ((26, 18, 46), (34, 26, 52)):
        line(d, (x1, y), (x2, y), 3.4, CLEAR)


def pack_leader(d):
    # a paw: one pad and four toes
    poly(d, [(18, 48), (22, 36), (32, 30), (42, 36), (46, 48), (40, 56), (24, 56)])
    for x, y, r in ((14, 30, 5.5), (26, 18, 6), (38, 18, 6), (50, 30, 5.5)):
        disc(d, x, y, r)


def starfall(d):
    # a star with a streak behind it
    poly(d, star(42, 26, 18, 8))
    for i, (a, b) in enumerate((((6, 54), (28, 36)), ((12, 60), (30, 46)), ((2, 44), (20, 34)))):
        line(d, a, b, 4 - i * 0.6)


def brace(d):
    # a shield held up, with a crossbar
    poly(d, [(32, 4), (56, 12), (54, 36), (32, 60), (10, 36), (8, 12)])
    poly(d, [(14, 26), (50, 26), (50, 32), (14, 32)], CLEAR)
    poly(d, [(29, 12), (35, 12), (35, 48), (29, 48)], CLEAR)


def drop_in(d):
    # a blade pointing down into a ring
    poly(d, [(25, 4), (39, 4), (39, 32), (32, 48), (25, 32)])
    poly(d, [(20, 28), (44, 28), (44, 33), (20, 33)])
    d.ellipse([14 * SS, 44 * SS, 50 * SS, 60 * SS], outline=W, width=4 * SS)


def riposte(d):
    # two blades crossed
    for deg in (40, -40):
        poly(d, rot([(27, 2), (37, 2), (38, 44), (32, 52), (26, 44)], deg))
        poly(d, rot([(22, 44), (42, 44), (42, 49), (22, 49)], deg))
        poly(d, rot([(30, 49), (34, 49), (34, 60), (30, 60)], deg))


def killing_surge(d):
    # a burst: eight spikes round a hot core
    poly(d, star(32, 32, 28, 12, 8, -90))
    disc(d, 32, 32, 8, CLEAR)


def vanish(d):
    # an eye, struck through: seen, then not
    poly(d, [(4, 32), (18, 18), (46, 18), (60, 32), (46, 46), (18, 46)])
    disc(d, 32, 32, 9, CLEAR)
    disc(d, 32, 32, 4.5)
    line(d, (12, 54), (52, 10), 5, CLEAR)
    line(d, (12, 54), (52, 10), 2.4)


def plating(d):
    # a chest plate with a rivet at each corner
    poly(d, [(14, 8), (50, 8), (56, 22), (50, 56), (14, 56), (8, 22)])
    poly(d, [(30, 8), (34, 8), (34, 56), (30, 56)], CLEAR)
    for x, y in ((18, 16), (46, 16), (18, 46), (46, 46)):
        disc(d, x, y, 2.4, CLEAR)


DRAW = {
    'conquerors_resolve': conquerors_resolve, 'relentless_strikes': relentless_strikes, 'momentum': momentum,
    'cadence': cadence, 'arc': arc, 'second_wind': second_wind, 'pack_leader': pack_leader,
    'starfall': starfall, 'brace': brace, 'drop_in': drop_in, 'riposte': riposte,
    'killing_surge': killing_surge, 'vanish': vanish, 'plating': plating,
}


def cell(name):
    img = Image.new('RGBA', (CELL * SS, CELL * SS), CLEAR)
    d = ImageDraw.Draw(img)
    DRAW[name](d)
    return img.resize((CELL, CELL), Image.LANCZOS)


def sheet():
    out = Image.new('RGBA', (CELL * len(NAMES), CELL), CLEAR)
    for i, name in enumerate(NAMES):
        out.alpha_composite(cell(name), (i * CELL, 0))
    return out


def main():
    assert set(NAMES) == set(DRAW), 'NAMES and DRAW must list the same glyphs'
    os.makedirs(OUT, exist_ok=True)
    s = sheet()
    path = os.path.join(OUT, 'rune_glyphs.png')
    s.save(path)
    with open(path + '.mcmeta', 'w', encoding='utf-8') as f:
        json.dump({'texture': {'blur': True}}, f)
    # A preview: each glyph in its path's colour on a dark disc, for looking at.
    tints = {'conquerors_resolve': (235, 184, 72), 'relentless_strikes': (235, 184, 72), 'momentum': (235, 184, 72),
             'cadence': (235, 184, 72), 'arc': (168, 124, 240), 'second_wind': (235, 184, 72),
             'pack_leader': (110, 160, 220), 'starfall': (168, 124, 240), 'brace': (110, 160, 220),
             'drop_in': (226, 80, 90), 'riposte': (110, 160, 220), 'killing_surge': (235, 184, 72),
             'vanish': (226, 80, 90), 'plating': (110, 160, 220)}
    prev = Image.new('RGBA', (len(NAMES) * 96, 96 + 40), (14, 18, 28, 255))
    for i, name in enumerate(NAMES):
        g = cell(name).resize((72, 72), Image.LANCZOS)
        tint = Image.new('RGBA', g.size, tints[name] + (255,))
        tinted = Image.composite(tint, Image.new('RGBA', g.size, CLEAR), g.split()[3])
        prev.alpha_composite(tinted, (i * 96 + 12, 12))
        # a second, small copy: how it reads at chip size
        small = cell(name).resize((24, 24), Image.LANCZOS)
        ts = Image.composite(Image.new('RGBA', small.size, tints[name] + (255,)), Image.new('RGBA', small.size, CLEAR),
                             small.split()[3])
        prev.alpha_composite(ts, (i * 96 + 36, 100))
    os.makedirs(os.path.dirname(PREVIEW), exist_ok=True)
    prev.convert('RGB').save(PREVIEW)
    print('wrote', path, 'and', PREVIEW)


if __name__ == '__main__':
    main()
