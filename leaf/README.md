# Leaf & Let Die --- Simulation and Balance Research

This repository contains the Kotlin rules engine and research tooling
used to simulate **Leaf & Let Die** for design analysis. Its purpose is
not to build a playable UI or to prove that the game has no exploits.
Its purpose is to run controlled, reproducible experiments that can
expose balance problems such as dominant strategies, forced purchases,
trap cards, runaway advantages, order bias, excessive luck, and simple
policies that outperform richer play.

The central design question is:

> **Do better decisions matter enough to overcome ordinary game
> randomness?**

The project separates three different questions:

-   **Unit testing** asks whether an individual rule, helper, or
    decision policy behaves as intended.
-   **Integration testing** asks whether the real rules engine behaves
    correctly in a deliberately constructed scenario.
-   **Simulation research** asks what happens statistically when
    complete games are repeated under different strategies, seeds,
    cards, seats, and experimental conditions.

The current simulation architecture separates **mechanical randomness**
(dice, decks, random game effects) from **strategy tie-breaking
randomness**, making controlled A/B experiments and deterministic replay
possible.

## Research Mission --- use the simulator to challenge the game

The simulator now has a certified Human Baseline control strategy. The
next major milestone is **not smarter AI for its own sake**. It is to
use complete-game experiments to challenge Leaf & Let Die's design and
look for conditions, cards, or simple strategies that make the game
collapse into something less interesting.

The immediate questions are practical design questions:

-   Can an extreme but legal early luck event create a persistent,
    disproportionate advantage?
-   Can a very simple development plan outperform richer play often
    enough that it becomes dominant?
-   Can a particular card, resource, or combo become effectively
    mandatory?
-   Do any rules need hard limits because an uncapped resource can
    snowball?
-   Are multiple development paths viable, or does one low-complexity
    strategy win too often?

Before interpreting an intervention, Milestone 3 first **calibrates the ordinary
four-player Human Baseline** for the exact selected nine-card Grove. A small
fixed-seed sanity cohort checks reproducibility and outcome/development
variation; a larger manual calibration reports cumulative seat win share at
increasing sample sizes so we can discover how many games are needed before the
four symmetric seats settle acceptably near the 25% reference. The calibration
is parameterized by both game count/checkpoints and selected Plant-card setup.

The first concrete intervention experiment is the **Six-Wisp Opening Stress
Test**. In a four-player Human Baseline game, one designated player has the
three dice from the opening Draw 3 forced to **2** in each of the first two
Cultivation rounds. This creates six early Wisp-producing rolls through the
normal Roll Reward path. The intervention will be compared with matched
ordinary Human Baseline controls using the calibrated Grove, seed schedule, and
sample-size evidence rather than assuming in advance that 2,000 is a magic
number.

The experiment architecture must keep one game's detailed typed
Chronicle only long enough to produce a compact per-game **summary
record**. High-volume runs retain those summaries, not thousands of full
Chronicles. Experiment-specific reports are then generated from the
summary records. Full Chronicles are reserved for selected diagnostic
reruns.

Start with [Simulation Plan](doc/SIMULATION_PLAN.md) for the research
questions and Milestone-3 roadmap, then [Experiment
Cookbook](doc/EXPERIMENT_COOKBOOK.md) for experiment patterns.

## Documentation

-   [Documentation Map](doc/README.md) --- index of cross-project and
    source-local documentation.
-   [Human Baseline Specification](doc/HUMAN_BASELINE.md) ---
    Milestone-2 certification map for the eight major decision areas and
    30 strategy hooks.
-   [Using the Simulator](doc/USING_THE_SIMULATOR.md) --- how to choose
    between unit, integration, and simulation work; how to run existing
    experiments; how to add new scenarios and experiments.
-   [Testing and Verification](doc/TESTING_AND_VERIFICATION.md) ---
    Gradle commands, test source sets, sanity checks, deterministic
    integration tests, and how to browse the existing suites.
-   [Code Architecture](doc/CODE_ARCHITECTURE.md) --- a deeper
    programming-oriented tour of the repository, important packages,
    decision architecture, randomness, Chronicle, and source-set
    boundaries.
-   [Chronicle and Output](doc/CHRONICLE_AND_OUTPUT.md) --- how to run
    one complete Mechanical Control game, read what happened, and how
    generated run artifacts are organized under `output/`.
-   [Simulation Plan](doc/SIMULATION_PLAN.md) --- the long-term research
    plan: exploit testing, luck resistance, strategy diversity, card
    studies, Wounds, Battle order, comeback behavior, and staged
    development.
-   [Experiment Cookbook](doc/EXPERIMENT_COOKBOOK.md) --- practical
    templates for adding controlled experiments, including the proposed
    **early-Wisp windfall** stress test.

## Quick Start

Run the normal v35 unit tests:

``` bash
./gradlew test
```

Run deterministic real-engine integration tests:

``` bash
./gradlew integrationTest
```

To **watch one complete game happen**, run the Mechanical Control smoke
game:

``` bash
./gradlew runMechanicalGameSmoke
```

This runs four deliberately naive Mechanical Control players through a
complete game and writes a readable Chronicle plus a short score summary
under:

``` text
output/smoke/mechanical-control/seed-13579/
  summary.txt
  chronicle.txt
```

Use another reproducible seed with, for example:

``` bash
./gradlew runMechanicalGameSmoke -PsmokeSeed=24680
```

This is the quickest visual sanity check that the complete rules engine
ran to completion and that the players actually took actions. It is
**not** a balance test and the Mechanical Control choices are
intentionally unsophisticated. See [Chronicle and
Output](doc/CHRONICLE_AND_OUTPUT.md).

Compile and test the simulation/research layer:

``` bash
./gradlew simulationCheck
```

For a full local verification pass of the three current layers:

``` bash
./gradlew test integrationTest simulationCheck
```

Milestone 2 established Human Baseline as the control strategy for
research; the full regression gate is green and the remaining work is
formal closeout/status bookkeeping. Milestone 3 begins the actual
design-research program. Start with [Simulation
Plan](doc/SIMULATION_PLAN.md), especially the Six-Wisp Opening Stress
Test and the summary/reporting architecture. For implementation
workflow, use [Experiment Cookbook](doc/EXPERIMENT_COOKBOOK.md). If you
instead need to understand how the engine is wired together, start with
[Code Architecture](doc/CODE_ARCHITECTURE.md).

## Current Research Model

The decision system currently distinguishes these roles:

  -----------------------------------------------------------------------
  Role                                Purpose
  ----------------------------------- -----------------------------------
  **Mechanical Control (Level 0)**    Deterministic, legal, deliberately
                                      naive engine/test control.

  **Human Baseline (Level 1)**        Canonical simulation control group
                                      intended to approximate simple,
                                      reasonable experienced-human play.

  **Planned Baseline**                Human Baseline plus a specific
                                      Plant-acquisition plan; useful for
                                      focused strategy/card experiments.

  **Tactical (Level 2)**              Short-horizon improved play;
                                      architecture exists for continued
                                      development.

  **Strategic (Level 3)**             Broader planning/opponent-aware
                                      play; architecture exists for
                                      continued development.

  **Learned/Adaptive (Level 4)**      Future trained/persisted behavior.
  -----------------------------------------------------------------------

A key principle is to keep **engine correctness** and **game-design
conclusions** separate. Integration tests should be exact and
deterministic. Simulation experiments should run many complete games,
rotate or control confounding factors, and report aggregates rather than
treating one game as evidence.
