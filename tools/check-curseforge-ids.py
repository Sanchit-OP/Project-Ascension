#!/usr/bin/env python3
"""Checks that every file the pack's manifest names still exists on CurseForge, before anyone installs the pack.

  python tools/build-curseforge-pack.py       (writes build/pack/<name>-<version>-ids.json)
  python tools/check-curseforge-ids.py        reads that list and asks CurseForge's website API about each file

Why: an author can withdraw a file after the launcher recorded it, and a pack that names a withdrawn file fails to install
for every player ("file not found"). This caught two on 2026-10-21 (Just Enough Resources 1.6.0.17 and Sophisticated Storage
1.6.7.2193), both already in the instance. Run it again right before uploading: some authors (the Sophisticated mods) publish
several builds a day and pull the odd one.

It reports, for each file: found or not, that it is for 1.21.1 and NeoForge, how old it is, and whether CurseForge lists a
newer 1.21.1 NeoForge file for the mod (information only: the pack stays on the file we tested). Exit code 1 if any file is
missing. Needs the network; standard library only. The website's JSON endpoint is used because the official API needs a key.
"""

import argparse
import datetime
import json
import os
import sys
import time
import urllib.error
import urllib.request

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
API = 'https://www.curseforge.com/api/v1/mods/%d/files?pageIndex=%d&pageSize=50&sort=dateCreated&sortDescending=true'
AGENT = {'User-Agent': 'Mozilla/5.0 (Project-Ascension pack check)'}


def listing(project, page):
    request = urllib.request.Request(API % (project, page), headers=AGENT)
    for attempt in range(3):
        try:
            with urllib.request.urlopen(request, timeout=30) as response:
                return json.load(response)
        except (urllib.error.URLError, TimeoutError) as error:
            if attempt == 2:
                raise
            time.sleep(2 * (attempt + 1))
    return {}


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('ids', nargs='?', help='the -ids.json the pack builder wrote (default: the newest in build/pack)')
    parser.add_argument('--pages', type=int, default=14, help='most listing pages to read per mod (50 files a page)')
    args = parser.parse_args()

    path = args.ids
    if not path:
        folder = os.path.join(ROOT, 'build', 'pack')
        found = sorted(n for n in os.listdir(folder) if n.endswith('-ids.json')) if os.path.isdir(folder) else []
        if not found:
            sys.exit('no ids file: run tools/build-curseforge-pack.py first')
        path = os.path.join(folder, found[-1])
    with open(path, encoding='utf-8') as handle:
        wanted = json.load(handle)

    now = datetime.datetime.now(datetime.timezone.utc)
    missing, odd, young, newer = [], [], [], []
    for mod in wanted:
        hit, latest = None, None
        for page in range(args.pages):
            data = listing(mod['projectID'], page)
            files = data.get('data') or []
            for f in files:
                versions = f.get('gameVersions') or []
                if latest is None and '1.21.1' in versions and 'NeoForge' in versions:
                    latest = f
                if f['id'] == mod['fileID']:
                    hit = f
            if hit and latest is not None:
                break
            if len(files) < 50:
                break
            time.sleep(0.1)
        if hit is None:
            missing.append(mod)
            print('MISSING  %-34s project %d file %d' % (mod['name'][:34], mod['projectID'], mod['fileID']))
            continue
        versions = hit.get('gameVersions') or []
        if '1.21.1' not in versions and '1.21' not in versions or 'NeoForge' not in versions:
            odd.append(mod['name'])
            print('ODD      %-34s %s %s' % (mod['name'][:34], hit['fileName'], versions))
        made = datetime.datetime.fromisoformat(hit['dateCreated'].replace('Z', '+00:00'))
        age = (now - made).days
        if age < 3:
            young.append('%s (%d days old)' % (mod['name'], age))
        if latest is not None and latest['id'] != hit['id'] and latest['dateCreated'] > hit['dateCreated']:
            newer.append('%s: %s is newer than %s' % (mod['name'], latest['fileName'], hit['fileName']))
    print()
    print('%d files checked: %d missing, %d not for 1.21.1 NeoForge' % (len(wanted), len(missing), len(odd)))
    for line in young:
        print('  young (a withdrawn build is likelier in the first days): ' + line)
    for line in newer:
        print('  newer exists: ' + line)
    sys.exit(1 if missing else 0)


if __name__ == '__main__':
    main()
