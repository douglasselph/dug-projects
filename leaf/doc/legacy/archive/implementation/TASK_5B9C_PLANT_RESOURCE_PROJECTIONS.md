# Task 5B.9C — Plant and resource-state evaluation

## Changes

- Introduce `PlantResourceProjection` as a deterministic, read-only evaluator using the observed `DecisionContext`.
- Butterfly refresh is valued by the number of **spent** Butterflies, and acquisition considers only actually available Grove or opponent butterflies. Stealing a ready Butterfly denies a ready use; stealing a spent one does not. The ensuing refresh is evaluated once.
- Bee acquisition considers Grove availability and individual opponent supply; the actor-side round effect is valued from its **current** Bee count and value, including the acquired Bee. The set-to-4 and stacking-+2 variants remain distinct. Choosing a source shares the resource projection, while its existing tactical Battle denial evaluator remains in place.
- Flipping a Plant now chooses the most promising qualifying Plant rather than multiplying a bonus by the number of spent cards. Reuse of Roots and Vines likewise considers the best qualifying spent card. Opponent Plant denial only counts face-up cards that can lose a current activation opportunity.

## Boundaries and approximations

- Plant-facing opportunity is a bounded proxy based on that Plant's current-phase configured baseline and card cost; **it is not full nested-effect simulation** and avoids recursively scoring every Plant through `CardScoringHelpers`.
- Future Butterfly/Bee utility is a bounded constant, not a claim of exact future usage. Source availability, current resource counts, and already-spent status are exact from the observation.
- Existing whole-card base priorities remain calibrated heuristics. This pass replaces their extra resource/Plant bonuses rather than changing all bases.
- Opponent Plant wounds are not identical to flips; this pass only changes the *scoring proxy*, not legal target selection or effect execution.
- Wisp-specific butterfly gain/refresh variants and next-round opponent-Bee opportunity costs remain candidates for later work.
- No mechanical RNG or game state mutation is performed by the evaluator.

## Validation

Run `./gradlew test --tests '*PlantResourceProjectionTest'` and the complete test suites. Compilation was not confirmed when packaged if Gradle could not resolve a local installation.
