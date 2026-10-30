# B04 — Sunlight token acquisition valuation

This change affects only Human Baseline Cultivation action scoring and related tests.
Official Sunlight still grants a token, never a Raise +3. Battle spending remains
owned by `SunlightBattleSpendingValue`: it scores the actual funded Battle Main
Action and its preservation cost. No rules or learned weights were changed.

The historical Cultivation Sunlight willingness-vs-Draw gate was already removed
in B03, so B04 does **not** reintroduce one. The obsolete `SunlightPriority` scorer
was also removed in B03.

`SunlightTokenPriority` now estimates an additional token at 4.0 pip-equivalents,
with +0.5 when Battle is next, −1 for each held Sunlight, and a 1.0 floor if
held tokens already cover remaining Battles; no remaining Battle => zero value.
These are provisional judgments pending the master calibration. The current
Cultivation Draw scorer uses `35 + 4 × expected roll`, so the token estimate
is temporarily converted with that same formula. This bridge is **not** the
final pip-scale scoring system for all action families.

No random value has been introduced here: seeded, player-specific Decision
Variability is a separate master-calibration task and must be applied equally
to all competing actions, not just Sunlight.

Verification: run `./gradlew test --tests '*SunlightTokenPriorityTest'` plus
`./gradlew test simulationTest` on Ubuntu. A03 boundaries require the explicit
Human Baseline-only scope; no mechanics/data/weights were edited.
