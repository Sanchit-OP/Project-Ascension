"""Gently lifts the latest Sable ship with a player aboard, standing in for engines.

Holds one RCON connection, sends a small upward impulse every 0.1 s sized to cancel gravity and
steer the ship's vertical speed toward a target: hover first, so the rider settles on the deck,
then climb steadily until the ship leaves the level (carried to space) or a time limit.

usage: PYTHONPATH=tools/worldgen-diagnostics python ship_lift.py <mass> [<setup command> ...]

RCON_PASSWORD as for rcon_client.py; PLAYER names the rider to report on (default Devil0701).
Used for the live test of 2026-10-11 (plans/live-test-2026-10-11.md).
"""
import os
import re
import sys
import time

from rcon_client import read_packet, send_packet  # noqa: E402
import socket  # noqa: E402

PASSWORD = os.environ["RCON_PASSWORD"]
PLAYER = os.environ.get("PLAYER", "Devil0701")
MASS = float(sys.argv[1])
SETUP = sys.argv[2:]
DT = 0.1
GRAVITY = 11.0
HOVER_STEPS = 10
CLIMB_SPEED = 15.0
MAX_STEPS = 400

sock = socket.create_connection(("127.0.0.1", 25575), timeout=10)
send_packet(sock, 1, 3, PASSWORD)
read_packet(sock)
req = 2


def run(command):
    global req
    req += 1
    send_packet(sock, req, 2, command)
    resp = read_packet(sock)
    return resp[2] if resp else ""


for command in SETUP:
    print(">", command, "->", run(command).strip()[:120])

for step in range(MAX_STEPS):
    info = run("sable info @l")
    pos = re.search(r"Position: (\S+) (\S+) (\S+)", info)
    vel = re.search(r"Linear Velocity: (\S+) (\S+) (\S+)", info)
    if not pos:
        print(f"step {step}: ship no longer in the Overworld -> {info.strip()[:80]}")
        break
    y, vy = float(pos.group(2)), float(vel.group(2))
    target = 0.0 if step < HOVER_STEPS else CLIMB_SPEED
    impulse = MASS * (GRAVITY * DT + 0.15 * (target - vy))
    impulse = max(0.0, min(impulse, MASS * 1.8))
    run(f"sable physics impulse @l linear 0 {impulse:.2f} 0")
    if step % 5 == 0:
        print(f"step {step}: ship y {y:.1f} vy {vy:.1f} impulse {impulse:.1f}")
    time.sleep(DT)

time.sleep(1.5)
print("player dimension:", run(f"execute as {PLAYER} run data get entity @s Dimension").strip())
print("player pos:", run(f"execute as {PLAYER} run data get entity @s Pos").strip())
print("space ships:", run("execute in ascension_worlds:space run sable info @e").strip()[:300])
sock.close()
