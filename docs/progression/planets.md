# Planets

> **Updated 2026-09-05.** Per [ADR-0005](../decisions/0005-v1-scope-vertical-slice.md), **v1
> builds Earth + Moon + Planet 3 only**. The seven-step critical path below remains the design
> target; only the *build* order is cut. Planet 3 is in the slice specifically because it
> proves the framework generalises rather than being Moon-shaped.
>
> Per [ADR-0004](../decisions/0004-fully-custom-dimensions.md), planets are **data-driven
> JSON**, so worlds 4-7 are authoring work rather than engineering work.

## Purpose

Defines the role of each planet in the progression route.

## Current Scope Assumptions

- Version-one critical path contains seven mandatory off-world progression steps.
- The Moon is a mid-campaign progression world, not a tutorial.
- The Moon contains its own meaningful content, boss, and connected portal progression element.
- Optional space stations or meteor events may exist as small side destinations with curated rewards.

## Provisional Critical-Path Skeleton

- Earth: controlled starting world, primitive survival, basic engineering, first ship readiness
- Moon: oxygen progression step, small boss, first repaired return connection
- Planet 3: radiated power world with new minerals for stronger power generation
- Planet 4: new armor materials without full upgrade potential
- Planet 5: structure world with forge needed to unlock the armor's full potential
- Planet 6: industrial war-machine world and gun unlock step
- Planet 7+: final steps not yet defined

## Earth Rules

- Earth is intentionally constrained rather than feature-complete vanilla.
- Many standard ores or progression shortcuts can be removed or relocated.
- Nether portal access is not available through normal vanilla progression.
- Earth should teach long-term preparation, engineering, and resource discipline before real off-world expansion.

## Locked World Notes

- Team members can piggyback on the team's current unlocked planet immediately.
- Optional side destinations should stay limited and curated rather than sprawling.
- Some off-world environments can borrow End-like or Nether-like visual and biome logic without using vanilla dimension progression directly.
- Planet 3 is currently defined as the radiated power world.
- Planet 4 is currently intended as a predator-pressure world tied to armor-material acquisition.
- Planet 5 is currently intended as an ancient megastructure world containing a dormant forge that must be powered on before full armor upgrading becomes possible.
- Planet 6 is currently intended as the industrialization and firearms step, with industrial war machines as the primary threat.
- Some mandatory progression beats can be structure clears, repairs, and siege survival sequences rather than boss kills.

## Planet Template

- Fantasy
- Threat
- Constraint
- Objective
- Prize
- Mastery loop

## Required Fields Per Planet

- Orbit gameplay purpose
- Surface survival mechanic
- New movement mechanic
- Unique resource gate
- Core structure or dungeon
- Boss or conquest condition
- Permanent post-clear travel improvement

## Planet Classification

- Starting world: primitive foundation and first long-term engineering goals
- Mandatory worlds: critical-path progression steps that unlock future capability
- Optional worlds or side locations: support mastery, upgrades, lore, or rare materials
- Orbital side destinations: stations, wrecks, meteors, or ruins that provide focused optional rewards

## Critical-Path Rules

- No mandatory world can exist only to provide a resource.
- Every mandatory world must introduce one primary survival rule.
- Every mandatory world must introduce one movement or traversal wrinkle.
- Every mandatory world must contain at least one permanent unlock that changes future play.
- Orbit must have gameplay purpose beyond being a loading layer.

## Open Questions

- What are the final names and identities of the remaining mandatory worlds?
- Which mandatory steps are full planets versus smaller moons, ruins, or orbital layers?
- Which late-game worlds introduce systems beyond armor and guns?
