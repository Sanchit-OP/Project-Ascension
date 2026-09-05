# Dimensions

> **Updated 2026-09-05.** Settled by
> [ADR-0004](../decisions/0004-fully-custom-dimensions.md): we author **fully custom
> dimensions**, no Ad Astra dependency, and planets are **data-driven JSON** rather than
> code-per-planet. `ascension-worlds` owns this.
>
> Per [ADR-0005](../decisions/0005-v1-scope-vertical-slice.md), v1 builds **Earth + Moon +
> Planet 3** only.
>
> **Still open and explicitly deferred:** whether orbit is a separate dimension or a high-Y
> band of the surface dimension. Seven planets x two dimensions is real chunk and memory cost,
> and the concern below that orbit risks being "just a loading layer" is the reason this is not
> being decided casually. It is resolved in the `ascension-worlds` design pass, before Moon
> work begins.

## Purpose

Defines the dimension model for planets, orbit layers, and transit spaces.

## Current Assumptions

- Each major planet has a surface dimension.
- Each major planet has an orbit dimension.
- Players arrive in orbit first and descend manually.
- Rift Gates connect orbital spaces after discovery.
- Optional orbital content can include stations, wrecks, or meteor events.
- Earth is a controlled starting dimension with reduced default progression routes.

## Questions To Resolve

- Is interplanetary space a navigable dimension, a set of routed orbit layers, or both?
- Where do ancient gateways physically exist?
- How are failed landings handled?
- How do orbital side destinations spawn, persist, or reset?

## Open Questions

- Which dimensions are mandatory for version one?
- What technical limits exist around ship behavior in orbit?
- How expensive should dimension travel be after mastery?
