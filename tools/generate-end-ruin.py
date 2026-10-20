"""Planet 4's ruins: five distinct places in the End, each holding some of its eight tablets, with the Sealers' carvings
that disagree with them (docs/lore/the-end.md; the text is tools/archive-lore.py). No build or runtime dependency.

Run from the repository root:  python tools/generate-end-ruin.py [--no-previews]
Writes, and hand edits to any of them are lost on the next run (edit this file):

  data/ascension_worlds/structure/end_ruin/ring.nbt       the Ring of Ten, on the central island, just outside the pillars
  data/ascension_worlds/structure/end_ruin/cell.nbt       the Quiet Cell, a sealed room on the central island
  data/ascension_worlds/structure/end_ruin/mooring.nbt    the Mooring, a quay and its empty cradles, on outer islands
  data/ascension_worlds/structure/end_ruin/cloister.nbt   the Cloister, a colonnade round a chorus garden, on outer islands
  data/ascension_worlds/structure/end_ruin/lamp.nbt       the Last Lamp, a tower with a light on top, on outer islands

and previews under design/end/. A structure is placed with a random rotation, so none of these has a "front": each is
laid out to read from any side (a ring, a quay seen along its length, a square, a tower).

What each one says without words (the tablets are the voice and lie; these are the Sealers and do not):

  the Ring    ten stumps with a light on each, the number of the Dragon's crystals, set in a circle round a dais: the keeper
              was placed on a gate and fed by ten lights. Two stelae carved in the Sealers' plaster, "the cutting" and "the
              keeper made", stand behind the first two tablets, whose words say the opposite.
  the Cell    a closed room off the road, the seven glyphs of the name set in its walls in the true order (4 1 6 3 0 5 2),
              the one tablet that is not the voice at its centre.
  the Mooring empty cradles for ships, a gantry over each, a row of open cages: the ships were loaded and sent out, and the
              carving "the ships" shows where to.
  the Cloister chorus planted in rows by hand in a walled court: someone tended what the tablet calls a wild fruit.
  the Lamp    a tower, tall enough to be seen from the next island, with a light that still burns: it marked the edge.

Deterministic like every other piece of art here: the only randomness is a seeded generator, and the NBT is written by the
Lunar Archive generator's writer (sorted palette, fixed block order, gzip with no name and mtime 0), so a re-run reproduces
the committed files byte for byte. Vanilla End blocks, our archive plinth, the Sealers' carving block (its catalogue is
tools/generate-nether-art.py's) and the mark glyph block; nothing here is a texture. Every template's floor is row F = 1
(one row of footing under it, as the Nether's ruins are).
"""
import importlib.util
import math
import os
import random
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, '..'))
OUT = os.path.join(ROOT, 'modules/worlds/src/main/resources/data/ascension_worlds/structure/end_ruin')
DESIGN = os.path.join(ROOT, 'design/end')
NS = 'ascension_worlds'


def _load(name, filename):
    spec = importlib.util.spec_from_file_location(name, os.path.join(HERE, filename))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


lunar = _load('lunar_archive', 'generate-lunar-archive.py')   # Piece and the NBT writer
nart = _load('nether_art', 'generate-nether-art.py')           # the carving catalogue
Piece, B, template_nbt = lunar.Piece, lunar.B, lunar.template_nbt
CORE, WALL, OPEN, DETAIL = lunar.CORE, lunar.WALL, lunar.OPEN, lunar.DETAIL
CARVINGS = nart.CARVINGS

AIR = 'minecraft:air'
F = 1                      # the floor row
MAX_PIECE = 48
ENTRIES = ['the_belt', 'the_keeper', 'the_wings', 'the_towers', 'the_fruit', 'the_watchers', 'the_edge', 'the_record']
TRUE_ORDER = [4, 1, 6, 3, 0, 5, 2]       # the seven glyphs in Tacita's order (tools/archive-lore.py)


def mc(name, **props):
    return B('minecraft:' + name, **props)


RAW = mc('end_stone')
BRICK = mc('end_stone_bricks')
PURPUR = mc('purpur_block')
PILLAR = mc('purpur_pillar', axis='y')
OBSIDIAN = mc('obsidian')
CRYING = mc('crying_obsidian')
ROD = mc('end_rod', facing='up')


def slab(kind='end_stone_brick', half='bottom'):
    return mc(f'{kind}_slab', type=half, waterlogged='false')


def bars():
    return mc('iron_bars', north='false', south='false', east='false', west='false', waterlogged='false')


class End(Piece):
    """A ruin under construction: the Archive's voxel piece plus the End's building moves."""

    def __init__(self, name, size, seed):
        super().__init__(name, size)
        self.rng = random.Random(seed)
        self.plinths = []         # (entry, (x, y, z), facing)
        self.carvings = []        # picture names
        self.glyphs = []
        self.reserved = set()     # where a reader stands: no rubble there

    # ground ---------------------------------------------------------------------------------------------
    def ground(self, cells, clear=10, floor=None):
        for (x, z) in cells:
            for y in range(0, F):
                self.put(x, y, z, RAW, CORE)
            self.put(x, F, z, floor(x, z) if floor else BRICK, WALL)
            for y in range(F + 1, min(self.size[1], F + 1 + clear)):
                self.put(x, y, z, AIR, OPEN)

    def disc_cells(self, cx, cz, r, wear=0.8):
        """The cells of a disc whose edge is worn away at random."""
        out = []
        for x in range(int(cx - r) - 1, int(cx + r) + 2):
            for z in range(int(cz - r) - 1, int(cz + r) + 2):
                d = math.hypot(x - cx, z - cz) / r
                if d > 1.0 or (d > wear and self.rng.random() < (d - wear) * 4):
                    continue
                if 0 <= x < self.size[0] and 0 <= z < self.size[2]:
                    out.append((x, z))
        return out

    def rect_cells(self, x0, z0, x1, z1):
        return [(x, z) for x in range(x0, x1 + 1) for z in range(z0, z1 + 1)]

    # masonry --------------------------------------------------------------------------------------------
    def masonry(self):
        return PURPUR if self.rng.random() < 0.12 else BRICK

    def pillar(self, x, z, h, rod=False):
        """A purpur pillar on an obsidian foot, `h` high; a light on top if it still has one."""
        self.put(x, F + 1, z, OBSIDIAN)
        for y in range(F + 2, F + 2 + h):
            self.put(x, y, z, PILLAR)
        if rod:
            self.put(x, F + 2 + h, z, ROD)

    def stump(self, x, z, h, rod):
        """A broken stub of obsidian, with a light on it if it kept one."""
        for y in range(F + 1, F + 1 + h):
            self.put(x, y, z, OBSIDIAN)
        if rod:
            self.put(x, F + 1 + h, z, ROD)

    def rubble(self, cx, cz, n, spread=1.6):
        for _ in range(n):
            x = int(round(cx + self.rng.gauss(0, spread)))
            z = int(round(cz + self.rng.gauss(0, spread)))
            y = F + 1
            if not self.inside(x, y, z) or self.get(x, y, z) not in (AIR, None) or (x, z) in self.reserved:
                continue
            if self.get(x, y - 1, z) in (AIR, None):
                continue
            self.put(x, y, z, self.rng.choice([BRICK, RAW, slab(), slab(), slab('purpur')]))

    # lore -----------------------------------------------------------------------------------------------
    def plinth(self, x, z, facing, entry, y=F + 1):
        self.put(x, y, z, B('archive_plinth', facing=facing), DETAIL,
                 nbt={'id': f'{NS}:archive_plinth', 'entry': f'{NS}:the_end/{entry}'})
        self.plinths.append((entry, (x, y, z), facing))
        fx, fz = {'north': (0, -1), 'south': (0, 1), 'east': (1, 0), 'west': (-1, 0)}[facing]
        self.reserved.add((x + fx, z + fz))

    def picture(self, name, left, y_bottom, wall_axis, wall_pos, facing):
        """Lay a picture's tiles into a wall so it reads left to right from the `facing` side."""
        m = CARVINGS[name]
        w, h = m['size']
        step = {'south': 1, 'north': -1, 'west': 1, 'east': -1}[facing]
        for col in range(w):
            for row in range(h):
                state = B('sealer_carving', tile=m['first'] + row * w + col)
                a = left + step * col
                y = y_bottom + (h - 1 - row)
                if wall_axis == 'z':
                    self.put(a, y, wall_pos, state, DETAIL)
                else:
                    self.put(wall_pos, y, a, state, DETAIL)
        self.carvings.append(name)

    def stele(self, name, axis, pos, centre, facing, depth=2, height=5):
        """A free-standing slab carved on one face with picture `name`: purpur end pillars, a sill under the picture, a
        capping course over it. `pos` is the carved face's coordinate across `axis`; the slab is `depth` thick behind it."""
        w, h = CARVINGS[name]['size']
        lo = centre - w // 2
        hi = lo + w - 1
        back = {'south': -1, 'north': 1, 'west': -1, 'east': 1}[facing]
        for a in range(lo - 1, hi + 2):
            for layer in range(depth):
                p = pos + back * layer
                for y in range(F + 1, F + 1 + height):
                    end = a in (lo - 1, hi + 1)
                    if layer == 0 and not end and F + 2 <= y <= F + 1 + h:
                        continue            # the picture goes here
                    state = PILLAR if end else self.masonry()
                    if axis == 'z':
                        self.put(a, y, p, state)
                    else:
                        self.put(p, y, a, state)
        # the picture reads from `facing`: its left edge is `lo` for south/west, `hi` for north/east
        left = lo if facing in ('south', 'west') else hi
        self.picture(name, left, F + 2, axis, pos, facing)


# --- the Ring of Ten ------------------------------------------------------------------------------------
#
# On the central island, outside the pillars where the fight is not. Ten obsidian stumps round a dais (the ten lights that
# fed the keeper), two carved stelae north and south behind two plinths. Round, so it reads from any side.

def ring():
    p = End('ring', (27, 12, 27), 4041)
    cx = cz = 13
    p.ground(p.disc_cells(cx, cz, 12.5, wear=0.82), floor=lambda x, z: PURPUR if math.hypot(x - cx, z - cz) > 10.5 else BRICK)
    for (x, z) in p.rect_cells(cx - 3, cz - 3, cx + 3, cz + 3):
        if math.hypot(x - cx, z - cz) <= 3.6:
            p.put(x, F + 1, z, slab('purpur'))
    p.put(cx, F + 1, cz, CRYING)
    for k in range(10):
        a = math.tau * (k + 0.5) / 10
        x, z = int(round(cx + math.cos(a) * 9.5)), int(round(cz + math.sin(a) * 9.5))
        p.stump(x, z, p.rng.choice([1, 2, 2, 3, 4]), rod=(k not in (2, 7)))
    # the stelae: north (z small) and south (z large), their picture faces turned to the dais
    p.stele('the_cutting', 'z', 4, cx, 'south')
    p.plinth(cx, 6, 'south', 'the_belt')
    p.stele('the_keeper_made', 'z', 22, cx, 'north')
    p.plinth(cx, 20, 'north', 'the_keeper')
    p.rubble(cx, cz, 14, spread=6.0)
    for x, z in ((cx - 7, cz), (cx + 7, cz)):
        p.rubble(x, z, 4, spread=1.2)
    return p


# --- the Quiet Cell ------------------------------------------------------------------------------------
#
# A sealed room off the road, plain from outside. Inside, the seven glyphs of the name set in the walls in the order the
# tablets get wrong, and the one tablet that is not the voice.

def cell():
    p = End('cell', (15, 10, 15), 4043)
    cx = cz = 7
    p.ground(p.rect_cells(0, 0, 14, 14))
    x0, x1, z0, z1 = 2, 12, 2, 12
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if x in (x0, x1) or z in (z0, z1):
                for y in range(F + 1, F + 5):
                    p.put(x, y, z, OBSIDIAN if y in (F + 1, F + 4) else BRICK, WALL)
            else:
                for y in range(F + 1, F + 5):
                    p.put(x, y, z, AIR, OPEN)
                p.put(x, F + 5, z, slab(), WALL)
    for x in range(x0, x1 + 1):                        # the roof over the walls too
        for z in (z0, z1):
            p.put(x, F + 5, z, slab(), WALL)
    for z in range(z0, z1 + 1):
        for x in (x0, x1):
            p.put(x, F + 5, z, slab(), WALL)
    # the door: a gap in the south wall, three wide, with two lit pillars outside it
    for x in (6, 7, 8):
        for y in range(F + 1, F + 4):
            p.put(x, y, z1, AIR, OPEN)
        p.put(x, F + 4, z1, PURPUR, WALL)
    p.pillar(5, z1 + 1, 3, rod=True)
    p.pillar(9, z1 + 1, 3, rod=True)
    # seven glyphs set in the inner course of the walls at reading height, in the true order, round from the door's left
    inner = [(x0, 10), (x0, 7), (x0, 4), (7, z0), (x1, 4), (x1, 7), (x1, 10)]
    for (x, z), glyph in zip(inner, TRUE_ORDER):
        p.put(x, F + 2, z, B('mark_glyph', glyph=glyph, lit='false'), DETAIL)
        p.glyphs.append(glyph)
    for (x, z) in p.rect_cells(cx - 1, cz - 1, cx + 1, cz + 1):
        p.put(x, F + 1, z, OBSIDIAN)
    p.plinth(cx, cz, 'south', 'the_record', y=F + 2)
    p.put(cx - 3, F + 1, cz + 3, ROD)
    p.put(cx + 3, F + 1, cz + 3, ROD)
    return p


# --- the Mooring ---------------------------------------------------------------------------------------
#
# A quay: three cradles where ships lay, each under a gantry, and a row of open cages beside them. The ships are gone and the
# cages are empty. A carving at the quay's end shows where they went.

def mooring():
    p = End('mooring', (33, 15, 17), 4044)
    p.ground(p.rect_cells(0, 2, 32, 14), floor=lambda x, z: BRICK if (x + z) % 7 else PURPUR)
    for x0 in (1, 11, 21):
        for x in range(x0, x0 + 8):
            for z in (4, 9):
                p.put(x, F + 1, z, PILLAR if x in (x0, x0 + 7) else slab('purpur'))
        for z in range(4, 10):
            for x in (x0, x0 + 7):
                p.put(x, F + 1, z, PILLAR if z in (4, 9) else slab('purpur'))
        for x in range(x0 + 1, x0 + 7):
            for z in range(5, 9):
                p.put(x, F, z, OBSIDIAN, WALL)                 # where the hull lay
        # a gantry over it: two pillars and a beam, partly fallen
        standing = [p.rng.random() < 0.6, p.rng.random() < 0.6]
        for gx, up in zip((x0 + 1, x0 + 6), standing):
            for y in range(F + 2, F + 2 + (7 if up else 3)):
                p.put(gx, y, 3, PILLAR)
        if all(standing):
            for x in range(x0 + 1, x0 + 7):
                if x not in (x0 + 3, x0 + 4):
                    p.put(x, F + 9, 3, slab('purpur', 'top'))
    # cages: 2 x 2 x 2 of bars on a purpur footing, one side open
    for x0 in (3, 7, 11, 15, 19, 23, 27):
        for x in (x0, x0 + 1):
            for z in (12, 13):
                p.put(x, F + 1, z, PURPUR)
        for y in (F + 2, F + 3):
            for x, z in ((x0, 12), (x0 + 1, 12), (x0, 13), (x0 + 1, 13)):
                if z == 12 and x == x0 + 1:
                    p.put(x, y, z, AIR)             # the door, hanging open
                else:
                    p.put(x, y, z, bars())
        p.put(x0, F + 4, 12, PURPUR)
        p.put(x0, F + 4, 13, PURPUR)
        p.put(x0 + 1, F + 4, 13, PURPUR)
    # the carving at the east end, read from the west; the tablet before it
    p.stele('the_ships', 'x', 32, 8, 'west')
    p.plinth(30, 8, 'west', 'the_wings')
    p.plinth(1, 11, 'south', 'the_towers')
    p.rubble(16, 11, 12, spread=10.0)
    return p


# --- the Cloister --------------------------------------------------------------------------------------
#
# A square colonnade round a walled garden where chorus is planted in rows. The tablet calls it a wild fruit; the rows are not wild.

def cloister():
    p = End('cloister', (23, 14, 23), 4045)
    p.ground(p.rect_cells(0, 0, 22, 22), floor=lambda x, z: RAW if 6 <= x <= 16 and 6 <= z <= 16 else BRICK)
    ring_cells = [(x, z) for x in range(0, 23, 4) for z in (0, 22)] + [(x, z) for x in (0, 22) for z in range(4, 19, 4)]
    for x, z in ring_cells:
        p.pillar(x, z, p.rng.choice([3, 4, 5, 5]), rod=p.rng.random() < 0.4)
    # the garden wall: low, with the rows inside
    for x in range(5, 18):
        for z in (5, 17):
            p.put(x, F + 1, z, slab())
    for z in range(5, 18):
        for x in (5, 17):
            p.put(x, F + 1, z, slab())
    for z in (9, 13):
        for x in (7, 9, 11, 13, 15):
            h = p.rng.randint(2, 4)
            for y in range(F + 1, F + 1 + h):
                p.put(x, y, z, mc('chorus_plant', down='true' if y == F + 1 else 'false', up='true',
                                  north='false', south='false', east='false', west='false'))
            p.put(x, F + 1 + h, z, mc('chorus_flower', age='0'))
    p.put(11, F, 11, PURPUR)
    p.plinth(11, 11, 'south', 'the_fruit')
    p.plinth(3, 19, 'north', 'the_watchers')
    p.rubble(11, 11, 10, spread=9.0)
    return p


# --- the Last Lamp -------------------------------------------------------------------------------------
#
# A tower on an outer island, tall enough to be seen from the next, with a light at the top that still burns, and at its foot
# the last tablet. It marked the edge of the belt.

def lamp():
    p = End('lamp', (11, 24, 11), 4046)
    p.ground(p.disc_cells(5, 5, 5.4, wear=0.7))
    for y in range(F + 1, F + 10):
        for x in range(4, 7):
            for z in range(4, 7):
                if x == 5 and z == 5:
                    p.put(x, y, z, OBSIDIAN)
                else:
                    p.put(x, y, z, PILLAR if (x in (4, 6) and z in (4, 6)) else (OBSIDIAN if y % 3 == 0 else PURPUR))
    for y in range(F + 10, F + 19):
        p.put(5, y, 5, PILLAR)
    p.put(5, F + 19, 5, ROD)
    p.plinth(5, 8, 'south', 'the_edge')
    p.rubble(5, 5, 14, spread=3.2)
    for x, z in ((2, 8), (8, 8)):
        p.stump(x, z, 2, rod=True)
    return p


# --- checks, previews, writing ------------------------------------------------------------------------

def check(pieces):
    problems = []
    seen = {}
    carvings = []
    for p in pieces:
        if any(n > MAX_PIECE for n in p.size):
            problems.append(f'{p.name}: size {p.size} exceeds {MAX_PIECE}')
        for entry, (x, y, z), facing in p.plinths:
            seen.setdefault(entry, []).append(p.name)
            if not lunar.is_solid(p.get(x, y - 1, z)):
                problems.append(f'{p.name}: plinth {entry} at {(x, y, z)} does not stand on a full block')
            fx, fz = {'north': (0, -1), 'south': (0, 1), 'east': (1, 0), 'west': (-1, 0)}[facing]
            rx, rz = x + fx, z + fz
            if (p.get(rx, y, rz) != AIR or p.get(rx, y + 1, rz) != AIR
                    or not lunar.is_solid(p.get(rx, y - 1, rz))):
                problems.append(f'{p.name}: plinth {entry} at {(x, y, z)} faces nowhere a reader can stand '
                                f'({p.get(rx, y, rz)}, {p.get(rx, y + 1, rz)}, below {p.get(rx, y - 1, rz)})')
        carvings += p.carvings
    for e in ENTRIES:
        if e not in seen:
            problems.append(f'entry {e} is in no ruin')
    for e in seen:
        if e not in ENTRIES:
            problems.append(f'unknown entry {e}')
        if len(seen[e]) > 1:
            problems.append(f'entry {e} is in {seen[e]}')
    for name in ('the_cutting', 'the_ships', 'the_keeper_made'):
        if name not in carvings:
            problems.append(f'carving {name} is in no ruin')
    cell_piece = next(p for p in pieces if p.name == 'cell')
    if cell_piece.glyphs != TRUE_ORDER:
        problems.append(f'the cell shows glyphs {cell_piece.glyphs}, not {TRUE_ORDER}')
    return problems, seen


def previews(pieces):
    """A plan of each piece's floor and first courses, the plinths numbered."""
    rsp = _load('render_structure_preview', 'render-structure-preview.py')
    os.makedirs(DESIGN, exist_ok=True)
    for p in pieces:
        _, vox, tags = rsp.load_nbt(os.path.join(OUT, p.name + '.nbt'))
        sx, sy, sz = p.size
        marks = {e: i + 1 for i, (e, _, _) in enumerate(p.plinths)}
        rsp.render_plan(vox, tags, (0, sx - 1), (0, sz - 1), F, F + 3, os.path.join(DESIGN, f'ruin-{p.name}-plan.png'),
                        scale=16, title=f'{p.name}, floor row {F}: plinths {", ".join(e for e, _, _ in p.plinths)}',
                        marks=marks)


def main():
    pieces = [ring(), cell(), mooring(), cloister(), lamp()]
    problems, seen = check(pieces)
    for p in pieces:
        print(f'{p.name}: {p.size}, {len(p.cells)} cells, plinths {[e for e, _, _ in p.plinths]}, carvings {p.carvings}')
    if problems:
        for q in problems:
            print('PROBLEM:', q)
        if '--force' not in sys.argv:
            sys.exit(1)
    os.makedirs(OUT, exist_ok=True)
    for p in pieces:
        path = os.path.join(OUT, p.name + '.nbt')
        with open(path, 'wb') as f:
            f.write(template_nbt(p))
        print('wrote', os.path.relpath(path, ROOT))
    for stale in ('ruin.nbt', 'outpost.nbt'):
        path = os.path.join(OUT, stale)
        if os.path.exists(path):
            os.remove(path)
            print('removed', os.path.relpath(path, ROOT))
    if '--no-previews' not in sys.argv:
        previews(pieces)


if __name__ == '__main__':
    main()
