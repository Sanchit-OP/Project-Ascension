"""Planet 4's small ruins: the stones on the central island near the Dragon's arena, and the lone outpost stone on an
outer island, each holding End archive tablets (docs/lore/the-end.md; the text is tools/archive-lore.py). No build or
runtime dependency.

Run from the repository root:  python tools/generate-end-ruin.py
Writes, and hand edits to either are lost on the next run (edit this file):

  data/ascension_worlds/structure/end_ruin/ruin.nbt      five plinths in a half-ring of broken wall (central island)
  data/ascension_worlds/structure/end_ruin/outpost.nbt   one plinth among broken pillars (an outer island)

Deterministic like every other piece of art here: the only randomness is a seeded generator, and the NBT is written
by the Lunar Archive generator's writer (sorted palette, fixed block order, gzip with no name and mtime 0), so a re-run
reproduces the committed files byte for byte. Vanilla blocks and the archive plinth only; nothing here is a texture.
Every template's floor is row 1 (one row of footing under it), as the Nether's ruins are.
"""
import importlib.util
import os
import random

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, '..'))
OUT = os.path.join(ROOT, 'modules/worlds/src/main/resources/data/ascension_worlds/structure/end_ruin')
NS = 'ascension_worlds'


def _load(name, filename):
    spec = importlib.util.spec_from_file_location(name, os.path.join(HERE, filename))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


lunar = _load('lunar_archive', 'generate-lunar-archive.py')   # Piece and the NBT writer
Piece, B, template_nbt = lunar.Piece, lunar.B, lunar.template_nbt
CORE, WALL, OPEN, DETAIL = lunar.CORE, lunar.WALL, lunar.OPEN, lunar.DETAIL


def mc(name, **props):
    return B('minecraft:' + name, **props)


AIR = 'minecraft:air'
RAW = mc('end_stone')
BRICK = mc('end_stone_bricks')
PURPUR = mc('purpur_block')
PILLAR = mc('purpur_pillar')
SLAB = mc('end_stone_brick_slab', type='bottom', waterlogged='false')
ROD = mc('end_rod', facing='up')
F = 1   # the floor row


def plinth(p, x, y, z, entry):
    p.put(x, y, z, B('archive_plinth', facing='south'), DETAIL,
          nbt={'id': f'{NS}:archive_plinth', 'entry': f'{NS}:the_end/{entry}'})
    p.plinths.append(entry)


def pave(p, rng, x0, z0, x1, z1):
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            p.put(x, 0, z, RAW, CORE)
            r = rng.random()
            p.put(x, F, z, PURPUR if r < 0.18 else BRICK, WALL)
            for y in range(F + 1, p.size[1]):
                p.put(x, y, z, AIR, OPEN)


def wall(p, rng, x0, z0, x1, z1, top):
    """A straight wall, ruined: corners and a few fragments stand, the rest is worn low."""
    n = max(abs(x1 - x0), abs(z1 - z0))
    sx = (x1 > x0) - (x1 < x0)
    sz = (z1 > z0) - (z1 < z0)
    for i in range(n + 1):
        x, z = x0 + sx * i, z0 + sz * i
        stand = 1.0 if i in (0, n) else rng.choice([0.0, 0.25, 0.25, 0.5, 0.75, 1.0])
        h = F + max(1, int(round(stand * (top - F))))
        for y in range(F + 1, h + 1):
            p.put(x, y, z, PILLAR if i in (0, n) else BRICK)
        if rng.random() < 0.3:
            p.put(x, h + 1, z, SLAB)


def ruin():
    p = Piece('ruin', (17, 9, 13))
    p.plinths = []
    rng = random.Random(4041)
    pave(p, rng, 0, 0, 16, 12)
    wall(p, rng, 0, 0, 16, 0, 5)       # the back wall, north
    wall(p, rng, 0, 0, 0, 8, 4)        # the west flank
    wall(p, rng, 16, 0, 16, 8, 4)      # the east flank
    # A shallow arc of five plinths facing south, the first the one a visitor meets on the left.
    for x, z, entry in ((2, 4, 'the_belt'), (5, 3, 'the_wings'), (8, 3, 'the_keeper'), (11, 3, 'the_fruit'),
                        (14, 4, 'the_watchers')):
        plinth(p, x, F + 1, z, entry)
    # A little light, and a few worn blocks of rubble on the paving.
    for x, z in ((0, 0), (16, 0), (0, 8), (16, 8)):
        top = max(y for y in range(F, p.size[1]) if p.get(x, y, z) not in (None, AIR))
        p.put(x, top + 1, z, ROD)
    for _ in range(9):
        x, z = rng.randint(1, 15), rng.randint(5, 11)
        if p.get(x, F + 1, z) in (None, AIR):
            p.put(x, F + 1, z, SLAB)
    return p


def outpost():
    p = Piece('outpost', (7, 7, 7))
    p.plinths = []
    rng = random.Random(4042)
    pave(p, rng, 0, 0, 6, 6)
    plinth(p, 3, F + 1, 3, 'the_edge')
    for x, z in ((0, 0), (6, 0), (0, 6), (6, 6)):
        for y in range(F + 1, F + 1 + rng.randint(2, 4)):
            p.put(x, y, z, PILLAR)
    p.put(0, F + 5, 0, ROD)
    p.put(6, F + 5, 0, ROD)
    return p


def main():
    os.makedirs(OUT, exist_ok=True)
    for piece in (ruin(), outpost()):
        for entry in piece.plinths:
            assert entry
        path = os.path.join(OUT, piece.name + '.nbt')
        with open(path, 'wb') as f:
            f.write(template_nbt(piece))
        print(f'wrote {os.path.relpath(path, ROOT)}: {piece.size}, plinths {piece.plinths}')


if __name__ == '__main__':
    main()
