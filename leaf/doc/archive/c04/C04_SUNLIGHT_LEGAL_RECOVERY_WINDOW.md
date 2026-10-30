# C04 — Observable legal recovery after Sunlight-funded Main

The Battle turn request supplies legal `BattleTurnAction.Support` and `FinalMain` candidates. This change threads those exact lists into Sunlight's *funded Main* valuation for `GAIN_TWO_WORMS` and `REFRESH_CREATURE`. The orchestrator eligibility gate, final choice score, and choice diagnostics all use the same legality-aware projection.

* The fact that a Strike row is open or a Plant is face-down is **not** by itself proof that a follow-up Support/Main is legally offered now.
* For a funded Worm gain, an existing legal Worm placement/flip is confirmation of a potential Support window. For funded refresh, a currently legal Plant activation indicates potential Main follow-up. Neither is proof that the *newly obtained* resource/refresh can be spent immediately; the valuation remains conservative and provisional.
* If those legal follow-ups are not on the request, the immediate funded recovery valuation is zero, even if board resources look tempting. A newly unlocked action would require a fresh legal-action request after the funded Main resolves. Other action families retain their existing evaluation.
* `BattleRecoveryResourceUtility.finalMain` continues to reject immediate Support after Final Main. No canonical stage order or legality code was modified.

## Test and sign-off

```
./gradlew test --tests '*C04SunlightRecoveryLegalWindowTest' --tests '*BattleTurnOrchestratorTest' --tests '*SunlightBattleSpendingValueTest'
./gradlew test simulationTest
python3 bin/a03_verify_boundaries.py
```

This is a legality-aware conservative bound, **not full C04 sign-off**. Required next: prove post-funded-Main newly generated legal options, extend same to non-Round Plant recoveries, and mixed multiplayer selection tests. Human Baseline scoring only; no canonical rules or frozen learned policy changes.
