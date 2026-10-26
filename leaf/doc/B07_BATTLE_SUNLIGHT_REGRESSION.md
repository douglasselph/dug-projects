# B07 — Battle Sunlight extra-Main regression

Scope: test-only changes to BattleActionCoordinatorTest. No gameplay, policy, CSV, launcher, or weight changes.

The existing tests already exercised a single Sunlight-funded Main, two Sunlights, an unspent Sunlight, legal action selection, and ordinary Support before/after a Sunlight action. B07 strengthens them with precise chronicle-stage and event-order assertions:

- FIRST Main precedes Sunlight Support and its funded Main.
- Ordinary Critter Support remains possible **after** the Sunlight-funded Main and **before** FINAL Main.
- FIRST and FINAL are each preserved; Sunlight is separately classified as SUNLIGHT, even when two tokens are spent.
- Sunlight transfers from a player's supply back to the shared Grove exactly once per expenditure, with conservation asserted.
- Interleaved Bee and Worm Support actions remain legal around a Sunlight Support.

This suite exercises the real BattleActionCoordinator, SupportActionExecutor, and SunlightTokenResolver together, with mocked Round-effect execution to isolate action sequencing. It does not change how a Sunlight token is acquired or the GameEffect enum.

Run on Ubuntu:

```
./gradlew test --tests '*BattleActionCoordinatorTest' --tests '*SunlightTokenResolverTest'
./gradlew test simulationTest
python3 bin/a03_verify_boundaries.py
```
