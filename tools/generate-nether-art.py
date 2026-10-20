"""Planet 3's block art: the colony plinth, the colonists' mural and the Sealers' carving. No build
or runtime dependency.

Run from the repository root:  python tools/generate-nether-art.py
Needs Pillow. Rewrites every PNG, model, blockstate and block loot table it owns under
modules/worlds/src/main/resources/, and previews under design/nether/, so hand edits to those files
are lost on the next run -- edit this file.

Owns: the look of ascension_worlds:colony_plinth (logic's block: an archive plinth in every way but
its look), and two decorative blocks registered in NetherDecorBlocks, each one block with a `tile`
property and a model per tile, like the Archive's murals:

  colony_mural     the colonists' own carving, gold inlaid in black stone: colonists in gold turning
                   away a figure without it (the truth under the voice's "thieves")
  sealer_carving   the Sealers' carving for the soul sand valleys, in their style from the Moon
                   (carved pale plaster, figures inlaid in stone): a soldier falling and rising,
                   darker each time, until only a black skeleton stands

tools/generate-nether-colony.py reads the catalogues here to lay them into the templates.

Palettes and helpers come from the gateway, Moon and Archive scripts (art guide: import, never
copy); the Sealers' carving is cut with the Archive's own Canvas, so it is visibly the same hand as
the Moon's murals. Everything is original: shapes are drawn by code or placed by hand in pixel maps
in this file. Nothing is traced or sampled from vanilla, another mod or a pack. Design record:
docs/technical/nether-art-design.md.
"""
import importlib.util
import json
import os
import random
import re

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))


def _load(name, filename):
    spec = importlib.util.spec_from_file_location(name, os.path.join(HERE, filename))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


art = _load('gateway_art', 'generate-gateway-art.py')
moon = _load('moon_art', 'generate-moon-materials-art.py')
archive = _load('archive_art', 'generate-archive-art.py')
end_carvings = _load('end_carvings', 'generate-end-carvings.py')

BASALT, ANORTH, TEAL = art.BASALT, art.ANORTH, art.TEAL
mix, shade, new = art.mix, art.shade, art.new
ROOT = art.ROOT
RES = os.path.join(ROOT, 'modules/worlds/src/main/resources')
ASSETS = os.path.join(RES, 'assets/ascension_worlds')
DATA = os.path.join(RES, 'data/ascension_worlds')
TEX = os.path.join(ASSETS, 'textures')
DESIGN = os.path.join(ROOT, 'design/nether')
JAVA = os.path.join(ROOT, 'modules/worlds/src/main/java/com/ascension/worlds/internal/nether')
NS = 'ascension_worlds'

# The colony's stone: a cool, faintly violet black, the colonists' dressed rock (the gate's
# blackstone is vanilla's; this is ours, for the plinth and the mural ground). Gold: the colonists'
# ward, warm, a metal, so it gets a specular top edge and a dark rim on its shadow side.
COLONY = [(17, 14, 19), (26, 22, 29), (36, 31, 40), (47, 41, 52), (60, 53, 66), (76, 68, 83)]
GOLD = [(86, 52, 12), (146, 94, 20), (200, 142, 34), (234, 188, 58), (250, 222, 112), (255, 246, 196)]
# The figure without gold: ash, unwarded. The rot on it: a dull bruise colour, no glow.
ASHEN = [(70, 64, 62), (98, 90, 86), (124, 116, 110)]
ROT = (78, 62, 84)
# The returned soldier, inlaid darker at each return: living warm stone, then ash, then black.
RETURNED = [(150, 96, 72), (110, 62, 46), (72, 40, 34), (22, 20, 24)]

# --- the catalogues ----------------------------------------------------------------------------------
#
# Tile 0 of each block is bare ground (polished colony stone, or bare plaster); each picture's tiles
# follow on, row by row from its top-left. Appending is safe; reordering renumbers every tile after
# the change and breaks the templates until they are regenerated.

COLONY_ORDER = [('turned_away', (5, 2))]
# The Nether's, then the End's (generate-end-carvings.py draws those three).
CARVING_ORDER = [('the_returned', (7, 2))] + end_carvings.END_CARVING_ORDER


def _catalogue(order):
    out, n = {}, 1
    for name, size in order:
        out[name] = {'size': size, 'first': n}
        n += size[0] * size[1]
    return out, n


COLONY_MURALS, COLONY_TILES = _catalogue(COLONY_ORDER)
CARVINGS, CARVING_TILES = _catalogue(CARVING_ORDER)


# --- the colony plinth ---------------------------------------------------------------------------------

def gold_band(rng):
    """Beaten gold: horizontal courses lit along their top, a dark rim along their foot, the odd
    specular glint. Used for the plinth's foot ring, its column's rings and the tablet's edge."""
    img = new()
    px = img.load()
    for y in range(16):
        row = y % 4
        for x in range(16):
            c = GOLD[3]
            if row == 0:
                c = GOLD[4]
            elif row == 3:
                c = GOLD[1]
            elif row == 2:
                c = mix(GOLD[3], GOLD[2], 0.5)
            c = shade(c, 1 + rng.uniform(-0.03, 0.03))
            if row == 0 and rng.random() < 0.1:
                c = GOLD[5]
            if x == 15:
                c = mix(c, GOLD[1], 0.5)
            px[x, y] = c + (255,)
    return img


def colony_stone(rng):
    return art.polished(COLONY, rng, base_idx=2)


def colony_column(rng):
    """The plinth's column: fluted colony stone with a gold ring at its head and its foot (the
    column element shows rows 5-11 of this texture)."""
    img = art.pillar_side(COLONY, rng)
    px = img.load()
    for y in (5, 11):
        for x in range(16):
            px[x, y] = (GOLD[4] if y == 5 else GOLD[2]) + (255,)
    for x in range(16):
        px[x, 6] = mix(px[x, 6][:3], GOLD[1], 0.5) + (255,)       # the ring's shadow on the stone
    return img


def colony_desk(rng):
    """The desk the tablet rests on: polished colony stone rimmed in gold. Its sides show rows 3-4
    (the rim), its top x 2-13, y 3-12."""
    img = art.polished(COLONY, rng, base_idx=2)
    px = img.load()
    for x in range(16):
        px[x, 3] = GOLD[4] + (255,)
        px[x, 4] = GOLD[2] + (255,)
    for y in range(3, 13):
        px[2, y] = GOLD[3] + (255,)
        px[13, y] = GOLD[1] + (255,)
    for x in range(2, 14):
        px[x, 12] = GOLD[1] + (255,)
    return img


def plinth():
    b = f'{NS}:block/'
    archive.plinth_model('colony_plinth', {
        'particle': b + 'colony_plinth_stone',
        'base': b + 'colony_plinth_stone', 'foot': b + 'colony_plinth_gold',
        'column': b + 'colony_plinth_column', 'column_top': b + 'colony_plinth_stone',
        'desk': b + 'colony_plinth_desk',
        # the tablet is the voice's, the same stone on every world; its edge is set in gold here
        'face': b + 'archive_tablet_face', 'edge': b + 'colony_plinth_gold',
        'glow': b + 'archive_tablet_face_glow',
    })


# --- the colonists' mural: gold-figure on black ----------------------------------------------------------
#
# The colonists cut their pictures the other way round from the Sealers: the ground is their dark
# stone and the figures are gold, because gold was what they were. Facing left, the colonists; the
# figure they turn away has no gold at all.

COLONIST = {
    # standing, facing left, one arm raised palm out: "no further"
    'R': [
        "..###...",
        ".#####..",
        ".#####..",
        "..###...",
        "...#....",
        "#.####..",
        "#######.",
        "..#####.",
        "..#####.",
        "..#####.",
        "..####..",
        "..####..",
        "..####..",
        "..####..",
        "..#..#..",
        ".##..##.",
    ],
    # standing, facing left, a spear held across the body
    'P': [
        "...###..",
        "..#####.",
        "..#####.",
        "...###..",
        "....#...",
        "..#####.",
        "#########",
        "..######",
        "..#####.",
        "..#####.",
        "..####..",
        "..####..",
        "..####..",
        "..####..",
        "..#..#..",
        ".##..##.",
    ],
    # the one without gold: stooped, facing right, a hand held out
    'H': [
        "........",
        "........",
        "...##...",
        "..####..",
        "..####..",
        "...####.",
        "..######",
        ".#####.#",
        ".#####..",
        ".#####..",
        "..####..",
        "..####..",
        "..####..",
        "..#.##..",
        "..#..#..",
        ".##..##.",
    ],
}


def m_turned_away(c):
    """Left to right: the rot's breath rising off a figure with no gold; the figure reaching out;
    the colonists in gold barring the way, the front one's hand raised, one holding a spear across;
    behind them their gate, banded in gold."""
    ground_y = 27
    archive.ground(c, ground_y, height=1)
    # the gate of their stronghold, right: posts and a lintel, gold banded
    for x in (66, 76):
        c.rect(x, 9, x + 2, ground_y - 1, height=1)
        c.rect(x, 12, x + 2, 12, inlay=GOLD[3], group='gate')
        c.rect(x, 20, x + 2, 20, inlay=GOLD[3], group='gate')
    c.rect(64, 6, 79, 8, height=1)
    c.rect(64, 7, 79, 7, inlay=GOLD[4], group='gate')
    for x in range(69, 76):
        c.px(x, 12, height=-1)                                      # the doorway's shadow
        for y in range(13, ground_y):
            c.px(x, y, height=-1)
    gold = dict(height=1, inlay=GOLD[3], group='colonists')
    c.sprite(COLONIST['R'], 42, ground_y - 16, {'#': gold})
    c.sprite(COLONIST['P'], 51, ground_y - 16, {'#': gold})
    c.sprite(COLONIST['R'], 59, ground_y - 16, {'#': gold})
    # the one without gold, and the rot's breath on it: a drift of dull specks round its head and
    # shoulders, rising behind it (specks, not strokes: a line of wiggles read as letters)
    ash = dict(height=1, inlay=ASHEN[1], group='outcast')
    c.sprite(COLONIST['H'], 22, ground_y - 16, {'#': ash})
    rot = random.Random(77)
    for _ in range(26):
        x = 22 + rot.gauss(1.5, 3.2)
        y = ground_y - 13 - abs(rot.gauss(0, 4.5))
        if c.ok(int(x), int(y)) and c.inlay[int(y)][int(x)] is None:
            c.px(x, y, inlay=ROT, group='rot')


# --- the Sealers' carving: the returned ---------------------------------------------------------------
#
# In the Sealers' own style (the Moon's murals and the Sealers' Record): carved pale plaster, the
# figures inlaid in stone. One soldier, seven times, read left to right: standing, fallen; rising,
# fallen; rising, fallen; then standing again as a black skeleton, taller, with its blade. Each
# return is inlaid in darker stone than the last.

SOLDIER = {
    # standing, facing right, blade held upright before it
    'S': [
        "...##.....",
        "..####...#",
        "..####...#",
        "...##....#",
        "..####...#",
        ".#######.#",
        ".#####.###",
        ".#####..#.",
        ".#####....",
        "..####....",
        "..####....",
        "..####....",
        "..####....",
        "..#..#....",
        "..#..#....",
        ".##..##...",
    ],
    # rising: one knee down, a hand on the ground, head up
    'K': [
        "..........",
        "..........",
        "..........",
        "..........",
        "....##....",
        "...####...",
        "...####...",
        "....##....",
        "...####...",
        "..######..",
        ".#.####...",
        ".#.#####..",
        ".#..#####.",
        ".#..##.##.",
        ".#.##..##.",
        "##.#....##",
    ],
}
# the black skeleton, taller than the man was: a blade in hand, ribs open to the plaster
SKELETON = [
    "...##.......",
    "..####.....#",
    "..####.....#",
    "...##......#",
    "...#.......#",
    ".#######...#",
    ".#.#.#.#..##",
    ".#######.##.",
    ".#.#.#.###..",
    "..#####.....",
    "...#.#......",
    "...###......",
    "...#.#......",
    "...#.#......",
    "...#.#......",
    "...#.#......",
    "...#.#......",
    "..##.##.....",
]


def lying(rows):
    """A standing sprite laid on its back, head to the left (archive.lying, for any pixel map)."""
    h, w = len(rows), len(rows[0])
    return [''.join(rows[h - 1 - y][x] for y in range(h))[::-1] for x in range(w)][::-1]


def unarmed(rows):
    """The standing soldier without its blade (the columns right of its body): the fallen have let
    it go, and only the first and the last of them hold one."""
    return [r[:7] + '.' * (len(r) - 7) for r in rows]


def m_the_returned(c):
    ground_y = 28
    archive.ground(c, ground_y, height=1)
    slot = 16
    seq = [('S', 0), ('L', 0), ('K', 1), ('L', 1), ('K', 2), ('L', 2), ('X', 3)]
    for i, (pose, shade_i) in enumerate(seq):
        ink = dict(height=1, inlay=RETURNED[shade_i], group=f'returned{i}')
        x0 = i * slot
        if pose == 'S':
            c.sprite(SOLDIER['S'], x0 + 3, ground_y - 16, {'#': ink})
        elif pose == 'K':
            c.sprite(SOLDIER['K'], x0 + 3, ground_y - 16, {'#': ink})
        elif pose == 'L':
            body = lying(unarmed(SOLDIER['S']))
            c.sprite(body, x0, ground_y - len(body), {'#': ink})
        else:
            c.sprite(SKELETON, x0 + 2, ground_y - len(SKELETON), {'#': ink})
    # the plaster the Sealers left bare above the falls: nothing to say
    return c


MURAL_FUNCS = {'turned_away': m_turned_away}
CARVING_FUNCS = {'the_returned': m_the_returned, **end_carvings.END_CARVING_FUNCS}


def colony_canvas(w, h, seed):
    return archive.Canvas(w, h, seed, ground=COLONY, stain_to=COLONY[0], base=0.52)


def colony_ground_tile(rng):
    """colony_mural tile 0: the bare dark stone, smooth and tileable."""
    img = new()
    p = img.load()
    low = moon.tile_noise(rng, 4)
    for y in range(16):
        for x in range(16):
            v = 0.52 + (low[y][x] - 0.5) * 0.10 + rng.uniform(-0.025, 0.025)
            p[x, y] = moon.ramp(COLONY, v) + (255,)
    return img


# --- models, blockstates, loot -------------------------------------------------------------------------

def write_json(rel, obj, root=ASSETS):
    path = os.path.join(root, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


def tiled_block(name, tiles):
    """One block, one model per tile: the tile on its four sides, tile 0 on its ends (as the
    Archive's murals are made), its item showing tile 0, its loot keeping its tile."""
    variants = {}
    for t in range(tiles):
        write_json(f'models/block/{name}/tile_{t}.json', {
            'parent': 'minecraft:block/cube_column',
            'textures': {'end': f'{NS}:block/{name}/tile_0', 'side': f'{NS}:block/{name}/tile_{t}'}})
        variants[f'tile={t}'] = {'model': f'{NS}:block/{name}/tile_{t}'}
    write_json(f'blockstates/{name}.json', {'variants': variants})
    write_json(f'models/item/{name}.json', {'parent': f'{NS}:block/{name}/tile_0'})
    write_json(f'loot_table/blocks/{name}.json', {
        'type': 'minecraft:block',
        'pools': [{'rolls': 1, 'entries': [{
            'type': 'minecraft:item', 'name': f'{NS}:{name}',
            'functions': [{'function': 'minecraft:copy_state', 'block': f'{NS}:{name}', 'properties': ['tile']}]}],
            'conditions': [{'condition': 'minecraft:survives_explosion'}]}]}, root=DATA)


def check_java():
    for cls, n in (('ColonyMuralBlock', COLONY_TILES), ('SealerCarvingBlock', CARVING_TILES)):
        src = open(os.path.join(JAVA, cls + '.java'), encoding='utf-8').read()
        m = re.search(r'TILES\s*=\s*(\d+)\s*;', src)
        if not m or int(m.group(1)) != n:
            raise SystemExit(f'{cls}.TILES is {m and m.group(1)}, the catalogue has {n}: update the Java constant')


# --- previews ------------------------------------------------------------------------------------------

def pictures_sheet(items, path, scale=6):
    """Each picture whole at `scale`, its tile grid faint over it, on a mid grey."""
    W = max(im.width for _, im in items) * scale + 40
    H = sum(im.height * scale + 34 for _, im in items) + 10
    out = Image.new('RGB', (W, H), (40, 42, 50))
    d = ImageDraw.Draw(out)
    y = 10
    for label, im in items:
        big = im.resize((im.width * scale, im.height * scale), Image.NEAREST).convert('RGB')
        out.paste(big, (20, y + 20))
        d.text((20, y + 4), label, fill=(230, 230, 230))
        for tx in range(0, im.width + 1, 16):
            d.line([(20 + tx * scale, y + 20), (20 + tx * scale, y + 20 + im.height * scale)], fill=(90, 90, 100))
        y += im.height * scale + 34
    out.save(path)


def plinth_preview(path):
    """The colony plinth beside the archive plinth, from its reader's side and from behind, drawn
    from their real models through the lore script's rasteriser (the structure tool's iso views
    draw mirror images; see art-guide.md)."""
    lore = _load('lore_art', 'generate-lore-art.py')
    rsp = _load('render_structure_preview', 'render-structure-preview.py')
    rim = lore._rim()
    vox = {(0, 0, 0): 'minecraft:polished_blackstone', (2, 0, 0): 'ascension_worlds:polished_moon_basalt',
           (0, 1, 0): f'{NS}:colony_plinth[facing=south]', (2, 1, 0): f'{NS}:archive_plinth[facing=south]'}
    shots = [lore.room_shot(rim, rsp, vox, (1.5, 3.2, 3.6), (1.5, 1.4, 0.5), size=(960, 720), background=(0.16, 0.16, 0.19)),
             lore.room_shot(rim, rsp, vox, (3.6, 2.6, -2.2), (1.5, 1.4, 0.5), size=(960, 720), background=(0.16, 0.16, 0.19))]
    w, h = shots[0].size
    out = Image.new('RGB', (w * 2 + 6, h + 22), (30, 31, 36))
    d = ImageDraw.Draw(out)
    for i, (im, label) in enumerate(zip(shots, ('from its reader (south): colony plinth left, archive plinth right',
                                                'from behind and to the east'))):
        out.paste(im, (i * (w + 6), 22))
        d.text((i * (w + 6) + 6, 5), label, fill=(230, 230, 230))
    out.save(path)


def main():
    check_java()
    made = {}

    def put(rel, img):
        art.save(img, rel)
        made[rel] = img

    put('block/colony_plinth_stone.png', colony_stone(random.Random(301)))
    put('block/colony_plinth_gold.png', gold_band(random.Random(302)))
    put('block/colony_plinth_column.png', colony_column(random.Random(303)))
    put('block/colony_plinth_desk.png', colony_desk(random.Random(304)))
    plinth()

    pictures = []
    put('block/colony_mural/tile_0.png', colony_ground_tile(random.Random(310)))
    for i, (name, (tw, th)) in enumerate(COLONY_ORDER):
        c = colony_canvas(tw * 16, th * 16, seed=320 + i)
        MURAL_FUNCS[name](c)
        base, _ = c.render()
        pictures.append((f'colony_mural: {name} (tiles {COLONY_MURALS[name]["first"]}-'
                         f'{COLONY_MURALS[name]["first"] + tw * th - 1})', base))
        for row in range(th):
            for col in range(tw):
                t = COLONY_MURALS[name]['first'] + row * tw + col
                art.save(base.crop((col * 16, row * 16, col * 16 + 16, row * 16 + 16)), f'block/colony_mural/tile_{t}.png')
    tiled_block('colony_mural', COLONY_TILES)

    put('block/sealer_carving/tile_0.png', archive.plaster(random.Random(330)))
    for i, (name, (tw, th)) in enumerate(CARVING_ORDER):
        c = archive.Canvas(tw * 16, th * 16, seed=340 + i)
        CARVING_FUNCS[name](c)
        base, _ = c.render()
        pictures.append((f'sealer_carving: {name} (tiles {CARVINGS[name]["first"]}-'
                         f'{CARVINGS[name]["first"] + tw * th - 1})', base))
        for row in range(th):
            for col in range(tw):
                t = CARVINGS[name]['first'] + row * tw + col
                art.save(base.crop((col * 16, row * 16, col * 16 + 16, row * 16 + 16)), f'block/sealer_carving/tile_{t}.png')
    tiled_block('sealer_carving', CARVING_TILES)

    os.makedirs(DESIGN, exist_ok=True)
    art.sheet(list(made.items()), os.path.join(DESIGN, 'textures.png'))
    pictures_sheet(pictures, os.path.join(DESIGN, 'murals.png'))
    plinth_preview(os.path.join(DESIGN, 'colony-plinth.png'))
    print(f'wrote {len(made)} textures, colony_mural ({COLONY_TILES} tiles), sealer_carving ({CARVING_TILES} tiles), '
          f'models, blockstates, loot')


if __name__ == '__main__':
    main()
