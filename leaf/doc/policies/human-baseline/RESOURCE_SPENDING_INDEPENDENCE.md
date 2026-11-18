# Resource Spending — Shared Tactical Decisions

## Design decision

A Human Baseline **personality controls a preference for acquiring** Water, Sunlight, or Mulch. It does **not** change the score, continuation decision, or preservation threshold when spending the same resource in the same state. An individual player may own different resources because of earlier choices, but if two players have identical decision contexts, legal offers and strategy seeds, the shared spending logic must agree.

Do not merge an earlier patch that adds a personality-based holding/spending bonus. Shared context-sensitive reserve and future-Battle considerations are **not** personality preferences and remain part of the tactical evaluator.

## November 18 source audit

- `HumanBaselinePolicy.resourcePersonality` is an immutable per-player configuration. `HumanBaselineCultivationStrategy` reads it in **Round resource acquisition scoring**, via `ResourceAcquisitionPreference`.
- Cultivation `CultivationSupportPriority` scores Water reroll, Water Refresh and spending stored Mulch using the actual offered action and the **common** protected-reserve policy. No personality is read.
- Battle `BattleDirectSupportAnalyzer` enforces the Water positive-transition and Mulch WIN_FLIPPED tactical gates; `BattleSupportPriority` and `BattleTurnOrchestrator` reuse these gates.
- `SunlightBattleSpendingValue` compares a funded Main Action with preserving Sunlight for a future Battle; its continuation and score are shared across personalities.
- Physical token expenditure, legal actions and Support/Main Action limits remain engine responsibilities and are not changed here.

## Regression contract

`ResourceSpendingPersonalityIndependenceTest` calls **real Human Baseline Cultivation and Battle strategy selectors** for all four named profiles with the same decision state, legal choices and fixed strategy seed. It also compares the independent tactical scores and premium-resource gates. Cases include:

| Spending opportunity | Expected shared outcome |
| --- | --- |
| Cultivation Water reroll of D20 showing 1 | Use when its immediate benefit beats Done |
| Cultivation Water reroll of D4 showing 4 | Preserve resource when reroll is weak |
| Cultivation Water Refresh with spent Butterflies | Use when sufficient benefit can be recovered |
| Cultivation Water Refresh with nothing spent | Preserve Water |
| Cultivation Mulch D12 with another stored token | Spend worthwhile surplus Mulch |
| Cultivation Mulch D12 with only one token | Preserve scarce Mulch |
| Battle Water reroll, transition flips loss | Pass the shared tactical gate |
| Battle Water reroll, row already won | Fail the positive-transition gate |
| Battle Mulch, expected die flips a Strike | Pass the shared WIN_FLIPPED gate |
| Battle Mulch on an already-won Strike | Fail the WIN_FLIPPED gate |
| Battle Sunlight, scarce, marginal early extra Main | Save for a later Battle |
| Battle Sunlight, surplus early / final Battle | Spend on a worthwhile extra Main |

The tests are **not** competitive win-rate tests, and don't require identical full-game trajectories: different personalities acquire resources differently, so full games will naturally diverge.

## Ubuntu verification

From the root of `leaf`:

```bash
./gradlew test \
  --tests '*ResourceSpendingPersonalityIndependenceTest' \
  --tests '*CultivationSupportPriorityTest' \
  --tests '*BattleSupportPriorityTest' \
  --tests '*BattleTurnOrchestratorTest' \
  --tests '*SunlightBattleSpendingValueTest' \
  --tests '*ResourceAcquisitionPreferenceTest' \
  --tests '*HumanBaselineResourcePersonalityWiringTest' \
  --tests '*SupportActionExecutorTest' \
  --tests '*SupportActionExecutorBattleTest' \
  --tests '*SunlightTokenResolverTest' \
  --tests '*BattleSupportActionCostTest' \
  --tests '*CultivationBuildCoordinatorTest'
```

The new regression protects score and action **choices**, not physical token movement. The existing executor/coordinator cases included above cover the game-rule side of resource expenditure, action caps and legality. The later personality behavioral observation program will measure real acquisition, use and ending inventories across seeded games. Do not mark the task complete until the Ubuntu regression passes.
