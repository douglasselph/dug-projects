# Simulation Operational Readiness — October 2026

## Purpose

This is a **stop-and-use-it** gate, not C04 human-preference calibration and not a
36-Plant balance certificate. It is deliberately independent of unexplained
absolute Human Baseline score magnitudes. It checks the ability to run the
current Kotlin engine end to end without known regression failures.

## Commands

From the repository root:

```bash
bash doc/SIMULATION_OPERATIONAL_READINESS.sh
cat output/readiness/latest/summary.txt
```

To additionally launch the small, real R3B.1 official/disabled/exaggerated/+20 VP
control experiment:

```bash
bash doc/SIMULATION_OPERATIONAL_READINESS.sh --extremes
```

**Caution:** Even with reduced population, generation and sample counts, the
experimental probe involves many game runs, separate processes, and frozen
policy controls. It can take substantially longer than the operational gate.
It is an intentionally weak sensitivity probe, **not** evidence of balance or
statistical reliability. Its own launcher enforces mechanical-output and
untreated-opponent checks. Consult its `isolation-results.json` for effect
opportunities and actual outputs; exit 0 does not guarantee that the extreme
had sufficient opportunities to be *detectable* by outcome metrics.

## Exit status and what to send for debugging

- 0: all requested checks passed; read the final PASS wording carefully.
- 1: at least one check failed, or an optional probe failed.
- 2: invalid invocation.
- Logs: `output/readiness/latest/*.log`.
- Summary: `output/readiness/latest/summary.txt`.
- Failed regression: send relevant `regression.log` tail and test failure summary.

### Scope

Default gate runs `test integrationTest simulationTest`, one full Mechanical
Control smoke game, one full four-player Human Baseline game using the project
3/2/2 research settings, then A03 boundary verification. The Gradle commands
use the actual engine and existing test suite. The smoke runs write chronicles
under `output/smoke/`. No training or decision coefficients are changed.

**Known limitations**: passing does not prove fine target choice, statistical
power, detection of exaggerated effects, perfect rules parity, or 36-Plant
balance. The default mode intentionally leaves R3B.1 for later. Use
`--extremes` before making claims about the sensitivity of experimental
controls. For a trustworthy research conclusion, run the full R3B.1 protocol
with adequate sample sizes as described in the master backlog.
