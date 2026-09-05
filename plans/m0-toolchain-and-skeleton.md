# M0 — Toolchain and Skeleton

**Goal:** prove the entire build → launch → test → debug loop works, before a single line of
gameplay code exists.

**Exit criteria:** an empty `ascension-core` mod loads in a dev client, on a dedicated server,
and in a real CurseForge instance — and we have a recorded performance baseline to measure
everything else against.

There is deliberately **no gameplay in M0**. If the loop is broken, we want to discover it now,
against a mod that cannot itself be the cause.

---

## Environment (already done, 2026-09-05)

- Temurin **JDK 21.0.12.1 LTS** installed at
  `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`
- Note: system `java` on PATH is still a **Java 8 JRE**. `JAVA_HOME` is unset.
  M0.1 resolves this.
- CurseForge is installed at `C:\Users\sanch\curseforge` — used for the production-instance test.
- Gradle itself is not installed and does not need to be; the wrapper provides it.

---

## Increments

### M0.1 — Build skeleton

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

**Verify:** `./gradlew build` succeeds from a clean clone.

### M0.2 — `ascension-core` loads

- Module `modules/core`, modid `ascension_core`, with `api/` and `internal/` package split
  per ADR-0003.
- Mod entrypoint, `neoforge.mods.toml`, `pack.mcmeta`, logo, licence.
- Nothing else. One log line on load.

**Verify:** `./gradlew :modules:core:runClient` launches, mod appears in the mod list, log line
present, no warnings.

### M0.3 — Dedicated server path

- `runServer` configuration working, EULA handling documented.
- Confirm the mod loads server-side with no client-only class leakage.

**Verify:** dedicated server starts, client connects, mod present on both sides.
Required by ADR-0008 — this path must exist before any world-state code is written.

### M0.4 — Production instance

- `./gradlew build` produces a real jar; install it into a CurseForge 1.21.1 NeoForge instance
  alongside Create + Sable + Aeronautics.

**Verify:** loads in a real instance next to the mods we will later integrate with. This is
also the first compatibility smoke test.

### M0.5 — Baseline measurement

Record, per ADR-0007 rule 12, into `docs/technical/performance-log.md`:

- average tick time, vanilla-equivalent instance vs. with `ascension-core`
- heap after forced GC
- heap after three dimension unload/reload cycles

With an empty mod these should be indistinguishable. That is the point — it establishes the
measurement method and the baseline numbers while nothing can be blamed on us.

Also profile **Sable + Aeronautics** here, per ADR-0006, so the cost of the ships route is
known before anything depends on it.

### M0.6 — Refactor pass

Per ADR-0008: no new features. Review the convention plugin, naming, version catalog, and the
`api`/`internal` split before four more modules inherit the shape.

---

## Definition of done

- [ ] Clean clone builds with one command
- [ ] `runClient` and `runServer` both work
- [ ] Jar loads in a real CurseForge instance beside Create/Sable/Aeronautics
- [ ] Baseline numbers recorded in `docs/technical/performance-log.md`
- [ ] Sable + Aeronautics profiled
- [ ] Refactor pass done
- [ ] Committed

## Risks

- **NeoForge/Parchment version drift** — pin from the maven at implementation time, never from
  memory or a tutorial.
- **Java 8 on PATH shadowing JDK 21** — the most likely early failure. `org.gradle.java.home`
  in `gradle.properties` is the guard.
- **Sable's mixin footprint showing up in M0.4** — if it destabilises a plain instance, that is
  valuable information arriving early, exactly as intended.
