#!/usr/bin/env python3
"""List every block with a given id in a dimension's region files, with its block-state properties.

Why this exists: checking that the game changed a block in the world (the Archive's pylon glyphs
lighting with the gates, 2026-10-11) without a client, and without knowing in advance where a
structure put the block. It reads the palette first, so a section without the block is skipped
unread.

Usage:
    python find_blocks.py <region-dir> <block-id>

e.g. python tools/worldgen-diagnostics/find_blocks.py
    run/server/<world>/dimensions/ascension_worlds/moon/region ascension_worlds:mark_glyph

Prints `x y z {properties}`, one line per block. Run against a stopped server or after
`save-all flush`, so the region files are current. Uses scan_region.py's NBT reader.
"""
import glob
import os
import sys
import zlib

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from scan_region import parse_nbt  # noqa: E402


def matches(section, block_id):
    """(x, y, z, properties) of every block_id in a section, section-relative."""
    states = section.get('block_states', {})
    palette = states.get('palette', [])
    if not any(entry['Name'] == block_id for entry in palette):
        return
    data = states.get('data')
    bits = max(4, (len(palette) - 1).bit_length())
    per_long = 64 // bits
    mask = (1 << bits) - 1
    for i in range(4096):
        if data is None:
            index = 0
        else:
            word = data[i // per_long] & 0xFFFFFFFFFFFFFFFF  # the unsigned bit pattern
            index = (word >> ((i % per_long) * bits)) & mask
        entry = palette[index]
        if entry['Name'] == block_id:
            yield i & 15, (i >> 8) & 15, (i >> 4) & 15, entry.get('Properties', {})


def main():
    if len(sys.argv) != 3:
        print(__doc__)
        sys.exit(2)
    region_dir, block_id = sys.argv[1], sys.argv[2]
    for path in sorted(glob.glob(os.path.join(region_dir, 'r.*.mca'))):
        with open(path, 'rb') as f:
            header = f.read(4096)
            if len(header) < 4096:
                continue  # empty region file: created, never written
            for i in range(1024):
                offset = int.from_bytes(header[i * 4:i * 4 + 3], 'big')
                if offset == 0:
                    continue
                f.seek(offset * 4096)
                length = int.from_bytes(f.read(4), 'big')
                compression = f.read(1)[0]
                raw = f.read(length - 1)
                if compression != 2:
                    continue
                chunk = parse_nbt(zlib.decompress(raw))
                cx, cz = chunk.get('xPos'), chunk.get('zPos')
                for section in chunk.get('sections', []):
                    for x, y, z, properties in matches(section, block_id):
                        print(cx * 16 + x, section['Y'] * 16 + y, cz * 16 + z, properties)


if __name__ == '__main__':
    main()
