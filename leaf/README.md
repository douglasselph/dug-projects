# Leaf & Let Die — Simulation and Balance Research

This repository contains the Kotlin rules engine and research tooling used to simulate **Leaf & Let Die** for design analysis. Its purpose is not to build a playable UI or to prove that the game has no exploits. Its purpose is to run controlled, reproducible experiments that can expose balance problems such as dominant strategies, forced purchases, trap cards, runaway advantages, order bias, excessive luck, and simple policies that outperform richer play.

The central design question is:

> **Do better decisions matter enough to overcome ordinary game randomness?**

The project supports two complementary kinds of verification:

- **Integration testing** asks whether the real rules engine behaves correctly in a deliberately constructed scenario.
- **Simulation research** asks what happens statistically when complete games are repeated under different strategies, seeds, cards, seats, and experimental conditions.

The current simulation architecture separates **mechanical randomness** (dice, decks, random game effects) from **strategy tie-breaking randomness**, making controlled A/B experiments and deterministic replay possible.

## Documentation

- [Using the Simulator](doc/USING_THE_SIMULATOR.md) — how to choose between unit, integration, and simulation work; how to run existing experiments; how to add new scenarios and experiments.
- [Testing and Verification](doc/TESTING_AND_VERIFICATION.md) — Gradle commands, test source sets, sanity checks, deterministic integration tests, and how to browse the existing suites.
- [Code Architecture](doc/CODE_ARCHITECTURE.md) — a deeper programming-oriented tour of the repository, important packages, decision architecture, randomness, Chronicle, and source-set boundaries.
- [Simulation Plan](doc/SIMULATION_PLAN.md) — the long-term research plan: exploit testing, luck resistance, strategy diversity, card studies, Wounds, Battle order, comeback behavior, and staged development.
- [Experiment Cookbook](doc/EXPERIMENT_COOKBOOK.md) — practical templates for adding controlled experiments, including the proposed **early-Wisp windfall** stress test.

## Quick Start

Run the normal v35 unit tests:

```bash
./gradlew test
```

Run deterministic real-engine integration tests:

```bash
./gradlew integrationTest
```

Compile and test the simulation/research layer:

```bash
./gradlew simulationCheck
```

For a full local verification pass of the three current layers:

```bash
./gradlew test integrationTest simulationCheck
```

The first place to look when working on balance research is [Using the Simulator](doc/USING_THE_SIMULATOR.md). If you instead need to understand how the engine is wired together, start with [Code Architecture](doc/CODE_ARCHITECTURE.md).

## Current Research Model

The decision system currently distinguishes these roles:

| Role | Purpose |
| --- | --- |
| **Mechanical Control (Level 0)** | Deterministic, legal, deliberately naive engine/test control. |
| **Human Baseline (Level 1)** | Canonical simulation control group intended to approximate simple, reasonable experienced-human play. |
| **Planned Baseline** | Human Baseline plus a specific Plant-acquisition plan; useful for focused strategy/card experiments. |
| **Tactical (Level 2)** | Short-horizon improved play; architecture exists for continued development. |
| **Strategic (Level 3)** | Broader planning/opponent-aware play; architecture exists for continued development. |
| **Learned/Adaptive (Level 4)** | Future trained/persisted behavior. |

A key principle is to keep **engine correctness** and **game-design conclusions** separate. Integration tests should be exact and deterministic. Simulation experiments should run many complete games, rotate or control confounding factors, and report aggregates rather than treating one game as evidence.
