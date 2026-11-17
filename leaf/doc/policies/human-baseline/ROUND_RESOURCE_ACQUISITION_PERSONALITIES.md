# Human Baseline — Personalities in Round Resource Acquisition

## What this changes

A player's fixed Resource Personality now influences its **Cultivation Build Main Action** choice when a legal Round effect offers **Water**, **Sunlight**, or **Mulch**. It does not change the effect, its target, die rolling, legal offers, Support spending, Battle decisions, or any learned expert policy.

The default **Neutral** personality uses exactly the historical candidate scores and willingness gates. Existing deterministic Build snapshots remain the compatibility reference for Neutral; other profiles intentionally choose differently near action-value ties.

The new `ResourceAcquisitionPreference` is **not** a pip forecast or a learned calibration weight. It is a transparent, small behavior modifier layered *after* the existing resource scorer. The independent `ImmediatePipValuation`, `FutureDiceForecast`, and read-only `CommonBuildActionValuation` stay unchanged. We must not imply that those read-only calculations have already replaced the legacy Round resource estimates.

## Plain-English decision process

1. The rules engine offers the currently legal actions. A personality cannot create new actions or grant resources that are unavailable.
2. The ordinary resource scorer determines the baseline usefulness: existing Water reserve, upcoming Battle usefulness for Sunlight, and the actual die to Mulch including its current Buy threshold cost.
3. Read the acting player's **own** Water/Sunlight/Mulch inclination and soft holding target from `HumanBaselinePolicy.resourcePersonality`.
4. Neutral: change nothing. Likes and below target: a modest bonus, slightly stronger if multiple desired units are missing. Likes but target already met: small penalty against excess stockpiling. Avoids: modest negative preference, not a prohibition.
5. Leave an action unchanged when its underlying score says no benefit (such as Sunlight after all Battles), or when the Mulch action has no legal Hand die to store.
6. Compare the resulting score to Draw, the other Round slot, and Plant activations using the **existing** selection engine. The change adds no new randomness. A chosen action retains its existing downstream target and effects.

### Magnitude of personality influence

For compatibility with existing `PriorityScore` scoring, these are **temporary behavior-preference priority points**, not additional pips, VP, or a calibration intended to maximize winning:

| Tendency | Preference adjustment |
| --- | ---: |
| Neutral | 0 (exact no-op) |
| Likes, one unit below soft target | +8 |
| Likes, two units below soft target | +10 |
| Likes, three or more below target | At most +12 |
| Likes, target already met | -4 |
| Avoids | -12 |

These points are intentionally bounded; they can change close choices but should not override a clear, strong alternative. The explanation is stored in the existing score-adjustment record, including the current holding and target. No new user-facing numerical personality configuration or training process is added.

**Held amount:** Water and Sunlight use the player's visible token counts. Mulch uses **stored and pending stored dice**, so a pending preparation is not ignored. `LIKE` never requires exactly two tokens, and `AVOID` does not force zero.

**Timing and constraints:** Sunlight cannot be promoted when there are no future Battles. Mulch requires a feasible Hand-die target and retains the existing probabilistic willingness gate; the personality does **not** alter that gate in this changeset. The chosen Mulch die remains the same as the die used by `MulchPriority` to evaluate the action. Water acquisition keeps the existing reserve-aware score, and is only modified when the rules engine actually offers it.

## Per-player examples

- A **Water Keeper** at zero Water is more likely to take a Water Round effect over Draw when the alternatives are close. At two Water, the additional preference fades.
- A **Battle Preparer** leans toward a useful Sunlight token and prepared Mulch, but cannot justify Sunlight when no Battle remains.
- The **Water & Mulch Specialist** likes those resources while tending to pass over Sunlight, even if both Round slots offer legal resources.
- A **Neutral** player keeps the exact pre-change score calculations.

The scorer does not use win rates, expert-policy coefficients, or a global personality setting. Two players with different `HumanBaselinePolicy` instances may favor different legal actions in identical states.

## Testing and current limits

Focused deterministic checks cover neutral invariance; soft target and avoidance effects; no-Battle/no-target safeguards; Water versus Draw; Sunlight versus Draw; Mulch versus Draw (including its willingness gate); and two simultaneous Round slots. These tests assert observable choices, not exact priority constants except the small unit tests specifying the personality modifier itself.

Run:

```bash
./gradlew test \
  --tests '*ResourceAcquisitionPreferenceTest' \
  --tests '*HumanBaselineCultivationStrategyTest' \
  --tests '*ResourcePersonalityTest' \
  --tests '*HumanBaselineResourcePersonalityWiringTest'
```

**This step covers Round resource acquisition only.** It does not adjust Water reroll/Refresh decisions, Mulch spending, Sunlight spending, or resource-producing **Plant effects**. Those require a subsequent spending/reserve step and then shared Plant valuations. The existing legacy scoring framework still contains arbitrary priority bases; migration to common pip-equivalent estimates remains pending and should not be confused with this preference layer.

The behavioral-game cohort will later check *opportunities offered, chosen, acquired, spent, and left unused* rather than win rates or fixed token totals. This changeset itself does not certify those full-game metrics or full Gradle results until the Ubuntu tests pass.
