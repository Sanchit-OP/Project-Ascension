#!/usr/bin/env python3
"""Oxygen v2 art (docs/technical/oxygen-v2.md, increment A): the tank, the refill station, the
emitter and the air fluid. Seeded and original (art guide rule 1); the PNGs are this script's output.

    python tools/generate-atmosphere-art.py

Writes modules/atmosphere/src/main/resources/assets/ascension_atmosphere/textures/{block,item} (the fluid is in block/: fluids are stitched from the block atlas)
and design/atmosphere/contact.png. Steel is blue-grey, air is pale frost, helium is the amber-cream
the Moon script already uses (teal stays the gateways). Re-running must change no PNG.
"""
import importlib.util
import json
import math
import os
import random

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, '..'))
TEX = os.path.join(ROOT, 'modules', 'atmosphere', 'src', 'main', 'resources', 'assets',
                   'ascension_atmosphere', 'textures')
TEX_CREATE = os.path.join(ROOT, 'modules', 'compat', 'create', 'src', 'main', 'resources', 'assets',
                          'ascension_compat_create', 'textures')
DESIGN = os.path.join(ROOT, 'design', 'atmosphere')

_spec = importlib.util.spec_from_file_location('moon_art', os.path.join(HERE, 'generate-moon-materials-art.py'))
moon = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(moon)
HELIUM = moon.HELIUM          # amber-cream ramp, dark to bright

STEEL = [(26, 32, 42), (52, 62, 76), (84, 97, 113), (124, 139, 156), (170, 184, 199), (208, 220, 232)]
FROST = (214, 234, 246)
FROST_D = (120, 156, 184)
DARK = STEEL[0]


def clamp(v):
    return max(0, min(255, int(v)))


def shade(c, f):
    return tuple(clamp(v * f) for v in c)


def mix(a, b, t):
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3))


def img(w=16, h=16):
    return Image.new('RGBA', (w, h), (0, 0, 0, 0))


def fill(im, x0, y0, x1, y1, c):
    px = im.load()
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px[x, y] = c + (255,)


def plate(im, x0, y0, x1, y1, rng, base=2, grain=5):
    """Brushed steel: lit top-left edge, shadowed bottom-right, a little grain."""
    px = im.load()
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            c = tuple(clamp(v + rng.randint(-grain, grain)) for v in STEEL[base])
            if y == y0 or x == x0:
                c = STEEL[base + 1]
            if y == y1 or x == x1:
                c = STEEL[base - 1]
            px[x, y] = c + (255,)


def rivet(im, x, y):
    px = im.load()
    px[x, y] = STEEL[4] + (255,)
    px[x + 1, y] = STEEL[2] + (255,)
    px[x, y + 1] = STEEL[2] + (255,)
    px[x + 1, y + 1] = STEEL[0] + (255,)


def tank_body(helium, seed):
    """One face of the cylinder: steel with a rounded highlight and two bands."""
    rng = random.Random(seed)
    im = img()
    plate(im, 0, 0, 15, 15, rng, base=2, grain=3)
    px = im.load()
    for y in range(16):
        for x in range(16):      # highlight column down the middle, fading to the sides
            d = abs(x - 7.5) / 7.5
            f = 1.0 + 0.28 * (1 - d) - 0.18 * d * d
            r, g, b, _ = px[x, y]
            px[x, y] = shade((r, g, b), f) + (255,)
    band = [HELIUM[2], HELIUM[3], HELIUM[4]] if helium else [STEEL[1], STEEL[2], STEEL[3]]
    for y0 in (3, 11):
        for x in range(16):
            px[x, y0] = DARK + (255,)
            px[x, y0 + 1] = band[0] + (255,)
            px[x, y0 + 2] = band[1] + (255,)
            px[x, y0 + 3] = band[2] + (255,)
            px[x, y0 + 4] = DARK + (255,)
    px[5, 4] = shade(band[2], 1.1) + (255,)     # a specular dot per band, so it reads as metal
    px[5, 12] = shade(band[2], 1.1) + (255,)
    rivet(im, 1, 7)
    rivet(im, 13, 7)
    return im


def tank_fitting():
    """Neck collar and valve wheel: darker steel, frost trim."""
    rng = random.Random(402)
    im = img()
    plate(im, 0, 0, 15, 15, rng, base=1, grain=4)
    px = im.load()
    for x in range(16):
        px[x, 7] = STEEL[0] + (255,)
        px[x, 8] = STEEL[3] + (255,)
    for y in range(0, 16, 5):
        for x in range(16):
            px[x, y] = FROST_D + (255,)
    return im


def station_side():
    """A steel casing: a gauge dial, an intake grille, rivets."""
    rng = random.Random(501)
    im = img()
    plate(im, 0, 0, 15, 15, rng, base=2, grain=4)
    px = im.load()
    for y in range(13, 16):                 # dark lower plinth
        for x in range(16):
            px[x, y] = shade(STEEL[1], 0.9 - 0.1 * (y - 13)) + (255,)
    dial = ['.kkk.',
            'kfffk',
            'kfnfk',
            'kfffk',
            '.kkk.']
    key = {'k': DARK, 'f': FROST, 'n': STEEL[1]}
    for dy, row in enumerate(dial):
        for dx, ch in enumerate(row):
            if ch != '.':
                px[2 + dx, 2 + dy] = key[ch] + (255,)
    px[5, 3] = (214, 120, 70, 255)          # a needle tip, warm so it stands out
    px[4, 4] = DARK + (255,)
    for y in range(3, 12, 2):               # intake louvres
        for x in range(9, 14):
            px[x, y] = DARK + (255,)
            px[x, y + 1] = STEEL[3] + (255,)
    rivet(im, 1, 11)
    rivet(im, 13, 0)
    return im


def station_top():
    rng = random.Random(502)
    im = img()
    plate(im, 0, 0, 15, 15, rng, base=3, grain=3)
    fill(im, 4, 4, 11, 11, DARK)            # hatch to the hose port
    fill(im, 5, 5, 10, 10, STEEL[1])
    fill(im, 6, 6, 9, 9, FROST_D)
    fill(im, 7, 7, 8, 8, DARK)
    for c in ((1, 1), (13, 1), (1, 13), (13, 13)):
        rivet(im, *c)
    return im


def station_bottom():
    rng = random.Random(503)
    im = img()
    plate(im, 0, 0, 15, 15, rng, base=1, grain=3)
    return im


def emitter_side():
    """A vent block: a framed louvre grille over a pale glow strip."""
    rng = random.Random(601)
    im = img()
    plate(im, 0, 0, 15, 15, rng, base=2, grain=3)
    px = im.load()
    fill(im, 2, 2, 13, 13, DARK)
    for y in range(3, 13, 2):
        for x in range(3, 13):
            px[x, y] = STEEL[3] + (255,)
            px[x, y + 1] = mix(FROST, STEEL[2], 0.5) + (255,)
    rivet(im, 0, 0)
    rivet(im, 14, 14)
    return im


def emitter_end():
    rng = random.Random(602)
    im = img()
    plate(im, 0, 0, 15, 15, rng, base=3, grain=3)
    fill(im, 3, 3, 12, 12, STEEL[1])
    fill(im, 4, 4, 11, 11, DARK)
    px = im.load()
    for r, c in ((5, FROST_D), (6, FROST), (7, FROST_D)):   # a small ring of air
        for x in range(r, 16 - r):
            for y in (r, 15 - r):
                px[x, y] = c + (255,)
                px[y, x] = c + (255,)
    return im


def dispenser_side():
    """A casing with a round nozzle and a frost-lit slot: the thing that breathes out."""
    rng = random.Random(801)
    im = img()
    plate(im, 0, 0, 15, 15, rng, base=2, grain=3)
    px = im.load()
    fill(im, 3, 3, 12, 12, DARK)
    fill(im, 4, 4, 11, 11, STEEL[1])
    fill(im, 5, 5, 10, 10, DARK)
    fill(im, 6, 6, 9, 9, FROST_D)
    fill(im, 7, 7, 8, 8, FROST)
    for x in range(3, 13):
        px[x, 13] = STEEL[3] + (255,)
    rivet(im, 0, 0)
    rivet(im, 14, 14)
    return im


def dispenser_end():
    rng = random.Random(802)
    im = img()
    plate(im, 0, 0, 15, 15, rng, base=3, grain=3)
    fill(im, 2, 2, 13, 13, STEEL[1])
    fill(im, 3, 3, 12, 12, STEEL[2])
    px = im.load()
    for y in range(4, 12, 2):                # a fluid intake grille, so pipes read as feeding it
        for x in range(4, 12):
            px[x, y] = DARK + (255,)
    return im


FAN = ['.....kk.....',
       '....kffk....',
       '....kffk....',
       '.....kffk...',
       '..kk..kffk..',
       'kfffkkkffk..',
       'kffffnnkk...',
       '.kkffnnfk...',
       '...kffk.kk..',
       '..kffk..kfk.',
       '..kffk...kk.',
       '...kk.......']


def compressor_side():
    """A steel casing with a three-bladed intake fan (a pinwheel, not a cross)."""
    rng = random.Random(901)
    im = img()
    plate(im, 0, 0, 15, 15, rng, base=2, grain=3)
    px = im.load()
    fill(im, 1, 1, 14, 14, STEEL[1])
    fill(im, 2, 2, 13, 13, DARK)
    key = {'k': STEEL[1], 'f': FROST_D, 'n': FROST}
    for dy, row in enumerate(FAN):
        for dx, ch in enumerate(row):
            if ch in key:
                px[2 + dx, 2 + dy] = key[ch] + (255,)
    rivet(im, 0, 0)
    rivet(im, 14, 14)
    return im


def compressor_end():
    """The shaft face: a plate with a square shaft in a collar."""
    rng = random.Random(902)
    im = img()
    plate(im, 0, 0, 15, 15, rng, base=3, grain=3)
    fill(im, 3, 3, 12, 12, STEEL[1])
    fill(im, 4, 4, 11, 11, DARK)
    fill(im, 5, 5, 10, 10, STEEL[2])
    fill(im, 6, 6, 9, 9, STEEL[4])
    fill(im, 7, 7, 8, 8, STEEL[3])
    px = im.load()
    px[6, 9] = STEEL[1] + (255,)
    px[9, 9] = STEEL[1] + (255,)
    for c in ((1, 1), (13, 1), (1, 13), (13, 13)):
        rivet(im, *c)
    return im


def wave(x, y, t, terms):
    """Tileable in x, y and t: every term uses whole numbers of periods."""
    v = 0.0
    for kx, ky, kt, ph, amp in terms:
        v += amp * math.cos(2 * math.pi * (kx * x / 16 + ky * y / 16 + kt * t) + ph)
    return v


def air(size, frames, seed):
    """Pale gas: low-contrast drifting frost on a faint translucent veil. White-ish, tinted in game."""
    rng = random.Random(seed)
    terms = [(rng.randint(1, 2), rng.randint(0, 2), rng.choice((-1, 1)), rng.uniform(0, 6.28), 1.0)
             for _ in range(4)]
    im = img(size, size * frames)
    px = im.load()
    for f in range(frames):
        for y in range(size):
            for x in range(size):
                v = wave(x * 16 / size, y * 16 / size, f / frames, terms) / 4
                a = clamp(70 + 40 * v)
                c = mix((200, 224, 240), (244, 250, 255), 0.5 + 0.5 * v)
                px[x, f * size + y] = c + (a,)
    return im


MODELS = os.path.join(ROOT, 'modules', 'atmosphere', 'src', 'main', 'resources', 'assets',
                      'ascension_atmosphere', 'models', 'item')

HELD = {
    'gui': {'rotation': [30, 45, 0], 'translation': [0, -1, 0], 'scale': [1.0, 1.0, 1.0]},
    'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.5, 0.5, 0.5]},
    'fixed': {'rotation': [0, 180, 0], 'translation': [0, 0, 0], 'scale': [1.0, 1.0, 1.0]},
    'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.5, 0.5, 0.5]},
    'thirdperson_lefthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.5, 0.5, 0.5]},
    'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 0, 0], 'scale': [0.6, 0.6, 0.6]},
    'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, 0, 0], 'scale': [0.6, 0.6, 0.6]},
}


def tank_model(body):
    """An octagonal cylinder from four boxes. Minecraft only allows rotations of -45, -22.5, 0, 22.5
    and 45 degrees on an element, so the octagon is: one box along x, one along z, and two at
    +/-45. Side faces only; one flat cap on top and underneath so nothing flickers."""
    side = 3.314
    lo, hi = 8 - side / 2, 8 + side / 2
    names = ('north', 'south', 'east', 'west')

    def box(frm, to, angle):
        e = {'from': frm, 'to': to,
             'faces': {n: {'uv': [0, 0, 16, 16], 'texture': '#body'} for n in names}}
        if angle:
            e['rotation'] = {'angle': angle, 'axis': 'y', 'origin': [8, 0, 8]}
        return e

    def flat(frm, to, face, uv):
        return {'from': frm, 'to': to, 'faces': {face: {'uv': uv, 'texture': '#fitting'}}}

    elements = [
        box([4, 0, lo], [12, 13, hi], 0),
        box([lo, 0, 4], [hi, 13, 12], 0),
        box([4, 0, lo], [12, 13, hi], 45),
        box([4, 0, lo], [12, 13, hi], -45),
        flat([5, 13, 5], [11, 13.01, 11], 'up', [5, 5, 11, 11]),
        flat([5, -0.01, 5], [11, 0, 11], 'down', [5, 5, 11, 11]),
    ]
    neck = {'from': [6.5, 13, 6.5], 'to': [9.5, 15, 9.5],
            'faces': {n: {'uv': [6.5, 0, 9.5, 2], 'texture': '#fitting'} for n in names}}
    neck['faces']['up'] = {'uv': [6.5, 6.5, 9.5, 9.5], 'texture': '#fitting'}
    wheel = {'from': [4.5, 15, 7.5], 'to': [11.5, 16, 8.5],
             'faces': {n: {'uv': [0, 5, 7, 6], 'texture': '#fitting'} for n in ('north', 'south', 'up')}}
    return {'parent': 'minecraft:block/block',
            'textures': {'particle': 'ascension_atmosphere:item/' + body,
                         'body': 'ascension_atmosphere:item/' + body,
                         'fitting': 'ascension_atmosphere:item/oxygen_tank_fitting'},
            'elements': elements + [neck, wheel], 'display': HELD}


def write_models():
    os.makedirs(MODELS, exist_ok=True)
    for name, body in (('oxygen_tank', 'oxygen_tank_body'), ('oxygen_tank_helium', 'oxygen_tank_helium_body')):
        with open(os.path.join(MODELS, name + '.json'), 'w', newline='\n') as f:
            json.dump(tank_model(body), f, indent=2)
            f.write('\n')


def save(im, rel):
    path = os.path.join(TEX, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    im.save(path)


def main():
    sheets = [
        ('item/oxygen_tank_body.png', tank_body(False, 301)),
        ('item/oxygen_tank_helium_body.png', tank_body(True, 301)),
        ('item/oxygen_tank_fitting.png', tank_fitting()),
        ('block/oxygen_refill_station_side.png', station_side()),
        ('block/oxygen_refill_station_top.png', station_top()),
        ('block/oxygen_refill_station_bottom.png', station_bottom()),
        ('block/oxygen_emitter_side.png', emitter_side()),
        ('block/oxygen_emitter_end.png', emitter_end()),
        ('block/air_dispenser_side.png', dispenser_side()),
        ('block/air_dispenser_end.png', dispenser_end()),
    ]
    for rel, im in sheets:
        save(im, rel)
    write_models()
    # The Create jar's own blocks are drawn here too, so every look stays in one script.
    compat = [('block/air_compressor_side.png', compressor_side()),
              ('block/air_compressor_end.png', compressor_end())]
    for rel, im in compat:
        path = os.path.join(TEX_CREATE, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        im.save(path)
    sheets += compat
    fluids = [('block/liquid_o2_still.png', air(16, 16, 701), 16), ('block/liquid_o2_flow.png', air(32, 16, 702), 32)]
    for rel, im, _ in fluids:
        save(im, rel)
        with open(os.path.join(TEX, rel + '.mcmeta'), 'w', newline='\n') as f:
            json.dump({'animation': {'frametime': 3, 'interpolate': True}}, f, indent=2)
            f.write('\n')
    os.makedirs(DESIGN, exist_ok=True)
    sheet = Image.new('RGBA', ((len(sheets) + 2) * 17, 17), (96, 96, 96, 255))
    for i, (_, im) in enumerate(sheets):
        sheet.alpha_composite(im, (i * 17, 0))
    for j, (_, im, s) in enumerate(fluids):
        sheet.alpha_composite(im.crop((0, 0, s, s)).resize((16, 16)), ((len(sheets) + j) * 17, 0))
    sheet.resize((sheet.width * 8, sheet.height * 8), Image.NEAREST).save(os.path.join(DESIGN, 'contact.png'))


if __name__ == '__main__':
    main()
