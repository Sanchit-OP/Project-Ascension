#!/usr/bin/env python3
"""HUD sprites (docs/technical/hud.md). Smooth, original, drawn at 4x and 4 texels per GUI unit.

    python tools/generate-hud-art.py

Writes modules/hud/src/main/resources/assets/ascension_hud/textures/gui/hud/*.png and a
`blur` mcmeta next to each, so the game filters them smoothly instead of showing hard texels.
Deterministic: no randomness. Every shape is ours (art guide: nothing copied).

Sheets:
  bar.png    3 rows of 32 px (track, fill, frame) x [left cap 16 | middle 8 | right cap 16]
  badge.png  a dark disc, 72 px
  food.png   a drumstick, 48 px
  panel.png  a glass panel with a gold rim, 40 x 40: corners 16, middle 8 (nine-slice)
  icons.png  six stat icons in a row, 48 px each: attack damage, attack speed, armor,
             magic resist, move speed, omnivamp (white; the game tints them)
  ring.png   64 frames of a ring filling clockwise from the top, 8 x 8, each 72 px
"""
import json
import math
import os

from PIL import Image, ImageDraw

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
OUT = os.path.join(ROOT, 'modules', 'hud', 'src', 'main', 'resources', 'assets', 'ascension_hud',
                   'textures', 'gui', 'hud')

SS = 4          # supersampling
TEXELS = 4      # texels per GUI unit

BAR_H = 8 * TEXELS          # 32
CAP = 4 * TEXELS            # 16
MID = 2 * TEXELS            # 8
BAR_W = CAP + MID + CAP     # 40

BADGE = 18 * TEXELS         # 72
FRAMES = 64
GRID = 8


def hexc(h, a=255):
    return (int(h[1:3], 16), int(h[3:5], 16), int(h[5:7], 16), a)


def pill(w, h, fill, outline=None, outline_w=0):
    img = Image.new('RGBA', (w * SS, h * SS), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([0, 0, w * SS - 1, h * SS - 1], radius=h * SS // 2, fill=fill,
                        outline=outline, width=outline_w * SS)
    return img.resize((w, h), Image.LANCZOS)


def gradient_pill(w, h, top, bottom):
    grad = Image.new('RGBA', (w * SS, h * SS))
    gd = ImageDraw.Draw(grad)
    for y in range(h * SS):
        t = y / (h * SS - 1)
        gd.line([(0, y), (w * SS, y)], fill=tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(4)))
    mask = Image.new('L', (w * SS, h * SS), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, w * SS - 1, h * SS - 1], radius=h * SS // 2, fill=255)
    grad.putalpha(mask)
    # a soft lit band along the top, like the mockups
    hl = Image.new('RGBA', grad.size, (0, 0, 0, 0))
    ImageDraw.Draw(hl).rounded_rectangle([h * SS // 4, h * SS // 10, w * SS - h * SS // 4, h * SS * 3 // 10],
                                         radius=h * SS // 10, fill=(255, 255, 255, 70))
    grad.alpha_composite(hl)
    return grad.resize((w, h), Image.LANCZOS)


def bar_sheet():
    sheet = Image.new('RGBA', (BAR_W, BAR_H * 3), (0, 0, 0, 0))
    # row 0: the dark track
    sheet.alpha_composite(pill(BAR_W, BAR_H, hexc('#05070c', 230)), (0, 0))
    # row 1: a white-grey gradient the game tints to any colour
    sheet.alpha_composite(gradient_pill(BAR_W, BAR_H, (255, 255, 255, 255), (150, 150, 150, 255)), (0, BAR_H))
    # row 2: the gold rim, drawn over everything
    rim = pill(BAR_W, BAR_H, (0, 0, 0, 0), outline=hexc('#c9a45a', 190), outline_w=2)
    sheet.alpha_composite(rim, (0, BAR_H * 2))
    return sheet


def badge_sheet():
    img = Image.new('RGBA', (BADGE * SS, BADGE * SS), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse([0, 0, BADGE * SS - 1, BADGE * SS - 1], fill=hexc('#0a0e18', 245))
    return img.resize((BADGE, BADGE), Image.LANCZOS)


def ring_frame(fraction):
    size = BADGE * SS
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    width = int(1.4 * TEXELS * SS)
    inset = int(0.7 * TEXELS * SS)
    box = [inset, inset, size - 1 - inset, size - 1 - inset]
    if fraction >= 0.999:
        d.ellipse(box, outline=(255, 255, 255, 255), width=width)
    elif fraction > 0:
        d.arc(box, -90, -90 + 360 * fraction, fill=(255, 255, 255, 255), width=width)
    return img.resize((BADGE, BADGE), Image.LANCZOS)


def ring_sheet():
    sheet = Image.new('RGBA', (BADGE * GRID, BADGE * GRID), (0, 0, 0, 0))
    for i in range(FRAMES):
        frame = ring_frame(i / (FRAMES - 1))
        sheet.alpha_composite(frame, ((i % GRID) * BADGE, (i // GRID) * BADGE))
    return sheet


def food_icon():
    size = 12 * TEXELS
    big = size * SS
    img = Image.new('RGBA', (big, big), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c = big / 2
    bone = (244, 246, 250, 255)
    d.line([(c - big * 0.30, c + big * 0.30), (c, c)], fill=bone, width=int(big * 0.15))
    d.ellipse([c - big * 0.36 - big * 0.085, c + big * 0.22 - big * 0.085, c - big * 0.36 + big * 0.085, c + big * 0.22 + big * 0.085], fill=bone)
    d.ellipse([c - big * 0.22 - big * 0.085, c + big * 0.36 - big * 0.085, c - big * 0.22 + big * 0.085, c + big * 0.36 + big * 0.085], fill=bone)
    d.ellipse([c - big * 0.02, c - big * 0.50, c + big * 0.50, c + big * 0.02], fill=(232, 146, 58, 255))
    d.ellipse([c + big * 0.08, c - big * 0.40, c + big * 0.20, c - big * 0.28], fill=(255, 210, 154, 200))
    return img.resize((size, size), Image.LANCZOS)


def panel_sheet():
    w = h = CAP + MID + CAP
    big = (w * SS, h * SS)
    grad = Image.new('RGBA', big)
    gd = ImageDraw.Draw(grad)
    top, bot = hexc('#1c2436', 205), hexc('#0a0e18', 205)
    for y in range(big[1]):
        t = y / (big[1] - 1)
        gd.line([(0, y), (big[0], y)], fill=tuple(int(top[i] + (bot[i] - top[i]) * t) for i in range(4)))
    mask = Image.new('L', big, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, big[0] - 1, big[1] - 1], radius=CAP * SS, fill=255)
    grad.putalpha(mask)
    rim = Image.new('RGBA', big, (0, 0, 0, 0))
    ImageDraw.Draw(rim).rounded_rectangle([0, 0, big[0] - 1, big[1] - 1], radius=CAP * SS,
                                          outline=hexc('#c9a45a', 150), width=2 * SS)
    grad.alpha_composite(rim)
    return grad.resize((w, h), Image.LANCZOS)


ICON = 48


def _poly(d, pts, rot=0.0, scale=1.0):
    a = math.radians(rot)
    out = []
    for (x, y) in pts:
        dx, dy = x - 50, y - 50
        rx = dx * math.cos(a) - dy * math.sin(a) + 50
        ry = dx * math.sin(a) + dy * math.cos(a) + 50
        out.append((rx * scale * ICON * SS / 100.0, ry * scale * ICON * SS / 100.0))
    d.polygon(out, fill=(255, 255, 255, 255))


def icon_sheet():
    names = ['attack_damage', 'attack_speed', 'armor', 'magic_resist', 'move_speed', 'omnivamp']
    sheet = Image.new('RGBA', (ICON * len(names), ICON), (0, 0, 0, 0))
    for i, name in enumerate(names):
        img = Image.new('RGBA', (ICON * SS, ICON * SS), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        if name == 'attack_damage':
            _poly(d, [(50, 0), (57, 12), (55, 62), (45, 62), (43, 12)], 40)
            _poly(d, [(28, 62), (72, 62), (72, 69), (28, 69)], 40)
            _poly(d, [(46, 69), (54, 69), (54, 88), (46, 88)], 40)
        elif name == 'attack_speed':
            for k in range(3):
                o = k * 24
                _poly(d, [(8 + o, 18), (24 + o, 18), (44 + o, 50), (24 + o, 82), (8 + o, 82), (28 + o, 50)], 0, 0.82)
        elif name == 'armor':
            _poly(d, [(50, 4), (88, 17), (83, 54), (50, 96), (17, 54), (12, 17)])
        elif name == 'magic_resist':
            _poly(d, [(50, 4), (90, 27), (90, 73), (50, 96), (10, 73), (10, 27)])
            # a diamond cut out of the middle
            d.polygon([(50 * ICON * SS / 100, 28 * ICON * SS / 100), (68 * ICON * SS / 100, 50 * ICON * SS / 100),
                       (50 * ICON * SS / 100, 72 * ICON * SS / 100), (32 * ICON * SS / 100, 50 * ICON * SS / 100)],
                      fill=(0, 0, 0, 0))
        elif name == 'move_speed':
            _poly(d, [(34, 8), (62, 8), (62, 48), (90, 62), (90, 88), (26, 88), (26, 70), (34, 70)])
        elif name == 'omnivamp':
            _poly(d, [(50, 4), (80, 52), (20, 52)])
            r = 0.31 * ICON * SS
            cx, cy = 0.5 * ICON * SS, 0.63 * ICON * SS
            d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(255, 255, 255, 255))
        sheet.alpha_composite(img.resize((ICON, ICON), Image.LANCZOS), (i * ICON, 0))
    return sheet


def save(name, img):
    path = os.path.join(OUT, name)
    img.save(path)
    with open(path + '.mcmeta', 'w', encoding='utf-8') as f:
        json.dump({'texture': {'blur': True}}, f)


def main():
    os.makedirs(OUT, exist_ok=True)
    save('bar.png', bar_sheet())
    save('badge.png', badge_sheet())
    save('food.png', food_icon())
    save('ring.png', ring_sheet())
    save('panel.png', panel_sheet())
    save('icons.png', icon_sheet())
    print('wrote', OUT)


if __name__ == '__main__':
    main()
