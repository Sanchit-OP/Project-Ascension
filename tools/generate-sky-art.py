"""Authoring helper for sky-body textures (Earth seen from the Moon). No build or runtime dependency.

Run from the repository root:  python tools/generate-sky-art.py
Needs Pillow. Rewrites every PNG it owns under
modules/worlds/src/main/resources/assets/ascension_worlds/textures/environment/ and a preview under
design/sky/, so hand edits to those PNGs are lost on the next run -- edit this file instead.

The texture is the body's *face*: the disc inscribed in the square is what the renderer maps onto
the sphere (SkyBodyRenderer). It is drawn flat, with no lighting baked in, because the renderer
shades it by the sun to draw the phase. Original procedural pixel art, seeded and reproducible.
"""
import math
import os
import random

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
OUT = os.path.join(ROOT, 'modules/worlds/src/main/resources/assets/ascension_worlds/textures/environment')
DESIGN = os.path.join(ROOT, 'design/sky')

SIZE = 64

DEEP = (18, 46, 108)
OCEAN = (28, 72, 150)
SHALLOW = (52, 118, 178)
GRASS = [(58, 112, 52), (74, 128, 60), (92, 140, 66)]
FOREST = (40, 88, 44)
DESERT = [(186, 160, 108), (204, 180, 124)]
MOUNTAIN = (120, 108, 92)
ICE = (226, 236, 242)
CLOUD = (244, 248, 252)


class Noise3:
    """Seeded 3D value noise with smoothstep interpolation; enough for continents and clouds."""

    def __init__(self, seed):
        rng = random.Random(seed)
        self.perm = list(range(256))
        rng.shuffle(self.perm)
        self.perm += self.perm
        self.values = [rng.random() for _ in range(256)]

    def _lattice(self, x, y, z):
        return self.values[self.perm[(self.perm[(self.perm[x & 255] + y) & 255] + z) & 255]]

    def at(self, x, y, z):
        xi, yi, zi = math.floor(x), math.floor(y), math.floor(z)
        xf, yf, zf = x - xi, y - yi, z - zi
        sx, sy, sz = (t * t * (3 - 2 * t) for t in (xf, yf, zf))

        def lerp(a, b, t):
            return a + (b - a) * t

        c = [[[self._lattice(xi + i, yi + j, zi + k) for k in (0, 1)] for j in (0, 1)] for i in (0, 1)]
        x00 = lerp(c[0][0][0], c[1][0][0], sx)
        x10 = lerp(c[0][1][0], c[1][1][0], sx)
        x01 = lerp(c[0][0][1], c[1][0][1], sx)
        x11 = lerp(c[0][1][1], c[1][1][1], sx)
        return lerp(lerp(x00, x10, sy), lerp(x01, x11, sy), sz)

    def fbm(self, x, y, z, octaves=4):
        total, amplitude, frequency, norm = 0.0, 1.0, 1.0, 0.0
        for _ in range(octaves):
            total += amplitude * self.at(x * frequency, y * frequency, z * frequency)
            norm += amplitude
            amplitude *= 0.5
            frequency *= 2.0
        return total / norm


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def earth():
    land = Noise3(1969)
    detail = Noise3(7)
    clouds = Noise3(42)
    img = Image.new('RGBA', (SIZE, SIZE))
    tilt = math.radians(23.4)
    for py in range(SIZE):
        for px in range(SIZE):
            u = (px + 0.5) / SIZE * 2 - 1
            v = 1 - (py + 0.5) / SIZE * 2
            r = math.hypot(u, v)
            if r > 0.999:  # outside the disc: clamp to the rim so edge texels never sample a seam
                u, v = u / r * 0.999, v / r * 0.999
            z = math.sqrt(max(0.0, 1 - u * u - v * v))
            # Tilt the globe so the poles are not dead centre-top: it reads as a planet, not a map.
            y = v * math.cos(tilt) - z * math.sin(tilt)
            zz = v * math.sin(tilt) + z * math.cos(tilt)
            x = u
            latitude = math.degrees(math.asin(max(-1.0, min(1.0, y))))

            height = land.fbm(x * 1.6 + 3.1, y * 1.6 + 1.7, zz * 1.6 + 5.3, 5)
            if height < 0.5:
                colour = mix(DEEP, OCEAN, min(1.0, height / 0.42))
                if height > 0.485:
                    colour = SHALLOW
            else:
                wet = detail.fbm(x * 3 + 9, y * 3, zz * 3)
                if 8 < abs(latitude) < 34 and wet < 0.43:
                    colour = DESERT[int(wet * 10) % 2]
                elif height > 0.63:
                    colour = MOUNTAIN
                elif wet > 0.56:
                    colour = FOREST
                else:
                    colour = GRASS[int(height * 37) % 3]
            if abs(latitude) > 68 + 6 * detail.at(x * 4, y * 4, zz * 4):
                colour = ICE

            cover = clouds.fbm(x * 2.0 + 11, y * 6.0, zz * 2.0 + 2, 4)
            if cover > 0.62:
                colour = mix(colour, CLOUD, min(1.0, (cover - 0.62) / 0.06) * 0.85)
            img.putpixel((px, py), colour + (255,))
    return img


def preview(tex):
    """The texture as the renderer shows it: clipped to the disc, lit from the upper left, with a
    terminator, so a design change can be judged without starting the game."""
    out = Image.new('RGBA', (SIZE, SIZE), (8, 8, 14, 255))
    sun = (-0.6, 0.5, 0.62)
    norm = math.sqrt(sum(c * c for c in sun))
    sun = tuple(c / norm for c in sun)
    for py in range(SIZE):
        for px in range(SIZE):
            u = (px + 0.5) / SIZE * 2 - 1
            v = 1 - (py + 0.5) / SIZE * 2
            if u * u + v * v > 1:
                continue
            z = math.sqrt(1 - u * u - v * v)
            light = 0.07 + 0.93 * max(0.0, u * sun[0] + v * sun[1] + z * sun[2])
            r, g, b, _ = tex.getpixel((px, py))
            out.putpixel((px, py), (int(r * light), int(g * light), int(b * light), 255))
    return out


def main():
    os.makedirs(OUT, exist_ok=True)
    os.makedirs(DESIGN, exist_ok=True)
    tex = earth()
    tex.save(os.path.join(OUT, 'earth.png'))
    sheet = Image.new('RGBA', (SIZE * 12, SIZE * 6))
    sheet.paste(tex.resize((SIZE * 6, SIZE * 6), Image.NEAREST), (0, 0))
    sheet.paste(preview(tex).resize((SIZE * 6, SIZE * 6), Image.NEAREST), (SIZE * 6, 0))
    sheet.save(os.path.join(DESIGN, 'earth-preview.png'))
    print('wrote earth.png')


if __name__ == '__main__':
    main()
