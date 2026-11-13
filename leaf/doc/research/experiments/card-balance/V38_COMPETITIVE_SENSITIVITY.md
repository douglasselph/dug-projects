# A2 — v38 Competitive Sensitivity, Root Four More

**Status:** Implemented pilot orchestration; Kotlin/Java experiment NOT YET EXECUTED.

## Research question

Having passed the **forced matched-state** Root Four More mechanical calibration,
can our **current v38 Three Engagements** full-game pipeline recognize a real
competitive consequence from suppressed or exaggerated effects, and how does
that consequence change after Buy-policy adaptation and opponent countersearch?

This is **not** a claim that Root Four More is too strong, too weak, or balanced.
The prior four-state forced calibration measured only local tactical sensitivity;
it did not measure game-level strategy and opportunity cost.

## Current experiment

Run `python3 bin/verify_v38_competitive_sensitivity.py --plan` first. The default
pilot has one legal nine-card Grove, two players, 3/2/2 rounds, and **only** the
`THREE_ENGAGEMENTS` Battle ruleset. It deliberately does not use the historical
`LEGACY_WHOLE_BATTLE` research default. No official CSVs or approved effects are
modified.

The coordinator compiles the Kotlin simulation once, then executes each
independent training/evaluation stage in a separate direct-Java JVM. Four
research-only conditions are involved:

| Condition | Target-seat effect | Purpose |
|---|---|---|
| Official | Raise +4 | Reference policy and effect |
| Suppressed | No effect | Negative control; still pays for/activates card |
| Raise +20 | Capped Raise +20 | Non-VP positive sensitivity control |
| Gain 20 VP | Immediate 20 VP per activation | Obvious scoring positive control |

Fourteen resumable stages consist of official targeted and unrestricted Buy
searches, an official held-out evaluation, three *frozen* variant evaluations,
two separately adapted targeted Buy searches with held-outs, and two challenger
searches with held-outs (against the official specialist and Raise +20 specialist).
The fixed-specialist countersearch applies the experimental effect to the
**specialist**, not to the opponent learning to beat it. Training-only purchase
incentives do not apply during evaluation.

`--holdout-games` defaults to 128 **per matchup**, with deterministic paired
seeds and alternating seats. Training and holdout seed schedules differ. These
128 samples do not establish a 5-pp equivalence bound or a reliable dominance
claim; the script reports counts and approximate paired confidence intervals.
Training policy family remains **Buy only**; all other decision families use
Human Baseline. A single trained challenger cannot certify expert counterplay.

The report separates these estimands:

- **Frozen:** same target and opponent weights, changed target-seat effect.
- **Adapted:** effect and separately learned target Buy policy both change.
- **Countersearch:** fixed specialist faces an independently trained challenger;
  this changes the opponent, not just the card effect.

Evidence includes target purchases, retained target grafts, actual effect
resolutions, realized raised pips, direct VP, final VP, Strike VP, win share,
seating, complete matchup rows, learning weights and stage logs. A suppressed
policy that never activates is reported **Investigate**, not certified zero-value.
The main reader rejects nonzero suppressed mechanical output, incorrectly
awarded VP20, unpaired/missing observations, nonfinite values, swapped roles,
changed untreated-control matchups and invalid completion/checkpoint markers.

## Ubuntu execution

From inside `leaf/`, after installing the changed-files ZIP:

```bash
python3 -m unittest discover -s bin/tests -p 'test_v38_competitive_sensitivity.py' -v
./gradlew simulationTest --tests '*R3AExperimentalOpponentIsolationTest'
python3 bin/verify_v38_competitive_sensitivity.py --plan
python3 bin/verify_v38_competitive_sensitivity.py --run |& tee ~/Downloads/v38-competitive-sensitivity.log
```

For a very small pipeline smoke, use `--generations 1 --population 3
--elites 1 --train-games 4 --holdout-games 8`. A smoke-only result cannot
establish competitive sensitivity. Increase the held-out budget after verifying
mechanical exercise and plausible search/countersearch behavior; only then
consider independent learner seeds, multiple Groves, and 3–4 players.

Each stage uses strict fingerprints and validated resumable checkpoints.
Completion or failure creates an upload-ready archive in `~/Downloads` and
prints `UPLOAD THIS FILE:`. Upload that archive and review its
`ASSESSMENT.md`, `assessment.json`, and per-stage held-out TSVs. Do not issue
**No concern demonstrated** or **Strong balance concern** from this pilot.

## Current limitations / next decisions

1. **Do not conflate effect utility with purchase utility**: the price and
   printed VP stay fixed here, but we have not yet compared legal same-tier
   substitute purchases comprehensively.
2. The source's target acquisition policy and Plant activation remain separate.
   One Buy-only optimizer can miss effect synergies despite using the card.
3. One market, one learner seed per condition, and one challenger per specialist
   are not enough for a 36-Plant verdict. Expand to independent policies, multiple
   Groves/player counts, and multi-decision learning after this pipeline works.
4. This runner records real effect exercise and competitive outcomes but is not
   a substitute for the existing forced matched-state mechanical test.
5. `bin/plant_catalog_screen.py` now defaults to an explicit v38 question
   template and rejects legacy-rule templates. That safety fix is **not**
   permission to launch the full 36-card screen before A2 acceptance.
