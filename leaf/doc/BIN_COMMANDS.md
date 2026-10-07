# `bin/` Command Reference

The `bin/` directory contains designer-facing wrappers around Gradle tasks. Run
these commands from any directory; each script resolves the repository root.
Use `--help` where supported. Generated research output belongs under `output/`
or `build/reports/`, not in the checked-in input data.

## `bin/baseline-game`

Runs one reproducible Human Baseline game from an explicit mechanical seed and
strategy seed. This is the most useful command for replaying a diagnostic case.

```bash
bin/baseline-game 12000 22000
bin/baseline-game --detail --grove 314241123 12000 22000
```

`--grove CODE` uses nine digits in slot order `R5 R7 R9 V7 V9 V11 F11 F14
F17`; digits 1-4 select the numbered card and 0 lets that slot be selected by
the game's mechanical RNG. Without `--grove`, `FirstGameDefault` is used.

## `bin/baseline-randomness`

Runs a small designer-facing Human Baseline variation diagnostic. It prints the
winner and final Plant/dice signatures so repeated complete games can be checked
for believable variation. This is not a pass/fail balance test.

```bash
bin/baseline-randomness 12
```

## `bin/variety-report`

Runs the Human Baseline early-purchase variety diagnostic. `--detail` adds one
reproducible seed example for each reported purchase outcome; replay one of
those examples with `bin/baseline-game --detail`.

```bash
bin/variety-report
bin/variety-report --detail 5000
```

The report is written under `build/reports/` and printed to the terminal.

## `bin/six-wisp`

Runs the matched Six-Wisp Opening Stress Test. One affected role is compared
against Human Baseline control using matched mechanical/strategy seeds and seat
rotation.

```bash
bin/six-wisp 1000
bin/six-wisp 1000 --wisps 4 --rounds 2/2/3
bin/six-wisp 1000 --random-grove
bin/six-wisp 1000 --grove 314241123
```

The default Grove is `FirstGameDefault`. `--random-grove` resolves one concrete
Grove once for the experiment, rather than choosing a different Grove per game.

## `bin/focused_plant_shape`

Runs the matched Lean Creature / Focused Plant Shape experiment against Human
Baseline. The learned/focused role rotates seats and the experiment currently
uses `FirstGameDefault` and the standard `3/2/2` round structure.

```bash
bin/focused_plant_shape 20
bin/focused_plant_shape 1000 --seed 31000 --strategy-seed 41000
```

## `bin/evaluate_buy_policy`

Runs matched held-out evaluation of a trained Buy-policy champion. For
each sample it runs a Human Baseline control and a learned-Buy variant with the
same mechanical seed, strategy seed, Grove, round structure, and affected
physical seat. The affected role rotates across all four seats.

```bash
bin/evaluate_buy_policy
bin/evaluate_buy_policy 1000 --input output/ai/buy-policy-v1-trained.weights
```

Defaults are 1000 matched samples, held-out mechanical seeds beginning at
161000 and strategy seeds beginning at 171000, `FirstGameDefault`, and `3/2/2`.
For Grove-robustness evaluation, `--grove CODE` uses the normal nine-position
Grove key but resolves every `0` to a new random legal card **once per matched
sample**. The CONTROL and LEARNED games in that sample receive the same concrete
Grove. Grove selection has its own deterministic RNG (`--grove-seed`, default
181000), so choosing a Grove does not consume mechanical or strategy randomness.
For example, `--grove 000100000` fixes `Vine_07_01` while independently
randomizing the other eight slots for every matched sample. `--random-grove` is
shorthand for `--grove 000000000`.

```bash
bin/evaluate_buy_policy --grove 000100000
bin/evaluate_buy_policy --random-grove --grove-seed 281000
```

The command refuses recorded training-seed overlap and validates the policy's
Plant manifest against current `CardDataFiles`. It reports control/learned win
share and VP, seat results, final Plant and dice-pool composition, Buy-phase shape,
Battle-by-Battle VP/wounds and dice utilization, effect/resource utilization (round effects, support actions, Compost/other upgrades, Plant effects, and Wisp lifecycle), an exact VP ledger reconciling Battle/direct-effect/other in-play/Plant/Wisp VP to FinalScore, and purchase behavior by Plant
cost/type/card and die size. It also reports exploratory Grove-conditional results
for the four 7-cost Vines and notable higher-VP Plants. Training fitness is printed only as provenance;
the held-out result is the independent evidence.

## `bin/train_buy_policy`

Evolves **only the Buy-selection policy**. Every other decision remains Human
Baseline, and Human Baseline still chooses payment for the selected purchase.
The trainer defaults to `FirstGameDefault` and `3/2/2`, rotates the learned role
through physical seats, and gives every candidate the same seed cohort. Training
can instead use `--grove CODE` or `--random-grove`; zero slots are resolved once
per training sample from a dedicated Grove RNG, and every candidate sees the
same concrete Grove for that sample. The default training Grove seed is `81000`
and can be changed with `--grove-seed`.

For a small end-to-end smoke run:

```bash
bin/train_buy_policy
```

Defaults are 5 generations, population 8, 20 games per candidate, 2 elites,
mutation sigma 0.25, and 6 mutated weights per child. The checked-in input is
`data/ai/4p/buy-policy-v1.weights`; the best candidate is checkpointed to
`output/ai/buy-policy-v1-trained.weights`. The input is never automatically
overwritten.

A larger run can be requested explicitly, for example:

```bash
bin/train_buy_policy --generations 20 --population 16 --games 100
```

For random-Grove training under a Plant experiment overlay:

```bash
bin/train_buy_policy --random-grove --grove-seed 81000 \
  --plant-overrides /path/to/v7-known-outliers-excluded.csv
```

`--random-grove` is equivalent to `--grove 000000000`. Explicitly fixed
unavailable cards and impossible constrained slots fail clearly.

Do not treat training fitness as held-out evidence and do not promote the
output into `data/ai/4p/` merely because its training result is good. Evaluate a
candidate on new seeds (and, as evaluation tooling expands, other Groves/round
patterns) before deliberate promotion.

The Buy model currently includes general action/type features, continuous and
exact Plant-cost features, Cultivation-stage interactions, and stable
card-identity features such as `CARD_Vine_09_01`. Trained policies also retain
provenance and a fingerprint snapshot of the Plant catalog used for training.
See [Learned Buy Policy](LEARNED_BUY_POLICY.md) for the model, CSV-change
workflow, manifest compatibility rules, and future training guidance.


## Player count

Core simulation/research commands that construct games accept `--players 2`, `--players 3`, or `--players 4` where applicable. The default remains 4. Learned-policy training and held-out evaluation rotate the affected learned role across exactly the configured physical seats. Saved trained-policy provenance records the training player count for newly trained policies.

Examples:

```bash
bin/train_buy_policy --players 3 --random-grove ...
bin/evaluate_buy_policy --players 3 --weights output/ai/example.weights --random-grove ...
bin/baseline-game --players=2
bin/baseline-randomness --players=2
bin/human-baseline-global-certification --players=3
bin/experiment_card_tier_calibration Vine_07_01 screen --players 3 --direction up
```

### `bin/experiment_card_tier_calibration` resync baseline

Current tier-calibration experiments default to the provisional Plant resync
baseline:

```text
data/research/4p/resync/resync-current.csv
```

The baseline is merged explicitly with the target-card intervention. Core
`train_buy_policy` / `evaluate_buy_policy` commands do **not** auto-load it.

Useful controls:

```bash
# Current provisional resync market (default)
bin/experiment_card_tier_calibration Root_09_03 screen --tiers 9,11,14,17

# Preserved resync checkpoint
bin/experiment_card_tier_calibration Root_09_03 screen \
  --baseline-overrides data/research/4p/resync/resync-v01.csv \
  --tiers 9,11,14,17

# Canonical-only Plant market
bin/experiment_card_tier_calibration Root_09_03 screen \
  --no-baseline-overrides \
  --tiers 9,11,14,17
```

The runner fingerprints and archives the baseline so changing
`resync-current.csv` produces a fresh output namespace rather than reusing old
completed runs.
