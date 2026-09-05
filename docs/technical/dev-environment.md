# Development Environment

## Toolchain (pinned from maven, 2026-09-05)

| | Version | Pinned in |
|---|---|---|
| Minecraft | 1.21.1 | `gradle.properties` |
| NeoForge | 21.1.249 (final 21.1 release) | `gradle.properties` |
| Parchment | 2024.11.17 | `gradle.properties` |
| Java | Temurin 21.0.12.1 LTS | `gradle.properties` (`org.gradle.java.home`) |
| Gradle | 8.14.4 | `gradle/wrapper/gradle-wrapper.properties` |
| ModDevGradle | 2.0.146 | `gradle/libs.versions.toml` |

**Why Gradle 8.x and not 9.x:** ModDevGradle documents compatibility with Gradle 8.8 and
states nothing about Gradle 9. 8.14.4 is the final 8.x release. Revisit when MDG declares
Gradle 9 support.

**Why NeoForge 21.1.249:** it is the last release of the 21.1 line — development moved to 26.x
— so our target platform is frozen rather than drifting under us.

### Dependency version ranges: floor, not pin

`neoforge_version` is what we **compile against** (21.1.249).
`neoforge_version_range` is the **compatibility floor** we declare to the loader, and it is
deliberately looser: `[21.1,)`.

Pinning the declared range to the compile version would make our mods refuse to load on any
slightly older 21.1.x build — for example the NeoForge 21.1.227 instance already on this
machine. For a project whose first priority is that other people can actually use these
modules (ADR-0003), that is the wrong default.

**Rule:** the declared floor is the lowest version a module genuinely needs. Raise it only
when a module starts using an API that requires it, and say so in the commit. Never raise it
automatically just because we bumped what we compile against.

### Version single-sourcing

Two files, one rule each. Never duplicate a version across both.

- **`gradle.properties`** — runtime platform versions (Minecraft, NeoForge, Parchment) and mod
  metadata. These live here because they are also interpolated into `neoforge.mods.toml` at
  build time.
- **`gradle/libs.versions.toml`** — Gradle plugin and Java library versions.

## Machine setup (done 2026-09-05)

- **Temurin JDK 21.0.12.1 LTS** — `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`
- `JAVA_HOME` set at user scope; `%JAVA_HOME%\bin` is first on the user PATH.
- CurseForge at `C:\Users\sanch\curseforge` — used for production-instance testing.
- Gradle is not installed system-wide and does not need to be; the wrapper provides it.

### The Java trap, and what was actually wrong

The user PATH contained `C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot\bin`, but that
directory **no longer existed**. PATH fell through to Oracle's `javapath` shim, so `java`
resolved to a **Java 8 JRE** and `javac` was absent entirely.

Fixed by removing the dead entry, setting `JAVA_HOME`, and putting JDK 21 first on PATH.
The previous PATH was backed up to `C:\Users\sanch\user-path-backup-20260905.txt`.

Belt and braces: the build also pins `org.gradle.java.home` in `gradle.properties`, so it uses
the right JDK regardless of PATH or of which shell it is launched from. If you ever see an
"unsupported class file version" error, check that pin first.

### Wrapper distribution check

`validateDistributionUrl=false` in the wrapper properties. The wrapper's URL liveness check
fails behind redirect-restricted networks; `distributionSha256Sum` is retained and is the
stronger integrity guarantee — it was verified against Gradle's published checksum before the
wrapper was generated.

## Commands

Build everything:

```bash
./gradlew build
```

Launch a dev client:

```bash
./gradlew :modules:core:runClient
```

Launch a dedicated server — required before merging anything touching world state, per ADR-0008:

```bash
./gradlew :modules:core:runServer
```

List modules and their tier:

```bash
./gradlew moduleReport
```

Run directories are created under `run/` at the repo root and are gitignored.

## Build structure

Shared configuration lives in `buildSrc` convention plugins, so a module build script is two
lines and conventions cannot drift between modules.

- `ascension.java-conventions` — Java 21 toolchain, UTF-8, lint flags, reproducible jars
- `ascension.mod-conventions` — NeoForge, Parchment, run configs, `mods.toml` interpolation,
  jar manifest
- `ascension.dev-runtime-conventions` — seeds fresh `run/*` directories with dev-friendly
  Minecraft options

#### Dev game options

Minecraft writes `options.txt` only *after* first launch, so every brand-new run directory
shows the accessibility onboarding prompt before the main menu, pauses when it loses focus,
and replays the movement tutorial. We create run directories constantly, so `seedDevGameOptions`
pre-writes four keys:

| Key | Value | Why |
|---|---|---|
| `onboardAccessibility` | `false` | Skips the narrator prompt before the main menu |
| `pauseOnLostFocus` | `false` | Game keeps running while alt-tabbed — pausing mid-test
invalidates timing observations |
| `tutorialStep` | `none` | No movement tutorial toasts |
| `narrator` | `0` | Narrator explicitly off |

It is idempotent and never clobbers existing settings: it creates `options.txt` when missing,
and otherwise appends only keys that are absent. It runs automatically before `runClient`,
`runServer` and `runData`.

This covers *development* only. Shipping the same defaults to players on their first launch
is a separate pack-level task, tracked in `todo.md`.

**Mod ids are derived from the project path, never hand-written:**

| Project path | Mod id |
|---|---|
| `:modules:core` | `ascension_core` |
| `:modules:atmosphere` | `ascension_atmosphere` |
| `:modules:compat:sable` | `ascension_compat_sable` |

This makes the ADR-0003 tier structure visible in the mod id and removes a class of
copy-paste mistakes.

### Configuration cache

Currently **off** (`org.gradle.configuration-cache=false`). ModDevGradle's compatibility with
it is unverified. Enable it, confirm `runClient` still works, and record the result before
leaving it on.

## The test loop we actually use

1. `./gradlew :modules:atmosphere:build`
2. Copy the jar into the CurseForge test instance:
   `C:\Users\sanch\curseforge\minecraft\Instances\Ascension Dev\mods\`
3. `./gradlew :modules:atmosphere:runServer` (dev server on `localhost`, port 25565)
4. Join from the **Ascension Dev** CurseForge instance via Multiplayer

The instance is MC 1.21.1 / neoforge-21.1.249, matching what we compile against. `Devil0701` is
opped at level 4 in `run/server/ops.json`, using the offline-mode UUID because the dev server
runs `online-mode=false`.

**Restart the server after every rebuild** — the jar is read at startup.

### Third-party mods in the test instance

Kept deliberately short, and each one is recorded here with the reason it is present. This is a
**development environment**, not a modpack: ADR-0002 still stands and
`docs/technical/mod-list.md` is still a landscape survey, not a dependency list.

| Mod | Why it is installed | Is it a dependency? |
|---|---|---|
| **JEI** (Just Enough Items) | Item search, and `U` / `R` to see what a thing is used in and how it is made. Turns "did the recipe load" from a guessing game into a lookup. | **No.** Client convenience only, never referenced in code. |
| **Curios API** | Provides the accessory slot the tank valve will bind to. | **Yes, but Tier 2 only.** `ascension-compat-curios` compiles against it; Tier 1 must never require it (ADR-0003 rule 1, ADR-0009 §4). |

Installed versions, as of 2026-09-05: `jei-1.21.1-neoforge-19.51.0.418`,
`curios-neoforge-9.5.1+1.21.1`. Versions are listed because they are part of any performance
reading taken on this instance — see
[`performance-log.md`](performance-log.md) on what a baseline may be compared to.

Install both through the CurseForge app's own mod browser for the instance, so it resolves the
correct 1.21.1 / NeoForge build and any dependencies.

**Curios also has to be on the dev server**, not just the client — it owns server-side slot
state. For the Gradle `runServer` that means declaring it on the compat module's runtime
classpath, not dropping a jar in `run/server/mods`.

### Profiling a run

```bash
./gradlew :modules:atmosphere:runServer -Pjfr
```

Adds a Java Flight Recorder recording to the run, written to `run/server/ascension-server.jfr`
when the server exits **cleanly**. Needs no mod: JFR is in the JDK the build already pins. Full
instructions, including how to read the file, are in
[`performance-log.md`](performance-log.md#profiling-with-jfr).

### Debug commands

```
/ascension atmosphere query      breathability, drain, lung reserve, valve state
/ascension atmosphere why        every provider that claimed this position, and which won
/ascension atmosphere volumes    emitter count, pressurised blocks, whether your head is inside one
/ascension atmosphere debug vacuum | clear
/ascension atmosphere debug oxygen <units>
/ascension atmosphere debug tank <units>    charge of the tank in your main hand
```

`why` and `volumes` exist because two separate bugs were invisible without them: a provider
losing to a higher band looks exactly like a provider that does not work.

## Testing expectations

Per [ADR-0008](../decisions/0008-build-and-test-cadence.md), compiling is not evidence.
Every increment is observed in a running client; anything touching world state or sync is also
verified on a dedicated server and across a world unload/reload.

Per [ADR-0007](../decisions/0007-performance-contract.md) rule 12, every milestone ends with a
measurement recorded in `docs/technical/performance-log.md`.
