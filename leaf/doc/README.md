# Documentation Map

The top-level `doc/` directory contains **cross-cutting project documentation**: material that explains how several source sets or subsystems fit together.

Use the repository-level [`README.md`](../README.md) as the main entry point.

## Cross-cutting documents

- [`HUMAN_BASELINE.md`](HUMAN_BASELINE.md) — Milestone-2 specification and certification map for the eight Human Baseline decision areas and 30 strategy hooks.
- [`HUMAN_BASELINE_CULTIVATION.md`](HUMAN_BASELINE_CULTIVATION.md) — detailed Cultivation architecture inventory and Milestone-2 review notes.
- [`USING_THE_SIMULATOR.md`](USING_THE_SIMULATOR.md) — practical workflow for using the project as a design-research tool.
- [`TESTING_AND_VERIFICATION.md`](TESTING_AND_VERIFICATION.md) — unit, integration, and simulation verification responsibilities and commands.
- [`CODE_ARCHITECTURE.md`](CODE_ARCHITECTURE.md) — deeper programming-oriented architecture.
- [`CHRONICLE_AND_OUTPUT.md`](CHRONICLE_AND_OUTPUT.md) — Chronicle inspection and generated output conventions.
- [`SIMULATION_PLAN.md`](SIMULATION_PLAN.md) — research questions and longer-term simulation plan.
- [`EXPERIMENT_COOKBOOK.md`](EXPERIMENT_COOKBOOK.md) — templates for constructing controlled experiments.

## Source-local README files

Some documentation is intentionally kept beside the code it describes. These files are useful when navigating the source tree directly:

```text
src/main/kotlin/dugsolutions/leaf/v35/player/decision/baseline/README.md
    Human Baseline package map and local navigation.

src/integration/kotlin/dugsolutions/leaf/integration/v35/README.md
    Purpose and organization of deterministic real-engine integration tests.

src/simulation/kotlin/dugsolutions/leaf/simulation/v35/README.md
    Purpose and organization of the simulation/research layer.
```

This split is intentional: `doc/` holds the durable cross-project explanation, while a source-local README answers “what is this directory for?” when encountered during code navigation.
