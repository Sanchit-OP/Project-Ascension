"""Flies the ship in space toward a point with its rider aboard, standing in for engines.

Small impulses every 0.1 s steer the ship's velocity toward a target: full speed while far, slowing
as it nears the planet's body (where it is carried down), with every change in speed kept gentle so
the rider stays on the deck. Stops when the ship leaves space.

usage: PYTHONPATH=tools/worldgen-diagnostics python ship_fly.py <mass> <target x> <target z> <body radius> [<cruise speed>]

RCON_PASSWORD as for rcon_client.py; PLAYER names the rider to report on (default Devil0701).
Used for the live test of 2026-10-11 (plans/live-test-2026-10-11.md).
"""
import math
import os
import re
import sys
import time

from rcon_client import RconSession

PASSWORD = os.environ["RCON_PASSWORD"]
PLAYER = os.environ.get("PLAYER", "Devil0701")
MASS = float(sys.argv[1])
TX, TZ = float(sys.argv[2]), float(sys.argv[3])
BODY = float(sys.argv[4])
CRUISE = float(sys.argv[5]) if len(sys.argv) > 5 else 60.0
DT = 0.1
MAX_DV = 1.0          # blocks/s per step: 10 b/s^2, gentle
SLOW_NEAR = 10.0      # speed when reaching the body
MAX_STEPS = 600

rcon = RconSession(PASSWORD)
run = rcon.run


for step in range(MAX_STEPS):
    info = run("execute in ascension_worlds:space run sable info @e")
    pos = re.search(r"Position: (\S+) (\S+) (\S+)", info)
    vel = re.search(r"Linear Velocity: (\S+) (\S+) (\S+)", info)
    if not pos:
        print(f"step {step}: ship no longer in space -> {info.strip()[:60]}")
        break
    x, y, z = (float(v) for v in pos.groups())
    vx, vy, vz = (float(v) for v in vel.groups())
    dx, dy, dz = TX - x, -y, TZ - z
    flat = math.hypot(dx, dz)
    dist = math.sqrt(dx * dx + dy * dy + dz * dz)
    to_edge = flat - BODY
    speed = max(SLOW_NEAR, min(CRUISE, 0.3 * to_edge))
    tvx, tvy, tvz = dx / dist * speed, dy / dist * speed, dz / dist * speed
    ex, ey, ez = tvx - vx, tvy - vy, tvz - vz
    err = math.sqrt(ex * ex + ey * ey + ez * ez)
    scale = min(1.0, MAX_DV / err) if err > 0 else 0.0
    ix, iy, iz = (MASS * e * scale for e in (ex, ey, ez))
    run(f"execute in ascension_worlds:space run sable physics impulse @e linear {ix:.2f} {iy:.2f} {iz:.2f}")
    if step % 10 == 0:
        print(f"step {step}: at {x:.0f} {y:.0f} {z:.0f}, {to_edge:.0f} to the body, speed {math.sqrt(vx*vx+vy*vy+vz*vz):.1f}")
    time.sleep(DT)

time.sleep(1.5)
print("player:", run(f"execute as {PLAYER} run data get entity @s Dimension").strip(),
      run(f"execute as {PLAYER} run data get entity @s Pos").strip())
print("moon ships:", run("execute in ascension_worlds:moon run sable info @e").strip()[:260])
rcon.close()
