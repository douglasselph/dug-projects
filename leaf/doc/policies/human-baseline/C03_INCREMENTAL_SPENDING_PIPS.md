# C03 — Cultivation direct incremental spending pips

`CultivationPipOutcome` provides the direct valuation unit before legacy priority conversion.

- Expected new drawable dice: D4=2.5, D6=3.5, D8=4.5, D10=5.5, D12=6.5, D20=10.5.
- Supply is drawn before discard is recycled, and a drawn die is counted once. Empty supply and discard yield `null` for a single draw and 0.0 for a sequence.
- Existing dice use real face differences; Raise effects are bounded by the die's maximum face.
- Forecast and Draw observation consume the shared primitive. A former extra-draw scoring helper no longer invents a D4 when no die is drawable.
- This incremental value is **not** the legacy priority-point scale. `DrawPriority.score()` and other action ranking still employ legacy conversions pending later unified calibration; C03 does not assert full priority-scale parity.
- The evaluator ignores speculative roll rewards and purchase thresholds; both are contextual factors for later tasks.

Run `./gradlew test --tests '*C03CultivationPipOutcomeTest'` and `python3 bin/a03_verify_boundaries.py`. No protected files are intended to change in C03; do not accept unrelated A03 differences.
