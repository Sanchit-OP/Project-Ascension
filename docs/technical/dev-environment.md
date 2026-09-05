# Development Environment

## Toolchain

| | |
|---|---|
| Minecraft | 1.21.1 |
| Loader | NeoForge 21.1.x (exact version pinned in `gradle/libs.versions.toml`) |
| Java | Temurin JDK 21 |
| Build | Gradle wrapper + NeoForge ModDevGradle |

## Installed on this machine (2026-09-05)

- **JDK 21.0.12.1 LTS** — `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`
- **CurseForge** — `C:\Users\sanch\curseforge` (used for production-instance testing)
- Gradle is **not** installed system-wide and does not need to be; the wrapper provides it.

### Known trap

The system `java` on PATH is a **Java 8 JRE**, and `JAVA_HOME` is unset. Gradle will pick the
wrong JVM if left to guess. The build pins `org.gradle.java.home` in `gradle.properties` so it
is correct regardless of PATH. If you hit an unexplained "unsupported class file version"
error, this is why.

## Commands

Build everything:

```bash
./gradlew build
```

Launch a dev client for one module:

```bash
./gradlew :modules:core:runClient
```

Launch a dedicated server (required before merging anything touching world state, per ADR-0008):

```bash
./gradlew :modules:core:runServer
```

## Testing expectations

Per [ADR-0008](../decisions/0008-build-and-test-cadence.md), compiling is not evidence.
Every increment is observed in a running client; anything touching world state or sync is also
verified on a dedicated server and across a world unload/reload.

Per [ADR-0007](../decisions/0007-performance-contract.md) rule 12, every milestone ends with a
measurement recorded in `docs/technical/performance-log.md`.
