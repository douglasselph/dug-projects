# A2 — Root Four More activation opportunity-cost diagnostic (v38)

**Status:** Implemented; diagnostic replay still requires execution on Ubuntu.
**Scope:** The existing v38 Two-Player competitive pilot's *frozen Buy policies*,
not a new card-balance determination. The official Root Four More effect and
all official card CSVs remain untouched.

## What the source actually says

`RootFourMoreBaseline` assigns Cultivation activation **58** legacy priority
points before its effect is evaluated. `CardScoringHelpers.playScore` then adds
**3 points per realized Raise pip**, using a die-size-capped projection, plus
any current Buy-tier threshold adjustment. `DrawPriority` instead passes the
next die's expected roll to `LegacyPreferenceBridge`, which assigns
**35 + 4 points per expected Draw pip**. For an available D8,
Draw scores 35 + 4 × 4.5 = **53**; Root Four More's *base* already exceeds 53,
while a full Raise +4 scores at least 58 + 3 × 4 = **70**.

These are not calibrated measures of objective effect value; they are
legacy policy preferences. The mismatch is a plausible cause of frequent
Root Four More activations beating better alternatives. The actual next
Draw can be D4, D6, D8, etc., and buying thresholds, dice caps, Refresh,
Round effects and future availability can change the comparison.

The per-seat `RESEARCH_NO_EFFECT` override appears in `DecisionContextFactory`'s
Creature view. `HumanBaselineCardScorer.playScore` reads the effective
contextual effect, and `CardScoringHelpers` assigns the research no-op
**-10,000** base points. Suppressed Root Four More therefore becomes extremely
unlikely to win an ordinary Main choice; actual substitute decisions should
be observed rather than guessed from win share.

## Repeat the actual substitutes, without retraining

The existing A2 evidence archive contains both frozen official Buy-policy
weights and the original suppressed-evaluation configuration. To replay a
small, *diagnostic* subset of the held-out seed schedule, from inside `leaf/`:

```bash
python3 bin/trace_v38_root_four_more_alternatives.py \
  --archive ~/Downloads/leaf-v38-competitive-sensitivity-f2854f6546708175e408.tgz \
  --games 32 --plan

python3 bin/trace_v38_root_four_more_alternatives.py \
  --archive ~/Downloads/leaf-v38-competitive-sensitivity-f2854f6546708175e408.tgz \
  --games 32
```

The launcher builds once, safely reads the named archive members, reconstructs
the 9-card v38 Three Engagements roster and **reuses original Buy weights**.
It replays a fresh suppressed-condition evaluation with the original pairing
seeds. It does *not* rerun evolution. It leaves the original archive intact.

Opt-in tracing uses `LEAF_TRACE_SUPPRESSED_PLANT_ALTERNATIVES=Root_05_02`
within the replay JVM. When suppressed Root Four More is **currently legal**
and the policy chooses a Main Action, the trace records:

- the Main Action actually chosen: `DRAW`, `PLANT:<id>`, or `ROUND1/2:<effect>`;
- effective selected score, suppressed Root Four More score, Draw score, and
  all other Main Action scores after ordinary policy influences;
- next available die type and expected Draw pips;
- best projected *official* +4 Raise pips available from that same hand;
- matchup/sample/seat, player, round and Main Actions remaining.

Support actions are not substitutes for Main Actions, so trace rows are not
emitted when the chooser first chooses a free Support. The next Main decision
is considered instead. Only Main actions when the suppressed card is legal are
counted. No additional RNG draws, alternative search passes or game mutations
are introduced by this optional diagnostic.

The launcher writes `alternatives.tsv`, `summary.json`, the raw `console.log`
and the replay's `holdout.tsv` to a `.tgz` in `~/Downloads`. Upload that archive.
The summary reports action-frequency counts; compare them with card exercise
and expected pip gain before considering any change to the Human Baseline.

**Causal caution:** This trace states what the suppressed-policy *actually*
selected. It does **not** prove what the same player would have selected
under the official effect in an identical decision state, nor how much of
an eventual game result was caused by that alternative. The suppressed and
official whole-game trajectories diverge once an action changes. Any
policy calibration change requires a distinct paired test, not silently
reinterpreting these observational rows as causal evidence.

## Verification

- Python replay/parse and invalid-archive tests: included in
  `bin/tests/test_trace_v38_root_four_more_alternatives.py`.
- Kotlin scoring/choice and trace tests: included in
  `src/test/kotlin/dugsolutions/leaf/v35/player/decision/baseline/cultivation/SuppressedPlantAlternativeTraceTest.kt`.
- Local Gradle wrapper cannot download Gradle 8.13; focused Kotlin test and
  direct-Java replay must be run on Ubuntu before accepting this instrument.
