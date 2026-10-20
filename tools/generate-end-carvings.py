"""Planet 4's three Sealer carvings, the pictures that disagree with the End's tablets. No build or runtime dependency.

Not run by itself: tools/generate-nether-art.py owns the `sealer_carving` block's catalogue and imports these
(`END_CARVING_ORDER`, `END_CARVING_FUNCS`), appends them after the Nether's, and cuts every tile. Each picture is
drawn on the Archive's own Canvas, so it is visibly the same hand as the Moon's murals and the Nether's carving:
carved pale plaster, the figures inlaid in stone. The rule of the whole story holds here as it does there: every
word may lie and every picture is true (docs/lore/what-the-warden-keeps.md). The words on the End's tablets say the
destroyer broke this world, that the wings were a gift, that the Dragon is the destroyer's beast; these say what
the Sealers saw (docs/lore/the-end.md):

  the_cutting      a whole world, cut in pieces by the ancients themselves, a gate under each piece
                   (the tablet "The Belt" says the destroyer broke it)
  the_ships        a line of ships leaving across the dark with a cage in each, toward a world with teeth
                   (the tablet "The Wings" says the wings were a kindness)
  the_keeper_made  two Sealers setting ten lights round a gate, and the winged keeper they cut to sit on it
                   (the tablet "The Last Keeper" says the beast is the destroyer's)

No glyphs and no letters in any of it: the Observers can rewrite words, so the Sealers did not use them.
"""
import importlib.util
import math
import os

HERE = os.path.dirname(os.path.abspath(__file__))


def _load(name, filename):
    spec = importlib.util.spec_from_file_location(name, os.path.join(HERE, filename))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


archive = _load('archive_art', 'generate-archive-art.py')

# The End's own stones as inlay: pale end stone, purpur, obsidian, and the dark between.
STONE = [(196, 190, 140), (220, 214, 168), (240, 236, 200)]
PURPUR = [(106, 74, 120), (142, 104, 158), (176, 138, 190)]
OBSIDIAN = [(22, 16, 34), (38, 28, 56)]
LIGHT = (205, 170, 245)           # the relays' light: a violet that is not the network's teal
BLOOD = (120, 52, 70)             # the far world's dark teeth
SOUL = (150, 150, 160)            # figures: plain grey stone

END_CARVING_ORDER = [('the_cutting', (7, 2)), ('the_ships', (7, 2)), ('the_keeper_made', (6, 2))]

# A Sealer: robed, hooded, an arm out; facing right.
SEALER = [
    "..##....",
    ".####...",
    ".####...",
    "..##....",
    "..###...",
    ".#####.#",
    ".######.",
    ".#####..",
    ".#####..",
    ".#####..",
    ".#####..",
    ".#####..",
    "##...##.",
]

# A ship: a long hull with a prow and a folded wing standing from it, facing right.
SHIP = [
    "......#.......",
    "......##......",
    ".....####.....",
    "....######....",
    "...########...",
    "..##########..",
    ".############.",
    "##############",
    ".############.",
    "..##########..",
    "....######....",
]

# A cage: a box with its bars, each ship's cargo.
CAGE = [
    "######",
    "#.#.##",
    "######",
    "#.#.##",
    "######",
]


def sealer(c, x, y, ink, flip=False):
    c.sprite(SEALER, x, y, {'#': dict(height=1, inlay=ink, group=f's{x}')}, flip=flip)


def tri(c, a, b, d, ink, group):
    """A filled triangle, raised and inlaid."""
    xs = [a[0], b[0], d[0]]
    ys = [a[1], b[1], d[1]]

    def side(p, q, r):
        return (p[0] - r[0]) * (q[1] - r[1]) - (q[0] - r[0]) * (p[1] - r[1])

    for y in range(int(min(ys)), int(max(ys)) + 1):
        for x in range(int(min(xs)), int(max(xs)) + 1):
            p = (x + 0.5, y + 0.5)
            s1, s2, s3 = side(p, a, b), side(p, b, d), side(p, d, a)
            if not ((s1 < 0 or s2 < 0 or s3 < 0) and (s1 > 0 or s2 > 0 or s3 > 0)):
                c.px(x, y, height=1, inlay=ink, group=group)


def the_cutting(c):
    """Left, a whole world; then the same world as pieces drifting apart, each with a tool beside it; right, a row of
    gates, one under each piece. The ancients' own hands do the cutting."""
    w, h = c.w, c.h
    cy = 15
    # The whole world, banded, small.
    cx0 = 13
    for y in range(cy - 11, cy + 12):
        for x in range(cx0 - 11, cx0 + 12):
            d = math.hypot(x + 0.5 - cx0, y + 0.5 - cy)
            if d <= 10.5:
                band = int((y - (cy - 10)) // 4) % 2
                c.px(x, y, height=1, inlay=STONE[band], group='whole')
    c.ring(cx0, cy, 12.5, 1.0, height=-1)
    # An arrow-less step: three small cuts (incised lines) across it, the cuts the next picture makes real.
    # The pieces: the same disc, wedges pushed outward by their own number.
    cx1 = 52
    wedges = 5
    for y in range(cy - 14, cy + 15):
        for x in range(cx1 - 20, cx1 + 21):
            dx, dy = x + 0.5 - cx1, y + 0.5 - cy
            d = math.hypot(dx, dy)
            if d > 10.5:
                continue
            a = (math.degrees(math.atan2(dy, dx)) + 360.0) % 360.0
            k = int(a // (360.0 / wedges))
            mid = math.radians((k + 0.5) * 360.0 / wedges)
            push = 2.0 + 0.9 * k
            nx, ny = x + math.cos(mid) * push, y + math.sin(mid) * push
            c.px(nx, ny, height=1, inlay=STONE[(k % 2)], group=f'piece{k}')
            c.px(nx + 0.6, ny + 0.6, height=1, inlay=STONE[(k % 2)], group=f'piece{k}')
    # A gate under each of three far pieces, raised in purpur, with a lit eye: they were cut to hold the gates.
    gx = [78, 90, 102]
    for i, x in enumerate(gx):
        c.rect(x, 12, x + 1, 26, height=1, inlay=PURPUR[1], group=f'gate{i}')
        c.rect(x + 7, 12, x + 8, 26, height=1, inlay=PURPUR[1], group=f'gate{i}')
        c.rect(x, 10, x + 8, 11, height=1, inlay=PURPUR[2], group=f'gate{i}')
        c.disc(x + 4.5, 17, 1.7, height=2, inlay=LIGHT, glow=True, group=f'eye{i}')
        # the little piece above, carried from the cutting: one wedge
        c.disc(x + 4.5, 5, 3.0, height=1, inlay=STONE[i % 2], group=f'rock{i}')
    # The cutters: a Sealer-sized robed figure at the world, a second at the pieces, plain stone.
    sealer(c, 26, 17, SOUL)
    sealer(c, 68, 17, SOUL, flip=True)
    # No ground line: the pieces hang in the dark, but the gates stand on a cut sill.
    c.rect(75, 27, 111, 28, height=1)


def the_ships(c):
    """The islands behind; a line of ships going right, a cage in each, small and then smaller; ahead a dark world with a
    ring of teeth. Nobody is shown: the ships carry what was made to guard, and nothing else."""
    h = c.h
    # The belt behind, left: a scatter of small rocks.
    for x, y, r in ((5, 6, 3), (14, 22, 4), (9, 14, 2), (22, 8, 2), (3, 26, 2), (20, 27, 3)):
        c.disc(x, y, r, height=1, inlay=STONE[0], group=f'rock{x}')
    # Ships, three, shrinking as they go: the last is almost gone.
    for i, (x, y, scale) in enumerate(((22, 14, 1.0), (50, 15, 0.8), (72, 16, 0.6))):
        rows = SHIP if scale == 1.0 else [r[::2] for r in SHIP[::2]] if scale == 0.6 else SHIP
        c.sprite(rows, x, y - len(rows) // 2, {'#': dict(height=1, inlay=PURPUR[1], group=f'ship{i}')})
        cage = CAGE if scale > 0.7 else [r[::2] for r in CAGE[::2]]
        c.sprite(cage, x + (len(rows[0]) - len(cage[0])) // 2, y - len(rows) // 2 - len(cage) - 1,
                 {'#': dict(height=1, inlay=OBSIDIAN[1], group=f'cage{i}')})
        # the wake: three incised dashes
        for k in range(3):
            c.line(x - 6 - 4 * k, y + 2, x - 4 - 4 * k, y + 2, height=-1)
    # The far world: an obsidian disc with a ring of teeth, right.
    cx, cy = 98, 16
    c.disc(cx, cy, 11, height=1, inlay=OBSIDIAN[0], group='far')
    c.disc(cx, cy, 8.0, height=1, inlay=OBSIDIAN[1], group='far2')
    for k in range(14):
        a = math.tau * k / 14
        tx, ty = cx + math.cos(a) * 12.5, cy + math.sin(a) * 12.5
        c.line(tx, ty, cx + math.cos(a) * 15.5, cy + math.sin(a) * 15.5, height=1, inlay=BLOOD, group=f'tooth{k}')
    c.ring(cx, cy, 18.5, 1.0, height=-1)


def the_keeper_made(c):
    """Two Sealers at a gate set ten lights round it in a ring; on it, the winged keeper they cut, lines of light running
    from the ten to its heart. They made it, and they fed it."""
    w = c.w
    cx = 62
    # The gate: a low round dais, cut in purpur, with the keeper's ring of ten lights above it.
    c.rect(cx - 22, 27, cx + 22, 28, height=1, inlay=PURPUR[0], group='dais')
    for k in range(10):
        a = math.pi + math.pi * (k + 0.5) / 10          # a half ring above the dais
        lx, ly = cx + math.cos(a) * 22, 27 + math.sin(a) * 21
        c.disc(lx, ly, 1.6, height=2, inlay=LIGHT, glow=True, group=f'light{k}')
        c.line(lx, ly, cx, 17, height=-1, every=3)
    # The keeper: a body on the dais, a long neck rising left, a head, two wings spread.
    c.disc(cx, 22, 4.5, height=1, inlay=OBSIDIAN[1], group='body')
    c.rect(cx - 6, 17, cx - 4, 21, height=1, inlay=OBSIDIAN[1], group='neck')
    c.rect(cx - 9, 14, cx - 4, 16, height=1, inlay=OBSIDIAN[1], group='head')
    c.px(cx - 8, 15, height=2, inlay=LIGHT, group='eye')
    for sx in (1, -1):
        # a wing: a solid sweep of three feathered points from the shoulder, not a fan of lines
        tri(c, (cx + sx * 3, 19), (cx + sx * 26, 5), (cx + sx * 15, 19), OBSIDIAN[0], f'wing{sx}')
        tri(c, (cx + sx * 3, 19), (cx + sx * 24, 12), (cx + sx * 12, 22), OBSIDIAN[1], f'wing{sx}b')
    c.rect(cx + 4, 23, cx + 11, 24, height=1, inlay=OBSIDIAN[1], group='tail')
    # The Sealers: one each side, an arm toward the lights.
    sealer(c, 4, 15, SOUL)
    sealer(c, 88, 15, SOUL, flip=True)


END_CARVING_FUNCS = {'the_cutting': the_cutting, 'the_ships': the_ships, 'the_keeper_made': the_keeper_made}
