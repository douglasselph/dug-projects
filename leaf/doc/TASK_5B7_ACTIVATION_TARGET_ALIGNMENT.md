# Task 5B.7 — Activation and Target Score Alignment

## Scope

This change aligns the available deterministic scoring paths without changing the game rules, card configurations, or learned policies.

### Petal To Die 4 (P1–P4)

Previously the activation used `PetalAcquisitionProjection.activationScore()` (which optimizes acquisition followed by optional cycling), while `choosePetalD4Source()` ranked choices using only `scoreSource()` (acquisition alone). Now both use `scoreAcquisitionSequence()` for each legal source, and the activation takes the maximum of precisely those candidate scores. This preserves source-specific predicted D4 faces and the special net-cycle calculation for a newly acquired Hand D4.

**Known limitation:** The subsequent `choosePetalToDie4()` optional branch still uses the prior branch heuristic/Battle analyzer, not a complete source-conditioned sequential state rollout. The source-choice projection uses expected cycling, not a binding commitment to cycle. This is a future integration target, not a claim of complete multi-step optimality.

### Sapping Snapdragon (S1, S2)

Both normal Battle Main pathways (`BattleMainPriority`, including First Main and Final Main) and the refresh-enabled Plant path (`BattleEnabledPlantAnalyzer`) now use `BattleSappingActivationPriority`. That adapter takes the highest complete deterministic projected Battle result from `BattleSappingSnapdragonProjector`, which the target strategy already uses to rank S1/S2 die candidates. The adapter adds that projection **once** to the card's intrinsic base; it does not add the separate generic +2/row-need heuristic adjustments again.

The two-stage S2 target callback still projects its second stage after the real initial Raise, using `initialRaisePending = false`. The best-first estimate is made before execution, and the actual second-stage legal choices are reevaluated after execution.

### Scoring semantics

1. Candidate comparisons are based on visible state, with no random sampling.
2. Hypothetical unknown dice use the established arithmetic-mean approximation.
3. Target-choice and top-level scoring reuse one factual projection, but context-dependent action/base priority may legitimately differ.
4. No unconditional claim is made that all other Plant effects now use a single scoring function. Broader audits and O Edelweiss's complete two-step scorer remain separate work.

## Validation

Run:

```bash
./gradlew test --tests '*PetalAcquisitionProjectionTest' --tests '*BattleSappingSnapdragonProjectorTest'
./gradlew test integrationTest simulationTest
```
