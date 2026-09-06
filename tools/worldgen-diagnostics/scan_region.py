#!/usr/bin/env python3
"""Hand-rolled Anvil (.mca) region + NBT reader for verifying generated terrain without a client.

Why this exists: after a worldgen change, force-generating chunks (see rcon_client.py) still
leaves the question "did it actually come out right?" unanswered until someone flies over it.
This reads the real generated chunk data directly -- surface height per column, and how many
non-solid ("hole") blocks sit below that surface -- so terrain-shape bugs (floating islands,
overlapping carves, disconnected ground) show up as numbers, not as something eyeballed later.

No NBT/Anvil library is a project dependency, so this is a from-scratch reader. It has already
caught two of its own bugs worth knowing about if you extend it:

  1. TAG_Byte (section "Y") must be read as SIGNED. Reading it unsigned turned section Y=-4 into
     252, silently breaking every negative-Y section's lookup and reporting ~64 phantom holes in
     every single column (the whole bottom of the world) even on flawless terrain.
  2. Palette indices are bit-packed into TAG_Long_Array values that Python's struct unpacks as
     SIGNED 64-bit ints. Right-shifting a negative Python int is arithmetic (sign-extending), not
     the logical shift needed to unpack a bitfield -- mask with 0xFFFFFFFFFFFFFFFF to get the
     unsigned bit pattern *before* shifting, or high palette indices near the top bit come out
     wrong.

Both are why a "scanner shows a problem" result must itself be sanity-checked (e.g. by dumping
one flagged column) before trusting it over the actual terrain -- see the false leads in
docs/technical/moon-terrain-tuning.md.

Usage:
    python scan_region.py <region-dir> [--stride N]

<region-dir> is a dimension's region folder, e.g.
run/server/ascension-dev/dimensions/ascension_worlds/moon/region. --stride controls column
sampling density within each chunk (default 4, i.e. every 4th block on each axis).
"""
import glob
import struct
import sys
import zlib


def read_payload(data, offset, tag_type):
    if tag_type == 1:
        return struct.unpack('>b', data[offset:offset + 1])[0], offset + 1
    if tag_type == 2:
        return struct.unpack('>h', data[offset:offset + 2])[0], offset + 2
    if tag_type == 3:
        return struct.unpack('>i', data[offset:offset + 4])[0], offset + 4
    if tag_type == 4:
        return struct.unpack('>q', data[offset:offset + 8])[0], offset + 8
    if tag_type == 5:
        return struct.unpack('>f', data[offset:offset + 4])[0], offset + 4
    if tag_type == 6:
        return struct.unpack('>d', data[offset:offset + 8])[0], offset + 8
    if tag_type == 7:
        length = struct.unpack('>i', data[offset:offset + 4])[0]
        offset += 4
        return data[offset:offset + length], offset + length
    if tag_type == 8:
        length = struct.unpack('>H', data[offset:offset + 2])[0]
        offset += 2
        return data[offset:offset + length].decode('utf8'), offset + length
    if tag_type == 9:
        elem_type = data[offset]
        offset += 1
        length = struct.unpack('>i', data[offset:offset + 4])[0]
        offset += 4
        result = []
        for _ in range(length):
            v, offset = read_payload(data, offset, elem_type)
            result.append(v)
        return result, offset
    if tag_type == 10:
        result = {}
        while True:
            sub_type = data[offset]
            offset += 1
            if sub_type == 0:
                break
            name_len = struct.unpack('>H', data[offset:offset + 2])[0]
            offset += 2
            name = data[offset:offset + name_len].decode('utf8')
            offset += name_len
            val, offset = read_payload(data, offset, sub_type)
            result[name] = val
        return result, offset
    if tag_type == 11:
        length = struct.unpack('>i', data[offset:offset + 4])[0]
        offset += 4
        val = struct.unpack(f'>{length}i', data[offset:offset + 4 * length])
        return val, offset + 4 * length
    if tag_type == 12:
        length = struct.unpack('>i', data[offset:offset + 4])[0]
        offset += 4
        val = struct.unpack(f'>{length}q', data[offset:offset + 8 * length])
        return val, offset + 8 * length
    raise ValueError(f"unknown tag type {tag_type}")


def parse_nbt(data):
    offset = 1  # skip the root TAG_Compound's own type byte
    name_len = struct.unpack('>H', data[offset:offset + 2])[0]
    offset += 2 + name_len
    val, _ = read_payload(data, offset, 10)
    return val


def get_block_at(section, x, y, z):
    block_states = section.get('block_states', {})
    palette = block_states.get('palette', [])
    if not palette:
        return 'minecraft:air'
    if len(palette) == 1:
        return palette[0]['Name']
    data_arr = block_states.get('data')
    if data_arr is None:
        return palette[0]['Name']
    bits = max(4, (len(palette) - 1).bit_length())
    idx_in_section = (y & 15) * 256 + (z & 15) * 16 + (x & 15)
    per_long = 64 // bits
    long_idx = idx_in_section // per_long
    bit_offset = (idx_in_section % per_long) * bits
    unsigned_long = data_arr[long_idx] & 0xFFFFFFFFFFFFFFFF
    val = (unsigned_long >> bit_offset) & ((1 << bits) - 1)
    return palette[val]['Name']


def is_solid(name):
    return name not in ('minecraft:air', 'minecraft:cave_air', 'minecraft:void_air')


def process_region(path, stride, results):
    with open(path, 'rb') as f:
        header = f.read(4096)
        if len(header) < 4096:
            return  # truncated/empty region file (e.g. still being written by a live server)
        for i in range(1024):
            sector_count = header[i * 4 + 3]
            if sector_count == 0:
                continue
            sector_offset = int.from_bytes(header[i * 4:i * 4 + 3], 'big')
            f.seek(sector_offset * 4096)
            length = struct.unpack('>I', f.read(4))[0]
            compression_type = f.read(1)[0]
            comp_data = f.read(length - 1)
            if compression_type != 2:
                continue  # only zlib payloads are supported
            chunk = parse_nbt(zlib.decompress(comp_data))
            cx, cz = chunk.get('xPos'), chunk.get('zPos')
            sec_by_y = {s['Y']: s for s in chunk.get('sections', []) if 'block_states' in s}

            for local_x in range(0, 16, stride):
                for local_z in range(0, 16, stride):
                    world_x = cx * 16 + local_x
                    world_z = cz * 16 + local_z
                    surface_y = None
                    for y in range(200, -65, -1):
                        sec = sec_by_y.get(y >> 4)
                        if sec is not None and is_solid(get_block_at(sec, world_x, y, world_z)):
                            surface_y = y
                            break
                    if surface_y is None:
                        continue
                    holes = 0
                    for y in range(-64, surface_y):
                        sec = sec_by_y.get(y >> 4)
                        if sec is None or not is_solid(get_block_at(sec, world_x, y, world_z)):
                            holes += 1
                    results.append((world_x, world_z, surface_y, holes))


def main():
    args = sys.argv[1:]
    stride = 4
    if "--stride" in args:
        i = args.index("--stride")
        stride = int(args[i + 1])
        del args[i:i + 2]
    if not args:
        print(__doc__)
        sys.exit(2)

    results = []
    for path in glob.glob(args[0] + '/*.mca'):
        process_region(path, stride, results)

    if not results:
        print("no columns sampled -- no region files found, or none of them have generated chunks")
        sys.exit(1)

    heights = [r[2] for r in results]
    holes = [r[3] for r in results]
    print(f"columns sampled: {len(results)}")
    print(f"surface_y range: {min(heights)} to {max(heights)}, avg {sum(heights) / len(heights):.1f}")
    print(f"total holes: {sum(holes)}, columns with any hole: {sum(1 for h in holes if h > 0)}")
    buckets = {}
    for h in heights:
        b = (h // 10) * 10
        buckets[b] = buckets.get(b, 0) + 1
    print("surface_y distribution:")
    for b in sorted(buckets):
        print(f"  {b:4d}..{b + 9:4d}: {buckets[b]}")
    worst = sorted(results, key=lambda r: -r[3])[:5]
    print("worst columns (x,z,surface_y,holes):", worst)


if __name__ == "__main__":
    main()
