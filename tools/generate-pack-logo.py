#!/usr/bin/env python3
"""The pack's logo: pack/launch/logo.png (512 x 512, what the CurseForge page takes) and logo-1024.png (the master).

  python tools/generate-pack-logo.py

Original art from a script, flat colour only (art-guide.md): no gradient, no glow, no texture, nothing random, so a rerun writes the
same bytes. Drawn on a 1000 x 1000 grid at three times the size and reduced.

What it says, from the outside in:
  the frame        a brass border with a diamond at each compass point, as on the quest book's star chart
  the orbit        a thin ellipse round the gate with small diamonds on it: the End's asteroid belt, and any planet's path
  the gate         the letter A, standing as the ancients' gate does: two pillars with a split top, a threshold bar, a plinth, and in
                   the opening four lines of teal light rising (the active gate's field)
  the eye          a hollow diamond floating over the split, a teal pupil in it: the network's motif, which the real gate carries
  the type         PROJECT in small tracked capitals, ASCENSION large, in Barlow SemiBold (already in the HUD module)
Colours are imported from the other art scripts, never copied: brass for the work of the builders, teal only for the network.
"""

import importlib.util
import math
import os

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, '..'))
OUT = os.path.join(ROOT, 'pack', 'launch')
FONT = os.path.join(ROOT, 'modules', 'hud', 'src', 'main', 'resources', 'assets', 'ascension_hud', 'font', 'barlow_semibold.ttf')


def _load(name, filename):
    spec = importlib.util.spec_from_file_location(name, os.path.join(HERE, filename))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


_runes = _load('rune_art', 'generate-rune-art.py')
_gateway = _load('gateway_art', 'generate-gateway-art.py')

SKY = [tuple(c[:3]) for c in _runes.SKY]            # night, darkest first
BRASS = [tuple(c[:3]) for c in _runes.STAR_BRASS]   # shadow, mid, main, highlight
TEAL = [tuple(c[:3]) for c in _gateway.TEAL]        # darkest first
CREAM = (246, 238, 214)

GRID = 1000
SCALE = 3


def S(value):
    return value * SCALE


def diamond(draw, cx, cy, a, b, fill):
    """A rhombus with half-width a and half-height b."""
    draw.polygon([(S(cx), S(cy - b)), (S(cx + a), S(cy)), (S(cx), S(cy + b)), (S(cx - a), S(cy))], fill=fill)


def rect(draw, x0, y0, x1, y1, fill):
    draw.rectangle((S(x0), S(y0), S(x1) - 1, S(y1) - 1), fill=fill)


def parallelogram(draw, x_bottom, x_top, y_bottom, y_top, width, fill):
    """A leaning strip: horizontal width `width`, left edge at x_bottom (bottom) and x_top (top)."""
    draw.polygon([(S(x_bottom), S(y_bottom)), (S(x_bottom + width), S(y_bottom)),
                  (S(x_top + width), S(y_top)), (S(x_top), S(y_top))], fill=fill)


def tracked_text(draw, text, font, cx, baseline, tracking, fill):
    """Capitals set with extra space between letters, centred on cx. Positions are in final (scaled) pixels."""
    widths = [font.getlength(ch) for ch in text]
    total = sum(widths) + tracking * (len(text) - 1)
    x = S(cx) - total / 2.0
    for ch, w in zip(text, widths):
        draw.text((x, S(baseline)), ch, font=font, fill=fill, anchor='ls')
        x += w + tracking
    return total / SCALE


def fit_font(text, cap_height, tracking_em, max_width):
    """The Barlow size whose capitals are `cap_height` tall (on the grid), shrunk if the set word would be wider than max_width."""
    probe = ImageFont.truetype(FONT, 200)
    cap_ratio = (-probe.getbbox('H', anchor='ls')[1]) / 200.0
    size = cap_height / cap_ratio
    while True:
        font = ImageFont.truetype(FONT, int(round(S(size))))
        tracking = font.size * tracking_em
        width = (sum(font.getlength(ch) for ch in text) + tracking * (len(text) - 1)) / SCALE
        if width <= max_width or size < 10:
            return font, tracking
        size *= 0.97


def draw_logo():
    image = Image.new('RGB', (S(GRID), S(GRID)), SKY[1])
    draw = ImageDraw.Draw(image)

    # ---- the frame: a brass border, a hairline inside it, a diamond at each compass point ------------------
    inset = 34
    for offset, width, colour in ((inset, 7, BRASS[1]), (inset + 14, 2.5, BRASS[0])):
        draw.rectangle((S(offset), S(offset), S(GRID - offset) - 1, S(GRID - offset) - 1), outline=colour, width=int(S(width)))
    for x, y in ((GRID / 2, inset), (GRID / 2, GRID - inset), (inset, GRID / 2), (GRID - inset, GRID / 2)):
        diamond(draw, x, y, 24, 24, BRASS[1])
        diamond(draw, x, y, 17, 17, SKY[0])
        diamond(draw, x, y, 7, 7, BRASS[2])

    # ---- the orbit: back half behind the gate, front half before it -----------------------------------------
    cx, cy, rx, ry = 500, 398, 318, 90
    box = (S(cx - rx), S(cy - ry), S(cx + rx), S(cy + ry))
    stroke = int(S(7.5))
    draw.arc(box, 180, 360, fill=BRASS[1], width=stroke)

    # Small diamonds on the orbit: fixed angles and sizes, so the logo never changes between runs.
    rocks = [(22, 13), (74, 8), (128, 11), (186, 7), (248, 12), (302, 8), (338, 10)]

    def rock(angle, size):
        t = math.radians(angle)
        return cx + rx * math.cos(t), cy + ry * math.sin(t), size

    for angle, size in rocks:
        x, y, s = rock(angle, size)
        if y < cy:                                    # the far side, drawn before the gate
            diamond(draw, x, y, s * 0.8, s, BRASS[3])

    # ---- the gate: the letter A -------------------------------------------------------------------------------
    y_base, y_top = 704, 296
    x_left_base, x_left_top = 300, 398
    width = 70

    # Threshold bar, drawn first so the legs pass in front of its ends.
    rect(draw, 360, 528, 640, 564, BRASS[2])
    rect(draw, 360, 528, 640, 534, BRASS[3])
    rect(draw, 360, 558, 640, 564, BRASS[1])

    # Each leg is three strips: a lit edge, the body, a shaded edge. Light comes from the top left, so on both legs the left strip is lit.
    for bottom, top in ((x_left_base, x_left_top), (GRID - x_left_base - width, GRID - x_left_top - width)):
        for start, strip, colour in ((0, 20, BRASS[3]), (20, 34, BRASS[2]), (54, 16, BRASS[1])):
            parallelogram(draw, bottom + start, top + start, y_base, y_top, strip, colour)

    # The light in the opening: three chevrons climbing, the highest the brightest (the active gate's field, and the word ascension).
    for apex, half, colour in ((592, 86, TEAL[4]), (630, 64, TEAL[3]), (668, 42, TEAL[2])):
        drop, thick = half * 0.45, 12
        draw.polygon([(S(500 - half), S(apex + drop)), (S(500), S(apex)), (S(500 + half), S(apex + drop)),
                      (S(500 + half), S(apex + drop + thick)), (S(500), S(apex + thick)), (S(500 - half), S(apex + drop + thick))],
                     fill=colour)

    # The plinth.
    rect(draw, 256, y_base, 744, y_base + 28, BRASS[2])
    rect(draw, 256, y_base, 744, y_base + 6, BRASS[3])
    rect(draw, 256, y_base + 22, 744, y_base + 28, BRASS[1])

    # The orbit's near half, over the legs, and the rocks on it.
    draw.arc(box, 0, 180, fill=BRASS[1], width=stroke)
    for angle, size in rocks:
        x, y, s = rock(angle, size)
        if y >= cy:
            diamond(draw, x, y, s * 0.8, s, BRASS[3])

    # ---- the eye, floating over the split ---------------------------------------------------------------------
    ex, ey, ea, eb = 500, 190, 58, 82
    diamond(draw, ex, ey, ea, eb, BRASS[2])
    d = ea * eb / math.hypot(ea, eb)                   # distance from the middle to an edge
    inner = (d - 13) / d
    diamond(draw, ex, ey, ea * inner, eb * inner, SKY[0])
    # A lit edge on the upper left of the frame.
    draw.line([(S(ex - ea + 6), S(ey)), (S(ex), S(ey - eb + 9))], fill=BRASS[3], width=int(S(5)))
    diamond(draw, ex, ey, 15, 22, TEAL[4])
    diamond(draw, ex, ey, 6, 9, TEAL[5])

    # ---- the type ---------------------------------------------------------------------------------------------
    small, small_track = fit_font('PROJECT', 30, 0.62, 420)
    big, big_track = fit_font('ASCENSION', 100, 0.07, 800)
    tracked_text(draw, 'PROJECT', small, 500, 790, small_track, BRASS[2])
    tracked_text(draw, 'ASCENSION', big, 500, 922, big_track, CREAM)
    return image


def main():
    os.makedirs(OUT, exist_ok=True)
    big = draw_logo()
    master = big.resize((1024, 1024), Image.LANCZOS)
    master.save(os.path.join(OUT, 'logo-1024.png'), optimize=False)
    big.resize((512, 512), Image.LANCZOS).save(os.path.join(OUT, 'logo.png'), optimize=False)
    print('wrote pack/launch/logo.png (512) and logo-1024.png')


if __name__ == '__main__':
    main()
