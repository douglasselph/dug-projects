# C1 — Per-player strategy randomness and isolated score ranges

The game already uses `GameConfig.seed` for mechanical game randomness and
`GameConfig.strategySeed` for independent strategy decisions; those streams are
separate and the original decision seed calculation is retained.

C1 centralizes deterministic strategy stream derivation in `StrategySeedStreams`.
Each player now has two distinct, stable streams:

- `DECISIONS`: the existing strategy stream used for learned policies, ties,
  and legacy baseline choices (unchanged seed sequence).
- `SCORE_RANGES`: the Phase B scored Cultivation Main Action Human Baseline,
  including its tie-breaking and sampled score ranges.

Both are seeded from the same optional `strategySeed`, but derive different
seeds without consuming each other's RNG. This preserves unrelated mechanical
RNG consumption when changing how many score ranges are sampled.

The `GameFactory` constructs fresh RNG instances for every player and game.
Changing strategy seed does not change the mechanical stream, provided the
actual game decisions are otherwise identical.

## Limitations

- C1 is **not** event-keyed randomness, round-resetting RNG, or whole-game
  counterfactual replay. A changed action can still alter the mechanical RNG's
  call order, changing later dice and shuffles.
- This does not separate each legacy Human Baseline decision domain from every
  other legacy domain: they continue to share that player's DECISIONS stream.
- When the optional strategy seed is `null`, seeded reproducibility is not
  promised, preserving the existing random default.
- The original baseline and existing simulations remain unchanged unless the
  Phase B scored Human policy is enabled.

## Verification

Run:

    ./gradlew test --tests 'dugsolutions.leaf.v35.player.decision.random.StrategySeedStreamsTest'
    ./gradlew test --tests 'dugsolutions.leaf.v35.game.di.GameFactoryTest'
