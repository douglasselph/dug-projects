# A1.6 — Standardized v38 Plant question evidence (first verified slice)

Status: **Python implementation validated against the actual November 5 two-question Ubuntu pilot.** This is an engineering evidence-reader, **not** an automatic card-balance assessor.

The corresponding verifier archive is `leaf-v38-question-runner-verification-20261105-144745.tgz`. It reports `PASS_TWO_V38_QUESTION_COHORTS_EXPLORATORY_ONLY` for Root Four More and Sapping Snapdragon under `THREE_ENGAGEMENTS` / v38-01.

## Reproduce from the complete pilot archive

From the `leaf/` top-level directory after installing the patch:

```bash
python3 -m unittest discover -s bin/tests -p 'test_plant_question_evidence.py' -v
python3 bin/plant_question_evidence.py \
  ~/Downloads/leaf-v38-question-runner-verification-20261105-144745.tgz
```

The second command **does not** start Gradle or any new game simulations. It reads the uploaded pilot archive, validates the four embedded stage archives and their SHA-256 checksums, checks ruleset/plan identity, and checks all official, suppressed, adapted, and countersearch held-out TSVs. It writes a fingerprint-specific directory in `~/Downloads`, along with a self-contained upload-ready `leaf-v38-question-evidence-<hash>.tgz` including the source verification archive, `evidence.json`, `ASSESSMENT.md`, and `input.sha256`.

## Semantics and limits

- Two different card IDs are required. All four stage-archive hashes, the shared `THREE_ENGAGEMENTS` fingerprint, nine-card Groves and plans must match. Failed, truncated, unrotated or incomplete held-out cohorts fail closed.
- The untouched `unrestricted_vs_baseline` control must remain byte-equivalent between official and suppressed frozen runs.
- A suppressed no-op activation may still **resolve**, incrementing `output_resolutions`; it must produce **zero measurable mechanical effect** (`immediate_vp`, raised pips, drawn dice and drawn pips). This prevents confusing activation count with game-state benefit.
- Report the actual number of acquired/grafted cards, measured effect resolutions, player seats, final and Strike VP, and paired deltas separately for official-frozen, suppressed-frozen, adapted-suppressed, and official-countersearch observations. Output counters are currently **incomplete** for many sophisticated effects (such as opponent dice reduction); they are not full causal valuation.
- Sample sizes here are eight held-out target observations per matchup in each stage, one Grove, two players, Buy-only learned policy. *No confidence interval or card-balance verdict is justified.* The correct assessment for both cards is **Investigate — insufficient evidence**.
- The report reader deliberately **does not** reimplement the R3A/Kotlin engine, change source CSVs, retrain policies, create new comparison conditions, or claim expert-quality tactical counterplay.

## What's still open

A1.6 is only **partially implemented**: the full generic question contract still needs richer per-effect causal outputs, uncertainty on adequately powered trials, independent seed/seat/Grove coverage, pricing alternatives, effect-family capability flags, and standard evidence generation for arbitrary conditions and 2/3/4-player studies. A1.5 likewise remains incomplete for unsupported conditions, and A2 must establish competence/sensitivity before issuing serious strength or weakness findings.

The functional two-question v38 pilot (A1.7 engineering slice) **passed**, but that does not close the much larger A1 and A2 research-definition gates.
