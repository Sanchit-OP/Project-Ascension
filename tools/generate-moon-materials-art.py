"""Authoring helper for the worlds module's raw materials: the Moon's three stones, its helium ore,
Earth's titanium ore, their raw drops, the titanium ingot, and the gateway key. No build or runtime
dependency.

Run from the repository root:  python tools/generate-moon-materials-art.py
Needs Pillow. Rewrites every PNG it owns under
modules/worlds/src/main/resources/assets/ascension_worlds/textures/ and a contact sheet under
design/moon-materials/, so hand edits to those PNGs are lost on the next run -- edit this file.

Palettes and helpers are imported from tools/generate-gateway-art.py, not copied: the raw
anorthosite and basalt here and the bricks the gateway is dressed from must stay one material.

Everything is original procedural or hand-placed pixel art (docs/technical/moon-materials-art.md).
Nothing is traced or sampled from vanilla or any other pack; vanilla is only the yardstick for
tone (an Earth ore that spawns in the deepslate band should sit at deepslate's brightness).
"""
import importlib.util
import math
import os
import random

HERE = os.path.dirname(os.path.abspath(__file__))
_spec = importlib.util.spec_from_file_location('gateway_art', os.path.join(HERE, 'generate-gateway-art.py'))
art = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(art)

BASALT, ANORTH, FLECK, TEAL = art.BASALT, art.ANORTH, art.FLECK, art.TEAL
mix, shade, new, save = art.mix, art.shade, art.new, art.save
DESIGN = os.path.join(art.ROOT, 'design/moon-materials')

# Regolith is neither of the rocks it is ground from: a neutral grey, between pale highland and
# dark mare, so the surface reads as one dust over both.
REGOLITH = [(92, 91, 89), (116, 115, 112), (137, 136, 132), (154, 153, 148), (171, 170, 165),
            (191, 190, 185)]
# Helium: the Sun's element (named for Helios; first seen as a yellow line in the solar spectrum),
# so its crystals are sunlit amber -- warm against the Moon's cold stone, and nobody's teal.
HELIUM = [(112, 56, 28), (204, 124, 70), (244, 178, 120), (255, 218, 172), (255, 240, 216),
          (255, 254, 246)]
# Earth's deep band: a neutral layered grey at deepslate's brightness, where titanium spawns.
DEEP = [(34, 34, 39), (48, 48, 54), (62, 62, 69), (78, 78, 86), (96, 96, 104), (116, 116, 124)]
# Titanium: cold silver with the blue-violet of anodised metal.
TITAN = [(58, 58, 82), (104, 108, 140), (150, 156, 188), (196, 202, 226), (238, 242, 255)]
TINTS = [(150, 128, 204), (112, 168, 214)]


def ramp(pal, v):
    """Continuous lookup: v in 0..1 across the whole palette."""
    v = max(0.0, min(1.0, v)) * (len(pal) - 1)
    i = min(int(v), len(pal) - 2)
    return mix(pal[i], pal[i + 1], v - i)


def tile_noise(rng, cells):
    """Smooth 0..1 noise that wraps at 16 px: a cells x cells lattice, cosine-interpolated."""
    g = [[rng.random() for _ in range(cells)] for _ in range(cells)]
    out = [[0.0] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            fx, fy = x * cells / 16, y * cells / 16
            x0, y0 = int(fx), int(fy)
            tx = (1 - math.cos((fx - x0) * math.pi)) / 2
            ty = (1 - math.cos((fy - y0) * math.pi)) / 2
            a, b = g[y0 % cells][x0 % cells], g[y0 % cells][(x0 + 1) % cells]
            c, d = g[(y0 + 1) % cells][x0 % cells], g[(y0 + 1) % cells][(x0 + 1) % cells]
            out[y][x] = (a * (1 - tx) + b * tx) * (1 - ty) + (c * (1 - tx) + d * tx) * ty
    return out


def wrap(x, y):
    return x % 16, y % 16


# --- the three stones ---------------------------------------------------------------------

def regolith(rng):
    """The surface of the whole planet, tiled over every hill and crater: fine grain, low
    contrast, and few enough features that 30 tiles in a row never show a grid."""
    img = new()
    px = img.load()
    low = tile_noise(rng, 4)
    for y in range(16):
        for x in range(16):
            v = 0.52 + (low[y][x] - 0.5) * 0.30 + rng.uniform(-0.20, 0.20)
            px[x, y] = ramp(REGOLITH, v) + (255,)
    for _ in range(4):                                   # small rock fragments, lit top-left
        x, y = rng.randrange(16), rng.randrange(16)
        px[wrap(x, y)] = REGOLITH[4] + (255,)
        px[wrap(x + 1, y + 1)] = REGOLITH[1] + (255,)
    # impact-glass beads: one grey-glossy, one faintly orange (Apollo 17's "orange soil")
    px[wrap(rng.randrange(16), rng.randrange(16))] = REGOLITH[5] + (255,)
    px[wrap(rng.randrange(16), rng.randrange(16))] = mix(REGOLITH[3], (196, 140, 92), 0.45) + (255,)
    return img


def anorthosite(rng):
    """Highland rock: impact breccia. Pale feldspar clasts, broken and re-cemented by four billion
    years of impacts, in a thin grey matrix -- not vanilla diorite's salt-and-pepper."""
    img = new()
    px = img.load()
    seeds = [(rng.uniform(0, 16), rng.uniform(0, 16), rng.choice((0.56, 0.62, 0.68, 0.74, 0.80)))
             for _ in range(12)]

    def tdist(ax, ay, bx, by):
        dx = min(abs(ax - bx), 16 - abs(ax - bx))
        dy = min(abs(ay - by), 16 - abs(ay - by))
        return math.hypot(dx, dy)

    for y in range(16):
        for x in range(16):
            ds = sorted((tdist(x + 0.5, y + 0.5, sx, sy), tone) for sx, sy, tone in seeds)
            (d1, tone), (d2, _) = ds[0], ds[1]
            if d2 - d1 < 0.6:
                c = ramp(ANORTH, 0.44 + rng.uniform(-0.04, 0.04))        # matrix seam
            else:
                c = ramp(ANORTH, tone + rng.uniform(-0.04, 0.04))
            r = rng.random()
            if r < 0.015:
                c = mix(BASALT[4], ANORTH[1], 0.6)                        # pyroxene grain
            elif r < 0.045:
                c = mix(c, FLECK, 0.5)                                    # feldspar glint
            px[x, y] = c + (255,)
    return img


def moon_basalt(rng):
    """Mare rock: fine-grained and vesicular -- frozen gas bubbles from when the seas were lava.
    Vanilla basalt's columns are a terrestrial thing; these are pits, lit on their far rim."""
    img = new()
    px = img.load()
    low = tile_noise(rng, 4)
    for y in range(16):
        for x in range(16):
            v = 0.44 + (low[y][x] - 0.5) * 0.22 + rng.uniform(-0.09, 0.09)
            px[x, y] = ramp(BASALT, v) + (255,)
    for _ in range(8):                                    # vesicles
        x, y = rng.randrange(16), rng.randrange(16)
        big = rng.random() < 0.3
        pit = [(0, 0), (1, 0), (0, 1), (1, 1)] if big else [(0, 0)]
        for dx, dy in pit:
            px[wrap(x + dx, y + dy)] = BASALT[0] + (255,)
        far = 2 if big else 1
        px[wrap(x + far, y + far)] = mix(BASALT[3], BASALT[4], 0.5) + (255,)   # lit far rim
        px[wrap(x - 1, y - 1)] = BASALT[1] + (255,)           # shadowed near wall
    for _ in range(2):                                    # plagioclase laths
        x, y = rng.randrange(16), rng.randrange(16)
        c = mix(BASALT[3], ANORTH[1], 0.3)
        px[wrap(x, y)] = c + (255,)
        px[wrap(x + 1, y - 1)] = c + (255,)
    for _ in range(3):                                    # ilmenite glints
        px[wrap(rng.randrange(16), rng.randrange(16))] = BASALT[5] + (255,)
    return img


# --- ores ----------------------------------------------------------------------------------

# Amber prisms, lit from the upper left: o edge, a body, b lit face, c highlight, w/W white-hot.
HELIUM_CRYSTALS = [
    "................",
    "...W............",
    "..wcb.......W...",
    "..cbba.....wcb..",
    "..cbao.W...cba..",
    "...o..wcb..cbo..",
    "......cba...o...",
    "......bo........",
    "................",
    ".........W......",
    "....W...wcb.....",
    "...wcb..cbba....",
    "...cba..cbao.W..",
    "...cbo...bo.wcb.",
    "....o.......cbo.",
    ".............o..",
]
HE_KEY = {'o': 0, 'a': 1, 'b': 2, 'c': 3, 'w': 4, 'W': 5}


def lunar_helium_ore(rng):
    """Host is the rock the ore actually sits in. It spawns at y -64..24, below the Moon's thin
    anorthosite and basalt surface layers, where the planet is deepslate -- so it shares
    titanium's deep-band host rather than looking like a dark square cut into the wall."""
    img = deep_stone(rng)
    px = img.load()
    for y, row in enumerate(HELIUM_CRYSTALS):
        for x, ch in enumerate(row):
            if ch in HE_KEY:
                px[x, y] = HELIUM[HE_KEY[ch]] + (255,)
                # a faint warm halo where light from the crystal falls on the stone
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = wrap(x + dx, y + dy)
                    if HELIUM_CRYSTALS[ny][nx] == '.':
                        r, g, b, _ = px[nx, ny]
                        px[nx, ny] = mix((r, g, b), HELIUM[1], 0.10) + (255,)
    return img


def lunar_helium_glow():
    """Emissive overlay: the crystal bodies only (edges stay dark, so the shape survives in a
    pitch-black cave). Static -- an ore is everywhere, and does not need to shimmer."""
    img = new()
    px = img.load()
    for y, row in enumerate(HELIUM_CRYSTALS):
        for x, ch in enumerate(row):
            if ch in HE_KEY and ch != 'o':
                px[x, y] = HELIUM[HE_KEY[ch]] + (255,)
    return img


def deep_stone(rng):
    """Earth's deep band, our own: horizontal strata, the way pressure lays rock down."""
    img = new()
    px = img.load()
    band = [rng.uniform(-0.06, 0.06) for _ in range(16)]
    for y in range(16):
        band[y] = (band[y] + band[(y - 1) % 16] + band[(y + 1) % 16]) / 3
    for y in range(16):
        for x in range(16):
            v = 0.45 + band[y] + rng.uniform(-0.08, 0.08)
            px[x, y] = ramp(DEEP, v) + (255,)
    for _ in range(6):                                    # strata streaks
        x, y = rng.randrange(16), rng.randrange(16)
        lit = rng.random() < 0.5
        for i in range(rng.randrange(3, 7)):
            nx, ny = wrap(x + i, y)
            px[nx, ny] = (DEEP[4] if lit else DEEP[1]) + (255,)
            if lit:
                px[wrap(nx, ny + 1)] = DEEP[1] + (255,)
    return img


def grains(rng, count, avoid):
    """Random small blobs, wrapped, kept a pixel apart from each other and from `avoid`."""
    taken = set(avoid)
    blobs = []
    shapes = [[(0, 0), (1, 0), (0, 1), (1, 1)], [(0, 0), (1, 0), (2, 0), (1, 1)],
              [(0, 0), (1, 0), (0, 1), (1, 1), (2, 1)], [(1, 0), (2, 0), (0, 1), (1, 1), (1, 2)],
              [(0, 0), (1, 0), (1, 1)]]
    tries = 0
    while len(blobs) < count and tries < 500:
        tries += 1
        ox, oy = rng.randrange(16), rng.randrange(16)
        cells = [wrap(ox + dx, oy + dy) for dx, dy in rng.choice(shapes)]
        halo = {wrap(cx + dx, cy + dy) for cx, cy in cells for dx in (-1, 0, 1) for dy in (-1, 0, 1)}
        if halo & taken:
            continue
        taken |= halo
        blobs.append(cells)
    return blobs


def titanium_ore(rng):
    """Silver grains with an anodised blue-violet sheen, in deepslate-dark strata: titanium
    spawns at y -64..0, the deep band, so a stone-grey host would look wrong where it is found."""
    img = deep_stone(rng)
    px = img.load()
    for cells in grains(rng, 6, []):
        s = set(cells)
        for x, y in cells:
            up_left = wrap(x - 1, y) not in s or wrap(x, y - 1) not in s
            down_right = wrap(x + 1, y) not in s or wrap(x, y + 1) not in s
            if up_left and not down_right:
                c = TITAN[3]
            elif down_right and not up_left:
                c = TITAN[1]
            else:
                c = TITAN[2]
            px[x, y] = c + (255,)
            # a dark rim on the shadow side, so grains sit in the rock rather than on it
            for dx, dy in ((1, 0), (0, 1)):
                n = wrap(x + dx, y + dy)
                if n not in s:
                    px[n] = TITAN[0] + (255,)
        top = min(cells, key=lambda c: (c[0] + c[1], c[1]))
        px[top] = TITAN[4] + (255,)                        # specular
        if rng.random() < 0.6:
            px[rng.choice(cells)] = rng.choice(TINTS) + (255,)
    return img


# --- items ---------------------------------------------------------------------------------

# k outline, r/R host rock, then the HE_KEY crystal tones.
RAW_HELIUM = [
    "................",
    ".......kk.......",
    "......kWck......",
    "......kwbk......",
    "..kk..kcbak.kk..",
    ".kWck.kcbak.kWk.",
    ".kwbak.kcbakkcbk",
    "..kcbak.kcbakcok",
    "..kcbokkkcbokbk.",
    "...kbokRRkbokRk.",
    "..kkokRRrrkkRrrk",
    ".kRRkRrrrrrRrrrk",
    ".kRrrrrrrrrrrrk.",
    "..krrrrrrrrrrk..",
    "...kkkkkkkkkk...",
    "................",
]
RAW_HELIUM_PAL = {'k': (52, 22, 14), 'r': BASALT[2], 'R': BASALT[4]}


def raw_helium():
    img = new()
    px = img.load()
    for y, row in enumerate(RAW_HELIUM):
        for x, ch in enumerate(row[:16]):
            if ch in RAW_HELIUM_PAL:
                px[x, y] = RAW_HELIUM_PAL[ch] + (255,)
            elif ch in HE_KEY:
                px[x, y] = HELIUM[HE_KEY[ch]] + (255,)
    return img


def outline_and_shade(mask, light, mid, dark, edge):
    """Pixel-art finish for a filled shape: lit where it faces up-left, shadowed where it faces
    down-right, and a one-pixel outline around the outside."""
    img = new()
    px = img.load()
    for (x, y) in mask:
        ul = (x - 1, y) not in mask or (x, y - 1) not in mask
        dr = (x + 1, y) not in mask or (x, y + 1) not in mask
        px[x, y] = (light if ul and not dr else dark if dr and not ul else mid) + (255,)
    for x in range(16):
        for y in range(16):
            if (x, y) not in mask and any((x + dx, y + dy) in mask
                                          for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                px[x, y] = edge + (255,)
    return img


RAW_TITANIUM_MASK = [
    "................",
    "................",
    ".......###......",
    "......#####.....",
    "...########.....",
    "..##########....",
    "..###########...",
    ".############...",
    ".#############..",
    "..#############.",
    "..############..",
    "...###########..",
    "....#########...",
    ".....#######....",
    "................",
    "................",
]


def raw_titanium(rng):
    """A rough, faceted chunk: metal faces broken out of dark matrix, not a smooth nugget."""
    mask = {(x, y) for y, row in enumerate(RAW_TITANIUM_MASK) for x, ch in enumerate(row) if ch == '#'}
    img = outline_and_shade(mask, TITAN[3], TITAN[2], TITAN[1], (30, 30, 44))
    px = img.load()
    # facets: diagonal bands of light and shade across the chunk
    for (x, y) in mask:
        if px[x, y][:3] != TITAN[2]:
            continue
        if x + y < 11:
            px[x, y] = TITAN[3] + (255,)                   # upper facet, facing the light
        elif x - y > 3:
            px[x, y] = TITAN[1] + (255,)                   # right facet, turned away
        elif x + y > 17:
            px[x, y] = mix(TITAN[1], TITAN[0], 0.4) + (255,)
    for x, y in [(x, y) for (x, y) in mask if x + y == 11 and (x + 1, y + 1) in mask]:
        px[x, y] = TITAN[4] + (255,)                       # the ridge between facets
    for _ in range(5):                                     # matrix pits
        x, y = rng.choice(sorted(mask))
        if all((x + dx, y + dy) in mask for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            px[x, y] = DEEP[1] + (255,)
    px[11, 9] = TINTS[0] + (255,)
    px[6, 11] = TINTS[1] + (255,)
    return img


def titanium_ingot():
    """The smelted metal: a clean bar, where the raw chunk is faceted and pitted. Drawn as a box in
    oblique view, depth running up and to the right, so its three faces take the light from the
    top-left: lit top, mid front, shadowed end. Its sheen is the raw chunk's anodised blue-violet,
    so the two read as one material."""
    left, length, base, height, depth = 1, 11, 11, 4, 3
    right = left + length - 1
    front = {(x, y) for x in range(left, right + 1) for y in range(base - height + 1, base + 1)}
    top = {(x + d, base - height + 1 - d) for d in range(1, depth + 1) for x in range(left, right + 1)}
    end = {(right + d, y - d) for d in range(1, depth + 1) for y in range(base - height + 2, base + 1)}
    end -= top
    img = new()
    px = img.load()
    for (x, y) in top:
        px[x, y] = TITAN[3] + (255,)
    for (x, y) in front:
        px[x, y] = TITAN[2] + (255,)
    for (x, y) in end:
        px[x, y] = TITAN[1] + (255,)
    edge_y = base - height + 1
    for x in range(left, right + 1):                       # the lit edge where top meets front
        px[x, edge_y] = mix(TITAN[3], TITAN[2], 0.4) + (255,)
    for x in range(left, right + 1):                       # bottom rim, turned from the light
        px[x, base] = mix(TITAN[1], TITAN[2], 0.5) + (255,)
    px[left + 1, edge_y - 1] = TITAN[4] + (255,)           # specular glint on the near corner
    px[left + 2, edge_y - 1] = mix(TITAN[4], TITAN[3], 0.5) + (255,)
    px[left + 6, edge_y + 2] = mix(TINTS[0], TITAN[2], 0.4) + (255,)   # anodised sheen
    px[left + 7, edge_y + 2] = mix(TINTS[1], TITAN[2], 0.4) + (255,)
    body = top | front | end
    for x in range(16):
        for y in range(16):
            if (x, y) not in body and any((x + dx, y + dy) in body
                                          for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                px[x, y] = (30, 30, 44, 255)
    return img


def gateway_key():
    """The Moon boss's drop, in the network's own language: a pale stone key whose bow is the
    gateway's floating eye, with a teal pupil and a run of rune inlay down the shaft."""
    body = set()
    cx, cy, r = 4, 4, 3.6
    for y in range(16):
        for x in range(16):
            d = abs(x + 0.5 - cx - 0.5) + abs(y + 0.5 - cy - 0.5)
            if r - 1.6 <= d <= r:
                body.add((x, y))                                     # hollow diamond bow
    for i in range(7, 15):                                           # shaft, two wide, diagonal
        body.add((i, i))
        body.add((i - 1, i))
    for i in (10, 13):                                               # bit: two teeth
        body.add((i - 2, i + 1))
        body.add((i - 3, i + 2))
    img = outline_and_shade(body, ANORTH[5], ANORTH[3], ANORTH[1], (40, 40, 46))
    px = img.load()
    for (x, y) in ((4, 4), (5, 4), (4, 5), (5, 5)):                  # pupil
        px[x, y] = TEAL[3] + (255,)
    px[4, 4] = TEAL[5] + (255,)
    for (x, y) in ((4, 3), (3, 4), (6, 5), (5, 6)):
        px[x, y] = TEAL[1] + (255,)
    for i in (8, 10, 12):                                            # rune inlay on the shaft
        px[i, i] = TEAL[2] + (255,)
    return img


# --- contact sheet -------------------------------------------------------------------------

def main():
    made = {}

    def put(rel, img):
        save(img, rel)
        made[rel] = img

    put('block/moon_regolith.png', regolith(random.Random(11)))
    put('block/anorthosite.png', anorthosite(random.Random(12)))
    put('block/moon_basalt.png', moon_basalt(random.Random(13)))
    put('block/lunar_helium_ore.png', lunar_helium_ore(random.Random(14)))
    put('block/lunar_helium_ore_glow.png', lunar_helium_glow())
    put('block/titanium_ore.png', titanium_ore(random.Random(15)))
    put('item/raw_helium.png', raw_helium())
    put('item/raw_titanium.png', raw_titanium(random.Random(16)))
    put('item/titanium_ingot.png', titanium_ingot())
    put('item/gateway_key.png', gateway_key())

    art.sheet(list(made.items()), os.path.join(DESIGN, 'textures.png'))
    in_context(made)
    for rel in made:
        print('wrote', rel)


def in_context(made, n=6, scale=2):
    """Each stone tiled n x n (a single texture's repeats are what a cliff face shows), then each
    ore as a vein in its own deep-band host. Only this file's art, so it is safe to commit."""
    from PIL import Image

    def patch(tile_of):
        img = Image.new('RGBA', (16 * n, 16 * n))
        for i in range(n):
            for j in range(n):
                img.paste(tile_of(i, j), (i * 16, j * 16))
        return img.resize((16 * n * scale, 16 * n * scale), Image.NEAREST)

    host = deep_stone(random.Random(15))
    vein = {(2, 2), (3, 2), (3, 3), (2, 4), (4, 3)}
    panels = [patch(lambda i, j, r=r: made[r].crop((0, 0, 16, 16))) for r in
              ('block/moon_regolith.png', 'block/anorthosite.png', 'block/moon_basalt.png')]
    for ore in ('block/lunar_helium_ore.png', 'block/titanium_ore.png'):
        panels.append(patch(lambda i, j, ore=ore: made[ore] if (i, j) in vein else host))
    gap = 12
    out = Image.new('RGBA', (sum(p.width for p in panels) + gap * (len(panels) - 1), panels[0].height),
                    (24, 26, 34, 255))
    x = 0
    for p in panels:
        out.paste(p, (x, 0))
        x += p.width + gap
    out.save(os.path.join(DESIGN, 'in-context.png'))


if __name__ == '__main__':
    main()
