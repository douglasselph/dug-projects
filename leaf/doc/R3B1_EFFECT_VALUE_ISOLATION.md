# R3B.1 — Effect-value isolation and calibration repair

## Research purpose

**Question:** Before an overnight 36-Plant screening run, can our experiment reliably detect (a) a mechanically dead effect, (b) a deliberately exaggerated but game-limited effect, and (c) a truly unmistakable +20-VP effect, *without inadvertently also strengthening the opponent*?

This is **calibration of the measurement apparatus**, not certification that either official card or all 36 Plant effects are balanced. Read `doc/SIMULATION_LAUNCH_ARCHITECTURE.md` before changing any launcher or runner.

## Control groups

Root Four More (`Root_05_02`) and Queen's Blossom (`Flower_17_04`) use the exact original nine-card Grove (one card per cost/type slot). No canonical CSV, cost, scoring rule, or printed VP changes.

For each card the existing R3B launcher first trains unrestricted/targeted Buy policies separately under canonical, disabled and extreme *shared-market* effects, plus cross-evaluations using frozen original champion weights. R3B.1 then **reuses the exact champions from the canonical condition** for three more **one-sided** cross-evaluations:

| Research variant | Root Four More | Queen's Blossom | What it probes |
|---|---|---|---|
| `disabled` | No effect | No effect | Mechanical negative control |
| `extreme` | Raise +20 (die-cap bounded) | Draw four (supply/grid bounded) | Search/execution response to exaggerated mechanics |
| `vp20` | Immediate +20 VP | Immediate +20 VP | Unambiguous detector sensitivity |

Only the player with the **targeted** Buy strategy receives the research effect. The unrestricted/baseline opponent uses the canonical effect, even if it purchases the same card. A separate `unrestricted_vs_baseline` matchup contains **no** changed effects and must match canonical output **row-for-row**. The existing R3B shared-market cross-evaluations remain available separately, because universal desirability is another design question.

The learned Buy weights, opponent policies, Grove, seed schedules, and seat orders are held fixed. Decision trajectories may diverge after an effect changes. Consequently paired win/VP differences are **fixed-policy total-outcome differences**, not isolated estimates of a counterfactual single-action Strike result. We do not claim causation for downstream VP beyond the controlled intervention.

## What is measured

R3A held-out TSVs now append five per-player measures from actual Plant effect resolutions:

- `output_immediate_vp`: change in the acting player's immediate VP during each resolution, summed.
- `output_die_raise_pips`: actual net value change of dice present both before and after an effect resolution (identity-matched); obeys die caps. Includes reductions when the delta is negative.
- `output_drawn_dice`: net newly present dice in the acting player's Hand, not attempted draws. Battle capacity and supply can limit this.
- `output_drawn_pips`: sum of values on those newly acquired Hand dice.
- `output_resolutions`: successfully executed target Plant effect resolutions measured.

These **direct mechanical outputs are not in themselves extra final VP or extra Strike wins**. A die may be raised and still lose a Strike; a drawn die may influence later turns. Other effects (Bees, Wounds, multi-player changes) require their own output instrumentation later. Full attribution for all 36 effects, adaptive countersearch and R3C all-card balance conclusions remain unimplemented.

The `Game` recorder is opt-in, game-local and observational; it does not consume randomness, change policy choices, or mutate core card values. Its records are created only after a successfully committed Plant effect resolution. All existing official game paths leave the collector inactive.

## Confidence checks and hard failures

The outer `bin/r3b1_effect_isolation` launcher fails if output rows fail to match by `(matchup, sample, seat, role)`; if the untouched `unrestricted_vs_baseline` rows differ **at all**; if the disabled effect produces any immediate VP/die raise/draw output; or if the +20-VP version fails to produce exactly 20 immediate VP per actual measured resolution. It does **not** mistake zero actual resolutions for proof that an effect is powerful: inspection of `output_resolutions` remains mandatory. The report retains paired total VP, Strike VP and win-share changes with approximate intervals and labels all balance conclusions as inconclusive.

`manifest.json` fingerprints sources, launcher files, research CSVs, selected seeds and budgets. Training/evaluation phases keep separate direct-Java JVMs; there is one explicit Gradle compile before all phases, a lock, validated resumable checkpoints, per-stage and overall progress, and one final upload-ready `.tgz`. No historical scripts are deleted.

## Run on Ubuntu (at the top-level `leaf` directory)

```bash
python3 -m unittest discover -s bin/tests -p 'test_r3b1_effect_isolation.py' -v
./gradlew test --tests '*R3B1ResearchEffectIsolationTest' --tests '*ResearchCalibrationEffectsTest'
./gradlew simulationTest --tests '*R3BEffectSanityOptionsTest'
bin/r3b1_effect_isolation --plan

# Small end-to-end calibration:
bin/r3b1_effect_isolation --generations 2 --population 4 --elites 1 \
  --games 8 --holdout-games 80 |& tee ~/Downloads/r3b1-effect-isolation.log
```

The launcher prints `UPLOAD THIS FILE:` when all 6 discovery conditions, 4 shared-market frozen-policy evaluations and 6 one-sided frozen-policy evaluations complete. Run the **same command** to resume interrupted/finished batches. Inspect `ASSESSMENT.md`, `isolation-results.json`, and the nested original R3B report. The larger experiment should not proceed if positive controls are insufficiently exercised or if leakage/instrumentation checks fail.

## Acceptance / interpretation

- **Disabled:** Output must be mechanically zero for target resolutions, even though printed VP may still make the card worthwhile.
- **+20 VP:** Measured immediate VP must be exactly 20 per resolved target effect. A strong win advantage is expected *if the specialist actually activates it enough*, but a short run may fail to establish statistical dominance.
- **Raise +20 / Draw 4:** Realized additional pips/dice and competitive outcomes may be limited by mechanics and policy, so statistical non-detection is not automatically a software bug.
- **No untreated-player leakage:** `unrestricted_vs_baseline` must be literally identical under all one-sided variants.
- **All card adequacy assessments:** Remain `Investigate` until investment-level search, card-specific attribution and deliberate countersearch are designed and tested.
