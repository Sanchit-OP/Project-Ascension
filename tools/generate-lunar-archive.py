"""The Lunar Archive's structure templates. No build or runtime dependency.

Run from the repository root:  python tools/generate-lunar-archive.py
Writes the vanilla structure templates (.nbt) under
modules/worlds/src/main/resources/data/ascension_worlds/structure/lunar_archive/ and the previews
under design/lunar-archive/. Hand edits to either are lost on the next run: edit this file.

The contract is docs/technical/lunar-archive-format.md; the design record is
docs/technical/lunar-archive-design.md. Two pieces, joined by one jigsaw:

  entrance.nbt  the surface (forecourt, two pylons, the open cutting, the first door) and the
                covered stair down. Placement starts here; its paving row is SURFACE_Y.
  vault.nbt     everything underground: the murals hall, the orrery rotunda, the stores, the deep
                passage, the gate hall and the sealed room.

Deterministic, like every other piece of art here (art-guide rule 2): the layout is fixed code,
the only randomness (erosion, rubble, hurried masonry) comes from seeded generators, and the NBT
is written with a sorted palette, a fixed block order and a gzip header with no name and mtime 0,
so re-running reproduces the committed files byte for byte.

Checks the contract before writing anything (every plinth names one of the eight entries exactly
once, one orrery with its clear volume, chests name their loot table, pieces fit 48 x 48 x 48, the
jigsaws pair up), and checks light: every floor cell a monster could stand on must have block
light above 0, because the Moon spawns hostiles at block light 0.
"""
import gzip
import importlib.util
import io
import math
import os
import random
import struct
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, '..'))
OUT = os.path.join(ROOT, 'modules/worlds/src/main/resources/data/ascension_worlds/structure/lunar_archive')
DESIGN = os.path.join(ROOT, 'design/lunar-archive')
DATA_VERSION = 3955          # 1.21.1
NS = 'ascension_worlds'
MAX_PIECE = 48


def _load(name, filename):
    spec = importlib.util.spec_from_file_location(name, os.path.join(HERE, filename))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


art = _load('archive_art', 'generate-archive-art.py')       # the mural catalogue lives there


# --- block states ----------------------------------------------------------------------------

def B(name, **props):
    """A block state as 'ns:id[k=v,...]', properties sorted, so equal states are equal strings."""
    if ':' not in name:
        name = f'{NS}:{name}'
    if not props:
        return name
    return name + '[' + ','.join(f'{k}={v}' for k, v in sorted(props.items())) + ']'


def parse_state(state):
    if '[' not in state:
        return state, {}
    name, rest = state[:-1].split('[', 1)
    return name, dict(kv.split('=', 1) for kv in rest.split(','))


AIR = 'minecraft:air'
RAW = B('moon_basalt')                       # rubble core behind every wall
BRICK = B('moon_basalt_bricks')
CRACKED = B('cracked_moon_basalt_bricks')
POLISHED = B('polished_moon_basalt')
PALE = B('anorthosite_bricks')
PALE_CRACKED = B('cracked_anorthosite_bricks')
CHISELED = B('chiseled_anorthosite')
PALE_RAW = B('anorthosite')
REGOLITH = B('moon_regolith')
GATE = B('inscribed_gatestone', lit='false')
LIT = B('inscribed_gatestone', lit='true')   # light 9: the Archive's lamps are its own script
ORRERY = B('archive_orrery')


def pillar(axis='y'):
    return B('anorthosite_pillar', axis=axis)


def slab(kind, half='bottom'):
    return B(f'{kind}_slab', type=half, waterlogged='false')


def stairs(kind, facing, half='bottom', shape='straight'):
    return B(f'{kind}_stairs', facing=facing, half=half, shape=shape, waterlogged='false')


def mural(tile):
    return B('archive_mural', tile=tile)


def plinth(facing):
    return B('archive_plinth', facing=facing)


def chest(facing):
    return B('minecraft:chest', facing=facing, type='single', waterlogged='false')


def jigsaw(orientation):
    return B('minecraft:jigsaw', orientation=orientation)


BRICK_SLAB, PALE_SLAB = 'moon_basalt_brick', 'anorthosite_brick'
BRICK_STAIRS, PALE_STAIRS = 'moon_basalt_brick', 'anorthosite_brick'

# The eight Moon inscriptions, each placed exactly once (contract table).
ENTRIES = ['the_road', 'the_builders', 'many_worlds', 'the_keepers', 'the_quiet', 'the_last_order',
           'the_sealing', 'unfinished']
LOOT = f'{NS}:archive/stores'

# What light passes through and what a mob can stand on. Light is propagated through anything not
# in SOLID; spawn floors are SOLID blocks with air above. Slabs and stairs count as see-through
# (light leaks past their open half) and as not standable (vanilla never spawns on a bottom slab
# or bottom stair) -- top slabs are full-height on top, so they are both solid and standable.
EMITS = {LIT: 9, ORRERY: 10}


def is_solid(state):
    if state is None or state == AIR:
        return False
    name, props = parse_state(state)
    if name.endswith('_slab'):
        return props.get('type') in ('top', 'double')
    return not (name.endswith('_stairs') or name in (f'{NS}:archive_plinth', 'minecraft:chest',
                                                     'minecraft:jigsaw'))


# --- the voxel grid --------------------------------------------------------------------------

# Who wins a cell: a room's interior beats any neighbour's wall, and walls beat the rubble core,
# so rooms can be laid down in any order. Details (pillars, murals, inlay, furniture) beat all.
CORE, WALL, OPEN, DETAIL = 1, 2, 3, 4


class Piece:
    def __init__(self, name, size):
        self.name, self.size = name, size
        self.cells = {}                 # (x, y, z) -> (priority, state)
        self.nbt = {}                   # (x, y, z) -> block-entity tag

    def inside(self, x, y, z):
        sx, sy, sz = self.size
        return 0 <= x < sx and 0 <= y < sy and 0 <= z < sz

    def put(self, x, y, z, state, prio=DETAIL, nbt=None, clip=False):
        if not self.inside(x, y, z):
            if clip:
                return
            raise ValueError(f'{self.name}: {(x, y, z)} is outside {self.size}')
        cur = self.cells.get((x, y, z))
        if cur is None or prio >= cur[0]:
            self.cells[(x, y, z)] = (prio, state)
            if nbt is not None:
                self.nbt[(x, y, z)] = nbt
            else:
                self.nbt.pop((x, y, z), None)

    def fill(self, x0, y0, z0, x1, y1, z1, state, prio=DETAIL, clip=False):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.put(x, y, z, state, prio, clip=clip)

    def erase(self, x, y, z):
        """Back to structure_void: the template leaves this cell's terrain alone."""
        self.cells.pop((x, y, z), None)
        self.nbt.pop((x, y, z), None)

    def get(self, x, y, z):
        c = self.cells.get((x, y, z))
        return c[1] if c else None


def room(p, x0, x1, y0, y1, z0, z1, wall=BRICK, floor=POLISHED, ceil=BRICK, core=RAW, core_t=1):
    """Carve the interior box (inclusive) to air, wrap it in one block of wall, floor and ceiling,
    and core_t blocks of rubble core beyond that. Interiors are OPEN so a later room's wall never
    fills an earlier room."""
    t = 1 + core_t
    for x in range(x0 - t, x1 + t + 1):
        for y in range(y0 - t, y1 + t + 1):
            for z in range(z0 - t, z1 + t + 1):
                dx, dy, dz = max(x0 - x, 0, x - x1), max(y0 - y, 0, y - y1), max(z0 - z, 0, z - z1)
                d = max(dx, dy, dz)
                if d == 0:
                    p.put(x, y, z, AIR, OPEN)
                elif d == 1:
                    if dx == 0 and dz == 0:
                        st = floor if y < y0 else ceil
                    else:
                        st = wall
                    p.put(x, y, z, st, WALL, clip=True)
                else:
                    p.put(x, y, z, core, CORE, clip=True)


def run(p, axis, a0, a1, rows, h, wall=BRICK, floor=POLISHED, ceil=BRICK, core=RAW, step_kind=None,
        down_dir=None, under=RAW):
    """A corridor whose floor changes height along its length: rows is [(position, floor_y,
    is_step)] along `axis` ('x' or 'z'); a0..a1 is its width on the other axis. Each row is carved
    h blocks tall above its floor, with walls, a ceiling following the steps, and rubble below.
    A step row puts a stairs block of step_kind at floor_y facing down_dir's opposite (the way up)."""
    up = {'north': 'south', 'south': 'north', 'east': 'west', 'west': 'east'}[down_dir] if down_dir else None
    for pos, fy, is_step in rows:
        for w in range(a0 - 2, a1 + 3):
            for y in range(fy - 2, fy + h + 3):
                x, z = (pos, w) if axis == 'x' else (w, pos)
                edge = w < a0 or w > a1
                far = w < a0 - 1 or w > a1 + 1 or y < fy - 1 or y > fy + h + 1
                if far:
                    p.put(x, y, z, core, CORE, clip=True)
                elif edge or y > fy + h:
                    p.put(x, y, z, ceil if (not edge and y > fy + h) else wall, WALL, clip=True)
                elif y > fy:
                    p.put(x, y, z, AIR, OPEN)
                elif y == fy:
                    if is_step:
                        p.put(x, y, z, stairs(step_kind, up), DETAIL)
                    else:
                        p.put(x, y, z, floor, WALL)
                else:
                    p.put(x, y, z, under, WALL)


def disc(cx, cz, r):
    for x in range(int(math.floor(cx - r)) - 1, int(math.ceil(cx + r)) + 2):
        for z in range(int(math.floor(cz - r)) - 1, int(math.ceil(cz + r)) + 2):
            d = math.hypot(x - cx, z - cz)
            if d < r:
                yield x, z, d


# --- murals ------------------------------------------------------------------------------------

def place_mural(p, name, left, y_bottom, wall_axis, wall_pos, facing):
    """Lay a mural from the catalogue into a wall. `facing` is the side the room is on: the picture
    must read left to right for someone standing there, so the tile order depends on it."""
    m = art.MURALS[name]
    w, h = m['size']
    for col in range(w):
        for row in range(h):
            tile = m['first'] + row * w + col
            # left-to-right as seen from the room side
            step = {'south': 1, 'north': -1, 'west': 1, 'east': -1}[facing]
            a = left + step * col
            y = y_bottom + (h - 1 - row)
            if wall_axis == 'z':          # wall runs along x at z = wall_pos
                p.put(a, y, wall_pos, mural(tile), DETAIL)
            else:                         # wall runs along z at x = wall_pos
                p.put(wall_pos, y, a, mural(tile), DETAIL)


def put_plinth(p, x, y, z, facing, entry):
    p.put(x, y, z, plinth(facing), DETAIL,
          nbt={'id': f'{NS}:archive_plinth', 'entry': f'{NS}:moon/{entry}'})


def put_chest(p, x, y, z, facing):
    p.put(x, y, z, chest(facing), DETAIL, nbt={'id': 'minecraft:chest', 'LootTable': LOOT})


def put_jigsaw(p, x, y, z, orientation, name, target, pool, final_state):
    p.put(x, y, z, jigsaw(orientation), DETAIL, nbt={
        'id': 'minecraft:jigsaw', 'name': name, 'target': target, 'pool': pool,
        'final_state': final_state, 'joint': 'rollable',
        'placement_priority': 0, 'selection_priority': 0})


# --- the vault ---------------------------------------------------------------------------------
#
# Vault coordinates: x east, z south, y up. The entrance joins at the south edge (z = 47) on the
# murals hall's axis (x = 23). Main floor row 5 (stand at y = 6); the gate hall and sealed room are
# four lower, floor row 1.

VAULT_SIZE = (48, 21, 48)
MAIN, LOW = 5, 1
HALL_X = 23
ROT_C = (23, 16)         # rotunda centre (x, z)
ROT_R, ROT_WALL, ROT_CORE = 8.6, 9.7, 10.9


def vault():
    p = Piece('vault', VAULT_SIZE)
    murals_hall(p)
    rotunda(p)
    stores(p)
    deep_passage(p)
    gate_hall(p)
    sealed_room(p)
    # The way in from the entrance: the jigsaw sits in the threshold's floor and becomes floor.
    put_jigsaw(p, HALL_X, MAIN, 47, 'south_up', f'{NS}:archive_hall', f'{NS}:archive_stair',
               'minecraft:empty', POLISHED)
    return p


def murals_hall(p):
    x0, x1, z0, z1 = 20, 26, 26, 46
    room(p, x0, x1, MAIN + 1, MAIN + 7, z0, z1)
    # threshold from the entrance stair, 5 wide and 5 tall, in the south wall
    p.fill(21, MAIN + 1, 47, 25, MAIN + 5, 47, AIR, OPEN)
    p.fill(21, MAIN, 47, 25, MAIN, 47, POLISHED, WALL)
    # floor: polished, a pale border, and the road -- lit script down the middle to the orrery
    p.fill(x0, MAIN, z0, x0, MAIN, z1, PALE, DETAIL)
    p.fill(x1, MAIN, z0, x1, MAIN, z1, PALE, DETAIL)
    p.fill(HALL_X, MAIN, z0, HALL_X, MAIN, 47, LIT, DETAIL)
    # bays: pillar, five-wide mural, pillar ... read from the entrance toward the rotunda
    pillars = [45, 39, 33, 27]
    for z in pillars:
        for x in (x0, x1):
            p.put(x, MAIN + 1, z, PALE)
            p.fill(x, MAIN + 2, z, x, MAIN + 6, z, pillar())
            p.put(x, MAIN + 7, z, CHISELED)
        p.fill(x0 + 1, MAIN + 7, z, x1 - 1, MAIN + 7, z, PALE)          # transverse rib
    # a pale band above the murals, both walls, full length
    p.fill(19, MAIN + 6, z0, 19, MAIN + 6, z1, PALE)
    p.fill(27, MAIN + 6, z0, 27, MAIN + 6, z1, PALE)
    bays = [(44, 40), (38, 34), (32, 28)]             # (south end, north end) of each bay
    west = ['stonecutters', 'the_step', 'the_road']   # the builders' story, on the left going in
    east = ['ring_ash', 'ring_ice', 'ring_forge']     # the same ring on every world, on the right
    for (zs, zn), wname, ename in zip(bays, west, east):
        # west wall, seen looking west: left is south
        place_mural(p, wname, zs, MAIN + 3, 'x', 19, 'east')
        # east wall, seen looking east: left is north
        place_mural(p, ename, zn, MAIN + 3, 'x', 27, 'west')
        # a lit script tablet in the dado under each mural, like a label light
        zc = (zs + zn) // 2
        p.put(19, MAIN + 2, zc, LIT)
        p.put(27, MAIN + 2, zc, LIT)
    put_plinth(p, 21, MAIN + 1, 42, 'east', 'the_builders')
    put_plinth(p, 25, MAIN + 1, 30, 'west', 'many_worlds')


def rotunda(p):
    cx, cz = ROT_C
    top = MAIN + 6                      # the drum's last open row; the dome corbels in above it
    dome = {top + 1: 8.0, top + 2: 7.2, top + 3: 6.2, top + 4: 5.0, top + 5: 3.6, top + 6: 2.0}
    cap = top + 7
    for x, z, d in disc(cx, cz, ROT_CORE + 1.5):
        if d >= ROT_CORE + 0.3:
            continue
        # foundation and floor
        p.put(x, MAIN - 1, z, RAW, CORE, clip=True)
        if d < ROT_R:
            p.put(x, MAIN, z, POLISHED, WALL)
        elif d < ROT_WALL:
            p.put(x, MAIN, z, BRICK, WALL, clip=True)
        else:
            p.put(x, MAIN, z, RAW, CORE, clip=True)
        # the drum
        for y in range(MAIN + 1, top + 1):
            if d < ROT_R:
                p.put(x, y, z, AIR, OPEN)
            elif d < ROT_WALL:
                p.put(x, y, z, BRICK, WALL, clip=True)
            else:
                p.put(x, y, z, RAW, CORE, clip=True)
        # the dome: corbelled rings, each a step in
        for y, r in dome.items():
            if d < r:
                p.put(x, y, z, AIR, OPEN)
            elif d < r + 1.15:
                p.put(x, y, z, BRICK, WALL, clip=True)
            elif d < r + 2.4:
                p.put(x, y, z, RAW, CORE, clip=True)
        if d < 3.2:
            p.put(x, cap, z, BRICK if d >= 0.5 else CHISELED, WALL)
        if d < 2.2:
            p.put(x, cap + 1, z, RAW, CORE)
    # floor rings: a pale border, two orbits in script (the inner one lit), the road coming in
    for x, z, d in disc(cx, cz, ROT_R):
        if d >= 7.6:
            p.put(x, MAIN, z, PALE)
        elif abs(d - 6.0) < 0.5:
            p.put(x, MAIN, z, GATE)
        elif abs(d - 4.2) < 0.5:
            p.put(x, MAIN, z, LIT)
    p.fill(HALL_X, MAIN, cz + 5, HALL_X, MAIN, 25, LIT)
    # the dais and the orrery on it; the hologram needs 5 x 5 x 4 clear above
    for x, z, d in disc(cx, cz, 3.6):
        if d < 2.6:
            p.put(x, MAIN + 1, z, POLISHED)
        else:
            p.put(x, MAIN + 1, z, slab(PALE_SLAB))
    p.put(cx, MAIN + 2, cz, ORRERY)
    # a ring of lit script at the dome's springing line
    for x, z, d in disc(cx, cz, ROT_WALL):
        if d >= ROT_R:
            p.put(x, top, z, LIT)
    # doorways: south to the murals hall, west to the deep passage, east to the stores, north the niche
    p.fill(21, MAIN + 1, 25, 25, MAIN + 5, 25, AIR, OPEN)
    p.fill(12, MAIN + 1, 15, 15, MAIN + 4, 17, AIR, OPEN)
    p.fill(31, MAIN + 1, 15, 34, MAIN + 4, 17, AIR, OPEN)
    # pillars flanking each opening
    for (x, z) in [(20, 24), (26, 24), (15, 13), (15, 19), (31, 13), (31, 19), (21, 8), (25, 8)]:
        p.fill(x, MAIN + 1, z, x, top - 1, z, pillar())
        p.put(x, top, z, CHISELED)
    # the keepers' niche
    room(p, 21, 25, MAIN + 1, MAIN + 5, 4, 7)
    p.fill(21, MAIN + 1, 7, 25, MAIN + 5, 8, AIR, OPEN)
    place_mural(p, 'keeper', 22, MAIN + 1, 'z', 3, 'south')
    p.fill(21, MAIN, 4, 25, MAIN, 7, PALE)
    p.put(23, MAIN, 6, LIT)
    put_plinth(p, 23, MAIN + 1, 9, 'south', 'the_keepers')


def stores(p):
    # a short passage from the rotunda's east door to a storeroom off the main path
    room(p, 33, 36, MAIN + 1, MAIN + 4, 15, 17)
    room(p, 37, 44, MAIN + 1, MAIN + 5, 12, 20)
    p.fill(37, MAIN + 1, 15, 37, MAIN + 4, 17, AIR, OPEN)
    p.fill(36, MAIN + 1, 15, 37, MAIN + 4, 17, AIR, OPEN)
    p.fill(33, MAIN, 16, 43, MAIN, 16, LIT)
    put_chest(p, 44, MAIN + 1, 14, 'west')
    put_chest(p, 44, MAIN + 1, 18, 'west')
    # shelves along the north and south walls
    for z in (12, 20):
        p.fill(38, MAIN + 3, z, 43, MAIN + 3, z, slab(PALE_SLAB, 'top'))


def deep_passage(p):
    # west from the rotunda, then south and down four, past the last two hall inscriptions
    room(p, 2, 13, MAIN + 1, MAIN + 5, 14, 18)
    p.fill(2, MAIN, 14, 13, MAIN, 18, POLISHED, WALL)
    # the unfinished mural on the north wall, read looking north
    place_mural(p, 'the_quiet', 6, MAIN + 2, 'z', 13, 'south')
    put_plinth(p, 8, MAIN + 1, 15, 'south', 'the_quiet')
    p.fill(2, MAIN, 16, 13, MAIN, 16, LIT)
    # the stair south: (z, floor_y, is_step)
    rows = [(19, MAIN, False), (20, MAIN, True), (21, MAIN - 1, True)]
    rows += [(z, MAIN - 2, False) for z in range(22, 26)]
    rows += [(26, MAIN - 2, True), (27, MAIN - 3, True), (28, LOW, False), (29, LOW, False), (30, LOW, False)]
    run(p, 'z', 2, 6, rows, 5, step_kind=BRICK_STAIRS, down_dir='south')
    p.put(4, MAIN - 2, 23, LIT)
    p.put(7, LOW + 2, 29, LIT)
    put_plinth(p, 3, MAIN - 1, 24, 'east', 'the_last_order')


def gate_hall(p):
    x0, x1, z0, z1 = 1, 16, 31, 40
    room(p, x0, x1, LOW + 1, LOW + 15, z0, z1)
    # the passage comes in through the north wall
    p.fill(2, LOW + 1, 30, 6, LOW + 5, 30, AIR, OPEN)
    # the dead gate on the south wall: a ring of gatestone, its field bricked up, its keystone gone
    gx = 12
    p.fill(gx - 6, LOW, 41, gx + 4, LOW + 13, 41, POLISHED)            # dark panel behind it
    for x in range(gx - 3, gx + 4):
        for y in range(LOW + 1, LOW + 10):
            ring = x in (gx - 3, gx + 3) or y in (LOW + 1, LOW + 9)
            if ring:
                p.put(x, y, 40, GATE)
            else:
                p.put(x, y, 40, AIR)
    rng = random.Random(41)
    for x in range(gx - 2, gx + 3):
        for y in range(LOW + 2, LOW + 9):
            p.put(x, y, 41, rng.choice([RAW, RAW, CRACKED, BRICK]))       # hurried sealing
    p.put(gx, LOW + 1, 40, AIR)                                              # keystone prised out
    for x in (gx - 4, gx + 4):
        p.fill(x, LOW + 1, 40, x, LOW + 9, 40, pillar())
    p.fill(gx - 4, LOW + 10, 40, gx + 4, LOW + 10, 40, PALE)
    # a stepped hood over it, the eye carved in its face, dead
    for i, half in enumerate((4, 3, 2, 1, 0)):
        p.fill(gx - half, LOW + 11 + i, 40, gx + half, LOW + 11 + i, 40, PALE)
    p.put(gx, LOW + 12, 40, CHISELED)
    # the road from the sealed room to the gate, broken where the dark took it
    for z in range(31, 40):
        p.put(gx, LOW, z, LIT if z % 3 else GATE)
    p.fill(x0, LOW, z0, x1, LOW, z0, PALE)
    # and the way in from the passage, lit, along the west side
    for z in (33, 36, 39):
        p.put(4, LOW, z, LIT)
    # tall pale pilasters down both side walls
    for z in (33, 37):
        for x in (x0, x1):
            p.fill(x, LOW + 1, z, x, LOW + 14, z, pillar())
            p.put(x, LOW + 15, z, CHISELED)
    # lamps: script set into the walls, never into the gate
    for pos in [(gx - 5, LOW + 10, 41), (17, LOW + 10, 39), (0, LOW + 10, 35), (17, LOW + 10, 35),
                (17, LOW + 3, 31), (gx, LOW + 16, 38)]:
        p.put(*pos, LIT)
    # a fallen piece of the ceiling in the empty half, the way the builders left it
    rng = random.Random(44)
    for x, z in [(6, 35), (7, 35), (7, 36), (6, 36), (8, 36), (7, 37)]:
        p.put(x, LOW + 1, z, rng.choice([RAW, CRACKED, slab(BRICK_SLAB)]))
    p.put(7, LOW + 2, 36, RAW)


def sealed_room(p):
    x0, x1, z0, z1 = 9, 15, 24, 29
    room(p, x0, x1, LOW + 1, LOW + 5, z0, z1)
    rng = random.Random(30)
    # the seal: rough blocks where the rest is dressed, laid in a hurry, broken through
    for x in range(x0, x1 + 1):
        for y in range(LOW + 1, LOW + 6):
            p.put(x, y, 30, rng.choice([RAW, RAW, CRACKED]))
    p.fill(11, LOW + 1, 30, 13, LOW + 3, 30, AIR)
    for (x, z, st) in [(11, 29, RAW), (13, 28, CRACKED), (10, 28, slab(BRICK_SLAB)), (14, 29, slab(BRICK_SLAB)),
                       (12, 31, slab(BRICK_SLAB))]:
        p.put(x, LOW + 1, z, st)
    put_plinth(p, 12, LOW + 1, 25, 'south', 'the_sealing')
    put_plinth(p, 12, LOW + 1, 28, 'north', 'unfinished')
    # the only light: script in the floor where the reader stands, between the last two
    p.fill(12, LOW, 26, 12, LOW, 27, LIT)
    # a stone bench along the west wall
    p.fill(x0, LOW + 1, 25, x0, LOW + 1, 27, slab(PALE_SLAB))


# --- the entrance --------------------------------------------------------------------------------
#
# Entrance coordinates: z = 0 is the north edge, where the vault joins; the player arrives from the
# south (high z) and goes down northward. Floor row 1 at the bottom; the forecourt paving is
# SURFACE_Y.

ENTRANCE_SIZE = (19, 47, 48)
XC = 9
FE = 1
SURFACE_Y = 21
CUT_FLOOR = SURFACE_Y - 7          # floor of the open cutting at the first door


def entrance():
    p = Piece('entrance', ENTRANCE_SIZE)
    covered_stair(p)
    cutting(p)
    forecourt(p)
    pylon(p, 2, 33, broken=False)
    pylon(p, 16, 33, broken=True)
    put_jigsaw(p, XC, FE, 0, 'north_up', f'{NS}:archive_stair', f'{NS}:archive_hall',
               f'{NS}:lunar_archive/vault', POLISHED)
    return p


def covered_stair(p):
    x0, x1 = XC - 2, XC + 2
    rows = [(z, FE, False) for z in range(0, 4)]
    rows += [(4 + i, FE + 1 + i, True) for i in range(6)]          # y 2..7
    rows += [(z, FE + 6, False) for z in range(10, 13)]             # mid landing, floor row 7
    rows += [(13 + i, FE + 7 + i, True) for i in range(7)]          # y 8..14
    rows += [(z, CUT_FLOOR, False) for z in (20, 21)]               # the first door
    run(p, 'z', x0, x1, rows, 5, step_kind=BRICK_STAIRS, down_dir='north')
    # lit script set into both walls at knee height, every other step
    for z, fy, _ in rows:
        if z % 2 == 0 and 2 <= z <= 19:
            p.put(x0 - 1, fy + 1, z, LIT)
            p.put(x1 + 1, fy + 1, z, LIT)
    p.put(XC, FE, 2, LIT)


def cutting(p):
    x0, x1 = XC - 2, XC + 2
    # the door: a narrow, deep opening under a lintel, the eye above it rising out of the ground,
    # so from the forecourt it shows as a pale gable and from the cutting as the whole diamond
    for z in (20, 21):
        p.fill(x0 - 1, CUT_FLOOR + 1, z, x1 + 1, SURFACE_Y, z, BRICK, WALL)
        p.fill(XC - 1, CUT_FLOOR + 1, z, XC + 1, CUT_FLOOR + 4, z, AIR, OPEN)
    for x in (XC - 2, XC + 2):
        p.fill(x, CUT_FLOOR + 1, 21, x, CUT_FLOOR + 4, 21, pillar())
    p.fill(XC - 2, CUT_FLOOR + 5, 21, XC + 2, CUT_FLOOR + 5, 21, PALE)
    lozenge(p, XC, SURFACE_Y + 1, 21, 3, PALE, POLISHED, LIT)
    # the open cutting: landing, then steps up to the surface, walls retaining the ground
    rows = [(z, CUT_FLOOR, False) for z in (22, 23, 24)]
    rows += [(25 + i, CUT_FLOOR + 1 + i, True) for i in range(7)]   # y 15..21
    for z, fy, is_step in rows:
        for x in range(x0 - 2, x1 + 3):
            edge = x in (x0 - 1, x1 + 1)
            far = x in (x0 - 2, x1 + 2)
            for y in range(fy - 2, SURFACE_Y + 5):
                if far:
                    if y <= SURFACE_Y - 1:
                        p.put(x, y, z, RAW, CORE)
                elif edge:
                    if y <= SURFACE_Y:
                        p.put(x, y, z, BRICK, WALL)
                    elif y == SURFACE_Y + 1:
                        p.put(x, y, z, slab(PALE_SLAB), DETAIL)
                elif y > fy:
                    p.put(x, y, z, AIR, OPEN)
                elif y == fy:
                    p.put(x, y, z, stairs(PALE_STAIRS, 'south') if is_step else POLISHED,
                          DETAIL if is_step else WALL)
                else:
                    p.put(x, y, z, RAW, WALL)
    p.fill(x0 - 1, CUT_FLOOR, 22, x1 + 1, CUT_FLOOR, 24, POLISHED, WALL)
    put_plinth(p, XC, CUT_FLOOR + 1, 23, 'south', 'the_road')


def forecourt(p):
    rng = random.Random(1969)
    sx, _, sz = p.size
    for x in range(sx):
        for z in range(22, sz):
            # an eroded ellipse of paving around the cutting's head
            e = ((x - XC) / 10.5) ** 2 + ((z - 34) / 14.0) ** 2
            if e > 1.0 or (e > 0.7 and rng.random() < (e - 0.7) * 2.6):
                continue
            if p.get(x, SURFACE_Y, z) is not None and z < 32:
                continue
            p.put(x, SURFACE_Y - 2, z, RAW, CORE)
            p.put(x, SURFACE_Y - 1, z, RAW, CORE)
            st = POLISHED if (x + z) % 2 == 0 else BRICK
            if rng.random() < 0.16:
                st = CRACKED
            if abs(x - XC) <= 2:
                st = PALE                                       # the avenue to the cutting's head
            p.put(x, SURFACE_Y, z, st, WALL)
            for y in range(SURFACE_Y + 1, SURFACE_Y + 4):
                p.put(x, y, z, AIR, OPEN)
            if rng.random() < 0.05:
                p.put(x, SURFACE_Y + 1, z, slab(BRICK_SLAB) if rng.random() < 0.5 else REGOLITH)


def lozenge(p, cx, cy, z, r, rim, inner=None, pupil=None, rim_from=None):
    """The network's eye as masonry: a filled diamond in the x-y plane at depth z. A one-block
    outline of a diamond reads as a ringed cross from any distance (the art guide's accidental
    symbol), so the diamond is always solid: a thick pale rim, an optional dark inner ring, a pupil."""
    rim_from = r - 1 if rim_from is None else rim_from
    for dx in range(-r, r + 1):
        for dy in range(-r, r + 1):
            d = abs(dx) + abs(dy)
            if d > r:
                continue
            st = rim if d >= rim_from else (pupil if d == 0 and pupil else inner or rim)
            p.put(cx + dx, cy + dy, z, st)


def pylon(p, cx, cz, broken):
    s = SURFACE_Y
    for x in range(cx - 2, cx + 3):
        for z in range(cz - 2, cz + 3):
            for y in range(s - 6, s + 1):
                p.put(x, y, z, RAW, WALL)                     # footing, down past any dip
            p.put(x, s + 1, z, POLISHED)
            edge = abs(x - cx) == 2 or abs(z - cz) == 2
            p.put(x, s + 2, z, slab(BRICK_SLAB) if edge else BRICK)
    top = s + 9 if broken else s + 17
    for x in range(cx - 1, cx + 2):
        for z in range(cz - 1, cz + 2):
            corner = abs(x - cx) == 1 and abs(z - cz) == 1
            for y in range(s + 3, top + 1):
                st = pillar() if corner else PALE
                if y == s + 3 and not corner:
                    st = CHISELED
                if y == s + 14 and not corner:
                    st = GATE
                p.put(x, y, z, st)
    if broken:
        rng = random.Random(7)
        for (x, z) in [(cx - 1, cz + 1), (cx, cz + 1), (cx + 1, cz), (cx - 1, cz)]:
            p.erase(x, top, z)
        p.erase(cx - 1, top - 1, cz + 1)
        p.put(cx + 1, top - 1, cz + 1, PALE_CRACKED)
        # the fallen top lies across the forecourt, broken into drums, its spire at the end
        for z0, z1 in [(cz + 4, cz + 6), (cz + 8, cz + 9), (cz + 11, cz + 12)]:
            for z in range(z0, z1 + 1):
                for x in range(cx - 1, cx + 2):
                    for y in range(s + 1, s + 4):
                        corner = abs(x - cx) == 1 and y in (s + 1, s + 3)
                        st = pillar('z') if corner else (PALE_CRACKED if rng.random() < 0.3 else PALE)
                        if z == cz + 9 and not corner and y == s + 2:
                            st = GATE
                        p.put(x, y, z, st)
        p.fill(cx, s + 1, cz + 13, cx, s + 1, cz + 13, PALE)
        p.put(cx, s + 1, cz + 14, CHISELED)
        return
    # taper to a needle, capped with the eye
    for x in range(cx - 1, cx + 2):
        for z in range(cz - 1, cz + 2):
            corner = abs(x - cx) == 1 and abs(z - cz) == 1
            p.put(x, top + 1, z, slab(PALE_SLAB) if corner else PALE)
    for y in range(top + 2, top + 5):
        p.put(cx, y, cz, pillar())
    p.put(cx, top + 5, cz, CHISELED)
    p.put(cx, top + 6, cz, slab(PALE_SLAB))


# --- checks --------------------------------------------------------------------------------------

def check(pieces):
    problems = []
    seen = {}
    orreries = []
    jigsaws = []
    for p in pieces:
        if any(n > MAX_PIECE for n in p.size):
            problems.append(f'{p.name}: size {p.size} exceeds {MAX_PIECE}')
        for pos, (_, state) in p.cells.items():
            name, _ = parse_state(state)
            if name == f'{NS}:archive_plinth':
                e = p.nbt.get(pos, {}).get('entry', '').split('/')[-1]
                seen.setdefault(e, []).append((p.name, pos))
            elif state == ORRERY:
                orreries.append((p, pos))
            elif name == 'minecraft:chest':
                if p.nbt.get(pos, {}).get('LootTable') != LOOT:
                    problems.append(f'{p.name}: chest at {pos} has no loot table')
            elif name == 'minecraft:jigsaw':
                jigsaws.append((p.name, pos, p.nbt[pos]))
    for e in ENTRIES:
        if len(seen.get(e, [])) != 1:
            problems.append(f'entry {e} used {len(seen.get(e, []))} times')
    for e in seen:
        if e not in ENTRIES:
            problems.append(f'unknown entry {e}')
    if len(orreries) != 1:
        problems.append(f'{len(orreries)} orreries')
    for p, (x, y, z) in orreries:
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                for dy in range(1, 5):
                    if p.get(x + dx, y + dy, z + dz) != AIR:
                        problems.append(f'orrery clear volume blocked at {(x + dx, y + dy, z + dz)}')
    names = {j[2]['name'] for j in jigsaws}
    for pname, pos, tag in jigsaws:
        if tag['pool'] != 'minecraft:empty' and tag['target'] not in names:
            problems.append(f'{pname}: jigsaw at {pos} targets {tag["target"]}, which no piece offers')
    return problems, seen


def light(p):
    """Block light over the piece, as a flood fill from every emitter, losing one per step and
    passing only through non-solid cells. Ignores sky light and light from the other piece, so it
    errs dark."""
    lvl = {}
    frontier = []
    for pos, (_, state) in p.cells.items():
        if state in EMITS:
            lvl[pos] = EMITS[state]
            frontier.append(pos)
    while frontier:
        nxt = []
        for (x, y, z) in frontier:
            v = lvl[(x, y, z)] - 1
            if v <= 0:
                continue
            for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
                n = (x + d[0], y + d[1], z + d[2])
                st = p.get(*n)
                if st is None or is_solid(st):
                    continue
                if lvl.get(n, 0) < v:
                    lvl[n] = v
                    nxt.append(n)
        frontier = nxt
    return lvl


def open_to_sky(p, pos):
    x, y, z = pos
    for yy in range(y + 1, p.size[1]):
        st = p.get(x, yy, z)
        if st is not None and st != AIR:
            return False
    return True


def dark_spots(p):
    lvl = light(p)
    dark = []
    floors = 0
    for pos, (_, state) in p.cells.items():
        if state != AIR:
            continue
        x, y, z = pos
        below = p.get(x, y - 1, z)
        if not is_solid(below) or open_to_sky(p, pos):
            continue
        floors += 1
        if lvl.get(pos, 0) <= 0:
            dark.append(pos)
    return floors, dark, lvl


# --- NBT ---------------------------------------------------------------------------------------

END, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, BYTES, STRING, LIST, COMPOUND = range(11)


def _type(v):
    if isinstance(v, bool):
        return BYTE
    if isinstance(v, int):
        return INT
    if isinstance(v, str):
        return STRING
    if isinstance(v, dict):
        return COMPOUND
    if isinstance(v, list):
        return LIST
    raise TypeError(v)


def _payload(out, v, t):
    if t == BYTE:
        out.write(struct.pack('>b', int(v)))
    elif t == INT:
        out.write(struct.pack('>i', v))
    elif t == STRING:
        b = v.encode('utf-8')
        out.write(struct.pack('>H', len(b)))
        out.write(b)
    elif t == LIST:
        et = _type(v[0]) if v else END
        out.write(struct.pack('>bi', et, len(v)))
        for e in v:
            _payload(out, e, et)
    elif t == COMPOUND:
        for k in sorted(v):
            kt = _type(v[k])
            out.write(struct.pack('>b', kt))
            _payload(out, k, STRING)
            _payload(out, v[k], kt)
        out.write(b'\x00')


def template_nbt(p):
    states = sorted({st for _, st in p.cells.values()})
    index = {st: i for i, st in enumerate(states)}
    palette = []
    for st in states:
        name, props = parse_state(st)
        entry = {'Name': name}
        if props:
            entry['Properties'] = props
        palette.append(entry)
    blocks = []
    for (x, y, z) in sorted(p.cells, key=lambda q: (q[1], q[2], q[0])):
        b = {'pos': [x, y, z], 'state': index[p.cells[(x, y, z)][1]]}
        if (x, y, z) in p.nbt:
            b['nbt'] = p.nbt[(x, y, z)]
        blocks.append(b)
    root = {'DataVersion': DATA_VERSION, 'size': list(p.size), 'palette': palette, 'blocks': blocks,
            'entities': []}
    raw = io.BytesIO()
    raw.write(struct.pack('>b', COMPOUND))
    _payload(raw, '', STRING)
    _payload(raw, root, COMPOUND)
    out = io.BytesIO()
    with gzip.GzipFile(filename='', mode='wb', fileobj=out, mtime=0, compresslevel=9) as gz:
        gz.write(raw.getvalue())
    return out.getvalue()


# --- assembly, for previews ------------------------------------------------------------------------

# Where the vault lands in entrance coordinates: its jigsaw (23, 5, 47) sits north of the
# entrance's (9, 1, 0).
VAULT_OFFSET = (XC - HALL_X, FE - MAIN, -VAULT_SIZE[2])


def assembled(ent, vlt):
    cells = {}
    for (x, y, z), (_, st) in vlt.cells.items():
        cells[(x + VAULT_OFFSET[0], y + VAULT_OFFSET[1], z + VAULT_OFFSET[2])] = st
    for pos, (_, st) in ent.cells.items():
        cells[pos] = st
    return cells


def main():
    ent, vlt = entrance(), vault()
    pieces = [ent, vlt]
    problems, seen = check(pieces)
    for p in pieces:
        floors, dark, _ = dark_spots(p)
        print(f'{p.name}: {len(p.cells)} blocks, {floors} floor cells, {len(dark)} dark')
        if dark:
            problems.append(f'{p.name}: {len(dark)} dark floor cells, e.g. {sorted(dark)[:6]}')
    if problems:
        for pr in problems:
            print('PROBLEM:', pr)
        if '--force' not in sys.argv:
            sys.exit(1)
    os.makedirs(OUT, exist_ok=True)
    for p in pieces:
        path = os.path.join(OUT, f'{p.name}.nbt')
        with open(path, 'wb') as f:
            f.write(template_nbt(p))
        print('wrote', os.path.relpath(path, ROOT))
    if '--no-previews' not in sys.argv:
        previews = _load('previews', 'render-structure-preview.py')
        previews.archive_previews(ent, vlt, sys.modules[__name__])


if __name__ == '__main__':
    main()
