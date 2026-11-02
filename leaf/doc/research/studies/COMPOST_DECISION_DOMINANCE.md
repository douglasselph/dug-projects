# Compost decision-dominance A/B/C experiment

The launcher `bin/experiment_compost_decision_dominance` runs **fresh sequential Buy + Cultivation Main training** under three Compost variants. It does not modify the other Round-card effects.

- `original`: `UPGRADE_DIE_FROM_HAND` (replacement to Discard).
- `immediate-no-rewards`: new `UPGRADE_DIE_AND_USE_NOW_NO_REWARDS` (replacement rolled into Hand; `RollRewardPolicy.IGNORE`).
- `immediate-with-rewards`: existing `UPGRADE_DIE_AND_USE_NOW` (replacement rolled into Hand; normal Roll Rewards).

The new enum is intentionally a separate effect rather than a global change to Root Awakening. Existing Root Awakening behavior remains as before.

## Running

From the repository root:

```bash
# Validate all three override files without sleeping or training
PREFLIGHT_ONLY=1 bin/experiment_compost_decision_dominance

# Short initial test (strongly advised first)
MODE=screen bin/experiment_compost_decision_dominance

# Full run, delayed 2 hours so the existing experiment can finish
DELAY_SECONDS=7200 MODE=confirm bin/experiment_compost_decision_dominance
```

The delay applies before training starts, **not** before preflight; if the other experiment overruns its ETA, the runs will compete for CPU. Prefer launching the command in a shell that remains alive (`tmux` or `nohup`). Example:

```bash
nohup env DELAY_SECONDS=7200 MODE=confirm bin/experiment_compost_decision_dominance > compost-dominance-run.log 2>&1 &
```

`MODE=confirm` defaults to 5 learners per arm, 8x10x50 for Buy and Cultivation Main learning, and 1000 held-out games/learner. This is **120,000 nominal training-game candidate evaluations (3 arms × 5 learners × 2 families × 8 generations × 10 population × 50 games) plus 15,000 held-out joint evaluations and 15,000 Buy-market evaluations** as nominal input budgets. Internal trainer/evaluator opponent rotation may multiply actual simulated matches; use its logs for authoritative work counts.

Set `RESUME=1` (default) to skip completed learner subjobs within each arm. Output goes to `output/experiments/compost-decision-dominance/`, including per-arm logs/weights/archives, a combined `summary.txt`, and combined `.tar.gz` archive. Each arm uses the **same deterministic training and held-out seed numbers**, but the game paths are not paired counterfactual trajectories. Comparing raw win rates across arms is **not** a controlled A-vs-B-vs-C win-rate estimate: the provided evaluator benchmarks against a Human Cultivation Main control *within* each arm.

## What this implementation measures

The existing evaluation logs already print legal opportunities and actual uses for each Round effect, Main Action counts, purchases/game, endgame total die count and side-power, VP, and win share. These are enough to screen for the **frequency** of Compost choices and for retention of other actions. Inspect each learner individually; do not average away disagreement.

## Explicit limits / follow-on instrumentation

This package is a **runnable three-arm screening experiment**, not yet the entire aspirational telemetry design. It does **not** implement per-size final dice histograms (D4 persistence), state-by-state alternative-action availability, or deterministic cloned-state Compost-vs-Draw action counterfactuals. Those require new evaluation telemetry and are not implied by the existing logs. The existing `summary.txt` is a condensation; retained raw per-learner logs are authoritative.

The new no-reward effect uses the same Hand upgrade and roll as use-now, with `RollRewardPolicy.IGNORE`; the `RollResolver` still records a roll without awarding Critters/Wisps.

## Verification status

Script preflight completed in the authoring environment. Kotlin compilation and gameplay tests were **not** completed there because the Gradle distribution was not locally available and downloading was blocked. Compile and smoke-run on the development machine before scheduling the long confirm run:

```bash
./gradlew compileKotlin compileSimulationKotlin test simulationTest
MODE=screen LEARNERS=1 BUY_GENERATIONS=1 CULT_GENERATIONS=1 BUY_POPULATION=2 CULT_POPULATION=2 BUY_TRAIN_GAMES=2 CULT_TRAIN_GAMES=2 EVAL_GAMES=10 bin/experiment_compost_decision_dominance
```

Before starting a full run, ensure no competing experiment modifies the same output tree or source checkout. Use a clean commit or branch for reproducibility.
