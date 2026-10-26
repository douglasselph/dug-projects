# C02 — Shared effect-outcome projection (first migration)

This change introduces `SharedDieEffectOutcome`, a typed-by-effect shared projection of **actual, capped die pips** for deterministic single-die transformations. `CardScoringHelpers.playScore` consumes the shared projection for doubling, Raise +4/+20, Raise +1, D8 Raise +1, flip-to-opposite, and lowest-to-max. `CardScoringHelpers.scoreDieTarget` consumes the *same* projection for those effects and additional covered transformations; unsupported effects fall back to their existing target logic.

The difference between projected pips and final action preference is intentional: `playScore` still combines its projected benefit with historical per-card bases, Buy-threshold bonuses and other heuristics. Target ranking likewise retains a separate priority-scale conversion (50 + 4×pips). Replacing those different scoring scales across *all* Plants and Battle effects is **not accomplished in this C02 slice**; it belongs to the ongoing master calibration. The shared projection is reusable by that work.

The legality of candidate targets is still controlled by effect decision requirements and legal-choice generation; the helper's `bestProjectedPips` only applies its own D8, D12 and lowest-value restrictions. Complex decisions (set-to-match pairs, resource tokens, Draw, nested effects and Battle Strike collateral) retain specialized evaluators and must be integrated with a unified outcome abstraction in a later increment.

Regression tests verify action-vs-target gain parity, physical caps, experimental Raise +20 and the distinction between unsupported and zero-gain effects.

No canonical mechanics, CSVs, researched effects, pretrained weights or learned policies are altered.
