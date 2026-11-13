# V38 Root Four More — effect-sensitivity calibration (A2, first slice)

**Status:** First November 13 Ubuntu attempt (`leaf-v38-effect-sensitivity-20261113-135015.tgz`)
was **FAIL_INCOMPLETE**. Official R3A Root Four More Buy training and held-out
comparison executed; the wrapper then failed selecting the current output because
a historical R3A output directory was also present. The selection code has now
been corrected to use exact current manifests and fingerprints, not directory
count or modification times. The suppressed, Raise +20, and +20 VP controls have
**not yet been jointly verified**. No sensitivity or balance verdict. **

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
