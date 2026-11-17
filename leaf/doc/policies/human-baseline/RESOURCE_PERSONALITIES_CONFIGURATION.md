# Resource Personalities — Configuration and Wiring

## Purpose

Different Human Baseline players can have stable, recognizable tendencies toward **Water, Sunlight, and Mulch** without trying to learn the weights that maximize their win rate. This is deliberately distinct from expert-policy training.

**This changeset configures those tendencies; it does not yet apply them to actions.** No Water/Mulch/Sunlight priority, Support spending, Build choice, official card effect, or shared pip forecast is modified. Applying the personalities to Round resource acquisition is the **next** task; aligning spending and reserve behavior follows it. A configured personality should currently make the same decisions it would have made under the former neutral baseline, provided its strategy RNG and game state are identical.

## Immutable model

`ResourcePersonality.kt` defines:

- `ResourceInclination`: `LIKE`, `NEUTRAL`, `AVOID`. These are **human tendencies**, not action weights or eligibility rules.
- `ResourceGoal`: one inclination plus a **soft inventory target**. The target is an aim, not a requirement to obtain or retain that many tokens; the scorer must still respect real opportunities, caps, Battle timing and action costs.
- `ResourcePersonality`: three independent resource goals and a stable descriptive identifier. `goal(PersonalityResource)` accesses each setting. `description()` returns all three preferences and targets for future evidence manifests.

Four initial personalities:

| Personality | Water | Sunlight | Mulch |
| --- | --- | --- | --- |
| Neutral | Neutral, aim 1 | Neutral, aim 1 | Neutral, aim 1 |
| Water Keeper | Like, aim 2 | Neutral, aim 1 | Neutral, aim 1 |
| Battle Preparer | Neutral, aim 1 | Like, aim 2 | Like, aim 2 |
| Water & Mulch, no Sunlight | Like, aim 2 | Avoid, aim 0 | Like, aim 2 |

These aims are provisional *descriptions* that make later behavior testable, not a claim that the player will always carry exactly 0/1/2 resources. The neutral profile's goals do **not** replace existing neutral scoring/reserve constants in this step.

## Per-player wiring

Each player's `DecisionDirector` already owns a separate `HumanBaselinePolicy` injected through the canonical factory. Add the immutable profile to that policy:

```kotlin
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.baseline.ResourcePersonality

val waterPlayer = DecisionDirector.humanBaseline(
    policy = HumanBaselinePolicy(resourcePersonality = ResourcePersonality.WATER_KEEPER)
)
val battlePlayer = DecisionDirector.humanBaseline(
    policy = HumanBaselinePolicy(resourcePersonality = ResourcePersonality.BATTLE_PREPARER)
)
```

The director already shares its policy instance with the Cultivation, Battle, Buy and effect strategies. No global mutable personality setting is introduced. Learned expert directors, Mechanical Control, and the mathematical value helpers do not read this new property and remain untouched.

A custom profile can vary one dimension without changing the others:

```kotlin
val custom = ResourcePersonality.NEUTRAL.copy(
    id = "water-avoider",
    water = ResourceGoal(ResourceInclination.AVOID, softTarget = 0)
)
```

The reader or simulator must explicitly pass this custom profile to that player's policy. The `id` and `description()` provide stable labels for later logs; no JSON or database persistence is introduced here.

## Tests and acceptance

- All four named profiles have the documented independent axes; negative targets and invalid IDs fail fast.
- A custom Water preference does not change Sunlight/Mulch settings or mutate the shared named profile.
- Two players' directors hold different policy objects and preserve different profiles; every strategy in a director sees the same policy object.
- Default `HumanBaselinePolicy()` remains `ResourcePersonality.NEUTRAL`; **no decision behavior change** is expected until resource scorer integration.

Run from repository root:

```bash
./gradlew test \
  --tests '*ResourcePersonalityTest' \
  --tests '*HumanBaselineResourcePersonalityWiringTest'
```

**Verification status:** The standalone Kotlin personality model compiles in this environment. Full Gradle compilation and the new JUnit tests still require an Ubuntu run. The most recent uploaded archive was truncated after most `src/main` and `src/simulation` content; the new tests are packaged as independent files and should be validated in the user's complete local checkout.

## Future implementation constraints

In the *acquisition* step, apply the profile as a small, explainable tie/near-tie preference after the common pip calculation and legal-target verification; no profile may cause an illegal or clearly pointless action. Preserve remaining-Battle timing for Sunlight, die/Buy tradeoffs for Mulch, and Water opportunities/cap. In the *spending* step, use compatible reserve logic but permit valuable use. Behavioral acceptance will be based on **resource opportunities offered versus chosen and actually used**, never on winning more games.
