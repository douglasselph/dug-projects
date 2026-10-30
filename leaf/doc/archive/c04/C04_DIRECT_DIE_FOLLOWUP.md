# C04 follow-up: direct die tactical projection

This increment routes the straightforward single-die Plant activation families through the same deterministic Battle Strike-row outcome projector used for other Battle analysis. The candidate-specific capped face delta comes from C02 `SharedDieEffectOutcome`; Battle outcome comes from `BattleOwnTotalChangeAnalyzer` / `BattleActionAnalyzer`. No Strike VP is added a second time. Closed/withdrawn rows and effect-specific D8/D12/lowest constraints are screened before projection.

**Not full C04 completion.** Existing legacy intrinsic base priorities still compete with tactical increments. Non-direct multi-die effects, opponent targeting, reroll randomness, Round effects and non-Strike utility still require a common within-phase scale and scenario verification. The fallback when no board target is projectable intentionally preserves historical behavior rather than incorrectly claiming a zero benefit.

Run `./gradlew test --tests '*C04DirectDieActivationPriorityTest'` and the full Kotlin suite. A03 should report no protected modifications.
