#!/usr/bin/env python3
"""Textures for the Tome of Scrapping and the Tome of Extraction (docs/progression/enchantments.md).

Two 16 x 16 item icons, drawn as hand-placed pixel maps and finished with the rune art's `shade`
helper (light from the top-left, a one-pixel outline). Everything is deterministic: re-running
reproduces both PNGs byte for byte.

  Tome of Scrapping    ash-grey cover, three scratches slanting across it: an enchantment wiped off.
  Tome of Extraction   deep violet cover, a gold clasp and three motes rising from it: an
                       enchantment drawn out.

Art rules (docs/technical/art-guide.md): original, no teal (the gateway network's), no cross, no
letter, no face. Both are books seen from the same angle so they read as a pair in an inventory.

Run:  python tools/generate-tome-art.py
"""

import importlib.util
import os

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(ROOT, 'modules', 'combat', 'src', 'main', 'resources', 'assets', 'ascension_combat',
                   'textures', 'item')


def load(name, filename):
    spec = importlib.util.spec_from_file_location(name, os.path.join(HERE, filename))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


rune = load('rune_art', 'generate-rune-art.py')
hexc, MaskShape, shade = rune.hexc, rune.MaskShape, rune.shade

# A book from above and the left: cover '#', its lit spine edge 'h', pages down the right and
# bottom are painted on afterwards.
BOOK = MaskShape([
    "................",
    "..##########....",
    ".#hh########....",
    ".#h#########....",
    ".#h#########....",
    ".#h#########....",
    ".#h#########....",
    ".#h#########....",
    ".#h#########....",
    ".#h#########....",
    ".#h#########....",
    ".#h#########....",
    ".#h#########....",
    "..###########...",
    "................",
    "................",
])

ASH = [hexc('#25222c'), hexc('#4a4654'), hexc('#6c6878'), hexc('#8e8a9a'), hexc('#b4b0c0')]
VIOLET = [hexc('#2a1442'), hexc('#4a2278'), hexc('#6a38a8'), hexc('#8a58cc'), hexc('#b088ee')]
PAGES = [hexc('#a89c84'), hexc('#d8ccb0'), hexc('#efe6cc')]
EMBER = [hexc('#7a2e10'), hexc('#d4561c'), hexc('#ff9a3c')]
GOLD = [hexc('#7a5a14'), hexc('#c8962c'), hexc('#f4d060')]


def paste_pages(img, tint):
    """The page block: a strip down the right edge and a lower edge, three tones."""
    for y in range(2, 13):
        img.putpixel((12, y), tint[1])
        img.putpixel((13, y), tint[2] if y < 12 else tint[1])
    for x in range(3, 13):
        img.putpixel((x, 13), tint[1])
    img.putpixel((13, 13), tint[0])
    img.putpixel((12, 13), tint[0])


def scrapping():
    img = shade(BOOK, ASH)
    paste_pages(img, PAGES)
    # Three scratches slanting down to the right, three apart so they stay separate strokes,
    # ember-bright where the cover was torn and dark at the tail.
    for (x0, y0) in ((3, 3), (6, 4), (9, 5)):
        for i in range(4):
            img.putpixel((x0 + i, y0 + i), EMBER[2] if i == 0 else EMBER[1])
        img.putpixel((x0 + 3, y0 + 3), EMBER[0])
    return img


def extraction():
    img = shade(BOOK, VIOLET)
    paste_pages(img, PAGES)
    # A gold clasp low on the cover, and three motes drifting up from it.
    for x in range(5, 10):
        img.putpixel((x, 11), GOLD[1])
    img.putpixel((5, 11), GOLD[2])
    img.putpixel((9, 11), GOLD[0])
    for (x, y, c) in ((6, 8, 2), (8, 6, 1), (6, 4, 2)):
        img.putpixel((x, y), GOLD[c])
    return img


def main():
    os.makedirs(OUT, exist_ok=True)
    scrapping().save(os.path.join(OUT, 'tome_of_scrapping.png'))
    extraction().save(os.path.join(OUT, 'tome_of_extraction.png'))
    print('wrote tome_of_scrapping.png and tome_of_extraction.png')


if __name__ == '__main__':
    main()
