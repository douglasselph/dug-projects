# C04 — Sunlight-funded Main continuation integration

Adds `execute_sunlightFundedWormGain_regeneratesLegalSupportAndConsultsHumanBaseline` to the existing real Battle coordinator tests.

The test forces a legal Sunlight-funded second Round effect that supplies two Worms, using the existing recording effect executor. The **first** post-First-Main request cannot contain Worm placements. After the funded Main resolves, the coordinator must regenerate legal Support choices including a newly available Worm placement. All subsequent `chooseTurnAction` calls are delegated to the actual `HumanBaselineBattleStrategy`, and the returned choices must belong to each freshly generated legal list. It also checks first/sunlit/final Main stage sequence and Sunlight supply conservation.

The test does not assert that the Human Baseline must spend a newly gained Worm: doing so requires a positive tactical opportunity. It tests recomputation and the real continuation response, not an artificial forced placement. Further realistic states can test the valuation of post-funding Worm placements.

Verify:

```
./gradlew test --tests '*BattleActionCoordinatorTest'
./gradlew test simulationTest
python3 bin/a03_verify_boundaries.py
```

No production rule, learned policy, card data or research manifest changed. Kotlin tests could not execute in the delivery environment because the Gradle wrapper could not download its distribution.
