"""Throwaway: for every Archive placed in the test world, compare the world with the templates
cell by cell, read the block entities, measure the terrain fit, and render the site as it really
generated (perspective from 64 blocks along the approach, and an overview)."""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import verify_placed as vp  # noqa: E402  (loads the region files on import)

rsp, block, rot_pos, rot_state = vp.rsp, vp.block, vp.rot_pos, vp.rot_state
OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.dirname(os.path.abspath(__file__))
_, evox, etags = rsp.load_nbt(os.path.join(vp.TPL, 'entrance.nbt'))
_, vvox, vtags = rsp.load_nbt(os.path.join(vp.TPL, 'vault.nbt'))
road_t = next(p for p, t in etags.items() if t.get('entry', '').endswith('the_road'))
orr_t = next(p for p, s in vvox.items() if s.endswith(':archive_orrery'))
roads = [p for p, b in vp.bes.items() if b.get('entry', '').endswith('the_road')]
orreries = vp.world_find(':archive_orrery')


def fit(vox, anchor_t, anchor_w):
    best = None
    sample = list(vox.items())[::29]
    for rot in ('NONE', 'CLOCKWISE_90', 'CLOCKWISE_180', 'COUNTERCLOCKWISE_90'):
        dx, dz = rot_pos(anchor_t[0], anchor_t[2], rot)
        o = (anchor_w[0] - dx, anchor_w[1] - anchor_t[1], anchor_w[2] - dz)
        score = sum(1 for (x, y, z), st in sample
                    if block(o[0] + rot_pos(x, z, rot)[0], o[1] + y, o[2] + rot_pos(x, z, rot)[1]) == rot_state(st, rot))
        if best is None or score > best[0]:
            best = (score, o, rot)
    return best[1], best[2]


def compare(vox, o, rot):
    diff = 0
    for (x, y, z), st in vox.items():
        dx, dz = rot_pos(x, z, rot)
        if block(o[0] + dx, o[1] + y, o[2] + dz) != rot_state(st, rot):
            diff += 1
    return diff


def top(x, z):
    for y in range(110, -64, -1):
        b = block(x, y, z)
        if b and b != 'minecraft:air':
            return y, b
    return None, None


def world_box(x0, x1, z0, z1, depth=3):
    """The visible skin of the world in a box: each column's top few blocks, plus anything above
    the ground that the structure put there."""
    vox = {}
    tops = {}
    for x in range(x0 - 1, x1 + 2):
        for z in range(z0 - 1, z1 + 2):
            tops[(x, z)] = top(x, z)[0]
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            ty = tops[(x, z)]
            if ty is None:
                continue
            # down to the lowest neighbouring top, so the sides of anything standing up are drawn
            nb = [tops.get(n) for n in ((x + 1, z), (x - 1, z), (x, z + 1), (x, z - 1))]
            lo = min([ty - depth] + [h - 1 for h in nb if h is not None])
            for y in range(lo, ty + 1):
                b = block(x, y, z)
                if b and b != 'minecraft:air':
                    vox[(x, y, z)] = b
    return vox


for i, rw in enumerate(sorted(roads)):
    eo, erot = fit(evox, road_t, rw)
    near = min(orreries, key=lambda o: (o[0] - rw[0]) ** 2 + (o[2] - rw[2]) ** 2)
    vo, vrot = fit(vvox, orr_t, near)
    ediff, vdiff = compare(evox, eo, erot), compare(vvox, vo, vrot)
    plinths = sorted(b['entry'].split('/')[-1] for p, b in vp.bes.items()
                     if b['id'].endswith('archive_plinth') and abs(p[0] - rw[0]) < 120 and abs(p[2] - rw[2]) < 120)
    # terrain under the court's rim and over the vault
    paving = eo[1] + 21
    rim = []
    for (x, y, z), st in evox.items():
        if y == 21 and z >= 26:
            dx, dz = rot_pos(x, z, erot)
            wx, wz = eo[0] + dx, eo[2] + dz
            for ox, oz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                tx, tz = x + ox, z + oz
                if (tx, 21, tz) not in evox:
                    ddx, ddz = rot_pos(tx, tz, erot)
                    h, _ = top(eo[0] + ddx, eo[2] + ddz)
                    if h is not None:
                        rim.append(h - paving)
    cover = []
    cols = {}
    for (x, y, z), st in vvox.items():
        if st != 'minecraft:air':
            cols[(x, z)] = max(cols.get((x, z), -99), y)
    for (x, z), y in cols.items():
        dx, dz = rot_pos(x, z, vrot)
        h, _ = top(vo[0] + dx, vo[2] + dz)
        cover.append(h - (vo[1] + y))
    print(f'site {i}: entrance at {eo} {erot}, vault at {vo} {vrot}')
    print(f'   template cells that differ: entrance {ediff}/{len(evox)}, vault {vdiff}/{len(vvox)}')
    print(f'   plinths: {len(plinths)} {plinths}')
    print(f'   ground at the court rim relative to the paving: min {min(rim)} max {max(rim)}')
    print(f'   ground over the vault roof: min {min(cover)} avg {sum(cover) / len(cover):.1f}; exposed columns {sum(1 for c in cover if c <= 0)}')
    # renders: the approach is the template's +z, rotated into the world
    ax, az = rot_pos(0, 1, erot)
    px, pz = rot_pos(9, 33, erot)
    pyl = (eo[0] + px + 0.5, eo[2] + pz + 0.5)
    cam_x, cam_z = pyl[0] + ax * 64, pyl[1] + az * 64
    h, _ = top(int(cam_x), int(cam_z))
    box = world_box(int(min(pyl[0], cam_x)) - 40, int(max(pyl[0], cam_x)) + 40,
                    int(min(pyl[1], cam_z)) - 40, int(max(pyl[1], cam_z)) + 40)
    rsp.render_perspective(box, (cam_x, h + 1 + 1.62, cam_z), (pyl[0], paving + 9, pyl[1]),
                           os.path.join(OUT, f'site{i}-marker-64.png'),
                           title=f'site {i}: the marker from 64 blocks along the approach, on generated terrain')
    gx0, gz0 = eo[0] + min(rot_pos(0, 0, erot)[0], rot_pos(18, 47, erot)[0]), eo[2] + min(rot_pos(0, 0, erot)[1], rot_pos(18, 47, erot)[1])
    over = world_box(gx0 - 14, gx0 + 62, gz0 - 14, gz0 + 62, depth=4)
    rsp.render_iso_states(over, (-1, -1.1, -1.2), os.path.join(OUT, f'site{i}-overview.png'), scale=7,
                          title=f'site {i}: the surface as it generated')
