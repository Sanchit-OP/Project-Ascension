#!/usr/bin/env python3
"""Check structure templates where worldgen really placed them: every cell, and how they met the ground.

Why this exists: a template that compiles and previews well can still land badly on real terrain
(hanging over a cliff, buried in a slope, set on a lava sea) or come out different from its file
(ores, features or another structure overwriting it). This reads the jigsaw pieces a world
generated, puts each template cell where vanilla's StructureTemplate puts it (rotation about the
template's origin, no mirror), and compares it with the world block state for block state. Then it
measures the fit: the natural ground just outside the footprint against the floor row, and whether
anything under the footing is air or lava.

Usage:
    python template_fit.py <region-dir> <filter> --surface-y N [--ring R]

<filter> keeps structure ids containing that text. Templates are found from each piece's pool
element location (ns:path -> modules/*/src/main/resources/data/<ns>/structure/<path>.nbt). Run
after `save-all flush`. Read-only: nothing is written to the world.

Used for Planet 3's colony ruins and the Sealers' carving (docs/technical/nether-art-design.md).
"""
import argparse
import glob
import importlib.util
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, '..', '..'))
sys.path.insert(0, HERE)
from structure_starts import starts_in  # noqa: E402


def _load(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


views = _load('render_nether_views', os.path.join(ROOT, 'tools', 'render-nether-views.py'))
rsp = views.rsp

ROT = {'NONE': 0, 'CLOCKWISE_90': 1, 'CLOCKWISE_180': 2, 'COUNTERCLOCKWISE_90': 3}
# What does not count as ground when measuring the land round a piece.
NOT_GROUND = ('air', 'lava', 'fire', 'roots', 'sprouts', 'vines', 'fungus', 'stem', 'hyphae', 'wart_block',
              'shroomlight', 'mushroom')


def template_path(location):
    ns, path = location.split(':')
    for p in glob.glob(os.path.join(ROOT, 'modules', '*', 'src', 'main', 'resources', 'data', ns, 'structure', path + '.nbt')):
        return p
    return None


def place(x, z, r):
    """StructureTemplate.transform for Mirror.NONE about pivot (0, 0, 0)."""
    return [(x, z), (-z, x), (-x, -z), (z, -x)][r]


def rotate_state(st, r):
    if r == 0 or '[' not in st:
        return st
    name, props = rsp.split_state(st)
    order = ['north', 'east', 'south', 'west']
    if props.get('facing') in order:
        props['facing'] = order[(order.index(props['facing']) + r) % 4]
    if r % 2 and props.get('axis') in ('x', 'z'):
        props['axis'] = {'x': 'z', 'z': 'x'}[props['axis']]
    return name + '[' + ','.join(f'{k}={v}' for k, v in sorted(props.items())) + ']'


def is_ground(state):
    short = state.split(':', 1)[1].split('[')[0]
    return not any(k in short for k in NOT_GROUND)


def check(world, piece, surface_y, ring):
    loc = piece['pool_element']['location']
    path = template_path(loc)
    if not path:
        return f'    no template for {loc}'
    _, vox, _ = rsp.load_nbt(path)
    r = ROT[piece['rotation']]
    px, py, pz = piece['PosX'], piece['PosY'], piece['PosZ']
    cells = {}
    for (x, y, z), st in vox.items():
        if st == 'minecraft:structure_void':
            continue
        dx, dz = place(x, z, r)
        cells[(px + dx, py + y, pz + dz)] = rotate_state(st, r)
    xs = [c[0] for c in cells]
    zs = [c[2] for c in cells]
    x0, x1, z0, z1 = min(xs) - ring, max(xs) + ring, min(zs) - ring, max(zs) + ring
    for cx in range(x0 >> 4, (x1 >> 4) + 1):
        for cz in range(z0 >> 4, (z1 >> 4) + 1):
            if not world.chunk(cx, cz):
                return None                                     # not finished generating: nothing to check
    y0, y1 = max(0, py - 12), min(255, py + max(c[1] for c in vox) + 2)
    box = world.box(x0, x1, y0, y1, z0, z1)

    def at(x, y, z):
        return world.palette[box[x - x0, y - y0, z - z0]]
    match, diff = 0, {}
    for (x, y, z), st in cells.items():
        got = at(x, y, z)
        # a jigsaw becomes its final state; fluids and plants may settle; compare by block and state
        if got == st or rsp.split_state(got)[0] == rsp.split_state(st)[0] and 'waterlogged' in st:
            match += 1
        else:
            key = (st.split('[')[0], got.split('[')[0])
            diff[key] = diff.get(key, 0) + 1
    floor = py + surface_y
    footprint = {(x, z) for (x, y, z), st in cells.items() if y == floor and st != 'minecraft:air'}
    footing_bottom = min(c[1] for c in cells)
    hanging = [(x, z) for (x, z) in footprint if at(x, footing_bottom - 1, z).split('[')[0] in
               ('minecraft:air', 'minecraft:cave_air', 'minecraft:lava')]
    # the ground round the footprint, R blocks out
    edge = set()
    for (x, z) in footprint:
        for ddx in range(-ring, ring + 1):
            for ddz in range(-ring, ring + 1):
                q = (x + ddx, z + ddz)
                if q not in footprint:
                    edge.add(q)
    deltas = []
    lava = 0
    for (x, z) in edge:
        top = None
        for y in range(y1, y0 - 1, -1):
            st = at(x, y, z)
            if st.startswith('minecraft:lava') and top is None:
                lava += 1
                break
            if is_ground(st) and (x, y, z) not in cells:
                top = y
                break
        if top is not None:
            deltas.append(top - floor)
    biome = world.biome((x0 + x1) // 2, floor, (z0 + z1) // 2) or '?'
    out = [f'    {loc} at ({px}, {py}, {pz}) {piece["rotation"]}, {biome.split(":")[-1]}: '
           f'{match}/{len(cells)} cells match, floor y {floor}']
    if diff:
        worst = sorted(diff.items(), key=lambda kv: -kv[1])[:6]
        out.append('      differ: ' + ', '.join(f'{a} -> {b} x{n}' for (a, b), n in worst))
    if deltas:
        deltas.sort()
        mean = sum(deltas) / len(deltas)
        high = sum(1 for d in deltas if d > 3) / len(deltas)
        low = sum(1 for d in deltas if d < -3) / len(deltas)
        out.append(f'      ground {ring} out: {deltas[0]:+d} to {deltas[-1]:+d} (mean {mean:+.1f}); '
                   f'{high:.0%} more than 3 above the floor, {low:.0%} more than 3 below; {lava} lava columns')
    out.append(f'      footing columns over air or lava: {len(hanging)} of {len(footprint)}')
    return '\n'.join(out)


def main():
    ap = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    ap.add_argument('region')
    ap.add_argument('filter')
    ap.add_argument('--surface-y', type=int, required=True)
    ap.add_argument('--ring', type=int, default=3)
    a = ap.parse_args()
    world = views.RegionWorld(a.region)
    n = skipped = 0
    for path in sorted(glob.glob(os.path.join(a.region, '*.mca'))):
        for sid, start in starts_in(path, a.filter):
            for piece in start.get('Children', []):
                if 'pool_element' not in piece:
                    continue
                report = check(world, piece, a.surface_y, a.ring)
                if report is None:
                    skipped += 1
                    continue
                n += 1
                print(f"{sid}: chunk ({start.get('ChunkX')},{start.get('ChunkZ')})")
                print(report)
    print(f'{n} pieces checked; {skipped} skipped, their chunks not finished generating')


if __name__ == '__main__':
    main()
