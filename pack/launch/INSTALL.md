# Installing Project Ascension (alpha)

For friends and testers. The pack is a normal CurseForge modpack: the CurseForge app downloads every mod itself.

1. **Install the CurseForge app** (the desktop app, not another launcher: three of the mods only let the CurseForge app fetch them).
2. **Add the pack.**
   - When it is listed: search for *Project Ascension* in the app and install it.
   - Before that: in the app choose **Create Custom Profile**, then **Import**, and pick the `ProjectAscension-0.1.0-alpha.1.zip` you were sent.
3. **Give it memory.** In the profile's settings set the allocation to **6 GB or more** (8 GB if you can spare it).
4. **Play, and start a new world.** An old world keeps the dimensions it was made with and will not have the roofless Nether or the End
   as a planet. Press **J** for the quest book.
5. **If it will not start,** send us `logs/latest.log` (and the newest file in `crash-reports/`) from the profile's folder
   (the app's *Open folder* button).

The keys, what the alpha is, and what is known to be wrong are in the pack's description and in KNOWN_ISSUES.md.

## For whoever is hosting the server (us)

Not written yet: the server is a separate piece of work (`plans/alpha-checklist.md`, A6). It must carry the same mods, `pack/config`
as well as `serverpack/config`, the three mods that do not allow other programs to download them (copied by hand), and
`online-mode=true`.
