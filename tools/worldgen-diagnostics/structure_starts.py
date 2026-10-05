#!/usr/bin/env python3
"""List the structures that actually generated in a dimension, straight from its region files.

Why this exists: "did my structure generate, and where?" is otherwise answered by flying around,
and `/locate` cannot find structures placed by `ascension_worlds:fixed_position` (vanilla's locate
only understands its own placement types). Every generated structure leaves a "start" in the NBT
of the chunk it began in; this reads them. It is how the Moon gateway (and earlier, which citadel
was the story one and which were wild copies) was confirmed without a client.

Usage:
    python structure_starts.py <region-dir> [filter]

<region-dir> e.g. run/server/<world>/dimensions/ascension_worlds/moon/region (the Overworld's is
run/server/<world>/region). [filter] keeps structure ids containing that text, e.g. "gateway".
Prints each start's id and chunk, and its pieces' saved fields (origin, facing, frame, site...).

Run against a stopped server, or after `save-all flush`, so the region files are current. Uses
scan_region.py's NBT reader, so keep the two side by side.
"""
import glob
import os
import struct
import sys
import zlib

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from scan_region import parse_nbt  # noqa: E402

PIECE_FIELDS = ("id", "Frame", "Site", "OriginX", "OriginY", "OriginZ", "Facing", "BB")


def starts_in(path, wanted):
    with open(path, "rb") as f:
        header = f.read(4096)
        if len(header) < 4096:
            return  # empty region file: created, never written
        for i in range(1024):
            if header[i * 4 + 3] == 0:
                continue
            f.seek(int.from_bytes(header[i * 4:i * 4 + 3], "big") * 4096)
            length = struct.unpack(">I", f.read(4))[0]
            if f.read(1)[0] != 2:
                continue
            try:
                chunk = parse_nbt(zlib.decompress(f.read(length - 1)))
            except Exception:  # a chunk mid-write on a live server; skip it
                continue
            for sid, start in chunk.get("structures", {}).get("starts", {}).items():
                if start.get("id") == "INVALID" or (wanted and wanted not in sid):
                    continue
                yield sid, start


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(2)
    region_dir = sys.argv[1]
    wanted = sys.argv[2] if len(sys.argv) > 2 else None
    found = 0
    for path in sorted(glob.glob(os.path.join(region_dir, "*.mca"))):
        for sid, start in starts_in(path, wanted):
            found += 1
            cx, cz = start.get("ChunkX"), start.get("ChunkZ")
            print(f"{sid}: chunk ({cx},{cz}) ~ block x={cx * 16 + 8}, z={cz * 16 + 8}")
            for piece in start.get("Children", []):
                fields = {k: piece[k] for k in PIECE_FIELDS if k in piece}
                print(f"    piece {fields}")
    if not found:
        print("no matching structure starts (has that area been generated yet?)")


if __name__ == "__main__":
    main()
