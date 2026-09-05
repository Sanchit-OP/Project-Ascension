# M0 — Toolchain and Skeleton

**Goal:** prove the entire build → launch → test → debug loop works, before a single line of
gameplay code exists.

**Exit criteria:** an empty `ascension-core` mod loads in a dev client, on a dedicated server,
and in a real CurseForge instance — and we have a recorded performance baseline to measure
everything else against.

There is deliberately **no gameplay in M0**. If the loop is broken, we want to discover it now,
against a mod that cannot itself be the cause.

---

## Environment — done (2026-09-05)

- Temurin **JDK 21.0.12.1 LTS** at `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`
- `JAVA_HOME` set at user scope, JDK 21 first on user PATH.
  **Root cause found:** PATH carried a JDK 17 entry pointing at a *deleted* directory, so it
  fell through to Oracle's javapath shim and `java` resolved to a Java 8 JRE with no `javac`.
  Dead entry removed; old PATH backed up to `C:\Users\sanch\user-path-backup-20260905.txt`.
- **Gradle 8.14.4** wrapper generated and committed. Distribution sha256 verified against
  Gradle's published checksum before use.
- CurseForge at `C:\Users\sanch\curseforge` for the production-instance test.

### Network note

This environment cannot follow Gradle's 307 redirect to GitHub release asset hosting, so the
wrapper's own bootstrap download times out. Worked around by seeding
`~/.gradle/wrapper/dists/gradle-8.14.4-bin/<url-hash>/` from the verified distribution.
`validateDistributionUrl=false` for the same reason — `distributionSha256Sum` is retained and
is the stronger guarantee. On an unrestricted network the wrapper bootstraps normally and
neither workaround is needed.

---

## Increments

### M0.1 — Build skeleton  *(done)*

- Set `JAVA_HOME` (user scope) to the Temurin 21 path, and pin `org.gradle.java.home` in
  `gradle.properties` so the build is reproducible regardless of PATH.
- Root Gradle multi-project: `settings.gradle.kts`, `build.gradle.kts`,
  `gradle/libs.versions.toml` as the single source of version truth.
- Gradle wrapper committed.
- NeoForge **ModDevGradle** (`net.neoforged.moddev`) applied via a convention plugin in
  `buildSrc`, so per-module build files stay near-empty and rules are enforced in one place.
- Pin exact NeoForge 21.1.x and Parchment versions by checking the NeoForged maven at
  implementation time — do not guess them.
- `.gitignore` for `build/`, `.gradle/`, `run/`, IDE files.

Pinned from maven on 2026-09-05, not from memory:

| | |
|---|---|
| NeoForge | 21.1.249 — the **final** 21.1 release, so the platform is frozen |
| Parchment | 2024.11.17 |
| ModDevGradle | 2.0.146 |
| Gradle | 8.14.4 — last 8.x; MDG documents 8.8 and says nothing about Gradle 9 |

**Verify:** `./gradlew build` succeeds from a clean clone.

### M0.2 — `ascension-core` loads  *(covered by M0.4)*

- Module `modules/core`, modid `ascension_core`, with `api/` and `internal/` package split
  per ADR-0003.
- Mod entrypoint, `neoforge.mods.toml`, `pack.mcmeta`, logo, licence.
- Nothing else. One log line on load.

**Verify:** `./gradlew :modules:core:runClient` launches, mod appears in the mod list, log line
present, no warnings.

### M0.3 — Dedicated server path  *(deferred to M1.2)*

- `runServer` configuration working, EULA handling documented.
- Confirm the mod loads server-side with no client-only class leakage.

**Deferred by decision, 2026-09-05.** Standing up a dedicated server to watch an empty mod
load proves nothing that M0.4 has not already proven. The check moves to the first
increment that actually has world state to desync.

**This is a deferral, not a cancellation.** ADR-0008 still requires it, and
`ascension-atmosphere` is world-state code from its very first increment. The
requirement lands on **M1.2**, which introduces per-player oxygen state and per-`ServerLevel`
zone state — exactly the code that hides side-only bugs when tested in single-player,
because single-player runs an integrated server.

**Verify (at M1.2):** dedicated server starts, client connects, mod present on both sides,
no client-only class leaks into server code.

### M0.4 — Production instance  *(done — passed 2026-09-05)*

- `./gradlew build` produces a real jar; install it into a CurseForge 1.21.1 NeoForge instance
  alongside Create + Sable + Aeronautics.

**Result:** passed in a clean CurseForge instance (`Ascension Dev`, 1.21.1 / neoforge-21.1.249).
Log confirmed `Ascension Core loaded (0.1.0)` from `modloading-worker-0`, zero errors, zero
warnings, and `mod/ascension_core` registered as a resource namespace.

This test earned its keep immediately: it exposed that `neoforge_version_range` was pinned to
the exact compile version `[21.1.249,)`, which would have silently refused to load on any
older 21.1.x install. The dev client would never have caught it — it runs exactly 21.1.249.
Fixed to a floor of `[21.1,)`; see "Dependency version ranges" in
`docs/technical/dev-environment.md`.

### M0.5 — Baseline measurement  *(partial — 2026-09-05)*

Record, per ADR-0007 rule 12, into `docs/technical/performance-log.md`:

- average tick time, vanilla-equivalent instance vs. with `ascension-core`
- heap after forced GC
- heap after three dimension unload/reload cycles

With an empty mod these should be indistinguishable. That is the point — it establishes the
measurement method and the baseline numbers while nothing can be blamed on us.

Also profile **Sable + Aeronautics** here, per ADR-0006, so the cost of the ships route is
known before anything depends on it.

### M0.6 — Refactor pass  *(done — 2026-09-05)*

Per ADR-0008: no new features. Reviewed the convention plugins, naming, version catalog and
the `api`/`internal` split before four more modules inherit the shape.

Found and fixed:

1. **A latent bug.** `seedDevGameOptions` was wired with `dependsOn` on the run tasks as
   well as `finalizedBy` on the prepare tasks. `dependsOn` gives no ordering guarantee
   relative to `prepareClientRun`, so on a fresh clone the seed could run first, find no
   run directory, silently do nothing, and the onboarding screen would appear anyway.
   It only looked correct because the run directory already existed locally. Now hooked
   solely to the prepare tasks, which is ordered by construction, and **verified by
   deleting `run/` entirely and re-running**.
2. `val minecraftVersion` in the mod conventions: declared, never used, and shadowed by
   the Parchment extension property of the same name. Actively misleading. Removed.
3. `extra["modId"]`: set, never read. Removed rather than kept speculatively.
4. JUnit entries in the version catalog were dead declarations. Now wired as test
   dependencies in the Java conventions, with a comment recording that unit tests are
   for pure logic only and are never evidence for world-state code (ADR-0008).

The silent-skip path also now logs, so a future failure of this kind is visible rather
than invisible.

---

## Definition of done

- [x] Clean clone builds with one command
- [x] `runClient` works; `runServer` deferred to M1.2 (see M0.3)
- [x] Jar loads in a real CurseForge instance (clean 1.21.1 / neoforge-21.1.249)
- [x] Baseline recorded in `docs/technical/performance-log.md` — tick 5 ms/50 ms, alloc 60 MB/s
- [ ] Sawtooth low point + 3 reload cycles (leak reference) — outstanding
- [ ] Sable + Aeronautics profiled — deferred, nothing depends on them yet
- [x] Refactor pass done
- [ ] Committed

## Risks

- **NeoForge/Parchment version drift** — pin from the maven at implementation time, never from
  memory or a tutorial.
- **Java 8 on PATH shadowing JDK 21** — the most likely early failure. `org.gradle.java.home`
  in `gradle.properties` is the guard.
- **Sable's mixin footprint showing up in M0.4** — if it destabilises a plain instance, that is
  valuable information arriving early, exactly as intended.
