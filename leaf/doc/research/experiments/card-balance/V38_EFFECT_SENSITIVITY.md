# V38 Root Four More — effect-sensitivity calibration (A2, first slice)

**Status:** First November 13 Ubuntu attempt (`leaf-v38-effect-sensitivity-20261113-135015.tgz`)
was **FAIL_INCOMPLETE**. Official R3A Root Four More Buy training and held-out
comparison executed; the wrapper then failed selecting the current output because
a historical R3A output directory was also present. The selection code has now
been corrected to use exact current manifests and fingerprints, not directory
count or modification times. The suppressed, Raise +20, and +20 VP controls have
**not yet been jointly verified**. No sensitivity or balance verdict. **

## November 13 results and corrected interpretation

The uploaded four-condition results archive `leaf-v38-effect-sensitivity-20261113-142159.tgz`
completed the actual evaluation jobs, but the reporting gate incorrectly raised
`FAIL_INCOMPLETE: suppression control did not produce zero output`.
The **data show zero suppressed output**. The actual limitation was **zero
suppressed activations**: the policy avoided an inert Main Action. We must not
credit that with having mechanically exercised an inert effect.

The targeted comparison subset contains **8 paired games per intervention**:

| Condition | Actual activations | Actual raised pips | Immediate VP | Paired win-share difference vs official |
|---|---:|---:|---:|---:|
| Official +4 | 61 | 190 | 0 | Reference |
| Disabled | 0 | 0 | 0 | 0.00 |
| Raise +20 | 73 | 308 | 0 | 0.00 |
| Gain 20 VP | 79 | 0 | 1,580 | +0.25 |

The +20 VP control is mechanically correct (`79 × 20 = 1580`). Raise +20
produced real pip increases (`308 / 73 = 4.22` per activation, versus
`190 / 61 = 3.11` official), although different game trajectories prevent
per-activation contrasts from being a causal same-state measurement. The
+20 VP paired final-VP difference was +190.75 points; the Raise +20 paired
final-VP difference was -0.125 points. Win-share estimates are far too
imprecise to claim competitive sensitivity. Full report and raw TSVs are in
the uploaded archive.

**Repaired reporting:** zero output plus zero activations is now
`SUPPRESSED_ZERO_OUTPUT_UNEXERCISED` for the intervention, yielding the overall
`INCONCLUSIVE_SUPPRESSION_NEVER_ACTIVATED` rather than a false assertion of
nonzero output. Neither state qualifies as a passed fully exercised suppression
control. A genuine failure remains an effect that produces nonzero suppressed
mechanical output or mismatched paired control observations.

**Reassess without rerunning simulations:**

```bash
python3 bin/verify_v38_effect_sensitivity.py --reassess-archive \
  ~/Downloads/leaf-v38-effect-sensitivity-20261113-142159.tgz
```

This reads and rechecks every paired TSV and writes a new `UPLOAD THIS FILE:`
archive; it does **not** rerun Kotlin/Gradle, redo training, or certify new evidence.
A return code of 2 means genuinely inconclusive. Next research increment:
force matched legal Root Four More activations at identical pre-action states
under official and disabled effects (bypassing no-op avoidance **only** in the
research control), then add appropriately powered held-out non-VP comparisons
and genuine counterstrategies before any all-36 audit.

## Why

The engineering gate must detect a deliberately suppressed effect and an
exaggerated effect **that actually produces mechanical change**, rather than
mistaking a nominal Raise +20 for 20 extra pips. The chosen reference effect,
Root Four More (`Root_05_02`), ordinarily Raises one die +4. If the target die
is near maximum, Raise +20 may deliver little or no additional value. A direct
+20 VP control separately confirms that output instrumentation detects a clear
positive control; it is not a substitute for a non-VP test.

The user-uploaded NC-27 research-readiness archives `133054` and `133105` both
reported `FAIL_INCOMPLETE` because their `--mechanical-archive` file was absent
at `/home/dug/Downloads/leaf-current-card-mechanics-20261113-131420.tgz`. This
was a file-input failure **before** any new card-question run, not a rules failure.
The original passing mechanical archive is still valid evidence of the
particular checkout and CSVs it tested, but changes to Git HEAD require a new
verification run.

## Historical result selection (November 13 fix)

R3A intentionally creates a separate hashed directory for each unique input
manifest. Multiple directories under an experiment's output root are normal
after changes to source or study settings. `plant_question_pair.py` and callers
now require the **current** manifest: same planned nine-card Grove and target,
source and R3A launcher digests, authoritative CSV and research-input hashes,
ruleset provenance, and specified seeds/training budget. Matching zero or
multiple current manifests is an explicit error; no stale or arbitrarily newest
run is selected. Existing valid checkpoints remain resumable. The same selector
is used by `plant_question_adapt.py` and the exploratory `plant_catalog_screen.py`
index, whose output-root lookup also follows the current Battle structure.

## Run on Ubuntu from `leaf/`

```bash
# Fast current-question validation; no Kotlin, Java or Gradle execution.
python3 bin/verify_v38_effect_sensitivity.py --plan

# First ensure you have a PASS mechanical archive on the current checkout.
bash bin/verify_current_card_mechanics

# Use the actual UPLOAD THIS FILE path printed by the previous command.
python3 bin/verify_v38_effect_sensitivity.py --run \
  --mechanical-archive ~/Downloads/leaf-current-card-mechanics-YYYYMMDD-HHMMSS.tgz
```

The `YYYYMMDD-HHMMSS` above is a **placeholder**; replace it with the exact
archive name printed by the mechanical gate. The research script prints a
single `UPLOAD THIS FILE:` path to a new `~/Downloads/leaf-v38-effect-sensitivity-*.tgz`
bundle, including on failure. Upload that archive for review.

## Implementation and interpretation

1. Check actual PASS JUnit suites, official CSV fingerprints and Git HEAD when
   available. The gate **cannot prove uncommitted-source parity**.
2. Use the official v38 Root Four More two-player nine-card Grove question.
   Run existing official Buy search + suppressed target-seat frozen comparison;
   reuse valid existing checkpoints when available. Compile Gradle once.
3. Hold specialist and opponent Buy weights fixed and use the same two-player
   game-seed/seat schedule to evaluate two additional **target-seat-only**
   controls, in separate direct-Java processes:
   `RESEARCH_RAISE_DIE_PLUS_20`, `RESEARCH_GAIN_TWENTY_VP`.
4. Require aligned paired row identities and **byte-equivalent unaffected
   control matchup rows**. Suppression must produce zero mechanical output.
   +20 VP must deliver exactly 20 per **actual** resolution, not per game.
5. Measure actual Raise pips and resolutions under physical caps. A Raise +20
   result with zero realized pips is **not** evidence of positive non-VP
   sensitivity. Flag unexercised or constrained results rather than passing
   them as successful amplification.
6. Report paired mean deltas for win share, final VP and Strike VP, with
   exploratory approximate 95% intervals and exact sample counts. The
   independence unit is a game sample, not an activation. With eight pilot
   held-out games per matchup, intervals are not decision-grade.

Existing `plant_question_pair.py` retains its suppressed-default behavior. The
new optional frozen `effect_variant` is restricted to the three well-known
research controls; canonical CSVs and Kotlin effect handlers are unchanged.

## Acceptance and next steps

- **Mechanical gate PASS:** actual suppressed zero output; +20 VP exactly per
  resolution; Raise +20 produces nonzero **realized** pips. This proves only
  that the controls executed and their telemetry is legible.
- **Competitive sensitivity:** requires materially different held-out outcomes
  on more independent samples, and controlled evidence that the non-VP effect
  was genuinely amplified rather than blocked by caps. A nominally stronger
  effect producing the same or fewer total pips under changed game trajectories
  cannot be labeled detected dominance.
- **Further research:** if the non-VP control is constrained, add a
  deterministic same-state forced-use Raise comparison and/or choose a more
  reliable mechanically exaggerated control. Then expand to larger matched
  holdouts and credible countersearch; only then run the 36-Plant screening.
- The current learned policy scope is **Buy only**. Activation and targeting
  remain Human Baseline. Do not infer dead/dominant effects from inadequate
  policy use, or transfer these two-player results to 3–4 players.
