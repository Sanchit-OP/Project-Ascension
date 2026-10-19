#!/usr/bin/env python3
"""Quest book art (docs/progression/quests.md). Original, seeded, smooth: nothing copied from FTB Quests,
another mod or a pack. Needs Pillow and numpy.

    python tools/generate-quest-art.py

Writes, under modules/compat/ftbquests/src/main/resources/assets/:

  ftbquests/ftb_quests_theme.txt             the book's colours and frame; Quests stacks every pack's copy
  ftbquests/textures/shapes/<name>/          our three quest shapes (shape / outline / background, 128 px)
  ascension_compat_ftbquests/textures/quests/
      theme/background.png                   a seamless night-sky tile behind every page
      planets/chart.png                      the Planets page: night sky, distance rings, bearings, brass frame
      planets/{earth,moon,nether}.png        a lit sphere for each world (from the planet files' colours)
      planets/ring.png                       the faint ring of a world not yet known
      planets/halo.png                       the teal halo of a world that has been visited
      icons/{first_days,planets,road_up,the_moon,the_rot,the_belt,gear_and_growth,together,
             the_gate,create_power,create_craft,ship_build,ship_fly}.png   chapter icons
      gate/plan.png                          Earth's gate: a view, a key and every layer (from gateway.json)

The look is the orrery's: a night sky in a brass frame (tools/generate-rune-art.py), and teal, which is
the network's colour (tools/generate-gateway-art.py), is spent only on worlds and gates.
Deterministic: every random choice comes from a fixed seed, so a rerun changes nothing.
"""

import importlib.util
import json
import math
import os
import random
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, '..'))
sys.path.insert(0, HERE)

from quests import chart  # noqa: E402


def _load(name, filename):
    spec = importlib.util.spec_from_file_location(name, os.path.join(HERE, filename))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


# Palettes are imported, never copied (art-guide.md).
_runes = _load('rune_art', 'generate-rune-art.py')
_gateway = _load('gateway_art', 'generate-gateway-art.py')
SKY = [c[:3] for c in _runes.SKY]
BRASS = [c[:3] for c in _runes.STAR_BRASS]
TEAL = [tuple(c) for c in _gateway.TEAL]

ASSETS = os.path.join(ROOT, 'modules', 'compat', 'ftbquests', 'src', 'main', 'resources', 'assets')
NS = os.path.join(ASSETS, 'ascension_compat_ftbquests', 'textures', 'quests')
FTB = os.path.join(ASSETS, 'ftbquests')
FONT = os.path.join(ROOT, 'modules', 'hud', 'src', 'main', 'resources', 'assets', 'ascension_hud', 'font',
                    'barlow_semibold.ttf')

CREAM = (246, 238, 214)


def save(image, path, blur=True):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    image.save(path)
    if blur:
        with open(path + '.mcmeta', 'w', encoding='utf-8', newline='\n') as handle:
            handle.write('{\n  "texture": {\n    "blur": true\n  }\n}\n')


def smoothstep(a, b, x):
    t = np.clip((x - a) / (b - a), 0.0, 1.0)
    return t * t * (3 - 2 * t)


def downsample(image, size):
    return image.resize((size, size), Image.LANCZOS)


# ----------------------------------------------------------------------------------------------
# Quest shapes: shape.png (a mask), outline.png (its edge), background.png (a soft fill).
# All white and grey: the book tints them by how far the quest has got.
# ----------------------------------------------------------------------------------------------

SHAPE = 128
SS = 4


def astroid_points(k, n=240):
    """A star with concave sides: |x|^(2/k) + |y|^(2/k) = 1, scaled to the unit square."""
    pts = []
    for i in range(n):
        t = 2 * math.pi * i / n
        c, s = math.cos(t), math.sin(t)
        pts.append((math.copysign(abs(c) ** k, c), math.copysign(abs(s) ** k, s)))
    return pts


def star_points(n, outer, inner, rotation=-90.0):
    pts = []
    for i in range(2 * n):
        r = outer if i % 2 == 0 else inner
        a = math.radians(rotation + i * 180.0 / n)
        pts.append((r * math.cos(a), r * math.sin(a)))
    return pts


def diamond_points():
    return [(0.0, -1.0), (0.80, 0.0), (0.0, 1.0), (-0.80, 0.0)]


def _canvas():
    return SHAPE * SS


def _to_px(pts, margin=0.06):
    half = _canvas() / 2.0
    scale = half * (1.0 - margin)
    return [(half + x * scale, half + y * scale) for x, y in pts]


def shape_images(points, iris=False):
    size = _canvas()
    mask = Image.new('L', (size, size), 0)
    ImageDraw.Draw(mask).polygon(_to_px(points), fill=255)

    # shape.png: the mask itself.
    shape = Image.new('RGBA', (size, size), (255, 255, 255, 0))
    shape.putalpha(mask)

    # outline.png: a band around the edge, drawn inside the mask's silhouette.
    inner = mask.filter(ImageFilter.MinFilter(2 * 3 * SS // 2 * 2 + 1))
    edge = Image.fromarray((np.asarray(mask, dtype=np.int16) - np.asarray(inner, dtype=np.int16)).clip(0, 255)
                           .astype(np.uint8))
    outline = Image.new('RGBA', (size, size), (255, 255, 255, 0))
    outline.putalpha(edge)

    # background.png: bright in the middle, falling to grey at the edge, as the stock fills do.
    ys, xs = np.mgrid[0:size, 0:size]
    d = np.hypot(xs + 0.5 - size / 2, ys + 0.5 - size / 2) / (size / 2)
    value = 255.0 - 125.0 * smoothstep(0.0, 1.0, d) ** 0.9
    if iris:
        # The network's eye: a darker iris ring and a bright pupil.
        value = value - 70.0 * np.exp(-((d - 0.34) / 0.07) ** 2)
        value = value + 60.0 * np.exp(-(d / 0.12) ** 2)
    rgba = np.dstack([np.clip(value, 0, 255)] * 3 + [np.asarray(mask, dtype=np.float64)]).astype(np.uint8)
    background = Image.fromarray(rgba, 'RGBA')

    return downsample(shape, SHAPE), downsample(outline, SHAPE), downsample(background, SHAPE)


def write_shapes():
    shapes = {
        'ascension_star': (astroid_points(2.4), False),                  # lesson: a four-pointed star-stone
        'ascension_nova': (star_points(8, 1.0, 0.62), False),            # goal: eight points
        'ascension_eye': (diamond_points(), True),                       # landmark: the network's diamond eye
    }
    for name, (points, iris) in shapes.items():
        shape, outline, background = shape_images(points, iris)
        folder = os.path.join(FTB, 'textures', 'shapes', name)
        save(shape, os.path.join(folder, 'shape.png'))
        save(outline, os.path.join(folder, 'outline.png'))
        save(background, os.path.join(folder, 'background.png'))


# ----------------------------------------------------------------------------------------------
# The sky behind every page
# ----------------------------------------------------------------------------------------------

def write_background(size=256):
    rng = random.Random(7001)
    image = Image.new('RGB', (size, size), SKY[1])
    draw = ImageDraw.Draw(image)
    for _ in range(46):
        x, y = rng.randrange(size), rng.randrange(size)
        tone = rng.choice([BRASS[2], CREAM, (170, 190, 230)])
        level = rng.uniform(0.25, 0.7)
        colour = tuple(int(SKY[1][i] + (tone[i] - SKY[1][i]) * level) for i in range(3))
        # Drawn at nine offsets so the tile has no seam.
        for ox in (-size, 0, size):
            for oy in (-size, 0, size):
                draw.point((x + ox, y + oy), fill=colour)
        if rng.random() < 0.18:
            for ox in (-size, 0, size):
                for oy in (-size, 0, size):
                    draw.point((x + ox + 1, y + oy), fill=colour)
    save(image, os.path.join(NS, 'theme', 'background.png'), blur=False)


# ----------------------------------------------------------------------------------------------
# The Planets chart
# ----------------------------------------------------------------------------------------------

def _font(size):
    return ImageFont.truetype(FONT, size)


def write_chart():
    size = chart.SIZE
    k = 3                                # supersample the line work
    big = size * k
    cx, cy = chart.CENTER[0] * k, chart.CENTER[1] * k
    disc_r = 478 * k

    ys, xs = np.mgrid[0:big, 0:big]
    d = np.hypot(xs + 0.5 - cx, ys + 0.5 - cy) / disc_r
    inside = d <= 1.0
    # The sky: lighter toward the middle, near-black at the edge.
    t = smoothstep(0.0, 1.0, d)
    sky = np.zeros((big, big, 4), dtype=np.float64)
    for i in range(3):
        sky[..., i] = SKY[2][i] + (SKY[0][i] - SKY[2][i]) * t
    sky[..., 3] = np.where(inside, 244.0, 0.0)
    base = Image.fromarray(sky.astype(np.uint8), 'RGBA')

    layer = Image.new('RGBA', (big, big), (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)

    # Stars, kept off the rim.
    rng = random.Random(7002)
    for _ in range(330):
        a = rng.uniform(0, 2 * math.pi)
        r = disc_r * math.sqrt(rng.uniform(0.0, 0.94))
        x, y = cx + r * math.cos(a), cy + r * math.sin(a)
        tone = rng.choice([BRASS[3], CREAM, (176, 196, 236)])
        alpha = int(rng.uniform(40, 170))
        s = rng.choice([1, 1, 1, 2, 2, 3]) * k * 0.5
        draw.ellipse((x - s, y - s, x + s, y + s), fill=tone + (alpha,))

    # Faint axes through Earth.
    axis = BRASS[1] + (46,)
    draw.line((cx - disc_r, cy, cx + disc_r, cy), fill=axis, width=k)
    draw.line((cx, cy - disc_r, cx, cy + disc_r), fill=axis, width=k)

    # Distance rings at true distances, on the chart's own curve.
    for blocks in (5000, 10000, 20000, 40000):
        r = chart.RADIUS * math.sqrt(blocks / chart.RANGE_BLOCKS) * k
        segments = 160
        for s in range(segments):
            if s % 2:
                continue
            a0 = 360.0 * s / segments
            a1 = 360.0 * (s + 1) / segments
            draw.arc((cx - r, cy - r, cx + r, cy + r), a0, a1, fill=BRASS[2] + (88,), width=int(1.6 * k))
    # The edge of the chart (the range) is a firm line.
    r = chart.RADIUS * k
    draw.ellipse((cx - r, cy - r, cx + r, cy + r), outline=BRASS[2] + (150,), width=int(2.2 * k))

    # Bearing ticks on the rim: every 5 degrees, longer every 15.
    for deg in range(0, 360, 5):
        a = math.radians(deg - 90)
        long_tick = deg % 15 == 0
        r0 = disc_r - (22 if long_tick else 12) * k
        r1 = disc_r - 3 * k
        draw.line((cx + r0 * math.cos(a), cy + r0 * math.sin(a), cx + r1 * math.cos(a), cy + r1 * math.sin(a)),
                  fill=BRASS[2] + (200 if long_tick else 120,), width=int((2.2 if long_tick else 1.4) * k))

    # The brass frame: a double rim with a diamond (the network's eye) at each compass point.
    for radius, width, colour in ((disc_r + 2 * k, 3, BRASS[2]), (disc_r + 9 * k, 2, BRASS[1]),
                                  (disc_r + 13 * k, 1.4, BRASS[0])):
        draw.ellipse((cx - radius, cy - radius, cx + radius, cy + radius), outline=colour + (255,),
                     width=int(width * k))
    for deg in (0, 90, 180, 270):
        a = math.radians(deg - 90)
        px, py = cx + (disc_r + 8 * k) * math.cos(a), cy + (disc_r + 8 * k) * math.sin(a)
        h = 11 * k
        draw.polygon([(px, py - h), (px + h * 0.8, py), (px, py + h), (px - h * 0.8, py)],
                     fill=SKY[0] + (255,), outline=BRASS[3] + (255,))

    image = Image.alpha_composite(base, layer)
    image = downsample(image, size)

    # Text is drawn after the downsample, in the final pixels, so it stays sharp.
    draw = ImageDraw.Draw(image)
    small, tiny = _font(21), _font(16)
    for blocks, label in ((5000, '5K'), (10000, '10K'), (20000, '20K'), (40000, '40K')):
        r = chart.RADIUS * math.sqrt(blocks / chart.RANGE_BLOCKS)
        a = math.radians(-38)
        x, y = chart.CENTER[0] + r * math.cos(a), chart.CENTER[1] + r * math.sin(a)
        draw.text((x + 4, y - 18), label, font=tiny, fill=BRASS[2] + (200,))
    for text, deg in (('N', 0), ('E', 90), ('S', 180), ('W', 270)):
        a = math.radians(deg - 90)
        r = 478 - 40
        x, y = chart.CENTER[0] + r * math.cos(a), chart.CENTER[1] + r * math.sin(a)
        draw.text((x, y), text, font=small, fill=BRASS[3] + (240,), anchor='mm')

    save(image, os.path.join(NS, 'planets', 'chart.png'))


# ----------------------------------------------------------------------------------------------
# Worlds: lit spheres, drawn from noise on the sphere itself
# ----------------------------------------------------------------------------------------------

def _lattice(seed, n=64):
    return np.random.default_rng(seed).random((n, n, n))


def fbm(x, y, z, seed, octaves=5, base=2.0):
    """3-D value noise, summed over octaves, in [0, 1]."""
    table = _lattice(seed)
    n = table.shape[0]
    total = np.zeros_like(x)
    amplitude, norm, freq = 1.0, 0.0, base
    for _ in range(octaves):
        px, py, pz = x * freq, y * freq, z * freq
        x0, y0, z0 = np.floor(px).astype(int), np.floor(py).astype(int), np.floor(pz).astype(int)
        fx, fy, fz = px - x0, py - y0, pz - z0
        fx, fy, fz = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy), fz * fz * (3 - 2 * fz)
        acc = 0.0
        for dx in (0, 1):
            for dy in (0, 1):
                for dz in (0, 1):
                    v = table[(x0 + dx) % n, (y0 + dy) % n, (z0 + dz) % n]
                    w = (fx if dx else 1 - fx) * (fy if dy else 1 - fy) * (fz if dz else 1 - fz)
                    acc = acc + v * w
        total = total + acc * amplitude
        norm += amplitude
        amplitude *= 0.5
        freq *= 2.0
    return total / norm


def _sphere(n):
    ys, xs = np.mgrid[0:n, 0:n]
    x = (xs + 0.5) / n * 2 - 1
    y = (ys + 0.5) / n * 2 - 1
    r2 = x * x + y * y
    mask = r2 <= 1.0
    z = np.sqrt(np.clip(1.0 - r2, 0.0, 1.0))
    return mask, x, y, z


LIGHT = np.array([-0.52, -0.58, 0.63])
LIGHT = LIGHT / np.linalg.norm(LIGHT)


def _mixc(a, b, t):
    t = t[..., None]
    return np.asarray(a, dtype=np.float64) * (1 - t) + np.asarray(b, dtype=np.float64) * t


def planet_sprite(style, colour, seed, out=256):
    n = out * 2
    mask, x, y, z = _sphere(n)
    spin = 0.9 * seed
    px, py, pz = x * math.cos(spin) + z * math.sin(spin), y, -x * math.sin(spin) + z * math.cos(spin)
    base = np.array([(colour >> 16) & 0xFF, (colour >> 8) & 0xFF, colour & 0xFF], dtype=np.float64)

    dot = x * LIGHT[0] + y * LIGHT[1] + z * LIGHT[2]
    lit = smoothstep(-0.18, 0.85, dot)
    emissive = np.zeros((n, n, 3))
    rim = np.zeros((n, n, 3))

    if style == 'earth':
        land_noise = fbm(px + 3.1, py + 1.7, pz + 2.3, 11, 6, 1.6)
        land = land_noise > 0.54
        ocean = _mixc(base * 0.55, base, fbm(px, py, pz, 12, 4, 3.0))
        land_col = _mixc((58, 118, 62), (128, 108, 70), smoothstep(0.45, 0.8, fbm(px, py, pz, 13, 4, 4.0)))
        albedo = np.where(land[..., None], land_col, ocean)
        ice = smoothstep(0.82, 0.93, np.abs(py))
        albedo = _mixc(albedo, (238, 244, 250), ice)
        cloud = smoothstep(0.55, 0.72, fbm(px + 9, py + 4, pz + 1, 14, 5, 2.4))
        albedo = _mixc(albedo, (250, 252, 255), cloud * 0.72)
        edge = (1 - z) ** 2.4
        rim = edge[..., None] * np.array([70, 140, 255]) * 0.9
    elif style == 'moon':
        patches = smoothstep(0.42, 0.62, fbm(px + 5, py + 2, pz + 8, 21, 4, 1.3))
        grain = fbm(px, py, pz, 22, 5, 6.0)
        tone = 0.78 + 0.22 * grain
        albedo = base[None, None, :] * tone[..., None]
        albedo = albedo * (1 - 0.30 * patches[..., None])
        rng = np.random.default_rng(23)
        for _ in range(70):
            v = rng.normal(size=3)
            v /= np.linalg.norm(v)
            r = rng.uniform(0.04, 0.2)
            ang = np.arccos(np.clip(px * v[0] + py * v[1] + pz * v[2], -1, 1))
            floor = smoothstep(r, r * 0.35, ang)
            wall = np.exp(-((ang - r) / (r * 0.12)) ** 2)
            albedo = albedo * (1 - 0.30 * floor[..., None]) + 255 * 0.10 * wall[..., None]
    elif style == 'nether':
        crust = fbm(px, py, pz, 31, 5, 2.4)
        albedo = _mixc((30, 12, 12), base * 0.55, smoothstep(0.35, 0.8, crust))
        ridge = 1.0 - np.abs(2.0 * fbm(px + 2, py + 6, pz + 1, 32, 5, 2.0) - 1.0)
        core = smoothstep(0.90, 0.975, ridge)
        glowing = smoothstep(0.78, 0.93, ridge)
        emissive = (core[..., None] * np.array([255, 190, 80]) + glowing[..., None] * np.array([210, 70, 20])) * 0.85
        edge = (1 - z) ** 3.0
        rim = edge[..., None] * np.array([210, 70, 30]) * 0.5
    else:
        bands = 0.5 + 0.5 * np.sin(py * 9 + 3 * fbm(px, py, pz, 41, 4, 2))
        albedo = _mixc(base * 0.6, base, bands)

    shade = 0.10 + 0.95 * lit
    colour_out = albedo * shade[..., None] + rim * (0.25 + 0.75 * lit[..., None]) + emissive
    rgba = np.dstack([np.clip(colour_out, 0, 255), np.where(mask, 255.0, 0.0)]).astype(np.uint8)
    return downsample(Image.fromarray(rgba, 'RGBA'), out)


def write_planets():
    styles = {'earth': ('earth', 1), 'moon': ('moon', 2), 'nether': ('nether', 3)}
    for planet in chart.load_planets():
        style, seed = styles.get(planet.id, ('generic', 4))
        save(planet_sprite(style, planet.color, seed), os.path.join(NS, 'planets', f'{planet.id}.png'))


def write_ring(size=192):
    """The rumour: a faint dotted ring, a world that is out there and not yet known."""
    k = 4
    big = size * k
    image = Image.new('RGBA', (big, big), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    c = big / 2
    r = big * 0.40
    draw.ellipse((c - r, c - r, c + r, c + r), outline=BRASS[2] + (60,), width=int(1.4 * k))
    dots = 40
    for i in range(dots):
        a = 2 * math.pi * i / dots
        x, y = c + r * math.cos(a), c + r * math.sin(a)
        s = (2.6 if i % 5 == 0 else 1.5) * k
        draw.ellipse((x - s, y - s, x + s, y + s), fill=BRASS[3] + (190 if i % 5 == 0 else 120,))
    image = image.filter(ImageFilter.GaussianBlur(0.6 * k))
    save(downsample(image, size), os.path.join(NS, 'planets', 'ring.png'))


def write_halo(size=256):
    """Visited: a soft teal glow, the network's colour, peaking just outside the sphere."""
    ys, xs = np.mgrid[0:size, 0:size]
    d = np.hypot(xs + 0.5 - size / 2, ys + 0.5 - size / 2) / (size / 2)
    glow = np.exp(-((d - 0.62) / 0.16) ** 2) * 0.85
    glow = glow * (d <= 1.0) * (1 - smoothstep(0.86, 1.0, d))
    rgba = np.zeros((size, size, 4))
    for i in range(3):
        rgba[..., i] = TEAL[3][i]
    rgba[..., 3] = np.clip(glow * 255.0, 0, 255)
    save(Image.fromarray(rgba.astype(np.uint8), 'RGBA'), os.path.join(NS, 'planets', 'halo.png'))


# ----------------------------------------------------------------------------------------------
# Chapter icons
# ----------------------------------------------------------------------------------------------

def _icon_base(size):
    k = 8
    big = size * k
    image = Image.new('RGBA', (big, big), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    c = big / 2
    r = big * 0.47
    draw.ellipse((c - r, c - r, c + r, c + r), fill=SKY[1] + (255,), outline=BRASS[2] + (255,), width=int(2.2 * k))
    return image, draw, k, c, r


def write_icons(size=64):
    # First Days: a sun on the horizon.
    image, draw, k, c, r = _icon_base(size)
    sun_r = r * 0.34
    horizon = c + r * 0.18
    draw.pieslice((c - sun_r, horizon - sun_r, c + sun_r, horizon + sun_r), 180, 360, fill=BRASS[3] + (255,))
    for deg in range(200, 345, 25):
        a = math.radians(deg)
        draw.line((c + sun_r * 1.25 * math.cos(a), horizon + sun_r * 1.25 * math.sin(a),
                   c + sun_r * 1.75 * math.cos(a), horizon + sun_r * 1.75 * math.sin(a)),
                  fill=BRASS[2] + (255,), width=int(2.2 * k))
    draw.line((c - r * 0.7, horizon, c + r * 0.7, horizon), fill=BRASS[2] + (255,), width=int(2.4 * k))
    save(downsample(image, size), os.path.join(NS, 'icons', 'first_days.png'))

    # Planets: a ring with a small world on it.
    image, draw, k, c, r = _icon_base(size)
    ring = r * 0.62
    draw.ellipse((c - ring, c - ring, c + ring, c + ring), outline=BRASS[1] + (255,), width=int(1.8 * k))
    draw.ellipse((c - r * 0.2, c - r * 0.2, c + r * 0.2, c + r * 0.2), fill=BRASS[3] + (255,))
    a = math.radians(-35)
    wx, wy = c + ring * math.cos(a), c + ring * math.sin(a)
    w = r * 0.16
    draw.ellipse((wx - w, wy - w, wx + w, wy + w), fill=TEAL[3] + (255,), outline=TEAL[5] + (255,), width=int(1.2 * k))
    save(downsample(image, size), os.path.join(NS, 'icons', 'planets.png'))

    # The Road Up: a hot-air balloon rising.
    image, draw, k, c, r = _icon_base(size)
    top = c - r * 0.60
    bw = r * 0.44
    rim_y = top + bw * 1.15                      # where the round top ends and the taper begins
    neck_y = top + bw * 2.35
    neck = r * 0.13
    draw.ellipse((c - bw, top, c + bw, top + bw * 2), fill=BRASS[3] + (255,), outline=BRASS[2] + (255,), width=int(1.3 * k))
    draw.polygon([(c - bw * 0.97, rim_y), (c + bw * 0.97, rim_y), (c + neck, neck_y), (c - neck, neck_y)],
                 fill=BRASS[3] + (255,))
    draw.line((c - bw * 0.97, rim_y, c - neck, neck_y), fill=BRASS[2] + (255,), width=int(1.3 * k))
    draw.line((c + bw * 0.97, rim_y, c + neck, neck_y), fill=BRASS[2] + (255,), width=int(1.3 * k))
    for f in (-0.5, 0.5):                         # the panels of the envelope
        draw.line((c + f * bw * 0.9, top + bw * 0.25, c + f * neck * 1.0, neck_y), fill=BRASS[1] + (255,), width=int(0.9 * k))
    draw.line((c, top + bw * 0.05, c, neck_y), fill=BRASS[1] + (255,), width=int(0.9 * k))
    basket = neck_y + r * 0.22
    draw.line((c - neck, neck_y, c - r * 0.16, basket), fill=BRASS[2] + (255,), width=int(1.0 * k))
    draw.line((c + neck, neck_y, c + r * 0.16, basket), fill=BRASS[2] + (255,), width=int(1.0 * k))
    draw.rectangle((c - r * 0.17, basket, c + r * 0.17, basket + r * 0.13), fill=BRASS[1] + (255,),
                   outline=BRASS[2] + (255,), width=int(1.0 * k))
    save(downsample(image, size), os.path.join(NS, 'icons', 'road_up.png'))

    # The Moon: a crescent over a crater.
    image, draw, k, c, r = _icon_base(size)
    m = r * 0.58
    draw.ellipse((c - m, c - m, c + m, c + m), fill=BRASS[3] + (255,))
    cut = Image.new('RGBA', image.size, (0, 0, 0, 0))
    cd = ImageDraw.Draw(cut)
    off = m * 0.55
    cd.ellipse((c - m + off, c - m - off * 0.15, c + m + off, c + m - off * 0.15), fill=SKY[1] + (255,))
    image = Image.alpha_composite(image, cut)
    draw = ImageDraw.Draw(image)
    draw.ellipse((c - r * 0.47, c + r * 0.05, c - r * 0.37, c + r * 0.15), fill=BRASS[1] + (255,))
    draw.ellipse((c - r * 0.40, c - r * 0.28, c - r * 0.32, c - r * 0.20), fill=BRASS[1] + (255,))
    draw.ellipse((c + r * 0.26, c - r * 0.50, c + r * 0.31, c - r * 0.45), fill=BRASS[3] + (255,))
    draw.ellipse((c + r * 0.50, c + r * 0.14, c + r * 0.54, c + r * 0.18), fill=BRASS[2] + (255,))
    save(downsample(image, size), os.path.join(NS, 'icons', 'the_moon.png'))

    # The Rot: a flame, the colour of the fire world.
    image, draw, k, c, r = _icon_base(size)
    outer = [(c, c - r * 0.62), (c + r * 0.20, c - r * 0.30), (c + r * 0.44, c + r * 0.02), (c + r * 0.40, c + r * 0.34),
             (c + r * 0.20, c + r * 0.55), (c, c + r * 0.60), (c - r * 0.20, c + r * 0.55), (c - r * 0.40, c + r * 0.34),
             (c - r * 0.44, c + r * 0.02), (c - r * 0.20, c - r * 0.22), (c - r * 0.12, c - r * 0.42)]
    draw.polygon(outer, fill=(210, 76, 24, 255), outline=(255, 190, 80, 255))
    inner = [(c, c - r * 0.20), (c + r * 0.15, c + r * 0.06), (c + r * 0.18, c + r * 0.34), (c, c + r * 0.50),
             (c - r * 0.18, c + r * 0.34), (c - r * 0.14, c + r * 0.06)]
    draw.polygon(inner, fill=(255, 190, 80, 255))
    save(downsample(image, size), os.path.join(NS, 'icons', 'the_rot.png'))

    # The Belt: a few pale islands hung in the dark.
    image, draw, k, c, r = _icon_base(size)
    for dx, dy, w, h in ((-0.30, -0.18, 0.34, 0.12), (0.20, 0.02, 0.40, 0.14), (-0.10, 0.30, 0.26, 0.10)):
        top = (c + r * dx - r * w, c + r * dy - r * h * 0.5, c + r * dx + r * w, c + r * dy + r * h * 0.5)
        draw.ellipse(top, fill=(214, 206, 170, 255), outline=(150, 100, 190, 255))
        draw.polygon([(top[0] + r * 0.05, c + r * dy), (top[2] - r * 0.05, c + r * dy),
                      (c + r * dx + r * 0.03, c + r * dy + r * h * 1.6)], fill=(120, 110, 100, 255))
    save(downsample(image, size), os.path.join(NS, 'icons', 'the_belt.png'))

    # Gear and Growth: a cogwheel with a spark at the centre.
    image, draw, k, c, r = _icon_base(size)
    teeth, body, hole = 8, r * 0.46, r * 0.17
    pts = []
    for i in range(teeth * 4):
        a = 2 * math.pi * i / (teeth * 4)
        out = body * 1.32 if (i % 4) in (1, 2) else body
        pts.append((c + out * math.cos(a), c + out * math.sin(a)))
    draw.polygon(pts, fill=BRASS[2] + (255,), outline=BRASS[3] + (255,))
    draw.ellipse((c - body * 0.62, c - body * 0.62, c + body * 0.62, c + body * 0.62), fill=SKY[1] + (255,),
                 outline=BRASS[1] + (255,), width=int(1.2 * k))
    draw.ellipse((c - hole, c - hole, c + hole, c + hole), fill=BRASS[3] + (255,))
    save(downsample(image, size), os.path.join(NS, 'icons', 'gear_and_growth.png'))

    # Together: two rings, joined.
    image, draw, k, c, r = _icon_base(size)
    ring = r * 0.36
    for dx, colour in ((-r * 0.22, BRASS[2]), (r * 0.22, BRASS[3])):
        draw.ellipse((c + dx - ring, c - ring, c + dx + ring, c + ring), outline=colour + (255,), width=int(2.2 * k))
    save(downsample(image, size), os.path.join(NS, 'icons', 'together.png'))

    # The Gate: two pillars and a lintel round a teal opening.
    image, draw, k, c, r = _icon_base(size)
    pw, px, top, bottom = r * 0.15, r * 0.40, c - r * 0.56, c + r * 0.56
    draw.rectangle((c - px + pw, top + pw * 1.7, c + px - pw, bottom), fill=TEAL[2] + (255,))
    for cx in (c - px, c + px):
        draw.rectangle((cx - pw, top, cx + pw, bottom), fill=BRASS[2] + (255,), outline=BRASS[3] + (255,), width=int(1.1 * k))
    draw.rectangle((c - px - pw * 1.3, top, c + px + pw * 1.3, top + pw * 1.7), fill=BRASS[3] + (255,),
                   outline=BRASS[2] + (255,), width=int(1.1 * k))
    draw.rectangle((c - px - pw * 1.3, bottom - pw * 0.5, c + px + pw * 1.3, bottom + pw * 0.5), fill=BRASS[1] + (255,),
                   outline=BRASS[2] + (255,), width=int(1.0 * k))
    save(downsample(image, size), os.path.join(NS, 'icons', 'the_gate.png'))

    # Create: Power: a large and a small cogwheel, meshed.
    def cog(cx, cy, body, teeth, fill, edge):
        pts = []
        for i in range(teeth * 4):
            a = 2 * math.pi * i / (teeth * 4)
            out = body * 1.3 if (i % 4) in (1, 2) else body
            pts.append((cx + out * math.cos(a), cy + out * math.sin(a)))
        draw.polygon(pts, fill=fill + (255,), outline=edge + (255,))
        draw.ellipse((cx - body * 0.55, cy - body * 0.55, cx + body * 0.55, cy + body * 0.55), fill=SKY[1] + (255,),
                     outline=edge + (255,), width=int(1.0 * k))
        draw.ellipse((cx - body * 0.2, cy - body * 0.2, cx + body * 0.2, cy + body * 0.2), fill=edge + (255,))
    image, draw, k, c, r = _icon_base(size)
    cog(c - r * 0.14, c + r * 0.12, r * 0.36, 10, BRASS[2], BRASS[3])
    cog(c + r * 0.40, c - r * 0.34, r * 0.19, 7, BRASS[3], BRASS[2])
    save(downsample(image, size), os.path.join(NS, 'icons', 'create_power.png'))

    # Create: Machines: a press coming down on a sheet.
    image, draw, k, c, r = _icon_base(size)
    draw.rectangle((c - r * 0.52, c + r * 0.30, c + r * 0.52, c + r * 0.48), fill=BRASS[1] + (255,),
                   outline=BRASS[2] + (255,), width=int(1.1 * k))
    draw.rectangle((c - r * 0.30, c + r * 0.22, c + r * 0.30, c + r * 0.30), fill=BRASS[3] + (255,))
    draw.rectangle((c - r * 0.08, c - r * 0.58, c + r * 0.08, c - r * 0.10), fill=BRASS[1] + (255,),
                   outline=BRASS[2] + (255,), width=int(1.0 * k))
    draw.rectangle((c - r * 0.38, c - r * 0.10, c + r * 0.38, c + r * 0.14), fill=BRASS[2] + (255,),
                   outline=BRASS[3] + (255,), width=int(1.1 * k))
    save(downsample(image, size), os.path.join(NS, 'icons', 'create_craft.png'))

    # Aeronautics: A Ship: a propeller, three blades round a hub.
    image, draw, k, c, r = _icon_base(size)
    for deg in (-90, 30, 150):
        a = math.radians(deg)
        dx, dy = math.cos(a), math.sin(a)
        px_, py_ = -dy, dx
        blade = [(c + dx * r * 0.10, c + dy * r * 0.10),
                 (c + dx * r * 0.30 + px_ * r * 0.15, c + dy * r * 0.30 + py_ * r * 0.15),
                 (c + dx * r * 0.62, c + dy * r * 0.62),
                 (c + dx * r * 0.30 - px_ * r * 0.15, c + dy * r * 0.30 - py_ * r * 0.15)]
        draw.polygon(blade, fill=BRASS[3] + (255,), outline=BRASS[2] + (255,))
    draw.ellipse((c - r * 0.13, c - r * 0.13, c + r * 0.13, c + r * 0.13), fill=BRASS[1] + (255,),
                 outline=BRASS[2] + (255,), width=int(1.2 * k))
    save(downsample(image, size), os.path.join(NS, 'icons', 'ship_build.png'))

    # Aeronautics: In Flight: a rocket on its flame.
    image, draw, k, c, r = _icon_base(size)
    draw.polygon([(c - r * 0.10, c + r * 0.34), (c + r * 0.10, c + r * 0.34), (c, c + r * 0.64)],
                 fill=(255, 190, 80, 255), outline=(210, 76, 24, 255))
    for s in (-1, 1):
        draw.polygon([(c + s * r * 0.17, c + r * 0.04), (c + s * r * 0.38, c + r * 0.40), (c + s * r * 0.17, c + r * 0.32)],
                     fill=BRASS[2] + (255,), outline=BRASS[3] + (255,))
    draw.polygon([(c, c - r * 0.64), (c + r * 0.18, c - r * 0.22), (c + r * 0.18, c + r * 0.34), (c - r * 0.18, c + r * 0.34),
                  (c - r * 0.18, c - r * 0.22)], fill=BRASS[3] + (255,), outline=BRASS[2] + (255,))
    draw.ellipse((c - r * 0.08, c - r * 0.14, c + r * 0.08, c + r * 0.02), fill=TEAL[3] + (255,),
                 outline=BRASS[1] + (255,), width=int(1.0 * k))
    save(downsample(image, size), os.path.join(NS, 'icons', 'ship_fly.png'))


# ----------------------------------------------------------------------------------------------
# The Earth gate's plan, drawn from the frame's own data so the picture cannot disagree with the game
# ----------------------------------------------------------------------------------------------

GATE_FRAME = os.path.join(ROOT, 'modules', 'worlds', 'src', 'main', 'resources', 'data', 'ascension_worlds',
                          'ascension_worlds', 'gateway_frame', 'gateway.json')
GATE_TAGS = os.path.join(ROOT, 'modules', 'worlds', 'src', 'main', 'resources', 'data', 'ascension_worlds', 'tags',
                         'block')

# One colour and one name for each kind of cell. The letters are the ones in gateway.json; 'd' is the same family
# as 'D' (the Moon's copy uses a different block for it, the rule accepts the same ones).
GATE_KINDS = {
    'D': ('Dark stone', (62, 72, 104), 'gateway_frame_dark'),
    'P': ('Pale stone', (228, 222, 202), 'gateway_frame_pale'),
    'I': ('Pillar', (204, 170, 104), 'gateway_frame_pillar'),
    '_': ('Step (a slab)', (112, 126, 156), 'gateway_frame_step'),
    'C': ('Accent', (200, 116, 72), 'gateway_frame_accent'),
    'G': ('Inscribed gatestone', (58, 202, 190), None),
    'K': ('Keystone', (176, 255, 246), None),
    'O': ('The opening: leave empty', (22, 52, 60), None),
}
GATE_NAMES = {
    'gateway_frame_dark': 'deepslate bricks or tiles, polished deepslate or blackstone, smooth basalt',
    'gateway_frame_pale': 'quartz, calcite, polished diorite',
    'gateway_frame_pillar': 'bone block, quartz pillar, polished basalt',
    'gateway_frame_step': 'deepslate, tile or blackstone slabs',
    'gateway_frame_accent': 'chiseled deepslate, stone bricks, tuff bricks or quartz',
}


def _gate_cells():
    """{(x, y, z): kind} for every cell of the frame, kind in GATE_KINDS (spaces are skipped)."""
    with open(GATE_FRAME, encoding='utf-8') as handle:
        data = json.load(handle)
    cells = {}
    for y, layer in enumerate(data['layers']):
        for z, row in enumerate(layer):
            for x, char in enumerate(row):
                if char == ' ':
                    continue
                cells[(x, y, z)] = 'D' if char == 'd' else char
    return cells, len(data['layers']), max(len(r) for layer in data['layers'] for r in layer), \
        max(len(layer) for layer in data['layers'])


def _shade(rgb, f):
    return tuple(max(0, min(255, int(v * f))) for v in rgb)


def _gate_view(cells, h, w, d, cell):
    """The ring as it stands, seen from the front a little to the side and above: faces in three shades."""
    k = 3
    c = cell * k
    ox, oy = int(c * 0.42), int(c * 0.26)           # how far one step of depth moves up and right
    width = w * c + d * ox + 4 * k
    height = h * c + d * oy + 4 * k
    image = Image.new('RGBA', (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    for z in range(d - 1, -1, -1):
        for y in range(h):
            for x in range(w):
                kind = cells.get((x, y, z))
                if kind is None or kind == 'O':
                    continue
                rgb = GATE_KINDS[kind][1]
                fx = 2 * k + x * c + z * ox
                fy = height - 2 * k - (y + 1) * c - z * oy
                front = [(fx, fy), (fx + c, fy), (fx + c, fy + c), (fx, fy + c)]
                top = [(fx, fy), (fx + ox, fy - oy), (fx + c + ox, fy - oy), (fx + c, fy)]
                side = [(fx + c, fy), (fx + c + ox, fy - oy), (fx + c + ox, fy + c - oy), (fx + c, fy + c)]
                edge = _shade(rgb, 0.45) + (255,)
                draw.polygon(top, fill=_shade(rgb, 1.18) + (255,), outline=edge)
                draw.polygon(side, fill=_shade(rgb, 0.62) + (255,), outline=edge)
                draw.polygon(front, fill=rgb + (255,), outline=edge)
    return downsample_to(image, (width // k, height // k))


def downsample_to(image, size):
    return image.resize(size, Image.LANCZOS)


def _gate_layers(cells, h, w, d, cell, columns, font):
    """Every layer from the bottom, seen from above with the front toward the bottom of each tile."""
    tile_w, tile_h = w * cell, d * cell
    label = 16
    gap = 12
    rows = (h + columns - 1) // columns
    width = columns * (tile_w + gap) - gap
    height = rows * (tile_h + label + gap) - gap
    image = Image.new('RGBA', (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    for y in range(h):
        col, row = y % columns, y // columns
        left = col * (tile_w + gap)
        top = row * (tile_h + label + gap) + label
        draw.text((left, top - label + 1), 'Layer %d' % (y + 1) + ('  (bottom)' if y == 0 else '  (top)' if y == h - 1 else ''),
                  font=font, fill=BRASS[3] + (255,))
        draw.rectangle((left - 1, top - 1, left + tile_w, top + tile_h), outline=BRASS[0] + (255,), fill=SKY[0] + (255,))
        for z in range(d):
            for x in range(w):
                kind = cells.get((x, y, z))
                if kind is None:
                    continue
                px = left + x * cell
                py = top + (d - 1 - z) * cell
                rgb = GATE_KINDS[kind][1]
                draw.rectangle((px, py, px + cell - 1, py + cell - 1), fill=rgb + (255,),
                               outline=_shade(rgb, 0.5) + (255,))
    return image


def _wrap(draw, text, font, limit):
    """Greedy word wrap to a pixel width."""
    lines, line = [], ''
    for word in text.split():
        trial = (line + ' ' + word).strip()
        if line and draw.textlength(trial, font=font) > limit:
            lines.append(line)
            line = word
        else:
            line = trial
    if line:
        lines.append(line)
    return lines


def write_gate_plan():
    cells, h, w, d = _gate_cells()
    font = _font(15)
    small = _font(13)
    tiny = _font(12)

    width, height = 1024, 600
    canvas = Image.new('RGBA', (width, height), SKY[0] + (255,))
    draw = ImageDraw.Draw(canvas)
    draw.rectangle((3, 3, width - 4, height - 4), outline=BRASS[1] + (255,), width=2)
    draw.rectangle((9, 9, width - 10, height - 10), outline=BRASS[0] + (255,), width=1)

    draw.text((24, 16), "THE RING  -  %d wide, %d deep, %d high" % (w, d, h), font=_font(20), fill=BRASS[3] + (255,))
    draw.text((24, 42), "Layers run from the ground up. Each tile is seen from above; the front is the bottom of the tile.",
              font=tiny, fill=BRASS[2] + (255,))

    view = _gate_view(cells, h, w, d, 12)
    canvas.alpha_composite(view, (24, 70))

    # The key: one swatch per kind of cell, with what Earth builds it from.
    ky = 70 + view.size[1] + 10
    for char in ('D', 'P', 'I', '_', 'C', 'G', 'K', 'O'):
        name, rgb, tag = GATE_KINDS[char]
        draw.rectangle((24, ky, 38, ky + 12), fill=rgb + (255,), outline=_shade(rgb, 0.5) + (255,))
        draw.text((46, ky - 2), name, font=small, fill=BRASS[3] + (255,))
        if tag:
            lines = _wrap(draw, GATE_NAMES[tag], tiny, 300)
            for i, line in enumerate(lines):
                draw.text((46, ky + 12 + 13 * i), line, font=tiny, fill=BRASS[2] + (200,))
            ky += 19 + 13 * len(lines)
        else:
            ky += 17

    sheet_left = 24 + max(view.size[0], 330) + 24
    layers = _gate_layers(cells, h, w, d, 9, 4, font=tiny)
    canvas.alpha_composite(layers, (sheet_left, 76))

    save(canvas, os.path.join(NS, 'gate', 'plan.png'), blur=False)
    return canvas.size


# ----------------------------------------------------------------------------------------------
# The theme: Quests' own file with our colours, stacked over its stock one (the last pack wins)
# ----------------------------------------------------------------------------------------------

def _hex(rgb, alpha=None):
    text = '#%02X%02X%02X' % tuple(rgb)
    return text if alpha is None else '#%02X%s' % (alpha, text[1:])


THEME = """\
[*]
background:          ascension_compat_ftbquests:textures/quests/theme/background.png; tile_size=256
chapter_panel_background:          hollow_rectangle:{{widget_border}}; round_edges + {{background}}; color=#D0D0D0; padding=1
key_reference_background:          ascension_compat_ftbquests:textures/quests/theme/background.png; tile_size=256
selected_chapter_highlight_1: %(highlight1)s
selected_chapter_highlight_2: %(highlight2)s

text_color:          %(text)s
hover_text_color:    %(hover)s
disabled_text_color: %(disabled)s

widget_border:     %(border)s
widget_background: %(widget_bg)s
symbol_in:         %(brass2)s
symbol_out:        %(brass1)s

button:                hollow_rectangle:{{widget_border}}
panel:                 {{container_slot}}
disabled_button:       hollow_rectangle:{{widget_border}}
hover_button:          {{button}} + {{widget_background}}; padding=1
context_menu:          hollow_rectangle:{{widget_border}}; round_edges + {{background}}; color=#C8C8C8; padding=1
scroll_bar_background: {{widget_background}}
scroll_bar:            {{button}} + {{widget_background}}; padding=1
container_slot:        {{button}}; padding=-1
text_box:              part:minecraft:textures/gui/container/enchanting_table.png; pos=0,185,108,19; corner=6

icon:                 none
check_icon:           builtin
add_icon:             builtin
alert_icon:           ftbquests:textures/gui/alert.png
support_icon:         ftblibrary:textures/icons/support.png
wiki_icon:            ftbquests:textures/gui/info.png
wiki_url:             -
pin_icon_on:          ftbquests:textures/gui/pin.png; color=%(brass3)s
pin_icon_off:         ftbquests:textures/gui/pin.png; color=#DBDBDB
editor_icon_on:       ftbquests:textures/gui/editor.png
editor_icon_off:      ftbquests:textures/gui/editor_white.png
hidden_icon:          ftbquests:textures/gui/hidden.png; color=#DBDBDB
link_icon:            ftbquests:textures/gui/link.png; color=#DBDBDB
save_icon:            ftbquests:textures/gui/save.png
settings_icon:        ftbquests:textures/gui/settings.png
prefs_icon:           ftblibrary:icons/player
close_icon:           ftbquests:textures/gui/close.png
emergency_items_icon: ftbquests:textures/gui/emergency_items.png; color=#F3D9FF
guide_icon:           ftbquests:textures/gui/guide.png; color=%(brass3)s
modpack_icon:         ftbquests:textures/item/book.png
reward_table_icon:    ftblibrary:icons/money_bag
shop_icon:            ftbquests:textures/gui/shop.png; color=%(brass3)s
collect_rewards_icon: ftbquests:textures/gui/collect_rewards.png; color=%(brass3)s
lock_icon:            ftbquests:textures/gui/quest_locked.png
delete_icon:          ftblibrary:icons/remove
reload_icon:          ftblibrary:icons/refresh
download_icon:        ftblibrary:icons/down
edit_icon:            ftblibrary:icons/settings
move_up_icon:         ftblibrary:icons/up
move_down_icon:       ftblibrary:icons/down
left_arrow:           ftblibrary:icons/left
right_arrow:          ftblibrary:icons/right
undo_icon:            ftblibrary:icons/undo
redo_icon:            ftblibrary:icons/redo

full_screen_quest:                  0
tasks_text_color:                   %(tasks)s
rewards_text_color:                 %(rewards)s
quest_view_background:              {{context_menu}}
quest_view_border:                  {{widget_border}}
quest_view_title:                   %(brass3)s
quest_completed_color:              %(completed)s
quest_started_color:                %(started)s
quest_not_started_color:            %(not_started)s
quest_locked_color:                 %(locked)s
dependency_line_texture:            ftbquests:textures/gui/dependency.png
dependency_line_completed_color:    %(line_done)s
dependency_line_uncompleted_color:  %(line_todo)s
dependency_line_unavailable_color:  %(line_locked)s
dependency_line_requires_color:     %(brass2)s
dependency_line_required_for_color: %(brass3)s
dependency_line_selected_speed:     1.0
dependency_line_unselected_speed:   0.0
dependency_line_thickness:          0.12
quest_spacing:                      1.0
pinned_quest_size:                  0.75

checkmark_task_active:   ftblibrary:icons/accept
checkmark_task_inactive: ftblibrary:icons/accept_gray
"""


def write_theme():
    values = {
        'highlight1': _hex(BRASS[1], 0x60), 'highlight2': _hex(BRASS[1], 0x28),
        'text': _hex((237, 230, 210)), 'hover': _hex((255, 233, 168)), 'disabled': _hex((140, 136, 124)),
        'border': _hex(BRASS[1]), 'widget_bg': _hex(SKY[1], 0xC0),
        'brass1': _hex(BRASS[1]), 'brass2': _hex(BRASS[2]), 'brass3': _hex(BRASS[3]),
        'tasks': _hex((160, 200, 240)), 'rewards': _hex(BRASS[3]),
        # How far a quest has got: brass when done, cold light when under way, dim brass before, slate if locked.
        'completed': _hex(BRASS[3], 0xE6), 'started': _hex((169, 199, 240), 0xE6),
        'not_started': _hex(BRASS[2], 0xA0), 'locked': _hex((96, 104, 120), 0xA0),
        'line_done': _hex(BRASS[2]), 'line_todo': _hex(BRASS[1], 0xB4), 'line_locked': _hex((96, 104, 120), 0x64),
    }
    path = os.path.join(FTB, 'ftb_quests_theme.txt')
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as handle:
        handle.write(THEME % values)


def main():
    write_theme()
    write_shapes()
    write_background()
    write_chart()
    write_planets()
    write_ring()
    write_halo()
    write_icons()
    write_gate_plan()
    print('quest art written under', os.path.relpath(ASSETS, ROOT))


if __name__ == '__main__':
    main()
