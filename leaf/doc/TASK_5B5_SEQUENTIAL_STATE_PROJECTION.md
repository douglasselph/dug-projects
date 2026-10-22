# 5B.5 follow-up — sequential O Edelweiss prediction

This incremental change carries deterministic, observable effects from a first Play into the second card's scoring context. It is prediction only: no game state is mutated and no mechanical RNG is requested.

Supported exact first-step projections: bounded +4 and +1 Raise (including the Battle row's visible die and totals), gained Water, gained Critters where supplies permit. For Draw Two, the lowest-sided visible Supply dice are represented by fractional arithmetic means (e.g. D6 = 3.5) in a separate prediction overlay. Those fractional dice are not made into real `DieView` values or given Roll Rewards. The next scorer sees updated Supply/Discard and the Raise-specific overlay adjusts its result if the unrolled expected dice would be a better target.

Facing changes and the two-choice sequence remain as in 5B.5. The next choice is evaluated after the first projection; a first Flip cannot make a face-down Plant playable until that Plant is flipped face up. First-step Play always spends the source Plant.

## Known limits / work still required

This is **not a general-purpose effect interpreter**. Target-dependent effects besides simple Raise, nested third-level effects, Wisp-triggering, steal effects, redraws, conditional bonuses, simultaneous resource interactions, and full Battle placement of predicted Draw dice are not projected exactly; these continue to use existing card scorers. For a Draw-first then Raise-second sequence, the fractional die can affect the Raise estimate but is not installed into a Strike Square. A future complete solution needs effect-specific projection adapters for the rest of the card catalog and a complete joint choice/outcome model.

The projection of a deterministic Raise chooses a highest-immediate-gain target, which is a heuristic choice and may differ from a downstream tactical target. This is intentionally limited; do not interpret expert-vs-human results as fully calibrated until the coverage gaps are addressed.

## Validation

Run `./gradlew test --tests '*HumanBaselineEffectStrategyOEdelweissBattleAlignmentTest'` followed by the full test suite. No successful Gradle result was obtained in the preparation environment.
