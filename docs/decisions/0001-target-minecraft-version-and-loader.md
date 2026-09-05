# ADR-0001: Target Minecraft 1.21.1 on NeoForge

- **Status:** Accepted
- **Date:** 2026-09-05

## Context

Nothing could be designed concretely until the version was fixed. Every mod interop question,
every API signature, and every mapping decision depends on it.

Create: Aeronautics was identified as the binding constraint — it is the mod most likely to
restrict our options, so the version choice was anchored to wherever it lives.

Research on 2026-09-05 established:

- Create: Aeronautics is at **1.3.2** (released 2026-08-29), actively maintained.
- It targets **Minecraft 1.21.1 on NeoForge**.
- It requires **Create 6.0.10** and **Sable**.
- 1.21.1 is the de-facto LTS anchor of the modding ecosystem, even though 1.21.5 and later
  have higher raw mod counts. NeoForge 26.1 is positioned as its eventual successor.

An earlier assumption in this project — that Aeronautics was a legacy 1.20.1 Forge alpha and
therefore a liability — was **wrong** and is corrected here.

## Decision

Target **Minecraft 1.21.1** on **NeoForge**. Compile against **Java 21**.

## Consequences

- Java 21 toolchain required (JDK 21, Temurin).
- The full candidate mod ecosystem is available on this version.
- Anchoring to Aeronautics costs nothing: 1.21.1 is both where it lives and where the mature
  ecosystem is.
- We are not on the newest Minecraft. Accepted deliberately: ecosystem maturity outweighs
  version recency for a long-form project.
- A future migration to 1.21.11+ / NeoForge 26.x is expected eventually. The module boundaries
  in ADR-0003 and the ban on deep vanilla coupling in ADR-0007 exist partly to keep that
  migration affordable.

## Alternatives rejected

- **1.20.1 Forge** — would have been correct under the stale assumption about Aeronautics.
  Research showed it is unnecessary; it would mean an older MC and a declining loader.
- **1.21.5 / 1.21.8+** — more total mods, but Aeronautics, Sable and Create 6 are not aligned
  there, and moving there would break the ships pillar.

## Sources

- https://modrinth.com/mod/create-aeronautics
- https://www.curseforge.com/minecraft/mc-mods/create-aeronautics/files/all
- https://neoforged.net/news/26.1release/
