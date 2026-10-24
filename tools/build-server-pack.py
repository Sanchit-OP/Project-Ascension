#!/usr/bin/env python3
"""Builds the server pack: the folder to put on a real dedicated server (PebbleHost or any other) as one zip.

  python tools/build-server-pack.py        writes build/server/ProjectAscension-server-<version>.zip

What goes in, and why it differs from the CurseForge pack (that one names mods by id; a server needs the files):

  mods/       every third-party jar of the test instance's mods folder that belongs on a server, plus our own 18 from the build
              output (modules/*/build/libs, the same place the CurseForge pack takes them from; the instance's copies can be
              old, which is how the alpha.2 server got the runes the pack had already replaced). A third-party jar is left off
              when its neoforge.mods.toml declares only CLIENT-side dependencies (Sodium, Iris, Entity Culling ...) or its
              name is in `dev_mods_server_exclude` in gradle.properties. The three mods whose third-party distribution is off
              (Create Aeronautics, Entity Culling, Keybind Atlas) ARE here, copied from the instance: this is for your own server.
              After writing, our jars are compared with the CurseForge pack of the same version in build/pack, and a
              difference fails the run (build that pack first: `python tools/build-curseforge-pack.py`).
  config/     pack/config (the pack's settings, the quest book) and serverpack/config (Chunky), without the first-launch client
              options (pack/config/defaultoptions).
  server.properties, user_jvm_args.txt, SERVER-README.md
              the settings with a reason behind them (serverpack/README.md), the garbage collector, and the steps.

**This zip carries other authors' jars, three of them from mods that forbid redistribution. Upload it to your own server only; never
publish it.** The EULA is not accepted for you: eula.txt is not in the zip. Standard library only; the zip is written in a fixed
order with fixed dates, so the same inputs make the same bytes.
"""

import json
import os
import re
import sys
import zipfile

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
OUT = os.path.join(ROOT, 'build', 'server')
FIXED_DATE = (2026, 10, 21, 0, 0, 0)
EXPECTED_OUR_JARS = 18
NEOFORGE_INSTALLER = 'https://maven.neoforged.net/releases/net/neoforged/neoforge/%s/neoforge-%s-installer.jar'


def gradle_property(name, default=None):
    with open(os.path.join(ROOT, 'gradle.properties'), encoding='utf-8') as handle:
        for line in handle:
            match = re.match(r'\s*%s\s*=\s*(.*?)\s*$' % re.escape(name), line)
            if match:
                return match.group(1)
    if default is None:
        sys.exit('%s not found in gradle.properties' % name)
    return default


def declares_client_only(path):
    """The same test as the build's `declaresClientOnly`: every dependency side in neoforge.mods.toml is CLIENT."""
    try:
        with zipfile.ZipFile(path) as jar:
            if 'META-INF/neoforge.mods.toml' not in jar.namelist():
                return False
            toml = jar.read('META-INF/neoforge.mods.toml').decode('utf-8', 'replace')
    except (zipfile.BadZipFile, OSError):
        return False
    sides = [s.upper() for s in re.findall(r'(?im)^\s*side\s*=\s*"([A-Za-z]+)"', toml)]
    return bool(sides) and all(s == 'CLIENT' for s in sides)


def read_pack():
    pack = json.load(open(os.path.join(ROOT, 'pack', 'pack.json'), encoding='utf-8'))
    return pack['name'], pack['version'], pack['neoforge']


def our_jars():
    """Our own jars as the build wrote them; the same search as tools/build-curseforge-pack.py."""
    version = gradle_property('mod_version')
    found = {}
    for folder, _, names in os.walk(os.path.join(ROOT, 'modules')):
        if os.sep + 'build' + os.sep + 'libs' not in folder + os.sep:
            continue
        for name in names:
            if re.fullmatch(r'ascension_[a-z_]+-%s\.jar' % re.escape(version), name):
                found[name] = os.path.join(folder, name)
    if len(found) != EXPECTED_OUR_JARS:
        sys.exit('expected %d built ascension jars, found %d (run ./gradlew build)' % (EXPECTED_OUR_JARS, len(found)))
    return sorted(found.items(), key=lambda item: item[0].lower())


def collect_mods():
    mods_dir = gradle_property('dev_mods_dir').replace('/', os.sep)
    excludes = [e.strip().lower() for e in gradle_property('dev_mods_server_exclude', '').split(',') if e.strip()]
    keep, skipped = list(our_jars()), []
    for name in sorted(os.listdir(mods_dir), key=str.lower):
        path = os.path.join(mods_dir, name)
        if not name.endswith('.jar') or not os.path.isfile(path):
            continue
        if name.startswith('ascension_'):
            continue  # the instance's copy of ours is not trusted; see our_jars()
        if any(e in name.lower() for e in excludes):
            skipped.append((name, 'named in dev_mods_server_exclude'))
        elif declares_client_only(path):
            skipped.append((name, 'declares only client-side dependencies'))
        else:
            keep.append((name, path))
    return keep, skipped


def collect_config():
    files = []
    for base, skip_top in ((os.path.join(ROOT, 'pack', 'config'), ('defaultoptions',)), (os.path.join(ROOT, 'serverpack', 'config'), ())):
        for folder, dirs, names in os.walk(base):
            rel_folder = os.path.relpath(folder, base)
            if rel_folder != '.' and rel_folder.split(os.sep)[0] in skip_top:
                continue
            if rel_folder == '.':
                dirs[:] = [d for d in dirs if d not in skip_top]
            for name in names:
                files.append(('config/' + (name if rel_folder == '.' else rel_folder.replace(os.sep, '/') + '/' + name),
                              os.path.join(folder, name)))
    return sorted(set(files))


SERVER_PROPERTIES = """# Project Ascension server. Every line here has a reason (serverpack/README.md in the repository).
# eula.txt is NOT included: you accept Mojang's EULA yourself (https://aka.ms/MinecraftEULA).
online-mode=true
# No whitelist (Sanchit, 2026-10-25: friends could not join). online-mode=true still checks every name with Mojang, so nobody
# can join as someone else; the cost is that anyone who finds the address can join. `whitelist on` brings it back.
white-list=false
enforce-whitelist=false
enforce-secure-profile=true
# Ships: a player standing on a moving airship can look like "flying" to the server and be kicked for it.
allow-flight=true
view-distance=8
simulation-distance=6
difficulty=normal
max-players=10
pvp=true
spawn-protection=0
sync-chunk-writes=false
motd=Project Ascension @VERSION@
"""

JVM_ARGS = """# Generational ZGC, the collector the pack was tested with (both flags are needed on Java 21).
-XX:+UseZGC
-XX:+ZGenerational
# Memory: the panel usually sets this from the plan. Leave 1 GB of the plan free for the system; the game wants 6 GB or more.
# -Xms6G
# -Xmx6G
"""

README = """# Project Ascension: server pack {version}

For a dedicated server on NeoForge {neoforge}, Minecraft 1.21.1, **Java 21**. **Do not publish this zip**: it holds other authors'
jars (three of them from mods that forbid redistribution). It is for your own server.

## Put it on the server

1. In the host's panel choose **Minecraft 1.21.1 with NeoForge {neoforge}** and **Java 21**. If the panel has no NeoForge picker,
   upload the installer ({installer}) and run `java -jar neoforge-{neoforge}-installer.jar --installServer` in the server's folder.
2. Stop the server. Upload this zip's contents (`mods/`, `config/`, `server.properties`, `user_jvm_args.txt`) into the server's
   root folder. Use the panel's "unarchive"/extract if it has one, or SFTP. (Updating a server that already has a world? Use
   "Updating" below instead: it keeps your settings and clears the old jars.)
3. Accept the EULA yourself (the panel's EULA button, or `eula.txt` with `eula=true`).
4. Give the server **6 GB or more** (the plan's memory, leaving a little for the system) and the two flags in `user_jvm_args.txt`
   if the panel does not already use ZGC.
5. Start it. The first start generates the world and takes a few minutes; wait for "Done".
6. In the console: `op <your name>`. There is no whitelist: anyone with the address and a real Minecraft account can join. To close it,
   `whitelist on` and then `whitelist add <name>` for each player.

## Check it

- The console lists no `ERROR` mod-loading lines and ends with `Done`.
- Join with the CurseForge pack (the same version). A mismatch refuses the connection and names the missing mod.
- Do **not** start Chunky pre-generation (`/chunky ...`) with players online; see serverpack/README.md in the repository.

## Updating a server that already has a world

A world made on alpha.1 loads on alpha.2 as it is: **do not wipe it**. Nothing about the dimensions, the structures or the saved data
changed (the only removals are two runes, and a saved rune page is moved to what replaced them). Chests nobody has opened yet roll the
new loot when they are first opened; chests already opened keep what they gave.

1. Tell players to leave, then stop the server.
2. **Back up the world** (the panel's backup, or download the `world/` folder). It takes a minute and undoes any surprise.
3. In `mods/`, **delete every jar** and upload this zip's `mods/` in their place. Old jars left behind next to the new ones are
   the usual cause of a server that will not start after an update.
4. Upload this zip's `config/` over the server's `config/` (it holds the quest book and the tuned mod settings).
5. **Leave alone:** `world/`, `whitelist.json`, `ops.json`, `banned-*.json`, `eula.txt`, and your own `server.properties` and
   `user_jvm_args.txt` (this zip's copies are for a first install only; take just the `motd` line if you want it).
6. Start it and watch the console end with `Done` and no `ERROR` mod-loading lines. Players must update their pack to the same version.

Every pack update means the same jars here: rebuild this zip with `python tools/build-server-pack.py`.
"""


def client_pack_mismatch(version, mods):
    """Names of our jars that differ from the CurseForge pack of the same version, or None when that pack is not built."""
    pack_zip = os.path.join(ROOT, 'build', 'pack', 'ProjectAscension-%s.zip' % version)
    if not os.path.isfile(pack_zip):
        return None
    wrong = []
    with zipfile.ZipFile(pack_zip) as pack:
        for name, path in mods:
            if not name.startswith('ascension_'):
                continue
            with open(path, 'rb') as handle:
                if pack.read('overrides/mods/' + name) != handle.read():
                    wrong.append(name)
    return wrong


def build():
    name, version, neoforge = read_pack()
    mods, skipped = collect_mods()
    wrong = client_pack_mismatch(version, mods)
    if wrong:
        sys.exit('our jars differ from build/pack/ProjectAscension-%s.zip, so clients and this server would run different code:\n  %s\n'
                 'rebuild that pack from the same ./gradlew build (python tools/build-curseforge-pack.py), then this one.'
                 % (version, '\n  '.join(wrong)))
    config = collect_config()
    entries = [('mods/' + n, p) for n, p in mods] + config
    text = {
        'server.properties': SERVER_PROPERTIES.replace('@VERSION@', version),
        'user_jvm_args.txt': JVM_ARGS,
        'SERVER-README.md': README.format(version=version, neoforge=neoforge, installer=NEOFORGE_INSTALLER % (neoforge, neoforge)),
    }
    os.makedirs(OUT, exist_ok=True)
    target = os.path.join(OUT, 'ProjectAscension-server-%s.zip' % version)
    with zipfile.ZipFile(target, 'w', zipfile.ZIP_DEFLATED, compresslevel=9) as z:
        for arc, content in sorted(text.items()):
            info = zipfile.ZipInfo(arc, FIXED_DATE)
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o644 << 16
            z.writestr(info, content.replace('\r\n', '\n').encode('utf-8'))
        for arc, path in sorted(entries):
            info = zipfile.ZipInfo(arc, FIXED_DATE)
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o644 << 16
            with open(path, 'rb') as handle:
                z.writestr(info, handle.read())
    ours = sum(1 for n, _ in mods if n.startswith('ascension_'))
    print('%s %s for NeoForge %s' % (name, version, neoforge))
    print('  mods:   %d (%d third-party, %d ours)' % (len(mods), len(mods) - ours, ours))
    print('  ours match the CurseForge pack: %s' % ('not checked (build/pack has no zip of this version)' if wrong is None else 'yes'))
    for n, why in skipped:
        print('  left off the server: %s (%s)' % (n, why))
    print('  config: %d files' % len(config))
    print('wrote %s (%.1f MB)' % (os.path.relpath(target, ROOT), os.path.getsize(target) / 1048576.0))


if __name__ == '__main__':
    build()
