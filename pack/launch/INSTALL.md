# Installing Project Ascension (alpha)

For friends and testers. The pack is a normal CurseForge modpack: the CurseForge app downloads every mod itself.

1. **Install the CurseForge app** (the desktop app, not another launcher: three of the mods only let the CurseForge app fetch them).
2. **Add the pack.**
   - When it is listed: search for *Project Ascension* in the app and install it.
   - Before that: in the app choose **Create Custom Profile**, then **Import**, and pick the `ProjectAscension-0.1.0-alpha.3.zip` you were sent.
   The app may warn that **about 49 files are not from CurseForge**. They are ours: 19 mod jars (`ascension_*.jar`, the mods we wrote) and
   the quest book, the settings and four shader option files. Accept and continue.
3. **Give it memory.** In the profile's settings set the allocation to **6 GB or more** (8 GB if you can spare it). The first start takes a few seconds longer: it builds the two Euphoria
   Patches shader packs in the profile's `shaderpacks` folder.
4. **Play.** A world made on alpha.2 loads (back it up first, and empty any Sophisticated backpack into a chest: that mod is gone and what is inside a backpack is lost; the known issues list the
   other changes). A world made on a development build from before alpha.1 keeps the dimensions it was made with and will not have the roofless Nether or the End as a planet, so start a new one.
   Press **J** for the quest book.
5. **If it will not start,** send us `logs/latest.log` (and the newest file in `crash-reports/`) from the profile's folder
   (the app's *Open folder* button).

The keys, what the alpha is, and what is known to be wrong are in the pack's description and in KNOWN_ISSUES.md.

## For whoever is hosting the server (us)

`python tools/build-server-pack.py` writes `build/server/ProjectAscension-server-<version>.zip` and its `SERVER-README.md` has the steps
(NeoForge 21.1.249, Java 21, 6 GB or more, EULA accepted by the owner, `online-mode=true`, no whitelist). That zip carries other authors' jars,
three of them from mods that forbid redistribution: **never share it or publish it**, only upload it to our own server. A friend joins with the
same pack version; there is no whitelist (`whitelist on` and `whitelist add <name>` bring one back). **Updating a server that already has a world**
does not need a wipe: its `SERVER-README.md` has the steps (back up the world, delete the old jars, upload the new `mods/` and `config/`,
keep the world and your own `server.properties`).
