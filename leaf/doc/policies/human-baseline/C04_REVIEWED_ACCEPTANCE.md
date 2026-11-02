# C04 — Reviewed mixed-action acceptance, first executable comparison batch

Source: user's annotated `blind_review_v3 (1).md` (36 judgments, October 2026).
This increment modifies **tests only**. It makes no scoring-coefficient changes.

## Covered cases

| Case | Human preference | Confidence | Choice families | Status |
| --- | --- | ---: | --- | --- |
| V3-02 | B (Draw D10) | 5 | Root Four More / Draw | diagnostic; review specified TOP target, while production Main scorer can select a different target |
| V3-05 | B (Draw D4) | 5 | Root Four More / Draw | diagnostic for same target mismatch; has additional roll-reward considerations |
| V3-13 | B (Draw D8) | 4 | Wisp / Draw | diagnostic, availability modeled |
| V3-18 | B (Draw D8) | 4 | immediate VP / Draw | diagnostic, contested rows modeled |

The test builds the complete three-row state for every listed player from explicit
individual dice. Every player has at least three placed dice; rows have at most
three dice per player. It calls production `BattleMainPriority.score`, prints

    C04_REVIEW|V3-13|human=B|confidence=4|baseline=B|A=...|B=...|agree=true

The output does **not** mean these are coordinator-verified legal situations.
Real legality, Draw lowest-sided priority, actual Plant holdings/readiness,
Round card combinations and resulting choice sets need independent acceptance.
Do not interpret diagnostic disagreement as a policy bug without confirming
candidate legality and exact target semantics.

## Why no enforced human-preference assertions yet?

The reviewed H02/H05 cases explicitly constrain Root's target to TOP, but the
real `BattleMainPriority` scores the **best** legal die across all own rows.
The test cannot legitimately assert the questionnaire's outcome against a
candidate with broader targeting. Tests avoid fabricating an agreement by
weakening the human response. V3-30 is excluded entirely: the scenario specifies a +2 Plant and a D20
as the next draw, neither of which can be safely substituted with a different
Plant or an unverified Supply ordering.

## Next required engineering increment

Build these cases through the Battle legal-choice generator with exactly
specified actual Plants and Supply/Discard order; lock the selected die target
where that is what was reviewed; then assert high-confidence rankings. Capture
actual scorer output first and investigate discrepancies before altering
coefficients. The held-out cases remain unused for calibration.

## Run

    ./gradlew test --tests '*C04ReviewedPreferenceAcceptanceTest' --info
    ./gradlew test simulationTest
    python3 bin/a03_verify_boundaries.py

Search test logs for `C04_REVIEW|` to collect the comparisons.
