"""Planet 3's structures: the colony's gate ruin, the colonists' ruins and the Sealers' carving. No
build or runtime dependency.

Run from the repository root:  python tools/generate-nether-colony.py [--world <DIM-1/region dir>]
Writes, and hand edits to any of them are lost on the next run (edit this file):

  data/ascension_worlds/ascension_worlds/gateway_site/gateway_nether_site.json
      the gate ruin round the shared gateway frame, in the site format of gateway-frame-format.md
  data/ascension_worlds/structure/nether_colony/<piece>.nbt
      the colonists' ruins, one whole ruin per template, holding the seven tablets between them
  data/ascension_worlds/structure/sealer_carving/<piece>.nbt
      the Sealers' carving for the soul sand valleys

and previews under design/nether/. With --world, the previews also paste each piece onto real
generated ground (a test world's Nether region folder; see render-nether-views.py).

The contract is docs/technical/nether-art-format.md; the design record is
docs/technical/nether-art-design.md; the story is docs/lore/the-nether.md.

Deterministic, like every other piece of art here (art-guide rule 2): the layout is fixed code, the
only randomness (erosion, rubble, cracks) comes from seeded generators, the site JSON is written
with a fixed key order and layout, and the NBT with a sorted palette, a fixed block order and a gzip
header with no name and mtime 0 (the Lunar Archive's writer, imported, not copied), so re-running
reproduces the committed files byte for byte.

Everything is built from vanilla blocks and our own; nothing here is a texture. Checks the contract
before writing anything.
"""
import importlib.util
import json
import math
import os
import random
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, '..'))
RES = os.path.join(ROOT, 'modules/worlds/src/main/resources')
DATA = os.path.join(RES, 'data/ascension_worlds')
FRAME_PATH = os.path.join(DATA, 'ascension_worlds/gateway_frame/gateway.json')
SITE_PATH = os.path.join(DATA, 'ascension_worlds/gateway_site/gateway_nether_site.json')
DESIGN = os.path.join(ROOT, 'design/nether')
NS = 'ascension_worlds'


def _load(name, filename):
    spec = importlib.util.spec_from_file_location(name, os.path.join(HERE, filename))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


# --- the gate ruin ---------------------------------------------------------------------------------
#
# The colony's gate, where the rot came through (nether-art-format.md section 3). It stands in the
# warped forest nearest the origin, on a plateau, front to the south. Site coordinates: x across,
# left to right as seen from the front; y up; z from the front (0) to the back. The frame's own cell
# (0, 0, 0) is at FRAME_OFFSET. Pre-generation places every block in its default state (bottom
# slabs, upright pillars), so nothing here needs a facing.
#
#   y 0-2   a rubble footing of raw blackstone under the whole terrace (the bottom row is what
#           pre-generation extends down to solid ground, up to 8 more blocks, through lava too)
#   y 3     the terrace: black paving, a ward-ring of gold set in it round the gate, an avenue
#   y 4     the frame stands on the terrace, so its first row is a real step up
#   y 4-8   cleared over the terrace, so rising ground is cut back into a bank, not left on it
#
# Black and gold: the colony's stone and the colonists' ward. Gold is the trim of the frame itself
# (its pale courses, its lintel and its floating eye), the ring in the paving, the caps of the two
# obelisks that stand above the forest so the gate can be found from the air. The warped forest has
# grown into it, as the lore says it grew round the gate: nylium creeping over the paving, two
# fungi rooted in the ruin.

SITE_SIZE = (29, 34, 18)
FRAME_OFFSET = (7, 4, 9)
TERRACE = 3
CLEAR = 5                     # rows cleared over the terrace
HAZE = (48, 192)              # ascension_sky/the_nether.json

# The frame's skin in Nether stone (frame_palette): any block in that symbol's tag.
FRAME_PALETTE = {
    'D': 'minecraft:polished_blackstone_bricks',
    'd': 'minecraft:polished_blackstone',
    'P': 'minecraft:gold_block',
    'I': 'minecraft:polished_basalt',
    'C': 'minecraft:gold_block',
    '_': 'minecraft:polished_blackstone_brick_slab',
}

# One character per block in the site file's key. Spaces are "leave the world alone".
SITE_KEY = {
    'B': 'minecraft:polished_blackstone_bricks',
    'c': 'minecraft:cracked_polished_blackstone_bricks',
    'p': 'minecraft:polished_blackstone',
    'r': 'minecraft:blackstone',
    'g': 'minecraft:gilded_blackstone',
    'G': 'minecraft:gold_block',
    'i': 'minecraft:polished_basalt',
    'n': 'minecraft:warped_nylium',
    's': 'minecraft:warped_stem',
    'w': 'minecraft:warped_wart_block',
    'l': 'minecraft:shroomlight',
    'o': 'minecraft:warped_roots',
    'h': 'minecraft:nether_sprouts',
    'k': 'minecraft:netherrack',
    'f': 'minecraft:fire',
    '_': 'minecraft:polished_blackstone_brick_slab',
    '.': 'minecraft:air',
}
SYMBOL = {block: ch for ch, block in SITE_KEY.items()}


def frame_grid():
    d = json.load(open(FRAME_PATH))
    cells = {}
    for y, layer in enumerate(d['layers']):
        for z, row in enumerate(layer):
            for x, ch in enumerate(row):
                cells[(x, y, z)] = ch
    return d, cells


class Site:
    def __init__(self):
        self.cells = {}
        _, self.frame = frame_grid()
        ox, oy, oz = FRAME_OFFSET
        self.frame_cells = {(x + ox, y + oy, z + oz) for (x, y, z), ch in self.frame.items() if ch != ' '}
        self.frame_box = {(x + ox, y + oy, z + oz) for (x, y, z) in self.frame}

    def put(self, x, y, z, block, over_frame_space=False, clip=False):
        sx, sy, sz = SITE_SIZE
        if not (0 <= x < sx and 0 <= y < sy and 0 <= z < sz):
            if clip:
                return False
            raise ValueError(f'site cell {(x, y, z)} outside {SITE_SIZE}')
        if (x, y, z) in self.frame_cells:
            return False
        if (x, y, z) in self.frame_box and not over_frame_space:
            return False
        self.cells[(x, y, z)] = block
        return True

    def get(self, x, y, z):
        return self.cells.get((x, y, z))


def gate_site():
    s = Site()
    rng = random.Random(1666)
    sx, sy, sz = SITE_SIZE
    cx, cz = 14, 8.5                       # the terrace's centre
    ax, az = 14.6, 9.1
    gate_c = (14, FRAME_OFFSET[2] + 3)     # the frame's middle, round which the ward ring runs
    terrace = set()
    for x in range(sx):
        for z in range(sz):
            e = ((x - cx) / ax) ** 2 + ((z - cz) / az) ** 2
            if e > 1.0 or (e > 0.78 and rng.random() < (e - 0.78) * 3.2):
                continue
            terrace.add((x, z))
    # the frame's own footprint always has terrace under it
    for (x, y, z) in s.frame_cells:
        if y == FRAME_OFFSET[1]:
            terrace.add((x, z))

    # footing, terrace, clearance
    for (x, z) in sorted(terrace):
        for y in range(TERRACE):
            s.put(x, y, z, SITE_KEY['r'])
        e = ((x - cx) / ax) ** 2 + ((z - cz) / az) ** 2
        st = SITE_KEY['B'] if (x + 2 * z) % 3 else SITE_KEY['p']
        roll = rng.random()
        if roll < 0.14:
            st = SITE_KEY['c']
        elif roll < 0.19:
            st = SITE_KEY['g']
        if e > 0.82 and rng.random() < 0.35:
            st = SITE_KEY['r']                                   # worn to the rubble at the rim
        s.put(x, TERRACE, z, st)
        for y in range(TERRACE + 1, TERRACE + 1 + CLEAR):
            s.put(x, y, z, SITE_KEY['.'])

    # the avenue: smooth black from the terrace's front edge to the dais, two gold lines beside it
    for z in range(0, FRAME_OFFSET[2]):
        for x in range(12, 17):
            if (x, z) in terrace:
                s.cells[(x, TERRACE, z)] = SITE_KEY['p']
        for x in (11, 17):
            if (x, z) in terrace and z % 2 == 0:
                s.cells[(x, TERRACE, z)] = SITE_KEY['G']

    # the ward ring: gold set in the paving all round the gate, a few stones prised out
    for (x, z) in sorted(terrace):
        e = math.hypot((x - gate_c[0]) / 12.4, (z - gate_c[1]) / 7.6)
        if abs(e - 1.0) < 0.055:
            s.cells[(x, TERRACE, z)] = SITE_KEY['G'] if rng.random() > 0.2 else SITE_KEY['r']

    # the forest reclaiming the terrace: nylium creeping in at the back corners, roots on it
    for (x, z) in sorted(terrace):
        creep = max(0.0, 1.0 - math.hypot(x - 2, z - 15) / 6.5) + max(0.0, 1.0 - math.hypot(x - 27, z - 12) / 4.5)
        if creep > 0 and rng.random() < creep * 0.9:
            s.cells[(x, TERRACE, z)] = SITE_KEY['n']
            if rng.random() < 0.35:
                s.cells[(x, TERRACE + 1, z)] = SITE_KEY['o'] if rng.random() < 0.6 else SITE_KEY['h']

    # the frame's hidden cells: rubble core, as ancient masonry was built; its open cells cleared,
    # so nothing the ground left there blocks the reveal or the walk-in
    hidden, open_ = frame_spaces(s)
    for c in sorted(hidden):
        s.put(*c, SITE_KEY['r'], over_frame_space=True)
    for (x, y, z) in sorted(open_):
        if y <= FRAME_OFFSET[1] + 12:
            s.put(x, y, z, SITE_KEY['.'], over_frame_space=True)

    obelisk(s, 4, 5, standing=True, rng=rng)
    obelisk(s, 24, 5, standing=False, rng=rng)
    for x in (10, 18):
        brazier(s, x, 7)
    back_wall(s, rng)
    return s


def frame_spaces(s):
    """The frame's space cells (inside its box), split into those enclosed on every side by frame
    blocks or other enclosed spaces (the hidden core) and the rest (open air round the frame)."""
    ox, oy, oz = FRAME_OFFSET
    spaces = {c for c in s.frame_box if c not in s.frame_cells}
    outside = set()
    xs = [c[0] for c in s.frame_box]
    ys = [c[1] for c in s.frame_box]
    zs = [c[2] for c in s.frame_box]
    lo, hi = (min(xs), min(ys), min(zs)), (max(xs), max(ys), max(zs))
    frontier = [c for c in spaces if c[0] in (lo[0], hi[0]) or c[1] in (lo[1], hi[1]) or c[2] in (lo[2], hi[2])]
    # the field: open to the world, though inside the frame
    _, fr = frame_grid()
    field = {(x + ox, y + oy, z + oz) for (x, y, z), ch in fr.items() if ch == 'O'}
    frontier += [c for c in spaces if any((c[0] + dx, c[1] + dy, c[2] + dz) in field
                                         for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)))]
    outside.update(frontier)
    while frontier:
        nxt = []
        for (x, y, z) in frontier:
            for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
                n = (x + dx, y + dy, z + dz)
                if n in spaces and n not in outside:
                    outside.add(n)
                    nxt.append(n)
        frontier = nxt
    return spaces - outside, outside


def obelisk(s, x, z, standing, rng):
    """A black obelisk with a gold cap, standing over the forest's crowns so the gate can be found
    from a ship; or its stump, with its drums and its cap fallen across the terrace."""
    y0 = TERRACE + 1
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            s.put(x + dx, y0, z + dz, SITE_KEY['B'])                 # plinth
            s.put(x + dx, y0 + 1, z + dz, SITE_KEY['p'])
    top = y0 + 27 if standing else y0 + 9
    for y in range(y0 + 2, top + 1):
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                corner = dx != 0 and dz != 0
                st = SITE_KEY['i'] if corner else SITE_KEY['B']
                if y in (y0 + 2, y0 + 15) and not corner:
                    st = SITE_KEY['G']                               # gold bands
                s.put(x + dx, y, z + dz, st)
    if standing:
        # a gold cap, and in it a fire that has never gone out: netherrack burns for ever, and the
        # flame is what a ship coming down through the haze sees first
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                s.put(x + dx, top + 1, z + dz, SITE_KEY['G'] if (dx == 0 or dz == 0) else SITE_KEY['_'])
        s.put(x, top + 1, z, SITE_KEY['k'])
        s.put(x, top + 2, z, SITE_KEY['f'])
        return
    # broken: a jagged top, and the fallen drums lying toward the avenue, the cap at their end
    for (dx, dz) in ((1, 1), (0, 1), (1, 0)):
        s.cells.pop((x + dx, top, z + dz), None)
    s.put(x - 1, top, z - 1, SITE_KEY['c'])
    # the drums lie where they fell, out toward the forecourt's front corner, clear of the avenue,
    # its cap at the end of the fall
    drums = [(-2, -2, 2), (-1, -2, 2), (-2, -3, 1), (-1, -3, 1), (0, -5, 2), (1, -5, 1), (0, -6, 1), (1, -6, 1)]
    for dx, dz, h in drums:
        for y in range(y0, y0 + h):
            st = SITE_KEY['c'] if rng.random() < 0.4 else SITE_KEY['B']
            if (dx, dz) == (0, -5) and y == y0:
                st = SITE_KEY['G']                                      # a gold band, still on its drum
            s.put(x + dx, y, z + dz, st, clip=True)
    s.put(x + 1, y0, z - 4, SITE_KEY['g'], clip=True)
    s.put(x + 2, y0, z - 4, SITE_KEY['G'], clip=True)


def brazier(s, x, z):
    """A fire kept before the gate: a black pedestal, a gold bowl, netherrack burning on it for
    ever. Lit for looks; the Nether spawns at any light."""
    y = TERRACE + 1
    s.put(x, y, z, SITE_KEY['B'])
    s.put(x, y + 1, z, SITE_KEY['G'])
    s.put(x, y + 2, z, SITE_KEY['k'])
    s.put(x, y + 3, z, SITE_KEY['f'])


def fungus(s, x, z, height, cap, rng):
    """A warped fungus grown up through the ruin: a stem out of a nylium crack, a ragged cap of
    wart with a shroomlight in it."""
    y0 = TERRACE
    s.cells[(x, y0, z)] = SITE_KEY['n']
    for y in range(y0 + 1, y0 + 1 + height):
        s.put(x, y, z, SITE_KEY['s'])
    top = y0 + height
    for dy in (0, 1):
        r = cap + 1 - dy
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                if abs(dx) + abs(dz) > r + 1 or (dx, dz) == (0, 0) and dy == 0:
                    continue
                if dy == 0 and max(abs(dx), abs(dz)) < r:
                    continue                                           # the cap is a hood, hollow under
                if rng.random() < 0.15:
                    continue
                s.put(x + dx, top + dy, z + dz, SITE_KEY['w'], clip=True)
    s.put(x, top + 1, z, SITE_KEY['w'])
    s.put(x, top + 2, z, SITE_KEY['w'])
    lx, lz = (x + 1, z) if x < 14 else (x - 1, z)
    s.put(lx, top, lz, SITE_KEY['l'])


def back_wall(s, rng):
    """A low wall across the back, broken, its coping gold where it survives."""
    z = SITE_SIZE[2] - 2
    for x in range(5, 24):
        if (x, TERRACE, z) not in s.cells or s.cells[(x, TERRACE, z)] in (SITE_KEY['.'],):
            continue
        h = 1 + int(2.5 * abs(math.sin(x * 0.7 + 1.3))) + (1 if rng.random() < 0.3 else 0)
        if 12 <= x <= 16:
            h = min(h, 1)
        for y in range(TERRACE + 1, TERRACE + 1 + h):
            s.put(x, y, z, SITE_KEY['c'] if rng.random() < 0.3 else SITE_KEY['B'])
        if h >= 2 and rng.random() < 0.55:
            s.put(x, TERRACE + 1 + h, z, SITE_KEY['G'])


def check_site(s):
    problems = []
    _, fr = frame_grid()
    ox, oy, oz = FRAME_OFFSET
    sx, sy, sz = SITE_SIZE
    if sx > 30 or sy > 40 or sz > 18:
        problems.append(f'site {SITE_SIZE} exceeds 30 x 40 x 18')
    if FRAME_OFFSET[0] + 15 > sx or FRAME_OFFSET[1] + 20 > sy or FRAME_OFFSET[2] + 7 > sz:
        problems.append('the frame does not fit inside the site')
    for c in s.cells:
        if c in s.frame_cells:
            problems.append(f'site cell {c} overlaps a frame cell')
    # the approach: nothing but floor and air in front of the opening, across its width, up to the
    # field's top (the field is x 5-9 of the frame, rows 3-9)
    for x in range(ox + 5, ox + 10):
        for z in range(0, oz + 3):
            for y in range(oy + 1, oy + 10):
                st = s.cells.get((x, y, z))
                if st not in (None, SITE_KEY['.']) and not (z >= oz and (x - ox, y - oy, z - oz) in fr
                                                            and fr[(x - ox, y - oy, z - oz)] != ' '):
                    problems.append(f'{st} at {(x, y, z)} stands in front of the opening')
    for ch, b in FRAME_PALETTE.items():
        tag = {'D': 'gateway_frame_dark', 'd': 'gateway_frame_dark', 'P': 'gateway_frame_pale',
               'I': 'gateway_frame_pillar', 'C': 'gateway_frame_accent', '_': 'gateway_frame_step'}[ch]
        values = json.load(open(os.path.join(DATA, 'tags/block', tag + '.json')))['values']
        if b not in values:
            problems.append(f'frame_palette {ch} = {b} is not in #{NS}:{tag}')
    if not any(y == 0 for (_, y, _) in s.cells):
        problems.append('nothing in the bottom layer: pre-generation would extend nothing down')
    return problems


def site_json(s):
    """The site file: key in a fixed order, layers bottom-up, rows front to back, as strings."""
    sx, sy, sz = SITE_SIZE
    used = sorted({SYMBOL[b] for b in s.cells.values()}, key=lambda ch: list(SITE_KEY).index(ch))
    layers = []
    for y in range(sy):
        rows = []
        for z in range(sz):
            rows.append(''.join(SYMBOL[s.cells[(x, y, z)]] if (x, y, z) in s.cells else ' ' for x in range(sx)))
        layers.append(rows)
    while layers and all(r.strip() == '' for r in layers[-1]):
        layers.pop()
    lines = ['{']
    lines.append('  "_comment": "Generated by tools/generate-nether-colony.py; edit that, not this. The colony\'s gate, '
                 'black stone and gold, in the warped forest (nether-art-design.md).",')
    lines.append(f'  "frame_offset": [{FRAME_OFFSET[0]}, {FRAME_OFFSET[1]}, {FRAME_OFFSET[2]}],')
    lines.append('  "frame_palette": {')
    pal = list(FRAME_PALETTE.items())
    for i, (ch, b) in enumerate(pal):
        lines.append(f'    "{ch}": "{b}"' + (',' if i < len(pal) - 1 else ''))
    lines.append('  },')
    lines.append('  "key": {')
    for i, ch in enumerate(used):
        lines.append(f'    "{ch}": {{"block": "{SITE_KEY[ch]}"}}' + (',' if i < len(used) - 1 else ''))
    lines.append('  },')
    lines.append('  "layers": [')
    for i, rows in enumerate(layers):
        lines.append('    [')
        for j, r in enumerate(rows):
            lines.append(f'      "{r}"' + (',' if j < len(rows) - 1 else ''))
        lines.append('    ]' + (',' if i < len(layers) - 1 else ''))
    lines.append('  ]')
    lines.append('}')
    return '\n'.join(lines) + '\n'


# --- previews -------------------------------------------------------------------------------------

def site_previews(views, world_dir=None):
    """The gate ruin from the front, from above and from 64 blocks: on a flat nylium plain, and,
    given a test world, on the ground it really stood on there."""
    from PIL import Image, ImageDraw
    os.makedirs(DESIGN, exist_ok=True)
    fog = views.FOG['minecraft:warped_forest']
    shots = []

    def flat_scene():
        sc = views.Scene.flat(-60, 80, 60, 180, -70, 110, 99, top='minecraft:warped_nylium')
        views.paste_gateway(sc, FRAME_PATH, SITE_PATH, (0, 100, 0))
        return sc
    sc = flat_scene()
    # frame-local (0, 0, 0) at (0, 100, 0); its middle is x 7, the front at z 0 facing +z (south)
    mid = (7.5, 100, -3.0)
    shots.append(('from the front, 20 blocks', sc.render((7.5, 102.6, 20), (7.5, 110, -3), haze=(24, 192), fog=fog)))
    shots.append(('from 64 blocks, south-south-east', sc.render((27, 102.6, 58), (7.5, 114, -3), haze=(24, 192), fog=fog)))
    shots.append(('from above, 40 blocks up', sc.render((7.5, 150, 30), mid, haze=(24, 192), fog=fog)))
    shots.append(('back right', sc.render((34, 112, -34), (7.5, 108, -3), haze=(24, 192), fog=fog)))
    _sheet(shots, os.path.join(DESIGN, 'gate-ruin.png'), 'the gate ruin on a level plain (vanilla blocks in rough colours)')
    if world_dir:
        gate_in_world(views, world_dir, fog)


# Where the placeholder ruin stood on the first test world (nether-art-format.md section 3): the
# frame's origin as GatewayFramePiece recorded it, facing south.
TEST_GATE_ORIGIN = (17, 105, 331)


def eye_over(scene, x, z, views, lift=1.62):
    """A standing eye height over the ground at (x, z), ignoring fungus trees."""
    for y in range(scene.origin[1] + scene.ids.shape[1] - 1, scene.origin[1], -1):
        st = scene.get(int(math.floor(x)), y, int(math.floor(z)))
        if st and views.replaceable(st) is False and not any(k in st for k in (
                'warped_stem', 'warped_wart', 'shroomlight', 'warped_roots', 'nether_sprouts', 'vines',
                'fungus', 'crimson_stem', 'nether_wart_block', 'crimson_roots', 'air')):
            return y + 1 + lift
    return 110 + lift


def gate_in_world(views, world_dir, fog):
    """The ruin pasted where the gate stood on the test world, on the ground that is really there:
    plateau, lava pools and the warped forest's fungi. Eyes stand on the ground where it is at least
    as high as the terrace, and at the terrace's height where the ground falls away (a valley or a
    lava sea), as if on the far rim."""
    ox, oy, oz = TEST_GATE_ORIGIN
    world = views.RegionWorld(world_dir)
    sc = views.Scene(world, ox - 150, ox + 150, 20, 230, oz - 150, oz + 150)
    if '--gate-as-generated' not in sys.argv:              # else the world already holds this site
        views.paste_gateway(sc, FRAME_PATH, SITE_PATH, TEST_GATE_ORIGIN)
    mid = (ox + 7.5, oy + 8, oz - 3)
    shots = []
    for label, (ex, ez), dy in [('from the front, 22 blocks', (ox + 7.5, oz + 22), 0),
                                ('from 64 blocks south-south-east', (ox + 30, oz + 60), 0),
                                ('from 64 blocks west', (ox - 56, oz - 3), 0),
                                ('from above: a ship at y 216, 70 blocks south', (ox + 7.5, oz + 70), None)]:
        ey = max(eye_over(sc, ex, ez, views), oy + 2.62) if dy is not None else 216
        shots.append((label, sc.render((ex, ey, ez), mid, haze=HAZE, fog=fog)))
    _sheet(shots, os.path.join(DESIGN, 'gate-ruin-in-world.png'),
           f'the gate ruin on generated ground, origin {TEST_GATE_ORIGIN}, haze {HAZE}'
           + (' (as worldgen placed it)' if '--gate-as-generated' in sys.argv else ' (pasted)'))


def _sheet(shots, path, title, cols=2):
    from PIL import Image, ImageDraw
    w, h = shots[0][1].size
    rows = (len(shots) + cols - 1) // cols
    out = Image.new('RGB', (cols * w + (cols - 1) * 6, rows * (h + 20) + 24), (14, 15, 20))
    d = ImageDraw.Draw(out)
    d.text((6, 5), title, fill=(230, 230, 230))
    for i, (label, im) in enumerate(shots):
        x, y = (i % cols) * (w + 6), 24 + (i // cols) * (h + 20)
        out.paste(im, (x, y + 16))
        d.text((x + 4, y + 2), label, fill=(220, 220, 220))
    out.save(path)
    print('wrote', os.path.relpath(path, ROOT))


# --- the colonists' ruins --------------------------------------------------------------------------
#
# The colony's homes and halls (nether-art-format.md section 4), each a whole ruin in one template,
# made of what the colonists made everything of: black stone, gold, basalt. The piglins' bastions are
# what their descendants kept building; these are older and finer. Three ruins, seven tablets between
# them, every ruin opening with the voice's greeting at its door, so whichever a player finds first
# begins at the beginning:
#
#   hall_a  the Hall of the Colony   the_colony, the_ward, the_strongholds
#   hall_b  the Granary court        the_colony, the_thieves, the_gift; the colonists' mural; stores
#   hall_c  the Forge-house          the_colony, the_weapon, the_keeper; stores
#
# Template coordinates: x across, z from the back (0) to the front (the player arrives from high z),
# y up. Every ruin's floor is row F, the row to set on the ground (surface_y). Under it a rubble
# footing F deep; over the ruin's own footprint, rows cleared above the floor so rising ground is cut
# back; everywhere else structure_void, so the plateau shows through. No entities.

lunar = _load('lunar_archive', 'generate-lunar-archive.py')   # Piece, the NBT writer
nart = _load('nether_art', 'generate-nether-art.py')           # the picture catalogues
Piece, B, parse_state, template_nbt = lunar.Piece, lunar.B, lunar.parse_state, lunar.template_nbt
CORE, WALL, OPEN, DETAIL = lunar.CORE, lunar.WALL, lunar.OPEN, lunar.DETAIL

COLONY_OUT = os.path.join(DATA, 'structure/nether_colony')
CARVING_OUT = os.path.join(DATA, 'structure/sealer_carving')
ENTRIES = ['the_colony', 'the_ward', 'the_thieves', 'the_strongholds', 'the_gift', 'the_weapon', 'the_keeper',
           'the_forge_record']
LOOT = f'{NS}:nether_colony/stores'
MAX_PIECE = 48
# The floor row, surface_y. One row of rubble under it and no deeper footing, on purpose: placed with
# terrain_adaptation beard_thin, vanilla levels the ground to a piece's lowest row plus one, so the
# plateau is built up to the floor and cut away above it. A deeper footing would be left standing
# as a bare podium (measured: a six-deep one stood six above the levelled ground everywhere).
F = 1
AIR = 'minecraft:air'


def mc(name, **props):
    return B('minecraft:' + name, **props)


BRICK = mc('polished_blackstone_bricks')
CRACKED = mc('cracked_polished_blackstone_bricks')
POL = mc('polished_blackstone')
RAW = mc('blackstone')
GILD = mc('gilded_blackstone')
GOLD_B = mc('gold_block')
COLUMN = mc('polished_basalt', axis='y')
NYLIUM = mc('crimson_nylium')
ROOTS = mc('crimson_roots')
FUNGUS = mc('crimson_fungus')
CRIMSON_STEM = mc('crimson_stem', axis='y')
WART = mc('nether_wart_block')
SHROOM = mc('shroomlight')
MAGMA = mc('magma_block')
LAVA = mc('lava', level='0')
NETHER_BRICK = mc('nether_bricks')
SOUL_SAND = mc('soul_sand')
CRYING = mc('crying_obsidian')
CHAIN = mc('chain', axis='y', waterlogged='false')
LANTERN = mc('lantern', hanging='true', waterlogged='false')
NETHERRACK = mc('netherrack')


def slab(kind='polished_blackstone_brick', half='bottom'):
    return mc(f'{kind}_slab', type=half, waterlogged='false')


def stairs(kind, facing, half='bottom'):
    return mc(f'{kind}_stairs', facing=facing, half=half, shape='straight', waterlogged='false')


def plinth_state(facing):
    return B('colony_plinth', facing=facing)


def mural_state(block, tile):
    return B(block, tile=tile)


class Ruin(Piece):
    """A ruin under construction: the Archive's voxel piece plus the colony's building moves."""

    def __init__(self, name, size, seed):
        super().__init__(name, size)
        self.rng = random.Random(seed)
        self.plinths = []

    # ground ---------------------------------------------------------------------------------------
    def ground(self, cells, clear=8, floor=None):
        """Footing, floor and cleared rows over a set of (x, z) cells."""
        for (x, z) in cells:
            for y in range(0, F):
                self.put(x, y, z, RAW, CORE)
            self.put(x, F, z, floor(x, z) if floor else self.paving(x, z), WALL)
            for y in range(F + 1, min(self.size[1], F + 1 + clear)):
                self.put(x, y, z, AIR, OPEN)

    def paving(self, x, z):
        r = self.rng.random()
        if r < 0.13:
            return CRACKED
        if r < 0.16:
            return GILD
        return BRICK if (x + z) % 2 else POL

    def rect_cells(self, x0, z0, x1, z1):
        return [(x, z) for x in range(x0, x1 + 1) for z in range(z0, z1 + 1)]

    def eroded(self, cells, keep=0.78, cx=None, cz=None, r=None, always=()):
        """The edge of a forecourt worn away: cells far from (cx, cz) dropped at random, except the
        `always` cells (where a plinth and its reader stand)."""
        out = list(always)
        for (x, z) in cells:
            if (x, z) in always:
                continue
            d = math.hypot((x - cx) / r[0], (z - cz) / r[1])
            if d > 1.0 or (d > keep and self.rng.random() < (d - keep) * 3):
                continue
            out.append((x, z))
        return out

    # masonry --------------------------------------------------------------------------------------
    def wall(self, x0, z0, x1, z1, top, broken=0.6, band=F + 6, phase=0.0, gaps=(), keep_ends=3):
        """A straight wall from (x0, z0) to (x1, z1), courses F+1 to `top`, ruined: its corners
        stand, a few fragments stand along its run, and between them it is worn down to `1 - broken`
        of its height; a polished course banded with gold at `band` where it survives; `gaps` are
        (start, end) runs along it left open (doors, breaches)."""
        n = max(abs(x1 - x0), abs(z1 - z0))
        sx = (x1 > x0) - (x1 < x0)
        sz = (z1 > z0) - (z1 < z0)
        for i in range(n + 1):
            x, z = x0 + sx * i, z0 + sz * i
            if any(a <= i <= b for a, b in gaps):
                continue
            end = max(0.0, 1 - min(i, n - i) / keep_ends)
            peak = max(0.0, math.sin(i * 0.47 + phase)) ** 6
            f = max(1 - broken, end, peak * 0.95) + self.rng.uniform(-0.07, 0.07)
            h = F + max(1, int(round(max(0.0, min(1.0, f)) * (top - F))))
            for y in range(F + 1, h + 1):
                st = self.masonry()
                if y == band:
                    st = GOLD_B if i % 4 == 2 else POL
                self.put(x, y, z, st)
            if h < top and self.rng.random() < 0.3:
                self.put(x, h + 1, z, slab())                       # a half course, worn

    def masonry(self):
        r = self.rng.random()
        return CRACKED if r < 0.2 else GILD if r < 0.22 else BRICK

    def column(self, x, z, h, capital=True):
        """A basalt column on a black base, its capital gold if it still stands to full height."""
        self.put(x, F + 1, z, POL)
        for y in range(F + 2, F + 1 + h):
            self.put(x, y, z, COLUMN)
        if capital:
            self.put(x, F + 1 + h, z, GOLD_B)

    def rubble(self, cx, cz, n, y=F + 1, spread=1.6):
        """Fallen stone heaped on the floor: blocks and slabs, a few two high."""
        for _ in range(n):
            x = int(round(cx + self.rng.gauss(0, spread)))
            z = int(round(cz + self.rng.gauss(0, spread)))
            if not self.inside(x, y, z) or self.get(x, y, z) not in (AIR, None):
                continue
            if self.get(x, y - 1, z) in (AIR, None):
                continue
            st = self.rng.choice([CRACKED, RAW, BRICK, slab(), slab(), RAW])
            self.put(x, y, z, st)
            if st in (RAW, CRACKED) and self.rng.random() < 0.3 and self.get(x, y + 1, z) == AIR:
                self.put(x, y + 1, z, slab())

    def roof(self, x0, z0, x1, z1, y, keep=0.7):
        """A slab roof (top slabs, so it is flat underneath), fallen in places."""
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if self.rng.random() < keep:
                    self.put(x, y, z, slab(half='top'))

    def lamp(self, x, top, z, drop=2):
        """A lantern hung on a chain from the block at `top` (a roof slab put there if it fell)."""
        if self.get(x, top, z) in (None, AIR):
            self.put(x, top, z, slab(half='top'))
        for y in range(top - drop, top):
            self.put(x, y, z, CHAIN)
        self.put(x, top - drop - 1, z, LANTERN)

    def vines(self, x, top, z, length):
        """Weeping vines hanging from the underside of the block at `top`."""
        for k in range(length):
            y = top - 1 - k
            if self.get(x, y, z) not in (AIR, None):
                return
            self.put(x, y, z, mc('weeping_vines_plant') if k < length - 1 else mc('weeping_vines', age='25'))

    def crimson(self, cx, cz, r, density=0.75, y=F):
        """The crimson forest come in: floor turned to nylium, roots and fungus on it."""
        for x in range(int(cx - r) - 1, int(cx + r) + 2):
            for z in range(int(cz - r) - 1, int(cz + r) + 2):
                d = math.hypot(x - cx, z - cz) / r
                if d > 1 or self.rng.random() > density * (1.2 - d):
                    continue
                if self.get(x, y, z) in (None, AIR) or self.get(x, y + 1, z) != AIR:
                    continue
                self.put(x, y, z, NYLIUM)
                roll = self.rng.random()
                if roll < 0.35:
                    self.put(x, y + 1, z, ROOTS)
                elif roll < 0.42:
                    self.put(x, y + 1, z, FUNGUS)

    def lamp_in_wall(self, x, y, z):
        """A shroomlight set into a wall, the courses under it made good so it never floats."""
        for yy in range(F + 1, y):
            if self.get(x, yy, z) in (None, AIR):
                self.put(x, yy, z, self.masonry())
        self.put(x, y, z, SHROOM)

    def plinth(self, x, y, z, facing, entry):
        self.put(x, y, z, plinth_state(facing), DETAIL, nbt={'id': f'{NS}:archive_plinth', 'entry': f'{NS}:nether/{entry}'})
        self.plinths.append((entry, (x, y, z), facing))
        self.plinths.sort(key=lambda q: -q[1][2])         # reading order: from the front inward

    def chest(self, x, y, z, facing):
        self.put(x, y, z, mc('chest', facing=facing, type='single', waterlogged='false'), DETAIL,
                 nbt={'id': 'minecraft:chest', 'LootTable': LOOT})

    def picture(self, block, catalogue, name, left, y_bottom, wall_axis, wall_pos, facing):
        """Lay a picture's tiles into a wall so it reads left to right from the `facing` side (the
        Archive's place_mural, for these blocks' catalogues)."""
        m = catalogue[name]
        w, h = m['size']
        step = {'south': 1, 'north': -1, 'west': 1, 'east': -1}[facing]
        for col in range(w):
            for row in range(h):
                tile = m['first'] + row * w + col
                a = left + step * col
                y = y_bottom + (h - 1 - row)
                if wall_axis == 'z':
                    self.put(a, y, wall_pos, mural_state(block, tile))
                else:
                    self.put(wall_pos, y, a, mural_state(block, tile))


# --- hall_a: the Hall of the Colony -----------------------------------------------------------------------
#
# The colonists' assembly hall: a long nave between two rows of basalt columns, its roof fallen in
# down the middle, the aisles still covered. The voice greets the reader in the forecourt; the ward
# is read standing inside a ring of gold set in the floor of the west aisle; the strongholds at the
# far end, on a dais under the stronghold's sign, cut in the Sealers' own red brick.

def hall_a():
    p = Ruin('hall_a', (21, F + 15, 32), seed=401)
    p.door_x = 10
    x0, x1, zb, zf = 2, 18, 3, 25                         # walls: west, east, back, front
    p.ground(p.rect_cells(x0, zb, x1, zf), clear=10)
    court = p.eroded(p.rect_cells(5, 26, 15, 31), keep=0.7, cx=10, cz=26, r=(6.5, 6.2),
                     always=[(10, 27), (10, 28), (10, 29), (9, 29), (11, 29)])
    p.ground(court, clear=6)
    top = F + 9
    p.wall(x0, zb, x1, zb, top, broken=0.3, phase=0.3)
    p.wall(x0, zb + 1, x0, zf - 1, top, broken=0.6, phase=1.0)
    p.wall(x1, zb + 1, x1, zf - 1, top, broken=0.75, phase=2.2, gaps=[(12, 16)])   # the east breach
    p.wall(x0, zf, x1, zf, top, broken=0.55, phase=0.8, gaps=[(6, 10)])            # the doorway
    # the doorway's lintel and posts: gold over the way in
    for x in range(8, 13):
        p.put(x, F + 6, zf, GOLD_B)
        for y in range(F + 7, F + 9):
            p.put(x, y, zf, p.masonry())
    # two tall columns flank it, the hall's mark from across the plateau: one still capped in gold,
    # one snapped
    for x, h in ((7, 13), (13, 10)):
        for y in range(F + 1, F + 1 + h):
            p.put(x, y, zf, COLUMN)
    p.put(7, F + 14, zf, GOLD_B)
    p.put(13, F + 11, zf, slab())
    # the colonnade: two rows, some fallen
    heights = {(6, 8): 8, (6, 12): 8, (6, 16): 3, (6, 20): 8, (14, 8): 8, (14, 12): 5, (14, 16): 8, (14, 20): 1}
    for (x, z), h in heights.items():
        p.column(x, z, h, capital=(h == 8))
    # the aisles keep their roof; beams across the nave survive at two bays
    p.roof(x0 + 1, zb + 1, 5, zf - 1, F + 9, keep=0.82)
    p.roof(15, zb + 1, x1 - 1, zf - 1, F + 9, keep=0.7)
    for z in (8, 20):
        for x in range(6, 15):
            p.put(x, F + 9, z, POL)
    # the fallen roof and the fallen column, in heaps down the nave
    for cx, cz, n in ((10, 13, 14), (9, 17, 10), (12, 21, 8), (16, 21, 6)):
        p.rubble(cx, cz, n)
    # the dais at the far end, and the strongholds read on it
    for x in range(7, 14):
        for z in range(zb + 1, 8):
            p.put(x, F + 1, z, POL)
    for x in range(8, 13):
        p.put(x, F + 1, 8, stairs('polished_blackstone_brick', 'north'))
    p.plinth(10, F + 2, 5, 'south', 'the_strongholds')
    # the strongholds' sign in the back wall: a fortress's battlements in the Sealers' red brick,
    # between two gold posts (the voice says the fortresses were ours)
    sign = ["#.#.#", "#####", "#####"]
    for r, row in enumerate(sign):
        for c, ch in enumerate(row):
            p.put(8 + c, F + 6 - r, zb, NETHER_BRICK if ch == '#' else POL)
    for y in range(F + 3, F + 8):
        p.put(7, y, zb, GOLD_B)
        p.put(13, y, zb, GOLD_B)
    # the ward: read standing in a ring of gold set in the west aisle's floor
    for x in range(3, 6):
        for z in range(12, 15):
            if (x, z) != (4, 13):
                p.put(x, F, z, GOLD_B)
    p.plinth(4, F + 1, 13, 'east', 'the_ward')
    p.lamp_in_wall(x0, F + 3, 13)                         # a lamp set in the wall above it
    # lamps on chains from the aisle roofs
    for (x, z) in ((4, 6), (4, 20), (16, 10), (16, 24), (10, 8)):
        p.lamp(x, F + 9, z)
    p.lamp_in_wall(x0, F + 3, 6)
    p.lamp_in_wall(x1, F + 3, 22)
    # the crimson forest coming in through the breach, and the wall's stones spilled out of it
    p.ground(p.rect_cells(19, 14, 20, 20), clear=5)
    p.crimson(17, 17, 4.0)
    p.crimson(20, 17, 2.5, y=F)
    for (x, z) in ((16, 15), (17, 18), (16, 19)):
        p.vines(x, F + 9, z, 2 + (x + z) % 3)
    p.rubble(19, 17, 8, spread=1.2)
    # the forecourt: the greeting, between two broken gate posts
    p.plinth(10, F + 1, 28, 'south', 'the_colony')
    for (x, h) in ((6, 4), (14, 2)):
        p.column(x, 30, h, capital=False)
    for x in range(8, 13):
        p.put(x, F, 27, POL)
    return p


# --- hall_b: the Granary court -------------------------------------------------------------------------
#
# A courtyard house by the colony's fields, the crimson forest grown into its court. On the court's
# far wall, facing the gate, the colonists' own picture: themselves in gold, turning away someone
# without it. The voice's "thieves" is read in front of it. In the east wing, the gift is read before
# an anchor of crying obsidian, the Openers' answer to the beds that burn; beside it, the stores.

def hall_b():
    p = Ruin('hall_b', (25, F + 14, 28), seed=402)
    p.door_x = 8
    xw, xe, zb, zf = 1, 23, 2, 21
    p.ground(p.rect_cells(xw, zb, xe, zf), clear=9)
    court = p.eroded(p.rect_cells(4, 22, 12, 27), keep=0.65, cx=8, cz=22, r=(5.5, 6.0),
                     always=[(8, 22), (8, 23), (8, 24), (7, 24), (9, 24)])
    p.ground(court, clear=6)
    top = F + 8
    p.wall(xw, zb, xe, zb, top, broken=0.35, phase=0.4)
    p.wall(xw, zb + 1, xw, zf - 1, top, broken=0.7, phase=1.7)
    p.wall(xe, zb + 1, xe, zf - 1, top, broken=0.45, phase=2.9)
    p.wall(xw, zf, xe, zf, top, broken=0.7, phase=0.1, gaps=[(6, 8)])            # the gate
    p.wall(15, zb + 1, 15, zf - 1, top - 1, broken=0.4, phase=3.3, gaps=[(5, 6), (13, 14)])   # the wing
    p.wall(16, 11, 22, 11, top - 1, broken=0.5, phase=0.9, gaps=[(3, 3)])
    # the granary's tower in its north-east corner, standing over the plateau, capped in gold
    for x in range(xe - 2, xe + 1):
        for z in range(zb, zb + 3):
            for y in range(F + 1, F + 13):
                if x == xe - 2 and z == zb + 2 and y <= F + 3:
                    continue                                    # its door from the shrine
                if (x, z) == (xe - 1, zb + 1) and y < F + 12:
                    p.put(x, y, z, AIR)                         # hollow
                    continue
                st = p.masonry()
                if y in (F + 6, F + 11):
                    st = GOLD_B if (x + z) % 2 == 0 else POL
                p.put(x, y, z, st)
            p.put(x, F + 13, z, GOLD_B if (x, z) == (xe - 1, zb + 1) else slab())
    # the gate: two columns and a gold lintel
    for x in (6, 10):
        for y in range(F + 1, F + 6):
            p.put(x, y, zf, COLUMN)
    for x in range(6, 11):
        p.put(x, F + 6, zf, GOLD_B)
    # the covered walk along the west wall
    for z in (5, 9, 13, 17):
        p.column(5, z, 5, capital=True)
    p.roof(2, 3, 5, 20, F + 7, keep=0.75)
    # the colonists' picture on the court's far wall, read from the court (looking north)
    m = nart.COLONY_MURALS['turned_away']
    p.picture(f'{NS}:colony_mural', nart.COLONY_MURALS, 'turned_away', 6, F + 2, 'z', zb, 'south')
    for x in range(5, 12):
        p.put(x, F + 1, zb, POL)                           # a sill under it
        p.put(x, F + 4, zb, POL)
    for x in range(5, 12):                                 # a backing, so its tiles never show from outside
        for y in range(0, F + 5):
            p.put(x, y, zb - 1, RAW if y < F else BRICK, CORE if y < F else DETAIL)
    p.put(5, F + 2, zb, GOLD_B)
    p.put(5, F + 3, zb, GOLD_B)
    p.put(11, F + 2, zb, GOLD_B)
    p.put(11, F + 3, zb, GOLD_B)
    del m
    p.plinth(8, F + 1, 5, 'south', 'the_thieves')
    # the basin in the court: gold like water, long dry, its bed still hot
    for x in range(6, 11):
        for z in range(11, 16):
            edge = x in (6, 10) or z in (11, 15)
            if edge:
                p.put(x, F + 1, z, GOLD_B if (x + z) % 2 == 0 else POL)
            else:
                p.put(x, F, z, MAGMA)
    # the fields gone wild in the court's south half: nylium, roots, a young crimson fungus
    p.crimson(9, 18, 4.2, density=0.9)
    p.crimson(4, 10, 2.0)
    for y in range(F + 1, F + 5):
        p.put(12, y, 18, CRIMSON_STEM)
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            if abs(dx) + abs(dz) <= 3 and p.rng.random() < 0.85:
                p.put(12 + dx, F + 5, 18 + dz, WART)
    p.put(12, F + 6, 18, WART)
    p.put(13, F + 4, 18, SHROOM)
    p.vines(11, F + 5, 17, 2)
    # the east wing: the shrine of the gift, north; the stores, south
    p.roof(16, 3, 22, 10, F + 7, keep=0.8)
    p.roof(16, 12, 22, 20, F + 7, keep=0.6)
    p.put(19, F + 1, 3, mc('respawn_anchor', charges='0'))
    p.put(18, F + 1, 3, CRYING)
    p.put(20, F + 1, 3, CRYING)
    p.put(19, F + 2, 3, CRYING)
    p.plinth(19, F + 1, 6, 'south', 'the_gift')
    p.lamp(19, F + 7, 9)
    p.chest(22, F + 1, 16, 'west')
    for (x, z) in ((22, 13), (22, 14), (21, 20)):
        p.put(x, F + 1, z, mc('barrel', facing='up', open='false'))
    p.lamp(18, F + 7, 16)
    p.rubble(19, 18, 6)
    # lamps in the court's walk and wall
    p.lamp(3, F + 7, 8)
    p.lamp(3, F + 7, 16)
    p.lamp_in_wall(xw, F + 3, 12)
    # the greeting, outside the gate
    p.plinth(8, F + 1, 23, 'south', 'the_colony')
    return p


# --- hall_c: the Forge-house ---------------------------------------------------------------------------
#
# Where the colony's smiths worked: a hearth of lava under a stone hood and a flue that still stands
# over the fallen roof, anvils, a blast furnace, heaps of gilded ore. The voice teaches the weapon
# before a shape laid out in soul sand, waiting for its three skulls; the keeper is read at the back,
# before the great forge wall.

def hall_c():
    p = Ruin('hall_c', (23, F + 16, 30), seed=403)
    p.door_x = 11
    xw, xe, zb, zf = 1, 21, 2, 22
    p.ground(p.rect_cells(xw, zb, xe, zf), clear=10)
    court = p.eroded(p.rect_cells(6, 23, 16, 29), keep=0.7, cx=11, cz=23, r=(6.0, 6.8),
                     always=[(11, 24), (11, 25), (11, 26), (10, 26), (12, 26)])
    p.ground(court, clear=6)
    top = F + 10
    p.wall(xw, zb, xe, zb, top, broken=0.25, phase=0.2)
    p.wall(xw, zb + 1, xw, zf - 1, top, broken=0.7, phase=1.3)
    p.wall(xe, zb + 1, xe, zf - 1, top, broken=0.55, phase=2.5)
    p.wall(xw, zf, xe, zf, top, broken=0.6, phase=0.6, gaps=[(6, 14)])          # the open front
    for x in (6, 16):
        for y in range(F + 1, F + 9):
            p.put(x, y, zf, COLUMN)
    for x in range(7, 16):
        p.put(x, F + 9, zf, POL if x % 3 else GOLD_B)                          # the front beam
    # the hearth: lava in a magma bed, a black rim, a hood on four posts, the flue above
    for x in range(9, 14):
        for z in range(11, 14):
            p.put(x, F, z, MAGMA)
            if x in (9, 13) or z in (11, 13):
                p.put(x, F + 1, z, POL)
    for x in range(10, 13):
        p.put(x, F, 12, LAVA)
    for (x, z) in ((9, 11), (13, 11), (9, 13), (13, 13)):
        for y in range(F + 2, F + 5):
            p.put(x, y, z, BRICK)
    for x in range(9, 14):
        for z in range(11, 14):
            p.put(x, F + 5, z, BRICK if (x, z) != (11, 12) else AIR)
    for y in range(F + 6, F + 15):
        for x in range(10, 13):
            for z in range(11, 14):
                if (x, z) == (11, 12):
                    p.put(x, y, z, AIR)
                elif y < F + 14 or p.rng.random() < 0.5:
                    p.put(x, y, z, BRICK if p.rng.random() > 0.15 else CRACKED)
    # the smiths' work: anvils, a smithing table, a blast furnace, a cauldron of lava, chains
    p.put(7, F + 1, 12, mc('damaged_anvil', facing='east'))
    p.put(15, F + 1, 12, mc('chipped_anvil', facing='west'))
    p.put(7, F + 1, 9, mc('smithing_table'))
    p.put(15, F + 1, 9, mc('blast_furnace', facing='west', lit='false'))
    p.put(16, F + 1, 15, mc('lava_cauldron'))
    # the ore heaps in the north-east corner
    for (x, z, h) in ((19, 4, 2), (18, 4, 1), (19, 5, 1), (20, 5, 1), (20, 4, 2), (18, 5, 1)):
        for y in range(F + 1, F + 1 + h):
            p.put(x, y, z, GILD if p.rng.random() < 0.6 else mc('nether_gold_ore'))
    # the great forge wall at the back: three blast furnaces under a gold-banded hood
    for x in (9, 11, 13):
        p.put(x, F + 1, zb + 1, mc('blast_furnace', facing='south', lit='false'))
    for x in (8, 10, 12, 14):
        p.put(x, F + 1, zb + 1, BRICK)
    for x in range(8, 15):
        p.put(x, F + 2, zb + 1, POL)
        p.put(x, F + 3, zb + 1, GOLD_B if x % 2 == 0 else POL)
    p.plinth(11, F + 1, 6, 'south', 'the_keeper')
    # the Forge Record, the Sealers' plates (not the voice), beside the forge: it used to drop from the Monstrosity, and
    # tablets are no longer loot (2026-10-21), so it is read here, where the metal was worked
    p.plinth(16, F + 1, 8, 'west', 'the_forge_record')
    # the weapon: a dais in the west, and on it the shape the voice taught, in soul sand
    for x in range(3, 7):
        for z in range(14, 19):
            p.put(x, F + 1, z, POL)
    p.put(4, F + 2, 16, SOUL_SAND)
    for z in (15, 16, 17):
        p.put(4, F + 3, z, SOUL_SAND)
    p.plinth(8, F + 1, 16, 'east', 'the_weapon')
    # the roof: beams across, most of the rest fallen into the hall
    for z in (6, 18):
        for x in range(xw + 1, xe):
            p.put(x, F + 10, z, POL)
    p.roof(xw + 1, zb + 1, 7, 9, F + 10, keep=0.7)
    p.roof(15, 15, xe - 1, zf - 1, F + 10, keep=0.55)
    for cx, cz, n in ((5, 10, 8), (17, 18, 9), (14, 7, 6)):
        p.rubble(cx, cz, n)
    p.put(15, F + 1, 8, AIR, DETAIL)                                           # the Forge Record's reader stands here
    # chains from the beams, some with lamps; one hanging over the hearth
    for (x, z, drop, lit) in ((5, 6, 3, True), (17, 6, 2, True), (8, 18, 4, False), (14, 18, 2, True), (3, 18, 1, True)):
        if lit:
            p.lamp(x, F + 10, z, drop=drop)
        else:
            for y in range(F + 10 - drop, F + 10):
                p.put(x, y, z, CHAIN)
    p.lamp_in_wall(xw, F + 4, 12)
    p.lamp_in_wall(xe, F + 4, 12)
    p.chest(20, F + 1, 20, 'west')
    p.crimson(3, 21, 2.5)
    p.crimson(20, 9, 2.0)
    # the greeting, in the yard before the open front
    p.plinth(11, F + 1, 25, 'south', 'the_colony')
    return p


def check_ruins(pieces):
    problems = []
    seen = {}
    for p in pieces:
        if any(n > MAX_PIECE for n in p.size):
            problems.append(f'{p.name}: size {p.size} exceeds {MAX_PIECE}')
        if not 2 <= len(p.plinths) <= 4:
            problems.append(f'{p.name}: {len(p.plinths)} plinths (two to four)')
        if p.plinths and max(p.plinths, key=lambda q: q[1][2])[0] != 'the_colony':
            problems.append(f'{p.name}: the plinth nearest its front is not the_colony')
        for entry, (x, y, z), facing in p.plinths:
            seen.setdefault(entry, []).append(p.name)
            if not lunar.is_solid(p.get(x, y - 1, z)):
                problems.append(f'{p.name}: plinth {entry} at {(x, y, z)} does not stand on a full block')
            fx, fz = {'north': (0, -1), 'south': (0, 1), 'east': (1, 0), 'west': (-1, 0)}[facing]
            rx, rz = x + fx, z + fz
            if (p.get(rx, y, rz) != AIR or p.get(rx, y + 1, rz) != AIR
                    or not lunar.is_solid(p.get(rx, y - 1, rz))):
                problems.append(f'{p.name}: plinth {entry} at {(x, y, z)} faces nowhere a reader can stand')
        for pos, (_, state) in p.cells.items():
            name, _ = parse_state(state)
            if name == 'minecraft:chest' and p.nbt.get(pos, {}).get('LootTable') != LOOT:
                problems.append(f'{p.name}: chest at {pos} has no loot table')
            if name == 'minecraft:lava':
                x, y, z = pos
                for d in ((1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1), (0, -1, 0)):
                    nb = p.get(x + d[0], y + d[1], z + d[2])
                    if nb is None or not lunar.is_solid(nb) and parse_state(nb)[0] != 'minecraft:lava':
                        problems.append(f'{p.name}: lava at {pos} can flow out toward {d}')
    # finding all three ruins finds all eight
    for e in ENTRIES:
        if e not in seen:
            problems.append(f'entry {e} is in no ruin')
    for e in seen:
        if e not in ENTRIES:
            problems.append(f'unknown entry {e}')
    return problems, seen


# --- the ruins' previews ----------------------------------------------------------------------------------

def ruin_previews(views, ruins):
    """Each ruin from the front, from above and from 64 blocks, on a level netherrack plain under the
    crimson forest's haze; and a plan of its floor with the plinths numbered."""
    rsp = views.rsp
    for k, v in views.VANILLA.items():
        rsp.VANILLA_GUESS.setdefault(k, v)
    fog = views.FOG['minecraft:crimson_forest']
    for r in ruins:
        _, vox, tags = rsp.load_nbt(os.path.join(COLONY_OUT, r.name + '.nbt'))
        sx, sy, sz = r.size
        sc = views.Scene.flat(-80, sx + 80, 60, 170, -80, sz + 90, 99, top='minecraft:crimson_nylium')
        at = (0, 100 - F, 0)                     # the floor row on the plain's top
        sc.paste(vox, at)
        mid = (sx / 2, 100 + 3, sz / 2)
        front = (sx / 2, 101.62, sz + 14)
        shots = [('from the front, 14 blocks out', sc.render(front, (sx / 2, 103, sz / 2), haze=HAZE, fog=fog)),
                 ('from above, the south-east', sc.render((sx + 18, 100 + 34, sz + 26), mid, haze=HAZE, fog=fog)),
                 ('from 64 blocks, south-west', sc.render((sx / 2 - 40, 101.62, sz / 2 + 50), (sx / 2, 104, sz / 2), haze=HAZE, fog=fog)),
                 ('inside, from the door', sc.render((r.door_x + 0.5, 101.62, sz - 6), (r.door_x + 0.5, 102.5, 4), haze=HAZE, fog=fog))]
        _sheet(shots, os.path.join(DESIGN, f'colony-{r.name}.png'),
               f'{r.name}: {", ".join(e for e, _, _ in r.plinths)} (vanilla blocks in rough colours)')
        marks = {e: i + 1 for i, (e, _, _) in enumerate(r.plinths)}
        rsp.render_plan(vox, tags, (0, sx - 1), (0, sz - 1), F, F + 3, os.path.join(DESIGN, f'colony-{r.name}-plan.png'),
                        scale=16, title=f'{r.name}, floor row {F}: plinths in reading order', marks=marks)


# --- the Sealers' carving -------------------------------------------------------------------------------
#
# The truth about the gift, with no words (nether-art-format.md section 5): a wall the Sealers
# raised in the soul sand valley, the battlefield of the last war, built in their fortress brick,
# with the soldier who falls and rises carved across it in their pale plaster. Before it burns their
# fire, the soul fire they burned the returned with, the one fire the piglins still fear. No plinth:
# the voice has nothing to say about it.
#
# Coordinates as the ruins': x across, z from the back (0) to the front, where its reader stands;
# floor row CARVING_F, the row to set on the ground.

CARVING_F = 1
NB = mc('nether_bricks')
NB_CRACKED = mc('cracked_nether_bricks')
NB_CHISELED = mc('chiseled_nether_bricks')
SOUL_SOIL = mc('soul_soil')
SOUL_FIRE = mc('soul_fire')
BONE = mc('bone_block', axis='y')


def sealer_wall():
    G = CARVING_F
    p = Ruin('wall', (13, G + 10, 7), seed=501)
    p.door_x = 6
    rng = p.rng

    def brick():
        return NB_CRACKED if rng.random() < 0.25 else NB
    # footing under it all, a court of soul soil before it, cleared
    for x in range(0, 13):
        for z in range(0, 7):
            for y in range(0, G):
                p.put(x, y, z, RAW if z < 3 else SOUL_SOIL, CORE)
            p.put(x, G, z, NB if z < 3 else SOUL_SOIL, WALL)
            for y in range(G + 1, G + 8):
                p.put(x, y, z, AIR, OPEN)
    # the wall, two thick, so the carving's back never shows
    for x in range(1, 12):
        for z in (1, 2):
            for y in range(G + 1, G + 5):
                p.put(x, y, z, brick())
            p.put(x, G + 5, z, slab('nether_brick'))
    # the carving, read from the court (looking north), on a sill, under a course
    p.picture(f'{NS}:sealer_carving', nart.CARVINGS, 'the_returned', 3, G + 2, 'z', 2, 'south')
    # its two piers, chiseled at their heads; the east one broken
    for x, h in ((1, 9), (11, 6)):
        for z in (1, 2):
            for y in range(G + 1, G + 1 + h):
                p.put(x, y, z, NB_CHISELED if y in (G + 4, G + h) else brick())
    p.put(11, G + 7, 2, slab('nether_brick'))
    # the Sealers' fire before it: soul soil in a brick bowl, burning for ever
    for x in (1, 11):                                      # before the piers, clear of the picture
        p.put(x, G + 1, 4, NB)
        p.put(x, G + 2, 4, SOUL_SOIL)
        p.put(x, G + 3, 4, SOUL_FIRE)
    # bones of the battlefield half-sunk in the court
    for (x, z) in ((5, 6), (8, 5)):
        p.put(x, G, z, BONE)
        p.put(x, G + 1, z, BONE)
    p.put(9, G + 1, 6, BONE)
    return p


def check_carving(p):
    problems = []
    if any(n > 16 for n in p.size):
        problems.append(f'sealer_carving/{p.name}: size {p.size} is not small')
    m = nart.CARVINGS['the_returned']
    want = set(range(m['first'], m['first'] + m['size'][0] * m['size'][1]))
    have = {int(parse_state(st)[1]['tile']) for _, st in p.cells.values() if st.startswith(f'{NS}:sealer_carving')}
    if have != want:
        problems.append(f'sealer_carving/{p.name}: tiles {sorted(want - have)} missing')
    # every tile's back and ends hidden behind brick, so no face shows a mirrored picture
    for (x, y, z), (_, st) in p.cells.items():
        if st.startswith(f'{NS}:sealer_carving'):
            for d in ((0, 0, -1), (1, 0, 0), (-1, 0, 0)):
                nb = p.get(x + d[0], y, z + d[2])
                if nb is None or nb == AIR:
                    problems.append(f'sealer_carving/{p.name}: tile at {(x, y, z)} shows a face toward {d}')
    return problems


def carving_previews(views, carving):
    rsp = views.rsp
    fog = views.FOG['minecraft:soul_sand_valley']
    _, vox, _ = rsp.load_nbt(os.path.join(CARVING_OUT, carving.name + '.nbt'))
    sx, sy, sz = carving.size
    sc = views.Scene.flat(-70, sx + 70, 60, 150, -70, sz + 80, 99, ground='minecraft:soul_soil', top='minecraft:soul_sand')
    sc.paste(vox, (0, 100 - CARVING_F, 0))
    shots = [('reading it, 5 blocks out', sc.render((6.5, 101.62, 2 + 5.5), (6.5, 102.4, 2), haze=HAZE, fog=fog)),
             ('from 16 blocks, south-east', sc.render((15, 101.62, 17), (6.5, 103, 2), haze=HAZE, fog=fog)),
             ('from 48 blocks', sc.render((-10, 101.62, 46), (6.5, 104, 2), haze=HAZE, fog=fog)),
             ('from above', sc.render((24, 122, 22), (6.5, 102, 3), haze=HAZE, fog=fog))]
    _sheet(shots, os.path.join(DESIGN, 'sealer-carving.png'),
           "the Sealers' carving on a soul sand plain, under the valley's haze")


# --- the sky, seen from a ship and from the ground ---------------------------------------------------------

SKY_PATH = os.path.join(RES, 'assets/ascension_worlds/ascension_sky/the_nether.json')


def sky_previews(views, world_dir):
    """The haze and the sun as ascension_sky/the_nether.json sets them, over the test world's real
    ground round the gate: coming down from y 216, and standing on a plateau looking up. Each beside
    the placeholder haze (24 to 160) it replaces. The fog colour is the biome's under the eye."""
    sky = json.load(open(SKY_PATH))
    haze = (sky['haze']['start'], sky['haze']['end'])
    bodies = []
    for b in sky.get('bodies', []):
        ns, path = b['texture'].split(':')
        bodies.append(dict(b, texture_path=os.path.join(RES, 'assets', ns, path)))
    ox, oy, oz = TEST_GATE_ORIGIN
    world = views.RegionWorld(world_dir)
    sc = views.Scene(world, ox - 200, ox + 200, 20, 240, oz - 175, oz + 170)
    if '--gate-as-generated' not in sys.argv:
        views.paste_gateway(sc, FRAME_PATH, SITE_PATH, TEST_GATE_ORIGIN)
    light = views.block_light(sc.ids, views.Looks(sc.palette))
    gate = (ox + 7.5, oy + 6, oz - 3)

    def fog_at(x, y, z):
        return views.FOG.get(world.biome(int(x), min(int(y), 120), int(z)), views.FOG['minecraft:nether_wastes'])

    views_ = [
        ('a ship at y 216, 90 blocks south of the gate, looking at it', (ox + 7.5, 216, oz + 90), gate),
        ('straight down from y 216 over the forest', (ox + 7.5, 216, oz + 30), (ox + 7.5, 100, oz + 29)),
        ('on a plateau 100 blocks south-east, looking up at the sun', (ox + 90.5, 113.62, oz + 95), None),
        ('before the gate, looking north at it', (ox + 7.5, oy + 2.62, oz + 22), (ox + 7.5, oy + 14, oz - 20)),
    ]
    for tag, h in (('', haze), ('-placeholder', (24, 160))):
        shots = []
        for label, eye, target in views_:
            if target is None:                                   # up toward the sun, over its bearing
                b = bodies[0] if bodies else {'yaw': 165, 'elevation': 32}
                yaw, el = math.radians(b['yaw']), math.radians(b['elevation'] - 8)
                target = (eye[0] - math.sin(yaw) * math.cos(el) * 50, eye[1] + math.sin(el) * 50,
                          eye[2] + math.cos(yaw) * math.cos(el) * 50)
            fog = fog_at(*eye)
            shots.append((label, sc.render(eye, target, haze=h, fog=fog, bodies=bodies, light=light)))
        _sheet(shots, os.path.join(DESIGN, f'sky{tag}.png'),
               f'the Nether sky: haze {h[0]} to {h[1]}' + (' (the placeholder)' if tag else ' (ascension_sky/the_nether.json)')
               + ', biome fog colours, the sun as the file sets it')


# --- the ruins where worldgen put them ----------------------------------------------------------------

def placed_previews(views, world_dir, structure_filter, names=('hall_a', 'hall_b', 'hall_c', 'wall')):
    """Each template as a world really generated it (a throwaway datapack placing them, see the
    design note), rendered from the world itself: its front from 16 blocks and from 64. The first
    finished placement of each is used."""
    sys.path.insert(0, os.path.join(HERE, 'worldgen-diagnostics'))
    from structure_starts import starts_in
    world = views.RegionWorld(world_dir)
    done = {}
    import glob
    for path in sorted(glob.glob(os.path.join(world_dir, '*.mca'))):
        for sid, start in starts_in(path, structure_filter):
            for piece in start.get('Children', []):
                loc = piece.get('pool_element', {}).get('location', '')
                name = loc.split('/')[-1]
                if name not in names or name in done:
                    continue
                r = {'NONE': 0, 'CLOCKWISE_90': 1, 'CLOCKWISE_180': 2, 'COUNTERCLOCKWISE_90': 3}[piece['rotation']]
                x0, y0, z0, x1, y1, z1 = piece['BB']
                if not all(world.chunk(cx, cz) for cx in range(x0 >> 4, (x1 >> 4) + 1)
                           for cz in range(z0 >> 4, (z1 >> 4) + 1)):
                    continue
                done[name] = (piece, r)
    for name, (piece, r) in sorted(done.items()):
        x0, y0, z0, x1, y1, z1 = piece['BB']
        floor = piece['PosY'] + (CARVING_F if name == 'wall' else F)
        cx, cz = (x0 + x1) / 2 + 0.5, (z0 + z1) / 2 + 0.5
        fx, fz = [(0, 1), (-1, 0), (0, -1), (1, 0)][r]                 # the template's front, turned
        sc = views.Scene(world, int(cx) - 120, int(cx) + 120, max(0, floor - 60), min(255, floor + 80),
                         int(cz) - 120, int(cz) + 120)
        biome = world.biome(int(cx), floor, int(cz)) or 'minecraft:nether_wastes'
        fog = views.FOG.get(biome, views.FOG['minecraft:nether_wastes'])
        mid = (cx, floor + 3, cz)
        near = 16 if name != 'wall' else 7
        shots = []
        for label, dist, side in ((f'from the front, {near} blocks', near + 6, 0.0), ('from 64 blocks', 64, 0.5),
                                  ('from above', 30, 0.3)):
            ex, ez = cx + (fx - side * fz) * dist, cz + (fz + side * fx) * dist
            ey = floor + 1.62 if label != 'from above' else floor + 28
            gy = None
            for y in range(int(floor + 40), int(floor - 20), -1):
                st = sc.get(int(ex), y, int(ez))
                if st and views.Looks([st]).solid[0] and 'lava' not in st and views.Looks([st]).sparse[0] == 0:
                    gy = y
                    break
            if gy is not None and label != 'from above':
                ey = max(ey, gy + 2.62)
            shots.append((label, sc.render((ex, ey, ez), mid, haze=HAZE, fog=fog)))
        _sheet(shots + [('', shots[0][1].copy())][:0], os.path.join(DESIGN, f'placed-{name}.png'),
               f'{name} where worldgen placed it: ({piece["PosX"]}, {piece["PosY"]}, {piece["PosZ"]}) '
               f'{piece["rotation"]}, {biome.split(":")[-1]}, terrain_adaptation beard_thin', cols=3)


def main():
    s = gate_site()
    problems = check_site(s)
    if problems:
        for p in problems:
            print('PROBLEM:', p)
        if '--force' not in sys.argv:
            sys.exit(1)
    with open(SITE_PATH, 'w', encoding='utf-8', newline='\n') as f:
        f.write(site_json(s))
    print('wrote', os.path.relpath(SITE_PATH, ROOT), f'({len(s.cells)} cells)')
    ruins = [hall_a(), hall_b(), hall_c()]
    problems, seen = check_ruins(ruins)
    for r in ruins:
        print(f'{r.name}: {r.size}, {len(r.cells)} cells, plinths ' + ', '.join(e for e, _, _ in r.plinths))
    if problems:
        for p in problems:
            print('PROBLEM:', p)
        if '--force' not in sys.argv:
            sys.exit(1)
    carving = sealer_wall()
    problems = check_carving(carving)
    if problems:
        for p in problems:
            print('PROBLEM:', p)
        if '--force' not in sys.argv:
            sys.exit(1)
    os.makedirs(CARVING_OUT, exist_ok=True)
    path = os.path.join(CARVING_OUT, carving.name + '.nbt')
    with open(path, 'wb') as f:
        f.write(template_nbt(carving))
    print('wrote', os.path.relpath(path, ROOT))
    os.makedirs(COLONY_OUT, exist_ok=True)
    for r in ruins:
        path = os.path.join(COLONY_OUT, r.name + '.nbt')
        with open(path, 'wb') as f:
            f.write(template_nbt(r))
        print('wrote', os.path.relpath(path, ROOT))
    if '--no-previews' in sys.argv:
        return
    views = _load('render_nether_views', 'render-nether-views.py')
    world = sys.argv[sys.argv.index('--world') + 1] if '--world' in sys.argv else None
    only = sys.argv[sys.argv.index('--only') + 1].split(',') if '--only' in sys.argv else ['site', 'ruins', 'carving', 'sky']
    if 'sky' in only and world:
        sky_previews(views, world)
    if '--placed' in sys.argv:
        i = sys.argv.index('--placed')
        placed_previews(views, sys.argv[i + 1], sys.argv[i + 2])
    if 'site' in only:
        site_previews(views, world)
    if 'ruins' in only:
        ruin_previews(views, ruins)
    if 'carving' in only:
        carving_previews(views, carving)


if __name__ == '__main__':
    main()
