#!/usr/bin/env python3
"""The End's asteroid rock (docs/technical/end-art-format.md): one 64 x 64 texture every rock of the belt wears.

Original art, from a seeded script: pale bone-and-lilac stone, pitted with craters and veined with a faint violet
glow, lit flat (the game shades it into a phase). Reproducible byte for byte.

  python tools/generate-end-belt-art.py
"""

import os

import numpy as np
from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
OUT = os.path.join(ROOT, 'modules', 'worlds', 'src', 'main', 'resources', 'assets', 'ascension_worlds',
                   'textures', 'environment', 'end_rock.png')
SIZE = 64


def noise(rng, size, octaves):
    total = np.zeros((size, size))
    amplitude = 1.0
    cells = 4
    for _ in range(octaves):
        grid = rng.random((cells + 1, cells + 1))
        xs = np.linspace(0, cells, size, endpoint=False)
        x0 = xs.astype(int)
        t = xs - x0
        t = t * t * (3 - 2 * t)
        rows = grid[x0][:, x0] * (1 - t)[None, :] * (1 - t)[:, None]
        rows += grid[x0][:, x0 + 1] * t[None, :] * (1 - t)[:, None]
        rows += grid[x0 + 1][:, x0] * (1 - t)[None, :] * t[:, None]
        rows += grid[x0 + 1][:, x0 + 1] * t[None, :] * t[:, None]
        total += rows * amplitude
        amplitude *= 0.5
        cells *= 2
    return total / total.max()


def main():
    rng = np.random.default_rng(20261020)
    base = noise(rng, SIZE, 4)
    ys, xs = np.mgrid[0:SIZE, 0:SIZE]
    stone = np.array([206.0, 196.0, 176.0])
    lilac = np.array([150.0, 128.0, 178.0])
    tone = (0.55 + 0.45 * base)[..., None]
    colour = stone * tone + lilac * (1 - tone) * 0.55
    # Craters: a darker floor and a bright lip.
    for _ in range(9):
        cx, cy = rng.uniform(6, SIZE - 6, 2)
        radius = rng.uniform(3, 8)
        d = np.sqrt((xs - cx) ** 2 + (ys - cy) ** 2)
        floor = np.clip((radius - d) / radius, 0, 1)
        lip = np.exp(-((d - radius) / 1.2) ** 2)
        colour = colour * (1 - 0.35 * floor[..., None]) + 38.0 * lip[..., None]
    # Faint violet veins.
    vein = 1.0 - np.abs(2.0 * noise(rng, SIZE, 3) - 1.0)
    glow = np.clip((vein - 0.9) / 0.1, 0, 1)[..., None]
    colour = colour + glow * np.array([70.0, 30.0, 120.0])
    image = np.dstack([np.clip(colour, 0, 255), np.full((SIZE, SIZE), 255.0)]).astype(np.uint8)
    # The inscribed circle is the disc; outside it is transparent.
    mask = ((xs - SIZE / 2 + 0.5) ** 2 + (ys - SIZE / 2 + 0.5) ** 2) <= (SIZE / 2) ** 2
    image[..., 3] = np.where(mask, 255, 0)
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    Image.fromarray(image, 'RGBA').save(OUT, optimize=False)
    print('wrote', os.path.relpath(OUT, ROOT))


if __name__ == '__main__':
    main()
