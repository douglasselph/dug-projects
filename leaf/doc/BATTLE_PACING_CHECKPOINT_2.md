# Battle pacing — Checkpoint 2 experimental execution (October 30, 2026)

## Scope and claims

This patch supplies a **research-only Candidate B execution path**, reusable canonical action/effect execution, a paired one-Battle comparison entry point, and regression cases. It **does not** switch `RoundCoordinator` or `BattleRound.executeRound` to Candidate B. It does **not** calibrate C04, run a batch, or claim a game-design outcome.

**Local compile/test status:** Gradle offline was blocked *before* Kotlin compilation by missing Compose, coroutines, Koin and Kotlin JARs. Thus the code and its regression cases are **not yet verified end-to-end**. Run tests on the developer machine before using results. Failures in edge-case card interactions are fidelity blockers, **not evidence about pacing/balance**.

## What is implemented

- `ThreeEngagementBattle`: High → Middle → Low, recompute frozen starter/clockwise order at each Strike, one turn opportunity per player, zero or one normal Main Action per Strike and at most **two** Supports per Strike; **two normal Main Actions total**, including when there are no legal normal actions. Sunlight is a Support and uses the canonical Sunlight-funded Main executor, not a normal token. Main/Support may occur in either order; a player may Pass at any point. Unspent allowances do not carry over.
- Each row is resolved through production `StrikeResolver.resolveRow` including immediate Flip-or-Snip Wounds, VP and shared tie rules, then locked. A row does not resolve again.
- `Die` blocks value/roll changes while locked. `BattleGrid` prevents changing closed rows by the normal place/remove/swap/replace/move/committed-Critter operations; Doom and Cleanup are permitted **only after** all three rows have closed (`beginPostBattleRemoval`), which unlocks reclaimed dice for subsequent rounds. `BattleState.lockResolvedRow` freezes the scored Critter values and row totals so later round-wide boosts cannot retroactively change a settled Strike. `LockedStrikeEvidence` verifies the identity, displayed value, and Critters of each closed row after every later action. If an effect still attempts an invalid selection/mutation, fail the experiment explicitly instead of recording invalid outcomes.
- Mass and targeted ordinary die Raise/Set effects in `DieValueEffectHandler` use open-row-only targeting when a Battle grid is present. Wispquake omits closed-row dice from its keep/reroll choices.
- Reuses production `BattleActionCoordinator`'s legal action generation/executors via narrowly named `research*` methods and `BattleRound.researchActionCoordinator` (the existing scheduling and production `executeRound` remain unchanged).
- Candidate B requires **Wisp's Resolve / Last Word absent from the game deck and held Wisps**. `BattlePacingWispDeck.withoutResolve(...)` prepares an exact, seed-deterministic reduced deck before game construction. **Both** paired control and variant receive that exact deck. Consequently the comparison is A-vs-B **conditional on a deck with Resolve excluded**, not the unmodified full v37.03 deck.
- `PairedBattleOpeningReplay.create(..., exactWispCards=...)` creates two independent, paired legal pre-Battle states, and `PairedBattleComparison.run(...)` executes baseline A and experimental B from those states. `ThreeEngagementBattleResult.trace` provides action/order/result evidence. This is a one-Battle mechanical experiment, not statistical inference.
- New test class exercises early/middle/final pairings and core locking; still requires real Gradle execution.

## Research policy caveat

Candidate B needs an explicit **Pass** and must allow actions before/after Main and Support. Canonical Battle's `BattleTurnAction.FinalMain` means **permanently stop Supports**, so blindly reusing it as an Engagement Pass changes behavior. `EngagementPolicy` is consequently a separate research seam. This checkpoint provides **MechanicalEngagementPolicy** (use one ordinary Main, then Pass) and **SupportSpendingEngagementPolicy** (use a normal Main, then preferentially spend Critters in current Strike). Neither is calibrated Human Baseline or a balance-quality tactical policy; **do not interpret A/B VP differences as causal or player-equivalent yet**. Checkpoint 3 needs matched/credible A and B policies plus explicit downgrade/upgrade cases.

## Fidelity caveats / remaining gate work

1. Some non-die-value handlers operate on raw Hand lists or do mandatory multi-die swaps/removals. Core locks **prevent** an illegal final outcome, but an effect whose legal behavior would be to apply only to open rows may currently fail fast instead of applying a narrowed partial effect. Audit **DrawEffectHandler**, Transplant Tulip, Pollen Theft, Root & Scoot, Queen's Blossom and mixed chains under realistic late-Battle states. Treat any such exception as a fidelity failure requiring an effect-specific test; **do not skip or silently count the failed state as a battle result**.
2. Closed Critters and dice remain visible in `BattleGrid` through Doom; the post-Battle drain phase is a lifecycle exception for removal, not permission for a fourth Engagement to modify them.
3. `PairedBattleOpeningReplay` verifies physically observable state fingerprint, not full RNG engine cursor equivalence. Event-keyed die-rolls and shared percentile option are available; divergence after the first action is expected.
4. Mechanical Control's conservative candidate policy rarely spends Supports; resource-hoarding and slow-support pacing cannot be evaluated using this policy. C04 detailed calibration stays out of scope.
5. A03: eight canonical engine source files were **intentionally** changed solely to expose research action reuse and enforce immutable resolved Strike rows. Review the source diff and updated manifest before committing; A03 proves boundaries, not legality or run results.

## Installation and verification (ZIP in ~/Downloads)

```bash
unzip -o ~/Downloads/leaf_battle_pacing_checkpoint2.zip -d ~/src/dug-projects/leaf
cd ~/src/dug-projects/leaf
python3 bin/a03_verify_boundaries.py
python3 -m unittest discover -s bin/tests -p 'test_a03_boundaries.py' -v
./gradlew test --tests '*BattleGridTest' --tests '*DieValueBattleRowEffectsTest' --tests '*WispquakeEffectTest' --tests '*BattleRoundTest'
./gradlew simulationTest --tests '*EngagementOrderSelectorTest' --tests '*PairedBattleOpeningReplayTest' --tests '*ThreeEngagementBattleTest'
```

If the `--tests` option is not supported in your `simulationTest` task, run `./gradlew simulationTest` instead. The run should be marked **not yet acceptance-tested** until these checks pass on the full project. In particular, investigate a failing card-effect/swap test rather than using the simulator to claim a game-design difference.
