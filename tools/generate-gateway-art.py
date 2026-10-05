"""Authoring helper for the gateway portal's textures. No build or runtime dependency.

Run from the repository root:  python tools/generate-gateway-art.py
Needs Pillow. Rewrites every PNG it owns under
modules/worlds/src/main/resources/assets/ascension_worlds/textures/ and a contact sheet under
design/gateway/, so hand edits to those PNGs are lost on the next run -- edit this file instead.

Everything here is original procedural or hand-placed pixel art (see
docs/technical/gateway-portal-design.md, "Texture sources"). Seeded, so output is reproducible.
"""
import math
import os
import random

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
TEX = os.path.join(ROOT, 'modules/worlds/src/main/resources/assets/ascension_worlds/textures')
DESIGN = os.path.join(ROOT, 'design/gateway')

# --- palette ------------------------------------------------------------------------------
# Lunar basalt: cool, faintly violet grey. Anorthosite: pale plagioclase, faintly warm.
# Teal: the network's own colour -- not the Nether's purple, not the End's black.
BASALT = [(20, 21, 26), (31, 33, 40), (42, 44, 53), (54, 57, 67), (68, 71, 83), (85, 89, 102)]
ANORTH = [(118, 116, 112), (154, 152, 146), (181, 179, 172), (202, 200, 193), (221, 219, 212),
          (238, 237, 231)]
FLECK = (229, 234, 240)
TEAL = [(8, 34, 42), (16, 70, 80), (28, 120, 128), (58, 192, 192), (138, 246, 234), (228, 255, 251)]
DULL = [(26, 44, 50), (36, 62, 68), (48, 84, 90)]


def clamp(c):
    return tuple(max(0, min(255, int(round(v)))) for v in c)


def shade(c, k):
    return clamp(tuple(v * k for v in c))


def mix(a, b, t):
    return clamp(tuple(a[i] + (b[i] - a[i]) * t for i in range(3)))


def new(w=16, h=16):
    return Image.new('RGBA', (w, h), (0, 0, 0, 0))


def save(img, rel):
    path = os.path.join(TEX, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)
    return path


def write_mcmeta(rel, frametime, interpolate=False, frames=None):
    path = os.path.join(TEX, rel + '.mcmeta')
    body = '{\n  "animation": {\n    "frametime": %d' % frametime
    if interpolate:
        body += ',\n    "interpolate": true'
    if frames is not None:
        body += ',\n    "frames": [%s]' % ', '.join(str(f) for f in frames)
    body += '\n  }\n}\n'
    with open(path, 'w', newline='\n') as f:
        f.write(body)


# --- stone ---------------------------------------------------------------------------------

def bricks(pal, rows, rng, fleck=None, mortar_idx=0, base_idx=3):
    """rows: list of (height, brick_width, offset). Mortar on each brick's bottom/left edge."""
    img = new()
    px = img.load()
    y0 = 0
    for height, bw, off in rows:
        tones = {}
        for y in range(y0, y0 + height):
            for x in range(16):
                lx = (x - off) % 16
                brick = (x - off) // bw if x >= off else (x - off + 16) // bw
                key = (y0, brick % (16 // bw))
                if key not in tones:
                    tones[key] = rng.choice((-1, 0, 0, 1)) * 0.035
                if y == y0 + height - 1 or lx % bw == 0:
                    px[x, y] = pal[mortar_idx] + (255,)
                    continue
                c = pal[base_idx]
                if y == y0:
                    c = pal[base_idx + 1]                     # lit top edge
                elif y == y0 + height - 2:
                    c = pal[base_idx - 1]                     # shadowed lower edge
                if lx % bw == 1 and y != y0:
                    c = mix(c, pal[base_idx + 1], 0.5)       # lit left edge
                if lx % bw == bw - 1:
                    c = mix(c, pal[base_idx - 1], 0.6)       # shadowed right edge
                c = shade(c, 1 + tones[key] + rng.uniform(-0.035, 0.035))
                if fleck and rng.random() < 0.05:
                    c = mix(c, fleck if rng.random() < 0.5 else pal[base_idx - 1], 0.55)
                px[x, y] = c + (255,)
        y0 += height
    return img


def crack(img, pal_dark, pal_lip, rng, paths):
    px = img.load()
    for (x, y), steps in paths:
        for _ in range(steps):
            if 0 <= x < 16 and 0 <= y < 16:
                px[x, y] = pal_dark + (255,)
                if 0 <= x + 1 < 16 and 0 <= y + 1 < 16 and rng.random() < 0.5:
                    px[x + 1, y + 1] = pal_lip + (255,)
            x += rng.choice((-1, 0, 1, 1))
            y += 1 if rng.random() < 0.7 else 0
    return img


def polished(pal, rng, base_idx=2):
    img = new()
    px = img.load()
    for y in range(16):
        for x in range(16):
            n = math.sin(x * 0.9 + y * 0.35) * 0.02 + rng.uniform(-0.025, 0.025)
            c = shade(pal[base_idx], 1 + n)
            if (x + y) % 7 == 0 and rng.random() < 0.5:
                c = mix(c, pal[base_idx + 1], 0.35)          # faint polish streak
            if x == 0 or y == 0:
                c = pal[base_idx + 2]
            elif x == 15 or y == 15:
                c = pal[base_idx - 1]
            elif x == 1 or y == 1:
                c = mix(c, pal[base_idx + 1], 0.5)
            elif x == 14 or y == 14:
                c = mix(c, pal[base_idx - 1], 0.5)
            px[x, y] = c + (255,)
    return img


def pillar_side(pal, rng):
    img = new()
    px = img.load()
    flute = [1, 3, 5, 4, 3, 2, 1, 3, 4, 5, 4, 3, 2, 1, 3, 4]
    for y in range(16):
        for x in range(16):
            i = flute[x]
            c = pal[i]
            if x in (0, 15):
                c = pal[1] if x == 15 else pal[2]
            c = shade(c, 1 + rng.uniform(-0.03, 0.03))
            if rng.random() < 0.03:
                c = mix(c, FLECK, 0.5)
            px[x, y] = c + (255,)
    return img


def pillar_top(pal, rng):
    img = new()
    px = img.load()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if x in (0, 15) or y in (0, 15):
                c = pal[1]
            elif d > 6.6:
                c = pal[3]
            elif d > 5.6:
                c = pal[1]                                   # carved ring
            elif d > 4.6:
                c = pal[5] if (x + y) < 15 else pal[3]
            elif d > 1.8:
                c = pal[4]
            else:
                c = pal[2]
            c = shade(c, 1 + rng.uniform(-0.03, 0.03))
            if rng.random() < 0.03:
                c = mix(c, FLECK, 0.5)
            px[x, y] = c + (255,)
    return img


# --- the eye motif: shared by chiseled anorthosite, the gatestone, the keystone, the crown ----

EYE = [
    "................",
    "................",
    ".......##.......",
    "......#..#......",
    ".....#....#.....",
    "....#..##..#....",
    "...#..#..#..#...",
    "..#..#.oo.#..#..",
    "..#..#.oo.#..#..",
    "...#..#..#..#...",
    "....#..##..#....",
    ".....#....#.....",
    "......#..#......",
    ".......##.......",
    "................",
    "................",
]


def chiseled(pal, rng):
    img = polished(pal, rng, base_idx=3)
    px = img.load()
    for y in range(16):
        for x in range(16):
            ch = EYE[y][x]
            if ch == '#':
                px[x, y] = pal[5] + (255,)
                if y + 1 < 16 and EYE[y + 1][x] == '.':
                    px[x, y + 1] = pal[1] + (255,)           # carved shadow under the relief
            elif ch == 'o':
                px[x, y] = pal[2] + (255,)
    # inner border groove
    for i in range(2, 14):
        for (x, y) in ((i, 1), (1, i)):
            px[x, y] = pal[2] + (255,)
    return img


# Three lines of the builders' script. Runes are 3 x 4; the alphabet is invented and deliberately
# not Latin-shaped, so the ring reads as writing without spelling anything.
RUNES = [
    [".#.", "#.#", ".#.", ".#."],
    ["###", "..#", ".#.", "#.."],
    ["#.#", ".#.", "#.#", "#.."],
    ["##.", ".##", "#..", "###"],
    [".##", "#..", ".##", "..#"],
    ["#..", "###", "..#", "#.#"],
    ["###", "#.#", "..#", ".##"],
    ["#.#", "###", ".#.", ".#."],
    [".#.", "##.", ".##", ".#."],
]
RUNE_LINES = [(1, (0, 1, 2)), (6, (3, 4, 5)), (11, (6, 7, 8))]
RUNE_X = (2, 7, 11)


def gatestone(rng):
    img = polished(BASALT, rng, base_idx=2)
    px = img.load()
    for y0, glyphs in RUNE_LINES:
        for gx, g in zip(RUNE_X, glyphs):
            for dy, row in enumerate(RUNES[g]):
                for dx, ch in enumerate(row):
                    if ch == '#':
                        x, y = gx + dx, y0 + dy
                        lit = rng.random() < 0.18
                        px[x, y] = (TEAL[2] if lit else DULL[2]) + (255,)
                        if y + 1 < 15 and px[x, y + 1][:3] != DULL[2]:
                            px[x, y + 1] = BASALT[1] + (255,)   # cut edge
    return img


def rune_pixels():
    """Every carved rune pixel on the gatestone, top line first."""
    for y0, glyphs in RUNE_LINES:
        for gx, g in zip(RUNE_X, glyphs):
            for dy, row in enumerate(RUNES[g]):
                for dx, ch in enumerate(row):
                    if ch == '#':
                        yield gx + dx, y0 + dy


def gatestone_glow(frames=24):
    """Emissive overlay for an attuned gate's ring: only the runes, drawn full-bright by the lit
    model. A slow glint runs down the three lines of script, so a whole ring of gatestones reads
    as one inscription waking up rather than as many separate blinking blocks."""
    strip = new(16, 16 * frames)
    px = strip.load()
    for f in range(frames):
        for x, y in rune_pixels():
            wave = 0.5 + 0.5 * math.cos(2 * math.pi * (f / frames) - y * 0.42)
            c = mix(TEAL[3], TEAL[5], 0.15 + 0.85 * wave ** 2)
            px[x, y + 16 * f] = c + (255,)
    return strip


KEY_GLYPH = [
    "................",
    ".#............#.",
    ".......##.......",
    "......#..#......",
    ".....#.##.#.....",
    "....#.#..#.#....",
    "...#.#.oo.#.#...",
    "..#.#.oOOo.#.#..",
    "..#.#.oOOo.#.#..",
    "...#.#.oo.#.#...",
    "....#.#..#.#....",
    ".....#.##.#.....",
    "......#..#......",
    ".......##.......",
    ".#............#.",
    "................",
]


def keystone_base(rng, lit):
    rng.seed(4242)
    img = polished(BASALT, rng, base_idx=2)
    px = img.load()
    # a raised bezel around the glyph
    for y in range(16):
        for x in range(16):
            d = abs(x - 7.5) + abs(y - 7.5)
            if 7.6 < d < 8.6:
                px[x, y] = (BASALT[4] if (x + y) < 15 else BASALT[1]) + (255,)
    for y in range(16):
        for x in range(16):
            ch = KEY_GLYPH[y][x]
            if ch == '#':
                px[x, y] = (TEAL[3] if lit else DULL[1]) + (255,)
            elif ch == 'o':
                px[x, y] = (TEAL[4] if lit else DULL[2]) + (255,)
            elif ch == 'O':
                px[x, y] = (TEAL[5] if lit else TEAL[0]) + (255,)
    return img


def keystone_glow(frames=16):
    """Emissive overlay: only the glyph, breathing. Drawn full-bright by the active model."""
    strip = new(16, 16 * frames)
    px = strip.load()
    for f in range(frames):
        k = 0.5 - 0.5 * math.cos(2 * math.pi * f / frames)          # 0..1..0
        for y in range(16):
            for x in range(16):
                ch = KEY_GLYPH[y][x]
                if ch == '.':
                    continue
                # the pulse travels inward along the channels
                d = abs(x - 7.5) + abs(y - 7.5)
                wave = 0.5 + 0.5 * math.cos(2 * math.pi * (f / frames) + d * 0.55)
                if ch == '#':
                    c = mix(TEAL[2], TEAL[4], 0.35 + 0.65 * wave)
                elif ch == 'o':
                    c = mix(TEAL[3], TEAL[5], 0.4 + 0.6 * k)
                else:
                    c = mix(TEAL[4], (255, 255, 255), 0.5 + 0.5 * k)
                px[x, y + 16 * f] = c + (255,)
    return strip


# --- the field -----------------------------------------------------------------------------

def field(frames=32):
    """Tileable in x, y and time: every wave has a whole number of periods across 16 px and across
    the loop, so a 5 x 7 opening shows no seams and the animation never jumps. Kept low-frequency
    and low-contrast on purpose -- one tile repeats 35 times across the opening, and anything busy
    turns into a visible grid. The motion rises, the way the network leads up and out."""
    strip = new(16, 16 * frames)
    px = strip.load()
    rng = random.Random(77)
    stars = [(rng.randrange(16), rng.randrange(16), rng.choice((1, 1, 2)), rng.random()) for _ in range(5)]
    tau = 2 * math.pi
    stops = [(0.0, TEAL[1]), (0.45, TEAL[2]), (0.75, TEAL[3]), (1.0, TEAL[4])]
    for f in range(frames):
        t = f / frames
        for y in range(16):
            for x in range(16):
                u, v = x / 16, y / 16
                wu = u + 0.10 * math.sin(tau * (v + t)) + 0.04 * math.sin(tau * (2 * v - t))
                wv = v + 0.10 * math.sin(tau * (u - t))
                s = (math.sin(tau * (wu + wv + t)) * 0.55
                     + math.sin(tau * (wu - 2 * wv + 2 * t)) * 0.30
                     + math.sin(tau * (2 * wu + wv - t)) * 0.15)
                s = (s + 1) / 2
                ridge = 1 - abs(2 * s - 1)
                val = 0.30 + 0.30 * s + 0.40 * ridge ** 6
                val = max(0.0, min(1.0, val))
                for i in range(len(stops) - 1):
                    if stops[i][0] <= val <= stops[i + 1][0]:
                        a, b = stops[i], stops[i + 1]
                        c = mix(a[1], b[1], (val - a[0]) / (b[0] - a[0]))
                        break
                px[x, y + 16 * f] = c + (int(165 + 70 * val),)
        # star glints rising through the field, wrapping seamlessly
        for sx, sy, speed, phase in stars:
            yy = int(round(sy - t * 16 * speed)) % 16
            tw = 0.5 + 0.5 * math.sin(tau * (t * 2 + phase))
            if tw > 0.45:
                px[sx, yy + 16 * f] = mix(TEAL[4], TEAL[5], tw) + (250,)
    return strip


# --- the Warden tendril --------------------------------------------------------------------

TENDRIL = [
    "..........k.....",
    ".........kck....",
    ".........kwck...",
    ".........kcck...",
    "........kcctk...",
    "........kcttk...",
    ".......kcwtgk...",
    ".......kcttgk...",
    "......kcctgtk...",
    ".....kcctgtk....",
    "....kcwtgttk....",
    "...kcttgttk.....",
    "..kcttgttk......",
    ".krrtttkk.......",
    ".krrrkk.........",
    "..kkk...........",
]
TENDRIL_PAL = {
    'k': (6, 22, 27),        # outline
    't': (17, 64, 72),       # tendril skin
    'g': (30, 108, 116),     # lit ridge
    'c': (58, 200, 196),     # bioluminescent tip
    'w': (205, 255, 246),    # hottest point
    'r': (52, 30, 36),       # torn root
}


def warden_tendril():
    img = new()
    px = img.load()
    for y in range(16):
        for x in range(16):
            ch = TENDRIL[y][x]
            if ch in TENDRIL_PAL:
                px[x, y] = TENDRIL_PAL[ch] + (255,)
    return img


# --- contact sheet -------------------------------------------------------------------------

def sheet(items, path, scale=8):
    cols = 5
    cell = 16 * scale + 24
    rows = (len(items) + cols - 1) // cols
    out = Image.new('RGBA', (cols * cell, rows * cell), (24, 26, 34, 255))
    for i, (name, img) in enumerate(items):
        tile = img.crop((0, 0, 16, 16)).resize((16 * scale, 16 * scale), Image.NEAREST)
        cx, cy = (i % cols) * cell + 12, (i // cols) * cell + 12
        bg = Image.new('RGBA', tile.size, (60, 64, 72, 255))
        out.paste(Image.alpha_composite(bg, tile), (cx, cy))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    out.save(path)


def main():
    rng = random.Random(1969)
    made = {}

    def put(rel, img):
        save(img, rel)
        made[rel] = img

    basalt_rows = [(4, 8, 0), (4, 8, 4), (4, 8, 0), (4, 8, 4)]
    anorth_rows = [(8, 8, 0), (8, 8, 4)]
    put('block/moon_basalt_bricks.png', bricks(BASALT, basalt_rows, random.Random(1)))
    put('block/cracked_moon_basalt_bricks.png',
        crack(bricks(BASALT, basalt_rows, random.Random(1)), BASALT[0], BASALT[3], random.Random(2),
              [((3, 0), 9), ((11, 5), 10), ((6, 9), 6)]))
    put('block/polished_moon_basalt.png', polished(BASALT, random.Random(3)))
    put('block/anorthosite_bricks.png', bricks(ANORTH, anorth_rows, random.Random(4), fleck=FLECK))
    put('block/cracked_anorthosite_bricks.png',
        crack(bricks(ANORTH, anorth_rows, random.Random(4), fleck=FLECK), ANORTH[0], ANORTH[4],
              random.Random(5), [((4, 0), 8), ((12, 7), 9), ((2, 9), 5)]))
    put('block/anorthosite_pillar.png', pillar_side(ANORTH, random.Random(6)))
    put('block/anorthosite_pillar_top.png', pillar_top(ANORTH, random.Random(7)))
    put('block/chiseled_anorthosite.png', chiseled(ANORTH, random.Random(8)))
    put('block/inscribed_gatestone.png', gatestone(random.Random(9)))
    put('block/inscribed_gatestone_glow.png', gatestone_glow())
    write_mcmeta('block/inscribed_gatestone_glow.png', frametime=3, interpolate=True)
    put('block/gateway_keystone.png', keystone_base(random.Random(), lit=False))
    put('block/gateway_keystone_active.png', keystone_base(random.Random(), lit=True))
    put('block/gateway_keystone_glow.png', keystone_glow())
    write_mcmeta('block/gateway_keystone_glow.png', frametime=3, interpolate=True)
    put('block/gateway_field.png', field())
    write_mcmeta('block/gateway_field.png', frametime=2, interpolate=True)
    put('item/warden_tendril.png', warden_tendril())

    sheet(list(made.items()), os.path.join(DESIGN, 'textures.png'))
    for rel in made:
        print('wrote', rel)


if __name__ == '__main__':
    main()
