# R3B — Deliberately broken-card effect sanity gate

**Purpose:** Before interpreting a 36-card overnight screen as evidence of balance, establish that the experiment can tell when an effect has been intentionally changed to do nothing or to offer a grossly exaggerated benefit. This is **calibration**, not certification.

Read `doc/SIMULATION_LAUNCH_ARCHITECTURE.md` before editing runners. This version preserves R3A's original CLI defaults and direct-Java, compile-once, per-condition process isolation, deterministic seeds, generation checkpoints, progress estimates, and output contract. It adds an opt-in, separately fingerprinted calibration effect mode. No official CSV, scoring rule, card cost, or historical launcher is altered.

## Two intentionally exaggerated effects and disabled controls

| Official card | Canonical effect | Experimental no-op | Experimental exaggeration |
|---|---|---|---|
| Root Four More (Root_05_02, cost 5, printed VP unchanged) | Raise a die +4 | `RESEARCH_NO_EFFECT` | `RESEARCH_RAISE_DIE_PLUS_20` |
| Queen's Blossom (Flower_17_04, cost 17, printed VP unchanged) | Draw 2 dice | `RESEARCH_NO_EFFECT` | `RESEARCH_DRAW_FOUR_DICE` |

The variants use the existing `PlantExperimentConfig` resolver and enter only through the new `--effect-variant` option. `GameEffect` adds three **RESEARCH_** enum values, executed by explicit handler routes. Disabled effects still issue `EffectResolved` chronicle events but perform no action. Raise +20 remains **capped by the die's physical sides**, and Draw 4 remains **limited by dice available to draw and by legal Battle placement**. Either might fail to show a large real-game win effect, especially if the Human Baseline fails to activate the card.

Research handlers are not new card designs and cannot be requested by the canonical text parser. R3A's default mode is `official`; old R3A commands without `--effect-variant` retain official semantics and separately identified outputs.

## Experiment design: independent adaptation plus frozen-policy comparison

For each card (one locked Grove), run six independent discovery studies:

1. Official effect; targeted and unrestricted Buy search.
2. Disabled effect; targeted and unrestricted Buy search.
3. Exaggerated effect; targeted and unrestricted Buy search.

Each study has three matchups: targeted vs unrestricted, targeted vs Human Baseline, unrestricted vs Human Baseline. During training only, target specialists are encouraged to acquire the subject card; held-out matches have no purchase reward. **Independently learned specialists are not causal comparisons**: their weights and behavior may differ.

Then, **keep the exact champion Buy weights discovered in the official condition**, and run them under the disabled and exaggerated effects against matching opponents using the identical held-out seeds, seats and Grove. This isolates changing the game effect *for fixed policy parameters* more closely, though future actions and random consumption can diverge. Paired win-share, final-VP, Strike-VP and Plant-VP changes receive descriptive approximate intervals. Exaggeration reports an explicit alarm when a positive directional paired win advantage clears the initial 5-point screen plus the approximate 95% interval; otherwise it displays **not established**, never a fake pass.

For the no-op condition the controlled intervention is known to make the effect mechanically dead, whether or not the card retains value from printed VP. A zero-purchase or zero-activation control **fails exposure**, not card adequacy. An exaggerated effect that does not raise competitive outcomes remains inconclusive on detector sensitivity, not evidence of balance.

## Run

From repository root:

```bash
bin/r3b_effect_sanity_gate --plan
bin/r3b_effect_sanity_gate --generations 2 --population 4 --elites 1 \
  --games 8 --holdout-games 40 |& tee ~/Downloads/r3b-effect-sanity.log
```

After reviewing the results and verifying enough actual resolutions, increase budgets for replication; the experiment's run ID includes code, catalogs, seeds and settings. A new budget gets a new output directory. Identical reruns reuse validated R3A training/evaluation checkpoints, rebuild invalid generations and evaluate again if frozen weights change. The runner compiles once and subsequently starts isolated direct-Java processes for each condition, never Gradle for each game or generation. It prints overall condition-level progress with ETA and a final **single upload-ready `.tgz`**.

Within the archive: `manifest.json`, `calibration-results.json`, `ASSESSMENT.md`, six separately fingerprinted R3A study directories with their weights and reports, four frozen-policy re-evaluations and log files. A final `No concern demonstrated` outcome for a real card **is not available** from this calibration alone.

## Required sensitivity and validity checks before overnight

- Validate canonical values and legal Grove with `bin/r3b_effect_sanity_gate --plan`.
- Run `python3 -m unittest discover -s bin/tests -p 'test_r3b_effect_sanity_gate.py' -v` and the existing R3A tests.
- Run `./gradlew test --tests '*ResearchCalibrationEffectsTest'` and `./gradlew simulationTest --tests '*R3BEffectSanityOptionsTest'` on an Ubuntu checkout with the test dependencies available. These test actual no-op, +20 capped raise, Draw 4 outcomes and opt-in validation.
- Compile with `./gradlew --no-daemon writeSimulationRuntimeClasspath`; repeat small control run on Ubuntu.
- Confirm logged source/target, effect mode and actual effect resolutions for each condition. Inspect `calibration-results.json` and `ASSESSMENT.md`.
- Verify controlled no-op execution makes zero mechanical contribution (individual event/handler regression recommended) and exaggerated effects materially alter outcomes in relevant states. **Do not infer this from `EffectResolved` alone.**
- Re-run identically; ensure validated chunks skipped and held-out files unchanged.
- If the exaggerated variants fail to trigger a detectable signal despite adequate exposure, improve tactical exploitation or the experimental decision policy *before* commissioning an overnight 36-card adequacy screen.
- The original R3A method does not optimize tactical activations, does not perform countersearch and does not attribute individual effect-caused VP; those remain prerequisites for a strong all-card adequacy verdict.

## Scientific and engineering limits

This is a targeted first R3B increment, supporting **one Grove per calibration card**. Multiple contexts and countersearch are later steps, not silently approximated. The baseline non-Buy strategy may not understand the new research effect enum values. This can lower actual activation, so negative results must be treated cautiously. No automatic `No concern demonstrated` or `Strong balance concern` classifications are issued here.
