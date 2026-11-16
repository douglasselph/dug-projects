# Forecast dice across future rounds

**Status (November 16, 2026):** Implemented as a read-only mathematical helper with focused tests; full Gradle acceptance awaits Ubuntu verification. The original four-game Build snapshot passed and remains the frozen reference. **The forecast has not been wired into Build choice selection.**

## Why this exists

The Human Baseline can already calculate that an ordinary D6→D8 upgrade improves the mean of a future roll by **1 pip**, whereas D12→D20 adds **4 expected pips** per future appearance. But the number of *future appearances* depends on the real Supply/Discard cycle and the time remaining. A purchased D20 may arrive far later than a recycled D4. We now calculate when it would appear rather than awarding a fixed benefit just for owning it.

**In plain English:** Start with the dice we actually have. Finish the **current** round without assigning it any new roll value. From the next round onward, draw the lowest-sided die from Supply, refill Supply from Discard only when it becomes empty, and return the just-drawn Hand to Discard only *after* each projected round. Add the mathematical mean of every die drawn. Follow the public future Cultivation/Battle phase schedule rather than inventing future Round card identities.

This is a **function over a game-state snapshot**, not a static score attached to D10, Water, or Compost. In the future, the same function will be evaluated twice: once on the result of choosing Draw and once on the result of choosing Compost or a Plant effect. The difference gives their *future-round* contribution. The current-round consequence is deliberately a separate component (`ImmediatePipValuation`).

## Defined scope and assumptions

- `FutureDiceZones` holds individually identified dice in **Supply, Hand, Discard**, validated as distinct physical identities. `fromBoard` converts the existing immutable `PlayerBoardView`, not a mutable Game. IDs such as `s0`, `h0` and `d0` denote snapshot identities, not permanent game IDs.
- The existing current Hand is returned to Discard at the end of the **current** round. Current-round pips are **never counted** in this forecast, even when the current Hand contains a D20 or has just received a Draw.
- Every **future Cultivation round** models 3 compulsory opening Draws plus an assumed number of optional Main-Action Draws. Supported scenarios: **3** (`OPENING_ONLY`), **4** (`TYPICAL_ONE_EXTRA`, default), or **5** (`DRAW_FOCUSED`). This is a model assumption, *not* a claim that a given future policy will always Draw once.
- Every **future Battle round** models the **3 compulsory opening Draws**. A speculative Battle Main Draw is not assumed. Explicitly known or hypothesized additional Draws can be supplied by future-round number, for either phase; those are declared scenario assumptions, not hidden future effects.
- There is no extra recycle between rounds other than the real Hand→Discard cleanup. When Supply becomes empty *within* a round, Discard can refill it; dice still in that round's Hand cannot be recycled prematurely. If both Supply and Discard are empty, there are no more predicted Draws that round.
- For each predicted Draw, actual expected **spending pips** use `(die sides + 1) / 2`. In addition, `ImmediatePipValuation.roll` provides provisional roll-reward equivalents (+2 / sides for an available roll-1 Critter option, +3 / sides for an available roll-2 Wisp option). Actual spending pips and these strategic approximations remain separate. The caller supplies visible reward availability. We **hold availability fixed** rather than predicting token depletion, immediate Wisp plays or future hand-cap changes; for long games this can overstate rewards.
- The baseline future assumes **no additional acquisitions, losses, later upgrades, Mulch storage/return, special Draw effects, Battle Doom/Trash, or changing action policy**. These are omitted rather than given invented value. The `additionalDrawsByFutureRound` input can model a *specified* extra Draw count, but does not predict its legal target or Battle placement. Higher-level extensions must make future changes explicit or label the projection partial.
- A separate candidate-state transformation supports **buying a die into Discard** and **upgrading a Hand die into Discard**. Their resulting snapshots can be compared, but the code does not assert those actions were legal, claim the current action's value, or create extra current rolls. Actual effects that upgrade-and-use-now will need their distinct state transition when integrated later.
- Actual future rolls are deliberately **not sampled** and no random numbers are consumed. Per-die encounters, first future encounter, per-round face-pip totals, reward equivalents and global totals are deterministic.

## API

`src/main/kotlin/dugsolutions/leaf/v35/player/decision/baseline/projection/FutureDiceForecast.kt`:

- `FutureDiceForecast.fromContext(context, assumption, additionalDrawsByFutureRound)` reads the **public** `context.progress.upcomingRoundTypes` and the observed player board. The upcoming pattern excludes the current revealed round. It never sees hidden upcoming Round card effects.
- `FutureDiceForecast.project(zones, upcomingRounds, assumption, rewards, additionalDrawsByFutureRound)` accepts an explicit future schedule for exact tests and controlled comparisons.
- `FutureDiceZones.withPurchaseIntoDiscard(id, sides)` and `.withHandUpgradeIntoDiscard(id, newSides)` create new snapshots without changing the original or game state.
- `FutureDiceValue.rounds` records actual forecast Draws and their phases; `.byDie` records future-use counts, first future appearance, and totals for each die. `.expectedSpendingPips` is actual pip mean; `.expectedPipEquivalents` includes available provisional rewards.

## Concrete examples that can be independently verified

**3D4 + 3D6, all in Supply, two future Cultivation rounds:**

| Assumed Draws/Cultivation | First future round | Second future round | Total expected pips |
|---|---|---|---:|
| 3 | D4, D4, D4 = 7.5 | D6, D6, D6 = 10.5 | **18.0** |
| 4 | D4, D4, D4, D6 = 11.0 | D6, D6, D4, D4 = 12.0 | **23.0** |
| 5 | D4, D4, D4, D6, D6 = 14.5 | D6, D4, D4, D4, D6 = 14.5 | **29.0** |

The second round can recycle the previous round's Discard only after drawing the still-available Supply dice. These figures exclude roll rewards, future purchases, future card effects and the *current* round.

**Buy a D20 into Discard while three D4s remain in Supply, with future Battle opening Draw 3:**

- Future Battle 1 draws the three D4s: the purchased D20 contributes **zero**.
- Future Battle 2 still draws those three D4s after Supply refills from Discard; the D20 is higher-sided and remains in Supply: it contributes **zero again**.
- Future Battle 3 finally draws the D20, followed by recycled D4s: the D20 contributes **10.5 expected spending pips** at its first future use. This is why “buying a D20” cannot automatically be credited with 10.5 in the next round.

**D12→D20, one future use:** The expected face improves from 6.5 to 10.5, so the *spending-pip* forecast improves by **4**, not 8. When the upgraded die is used twice, the cumulative gain is **8**; when it is never used, **0**. An ordinary D6→D8 gives **1** per subsequent use. Reward equivalents can change slightly in the opposite direction because high-sided dice roll 1 and 2 less frequently.

## Verification and next steps

Run:

```bash
./gradlew test --tests '*FutureDiceForecastTest' --tests '*ImmediatePipValuationTest'
```

The new test suite covers the three Cultivation draw assumptions, future Battle opening draw count, explicit extra Draws, purchased-D20 delay, upgrades, future-only separation, rewards, input validation, and a *real `PlayerDice`* draw/cleanup parity check. These check the forecasting mathematics, **not strategic correctness**.

Next, the **Compare Build actions on a common valuation scale** step must combine `ImmediatePipValue` with **only the difference in future forecasts** for candidate action states. It must not count current pips or the same acquired die twice, must report cases where future actions are speculative, and must not add large unrelated legacy card bases on top. Personalities later provide fixed Water/Sunlight/Mulch preferences rather than learned win-rate weights.
