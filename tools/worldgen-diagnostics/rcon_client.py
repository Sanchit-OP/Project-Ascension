#!/usr/bin/env python3
"""Minimal Source RCON client for hands-off dev-server verification.

Why this exists: verifying worldgen changes by hand means launching a client, flying around,
and eyeballing terrain -- slow, and it does not scale to sampling thousands of blocks. RCON lets
a script force-generate real chunks (`forceload add`) and inspect them without a player ever
connecting. See docs/technical/moon-terrain-tuning.md for the full verification recipe this is
part of.

No third-party RCON library is a project dependency, and pulling one in for a dev-only script
isn't worth it -- the protocol is ~30 lines.

Usage:
    python rcon_client.py <command...>

Reads connection details from RCON_HOST / RCON_PORT / RCON_PASSWORD env vars, defaulting to
127.0.0.1:25575. The server must have enable-rcon=true and a rcon.password set in
run/server/server.properties -- both default to disabled/blank; do not leave them enabled on
anything but a local dev server.
"""
import os
import socket
import struct
import sys


def send_packet(sock, req_id, ptype, body):
    payload = struct.pack('<ii', req_id, ptype) + body.encode('utf8') + b'\x00\x00'
    sock.sendall(struct.pack('<i', len(payload)) + payload)


def read_packet(sock):
    raw_len = sock.recv(4)
    if len(raw_len) < 4:
        return None
    length = struct.unpack('<i', raw_len)[0]
    data = b''
    while len(data) < length:
        chunk = sock.recv(length - len(data))
        if not chunk:
            break
        data += chunk
    req_id, ptype = struct.unpack('<ii', data[:8])
    body = data[8:-2].decode('utf8', errors='replace')
    return req_id, ptype, body


def rcon_exec(host, port, password, command):
    sock = socket.create_connection((host, port), timeout=10)
    try:
        send_packet(sock, 1, 3, password)
        auth_resp = read_packet(sock)
        if auth_resp is None or auth_resp[0] == -1:
            raise RuntimeError("RCON auth failed")
        send_packet(sock, 2, 2, command)
        resp = read_packet(sock)
        return resp[2] if resp else ""
    finally:
        sock.close()


if __name__ == "__main__":
    host = os.environ.get("RCON_HOST", "127.0.0.1")
    port = int(os.environ.get("RCON_PORT", "25575"))
    password = os.environ.get("RCON_PASSWORD", "")
    if not password:
        print("FATAL: set RCON_PASSWORD to match run/server/server.properties' rcon.password", file=sys.stderr)
        sys.exit(2)
    print(rcon_exec(host, port, password, " ".join(sys.argv[1:])))
