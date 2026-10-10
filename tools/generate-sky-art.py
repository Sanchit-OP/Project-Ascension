"""Authoring helper for sky-body textures (Earth seen from the Moon, the Moon seen from Earth). No
build or runtime dependency.

Run from the repository root:  python tools/generate-sky-art.py
Needs Pillow. Rewrites every PNG it owns under
modules/worlds/src/main/resources/assets/ascension_worlds/textures/environment/, the blank vanilla
moon override, and previews under design/sky/, so hand edits to those PNGs are lost on the next
run -- edit this file instead.

The texture is the body's *face*: the disc inscribed in the square is what the renderer maps onto
the sphere (SkyBodyRenderer). It is drawn flat, with no lighting baked in, because the renderer
shades it by the sun (or the moon phase) to draw the phase. Original procedural pixel art, seeded
and reproducible.

The blank override: minecraft:textures/environment/moon_phases.png, fully transparent at vanilla's
size, so vanilla's additively blended moon adds nothing and ours (ascension_sky/overworld.json) is
the only Moon over Earth. Nothing of vanilla's image is read or copied; this file only makes an
empty one.
"""
import math
import os
import random

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
OUT = os.path.join(ROOT, 'modules/worlds/src/main/resources/assets/ascension_worlds/textures/environment')
VANILLA_OUT = os.path.join(ROOT, 'modules/worlds/src/main/resources/assets/minecraft/textures/environment')
DESIGN = os.path.join(ROOT, 'design/sky')
# Vanilla's moon_phases.png is a 4 x 2 grid of 32 px phases; the blank keeps that size so the UVs
# vanilla computes still land inside it.
VANILLA_MOON_SIZE = (128, 64)

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

# The Moon from Earth: the same two grounds you walk on there, regolith highlands and basalt maria
# (moon_regolith, moon_basalt), lifted toward full moonlight because the sky body is drawn by
# adding light, and dark texels would only read as a dim moon.
HIGHLAND = [(206, 204, 196), (218, 216, 208), (194, 192, 186)]
MARE = [(112, 116, 130), (124, 128, 140)]
CRATER_FLOOR = (150, 150, 148)
CRATER_RIM = (238, 236, 228)
RAY = (232, 230, 222)


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


def sphere_point(px, py):
    """The point of the visible hemisphere under a texel, (x right, y up, z toward the viewer)."""
    u = (px + 0.5) / SIZE * 2 - 1
    v = 1 - (py + 0.5) / SIZE * 2
    r = math.hypot(u, v)
    if r > 0.999:  # outside the disc: clamp to the rim so edge texels never sample a seam
        u, v = u / r * 0.999, v / r * 0.999
    return u, v, math.sqrt(max(0.0, 1 - u * u - v * v))


def unit(a):
    n = math.sqrt(sum(c * c for c in a))
    return tuple(c / n for c in a)


def moon():
    """The near side: pale regolith highlands, dark basalt maria massed toward the upper half, a
    scatter of craters (dark floor, bright rim), and one young crater low on the disc throwing
    bright rays across everything."""
    maria = Noise3(1610)
    detail = Noise3(27)
    rng = random.Random(1609)
    craters = []
    while len(craters) < 16:
        u, v = rng.uniform(-0.9, 0.9), rng.uniform(-0.9, 0.9)
        if u * u + v * v > 0.8:
            continue
        craters.append((unit((u, v, math.sqrt(1 - u * u - v * v))), rng.uniform(0.05, 0.15)))
    young = unit((0.16, -0.58, math.sqrt(1 - 0.16 ** 2 - 0.58 ** 2)))
    up = (0.0, 1.0, 0.0)
    t1 = unit((up[1] * young[2] - up[2] * young[1], up[2] * young[0] - up[0] * young[2],
               up[0] * young[1] - up[1] * young[0]))
    t2 = (young[1] * t1[2] - young[2] * t1[1], young[2] * t1[0] - young[0] * t1[2],
          young[0] * t1[1] - young[1] * t1[0])

    img = Image.new('RGBA', (SIZE, SIZE))
    for py in range(SIZE):
        for px in range(SIZE):
            x, y, z = sphere_point(px, py)
            grain = detail.fbm(x * 5 + 3, y * 5 + 8, z * 5 + 1, 3)
            colour = HIGHLAND[0] if grain < 0.45 else HIGHLAND[1] if grain < 0.58 else HIGHLAND[2]
            sea = maria.fbm(x * 2.3 + 2.2, y * 2.3 + 4.1, z * 2.3 + 0.7, 4) + 0.05 * y - 0.04 * x
            if sea > 0.57:
                colour = MARE[0] if grain < 0.52 else MARE[1]
            for centre, radius in craters:
                d = math.acos(max(-1.0, min(1.0, x * centre[0] + y * centre[1] + z * centre[2])))
                if d < radius * 0.7:
                    colour = mix(colour, CRATER_FLOOR, 0.6)
                elif d < radius:
                    colour = CRATER_RIM
            d = math.acos(max(-1.0, min(1.0, x * young[0] + y * young[1] + z * young[2])))
            if d < 0.9:
                bearing = math.atan2(x * t2[0] + y * t2[1] + z * t2[2], x * t1[0] + y * t1[1] + z * t1[2])
                ray = max(0.0, math.cos(9 * bearing + 2 * detail.at(x * 3, y * 3, z * 3))) ** 24
                colour = mix(colour, RAY, 0.6 * ray * (1 - d / 0.9))
            if d < 0.06:
                colour = CRATER_RIM
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


def write_body(name, tex):
    tex.save(os.path.join(OUT, name + '.png'))
    sheet = Image.new('RGBA', (SIZE * 12, SIZE * 6))
    sheet.paste(tex.resize((SIZE * 6, SIZE * 6), Image.NEAREST), (0, 0))
    sheet.paste(preview(tex).resize((SIZE * 6, SIZE * 6), Image.NEAREST), (SIZE * 6, 0))
    sheet.save(os.path.join(DESIGN, name + '-preview.png'))
    print('wrote ' + name + '.png')


def main():
    os.makedirs(OUT, exist_ok=True)
    os.makedirs(VANILLA_OUT, exist_ok=True)
    os.makedirs(DESIGN, exist_ok=True)
    write_body('earth', earth())
    write_body('moon', moon())
    Image.new('RGBA', VANILLA_MOON_SIZE, (0, 0, 0, 0)).save(os.path.join(VANILLA_OUT, 'moon_phases.png'))
    print('wrote the blank minecraft:textures/environment/moon_phases.png')


if __name__ == '__main__':
    main()
