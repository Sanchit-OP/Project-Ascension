#!/usr/bin/env python3
"""The End's asteroid belt art (docs/technical/end-art-format.md, section 1): three textures and a preview.

Original art, from a seeded script, reproducible byte for byte:
  end_rock.png  128 x 64, an atlas of eight 32 x 32 irregular rocks (four to a row). Not discs: each is a lumpy,
                elongated, faceted shape lit from the top left (art-guide), with craters, pale dust on the lit
                side and a thread of violet glow in the cracks. The renderer turns them as they tumble.
  end_dust.png  64 x 64, the dust ring: v runs from the belt's inner edge (top) to its outer edge (bottom), in the same
                bands the rocks keep (BeltRocks.density); u runs round and tiles. Soft, streaked, additive.
  end_glow.png  64 x 64, a soft round glow for the core, violet-white.

  python tools/generate-end-belt-art.py              writes the three textures
  python tools/generate-end-belt-art.py --preview    also writes design/end/belt-*.png (a mock of the renderer)
"""

import math
import os
import sys

import numpy as np
from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
ENV = os.path.join(ROOT, 'modules', 'worlds', 'src', 'main', 'resources', 'assets', 'ascension_worlds',
                   'textures', 'environment')
PREVIEW = os.path.join(ROOT, 'design', 'end')
CELL = 32
SPRITES = 8
COLUMNS = 4


def periodic_noise(rng, shape, cells, octaves=4, tile_x=True):
    """Smooth value noise, 0..1, tiling along x (axis 1) if tile_x."""
    height, width = shape
    total = np.zeros(shape)
    amplitude = 1.0
    norm = 0.0
    cx, cy = cells
    for _ in range(octaves):
        grid = rng.random((cy + 2, cx + 2))
        if tile_x:
            grid[:, cx] = grid[:, 0]
        xs = np.linspace(0, cx, width, endpoint=False)
        ys = np.linspace(0, cy, height, endpoint=False)
        x0 = xs.astype(int)
        y0 = ys.astype(int)
        tx = xs - x0
        ty = ys - y0
        tx = tx * tx * (3 - 2 * tx)
        ty = ty * ty * (3 - 2 * ty)
        a = grid[np.ix_(y0, x0)]
        b = grid[np.ix_(y0, x0 + 1)]
        c = grid[np.ix_(y0 + 1, x0)]
        d = grid[np.ix_(y0 + 1, x0 + 1)]
        layer = a * (1 - tx)[None, :] * (1 - ty)[:, None] + b * tx[None, :] * (1 - ty)[:, None] \
            + c * (1 - tx)[None, :] * ty[:, None] + d * tx[None, :] * ty[:, None]
        total += layer * amplitude
        norm += amplitude
        amplitude *= 0.5
        cx *= 2
        cy *= 2
    return total / norm


def rock(rng, index):
    """One 32 x 32 RGBA rock: a lumpy elongated body, lit from the top left."""
    size = CELL
    ys, xs = np.mgrid[0:size, 0:size]
    cx = cy = size / 2 - 0.5
    # Outline: radius by angle from a few low harmonics, so it is lumpy but one piece.
    squash = rng.uniform(0.62, 0.95)
    tilt = rng.uniform(0, math.pi)
    dx = xs - cx
    dy = ys - cy
    ca, sa = math.cos(tilt), math.sin(tilt)
    ex = dx * ca + dy * sa
    ey = (-dx * sa + dy * ca) / squash
    angle = np.arctan2(ey, ex)
    radius = np.hypot(ex, ey)
    edge = 11.0 + sum(rng.uniform(0.0, 2.2) / k * np.cos(k * angle + rng.uniform(0, 6.28)) for k in (2, 3, 4, 5, 7))
    edge = np.clip(edge, 6.0, 14.5)
    inside = radius < edge
    # Height of a lumpy dome, with the faceting of a broken rock.
    dome = np.sqrt(np.clip(1.0 - (radius / edge) ** 2, 0, 1))
    lumps = periodic_noise(rng, (size, size), (3, 3), 3, tile_x=False)
    height = dome * 6.0 + lumps * 2.5
    # Craters: a bowl and a raised lip.
    for _ in range(int(rng.integers(2, 5))):
        px, py = rng.uniform(8, size - 8, 2)
        pr = rng.uniform(1.6, 4.2)
        d = np.hypot(xs - px, ys - py)
        height -= 1.8 * np.clip(1 - d / pr, 0, 1) ** 0.7
        height += 0.9 * np.exp(-((d - pr) / 0.8) ** 2)
    # Light from the top left.
    gy, gx = np.gradient(height)
    normal = np.dstack([-gx, -gy, np.full_like(gx, 1.0)])
    normal /= np.linalg.norm(normal, axis=2, keepdims=True)
    light = np.array([-0.6, -0.6, 0.55])
    light /= np.linalg.norm(light)
    lambert = np.clip(normal @ light, 0, 1)
    tone = 0.18 + 0.95 * lambert
    # Bone and lilac stone, one family per rock.
    families = [((214, 204, 184), (130, 110, 160)), ((196, 190, 200), (110, 100, 150)),
                ((222, 206, 176), (142, 112, 150)), ((178, 172, 190), (96, 90, 140))]
    lit, shade = families[index % len(families)]
    lit = np.array(lit, dtype=float)
    shade = np.array(shade, dtype=float)
    grain = periodic_noise(rng, (size, size), (6, 6), 3, tile_x=False)
    t = np.clip(tone * (0.85 + 0.3 * grain), 0, 1)[..., None]
    colour = shade * (1 - t) + lit * t
    colour *= (0.9 + 0.2 * grain)[..., None]
    # A thread of violet light in the cracks: thin ridges of a noise field, on the shadow side.
    ridge = 1.0 - np.abs(2.0 * periodic_noise(rng, (size, size), (4, 4), 3, tile_x=False) - 1.0)
    crack = np.clip((ridge - 0.955) / 0.045, 0, 1) * (1 - lambert)
    colour += crack[..., None] * np.array([90.0, 40.0, 150.0])
    image = np.zeros((size, size, 4))
    image[..., :3] = np.clip(colour, 0, 255)
    image[..., 3] = np.where(inside, 255, 0)
    # A darker rim keeps the outline readable against pale dust.
    rim = inside & (radius > edge - 1.2)
    image[..., :3][rim] *= 0.62
    return image.astype(np.uint8)


def atlas():
    rng = np.random.default_rng(20261021)
    rows = (SPRITES + COLUMNS - 1) // COLUMNS
    out = np.zeros((rows * CELL, COLUMNS * CELL, 4), dtype=np.uint8)
    for i in range(SPRITES):
        r, c = divmod(i, COLUMNS)
        out[r * CELL:(r + 1) * CELL, c * CELL:(c + 1) * CELL] = rock(rng, i)
    return out


def density(f):
    """BeltRocks.density, repeated here so the dust lies where the rocks do."""
    inner = math.exp(-(((f - 0.40) / 0.14) ** 2))
    outer = 0.8 * math.exp(-(((f - 0.82) / 0.07) ** 2))
    gap = 1.0 - 0.85 * math.exp(-(((f - 0.64) / 0.025) ** 2))
    return min(1.0, (0.10 + inner + outer) * gap)


def dust():
    rng = np.random.default_rng(20261022)
    size = 64
    fs = (np.arange(size) + 0.5) / size
    profile = np.array([density(f) for f in fs])[:, None]
    streak = periodic_noise(rng, (size, size), (8, 2), 4, tile_x=True)
    clumps = periodic_noise(rng, (size, size), (4, 4), 3, tile_x=True)
    alpha = profile * (0.35 + 0.9 * streak) * (0.5 + clumps)
    # Soft at the very inner and outer edges.
    edge = np.clip(np.minimum(fs, 1 - fs) / 0.08, 0, 1)[:, None]
    alpha = np.clip(alpha * edge, 0, 1) ** 1.2
    image = np.zeros((size, size, 4))
    image[..., 0] = 255
    image[..., 1] = 240
    image[..., 2] = 255
    image[..., 3] = alpha * 255
    return image.astype(np.uint8)


def glow():
    size = 64
    ys, xs = np.mgrid[0:size, 0:size]
    d = np.hypot(xs - size / 2 + 0.5, ys - size / 2 + 0.5) / (size / 2)
    core = np.clip(1 - d, 0, 1)
    alpha = core ** 2.2 * 0.9 + np.exp(-((d / 0.18) ** 2)) * 0.6
    image = np.zeros((size, size, 4))
    image[..., 0] = 255
    image[..., 1] = 240
    image[..., 2] = 255
    image[..., 3] = np.clip(alpha, 0, 1) * 255
    return image.astype(np.uint8)


def save(array, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    Image.fromarray(array, 'RGBA').save(path, optimize=False)
    print('wrote', os.path.relpath(path, ROOT))


def preview(rocks_atlas, glow_tex):
    """A mock of the renderer seen obliquely and from near the plane: not the game, a look at the art."""
    rng = np.random.default_rng(7)
    bodyr = 320.0
    inner = 0.2
    count = 900
    pts = [(0.0, 0.0, 0.0, 42.0, 0, 0.0, 1.0)]
    while len(pts) < count:
        f = inner + (1 - inner) * rng.random()
        if rng.random() > density(f):
            continue
        a = rng.random() * math.tau
        reach = f * (bodyr - 30)
        layer = (rng.random() + rng.random() - 1) * 0.08 * bodyr * (0.35 + 0.65 * (1 - density(f)))
        big = len(pts) >= count - count // 80
        size = 30 * (0.7 + 0.3 * rng.random()) if big else 1.5 + (30 * 0.45 - 1.5) * rng.random() ** 4
        pts.append((math.cos(a) * reach, layer, math.sin(a) * reach, size, int(rng.integers(0, SPRITES)),
                    rng.random() * math.tau, 0.75 + 0.35 * rng.random()))
    for name, pitch, w, h in (('belt-oblique', 28.0, 960, 540), ('belt-edge', 5.0, 960, 360)):
        canvas = np.zeros((h, w, 3), dtype=float)
        sky = np.random.default_rng(3).random((h, w)) > 0.9985
        canvas[sky] = 0.8
        distance = 1250.0
        pr = math.radians(pitch)
        scale = w / 2 / math.tan(math.radians(36)) / distance

        def project(x, y, z):
            y2 = y * math.cos(pr) - z * math.sin(pr)
            z2 = y * math.sin(pr) + z * math.cos(pr) + distance
            return w / 2 + x / z2 * distance * scale, h / 2 - y2 / z2 * distance * scale, z2

        def blit(img, cx, cy, half, additive=False, tint=None):
            side = max(2, int(half * 2))
            sprite = Image.fromarray(img, 'RGBA').resize((side, side), Image.NEAREST)
            arr = np.asarray(sprite).astype(float)
            x0 = int(cx - side / 2)
            y0 = int(cy - side / 2)
            xs0, ys0 = max(0, x0), max(0, y0)
            xs1, ys1 = min(w, x0 + side), min(h, y0 + side)
            if xs0 >= xs1 or ys0 >= ys1:
                return
            part = arr[ys0 - y0:ys1 - y0, xs0 - x0:xs1 - x0]
            a = part[..., 3:4] / 255.0
            rgb = part[..., :3] / 255.0
            if tint is not None:
                rgb = rgb * np.array(tint)
            if additive:
                canvas[ys0:ys1, xs0:xs1] += rgb * a
            else:
                canvas[ys0:ys1, xs0:xs1] = canvas[ys0:ys1, xs0:xs1] * (1 - a) + rgb * a

        for r in np.linspace(inner * bodyr, bodyr, 60):
            alpha = density(r / bodyr) * 0.05
            for a in np.linspace(0, math.tau, 200, endpoint=False):
                px, py, _ = project(math.cos(a) * r, 0, math.sin(a) * r)
                canvas[int(np.clip(py, 0, h - 1)), int(np.clip(px, 0, w - 1))] += alpha * np.array([0.55, 0.47, 0.72])
        cx, cy, _ = project(0, 0, 0)
        blit(glow_tex, cx, cy, 80, additive=True, tint=(0.78, 0.69, 0.94))
        for x, y, z, size, sprite, phase, shade in sorted(pts, key=lambda p: -project(p[0], p[1], p[2])[2]):
            px, py, depth = project(x, y, z)
            half = max(size * scale * distance / depth, 1.0)
            day = 0.5 + 0.5 * (x / bodyr)
            tint = (shade * (0.55 + 0.45 * day), shade * (0.42 + 0.58 * day), shade * (0.75 + 0.25 * day))
            r, c = divmod(sprite, COLUMNS)
            cell = np.rot90(rocks_atlas[r * CELL:(r + 1) * CELL, c * CELL:(c + 1) * CELL], int(phase / (math.pi / 2)) % 4)
            blit(np.ascontiguousarray(cell), px, py, half, tint=tint)
        image = (np.clip(canvas, 0, 1) * 255).astype(np.uint8)
        os.makedirs(PREVIEW, exist_ok=True)
        Image.fromarray(image, 'RGB').save(os.path.join(PREVIEW, name + '.png'))
        print('wrote design/end/' + name + '.png')
    sheet = Image.fromarray(rocks_atlas, 'RGBA').resize((COLUMNS * CELL * 6, 2 * CELL * 6), Image.NEAREST)
    back = Image.new('RGBA', sheet.size, (10, 8, 20, 255))
    back.alpha_composite(sheet)
    back.convert('RGB').save(os.path.join(PREVIEW, 'belt-rocks.png'))
    print('wrote design/end/belt-rocks.png')


def main():
    rocks_atlas = atlas()
    save(rocks_atlas, os.path.join(ENV, 'end_rock.png'))
    save(dust(), os.path.join(ENV, 'end_dust.png'))
    glow_tex = glow()
    save(glow_tex, os.path.join(ENV, 'end_glow.png'))
    if '--preview' in sys.argv:
        preview(rocks_atlas, glow_tex)


if __name__ == '__main__':
    main()
