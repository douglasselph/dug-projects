# Human Baseline Build valuation and resource personalities — implementation plan

**Status:** DESIGN / IMPLEMENTATION BACKLOG, 2026-11-16. **No Kotlin behavior change is claimed by this document.**

**Source audited:** `leaf(20261116-201417).tgz`, current four CSVs in `data/`, and `src/main/kotlin/dugsolutions/leaf/v35/...`. This plan supersedes the *proposed* November 13 review-first / Root Four More fixed-base retuning approach for the **new** workstream; it does not erase previously certified mechanics or historical results.

**Objective:** Human Baseline is a family of **plausible, explainable human playing styles**. It is **not** an optimizer of win rate. Learned/expert policies remain the independent mechanism for finding effective weights, strong strategies, and counterstrategies. Share mathematically grounded action facts across Human Baseline personalities; make **acquiring, keeping, and spending Water, Sunlight, and Mulch** express intentionally different personalities. Extend the shared calculations to straightforward Plant activation effects wherever justified, without changing their official gameplay effects.

## 1. Findings from the November 16 source audit

### Decision path — and what is already available

1. `CultivationBuildCoordinator` creates legal Build candidates; `HumanBaselineCultivationStrategy.chooseAction` ranks Draw, activated Plant, and the **two actual current Round effect** choices along with Supports/Done. Two Main Actions are required. The rules engine, not the baseline, owns legality and mutation.
2. `DrawPriority` uses `CultivationPipOutcome.nextDraw` and `ExpectedDrawProjection.drawSides`: lowest-sided available die, refill Supply from Discard on exhaustion, expected roll `(sides+1)/2`. It converts Draw to legacy priority by `LegacyPreferenceBridge` **35 + 4 × expected pips**. Its `rollRewardChance` is diagnostic only: **Critter/Wisp strategic equivalents are not included in its score**.
3. `CardScoringHelpers.playScore` already projects **actual capped** Raise, Double, Flip, Set-to-Max, several grouped Raises; expected rerolls/draws; some purchase-tier improvements. `SharedDieEffectOutcome`, `DieValueHeuristics`, `ExpectedDrawProjection`, `CultivationPipOutcome`, and `PurchaseThresholdHeuristics` are reusable. However, card-local `ConfiguredCardScorer` cultivation bases still compete on the legacy priority scale; the code does **not** derive every candidate's value from the same outcome metric. For example, Root Four More carries a base of **58**, before its +4 Raise projection. Queen's Blossom has a base of **82** plus its projected Draw benefit.
4. `CompostPriority` already considers the available next die, current Hand die lost, Buy thresholds, development deficit and remaining Cultivation rounds; its **75 base**, **2 priority/upgrade side**, capped **2 priority/remaining Cultivation round** and probabilistic willingness gate do **not** forecast the upgraded die's expected future encounters. Critically, the D12→D20 *expected roll* improvement is **+4**, while normal two-sided upgrades are **+1**; don't use the difference in maximum sides as future expected pips.
5. `WaterPriority` currently uses only inventory (`30 + 20` for first, `30 + 10` for second, then `30 - 15`). `SunlightTokenPriority` provisionally assumes roughly **4 future-action pip-equivalents**, adjusts for Battle timing and inventory, then converts to legacy priority. `MulchPriority` uses a **45 base** plus a preferred Hand target, face/size, timing, reserve and current Buy-threshold adjustments. Legacy Cultivation also **randomly gates Compost and Mulch** (`usePercentage`, `mulchUsePercentage`) *before* final scoring. These are human-style heuristics already, but **not explicit, injectable resource personalities** and not unified with time-adjusted valuation.
6. `CultivationSupportPriority` already estimates Water reroll gain, spent-Plant/Butterfly Refresh opportunity, and Mulch use; Battle Sunlight spending has a dedicated `SunlightBattleSpendingValue`. These are **spending** decisions, separate from Cultivation **acquisition**, and are not yet coupled to a consistent personality's reserve/usage goals.
7. `HumanBaselinePolicy` has injectable reserve values and behavior probabilities; `ResourceReserveHeuristics` has reserve targets; `HumanBaselineDecisionDirector` shares one policy with its strategies. These are good insertion seams. The default director does **not** select a named/composable resource personality. `HumanBaselineCultivationStrategy`'s optional `modernScoring` path is not used by the default director, so do not assume that path's experimental influences are the current production baseline.
8. `ActionValue` and `PreferenceUnit` distinguish legacy points from expected pips; the latter is **partially** implemented (`C01–C05`). This is a strong type/diagnostic seam, **not** a completed common Build valuation engine. `BaselineScoreEngine` chooses the top modified priority after semantic card influences; profile effects must be observable in final candidate comparisons, not just in a preliminary resource scorer.
9. `PlayerDice.draw()` and `ExpectedDrawProjection.drawSides()` expose the actual deterministic type order. `DecisionContext` exposes Supply, Hand, Discard, tokens, available Plants, partial current-round view and future round **types**. These are enough to build a read-only forward projection with no hidden information and no changes to the game's random stream.
10. The 36 official Plant IDs all have scorer registrations, **but registration is not mathematical valuation coverage**. Several file/class names are historical (`TransplantTulipBaseline.kt` implements official Someday Sunflower); use canonical **CSV IDs and current `GameEffect`**, not filenames or historical inventory CSV. Verify scorer/current-card identity as part of NC-24 compatibility auditing.

**Distance from the desired architecture:** Near for deterministic *immediate* die arithmetic and target-agnostic utilities; **partial** for shared card effects, reward valuation, reserves and legality-aware target projections; **far/not implemented** for future-only dice lifecycle value, one common comparable pip-equivalent Build score, a named/composable personality model, opportunity-conditioned personality tests, and full-game behavioral telemetry. This is a **targeted refactor of the Human Baseline decision layer**, not a new game engine or a new learning algorithm.

### Existing files to reuse rather than duplicate

| Responsibility | Existing production code | Gap / intended change |
| --- | --- | --- |
| Build candidate selection | `.../baseline/cultivation/HumanBaselineCultivationStrategy.kt` | Add one path for comparing typed consequences; profile applied after common estimate; preserve legacy flag/fallback |
| Dice arithmetic / single-target effects | `.../baseline/common/DieValueHeuristics.kt`; `.../baseline/projection/{CultivationPipOutcome,SharedDieEffectOutcome,ExpectedDrawProjection}.kt` | Reuse; include eligible roll rewards and **future** state deltas; no independent cloned effect maths |
| Future dice positions | `.../player/dice/PlayerDice.kt`; `.../baseline/context/HumanBaselineContextValues.kt` (`FutureDiceAvailability`, `DicePoolQuality`) | Build a multi-round **forecast**; current helpers largely cover *next draw/recycle distance* |
| Resources / reserve | `.../baseline/cultivation/resource/{WaterPriority,SunlightTokenPriority,MulchPriority,CompostPriority}.kt`; `.../baseline/common/ResourceReserveHeuristics.kt` | Separate common utility from **personality preference** and from legal/actual spend decisions |
| Plant play values | `.../baseline/card/CardScoringHelpers.kt` and 36 `.../baseline/card/plant/**Baseline.kt` files | Migrate bounded, objectively measurable effect families; remove migrated legacy per-card base domination |
| Score types / diagnostics | `.../baseline/scoring/{ActionValue,PreferenceUnits,BaselineScoreEngine}.kt`; `.../player/decision/trace/` | Explain source of benefit, immediate/future separation, option/reward value and final personality adjustment |
| Wiring / test harness | `.../baseline/{HumanBaselinePolicy,HumanBaselineDecisionDirector}.kt`; `src/test/.../baseline/cultivation/`; `src/simulationTest/...` | Per-player immutable profile; deterministic tests and opportunity-conditioned cohort tests |

## 2. Design contracts — what the common engine should mean

### Two distinct accounting periods

- **Current decision / current round**: represent the immediate change in expected usable pips, current already rolled face changes, current Buy thresholds, action-slot use, and *current* resource movements. For an actual Raise, use realized uncapped headroom; for Draw, use the next *known die size* and its expected roll; for Upgrade-and-use-now, use expected new roll minus the removed current face.
- **Strictly future rounds**: a separate *read-only state valuation function*. Begin **after the current round**, after applying legal actions and cleanup/zone transitions. Follow the public remaining 3/2/2 phase schedule (7 Cultivation + 3 Battle in the standard full game); no hidden Round face or future roll peek. Do not count an immediate Draw or upgrade in both terms. A purchased die in Discard might contribute zero if it won't recycle/draw again.
- For future rolls, arithmetic means are the starting point: `E(Ds) = (s+1)/2`; normal upgrades D4→D6→D8→D10→D12 each give **+1 mean per subsequent use** and D12→D20 gives **+4 mean**. Multiply by the **forecast expected number of future uses**, not by rounds remaining directly.
- Future-roll *reward-adjusted* estimate is a separate output: when allowed and obtainable, add provisional **Critter = 2 pip-equivalents** for a 1 and **Wisp = 3** for a 2, each with probability `1/sides` (example D10 Cultivation: `5.5 + 2/10 + 3/10 = 6.0`). Do not award speculative rewards on a Raise or on no-reward rolls; handle phase, Wisp cap, availability and actual eligibility; do not silently equate nominal pip-equivalents with purchasable pips.
- Explicitly distinguish **immediate owned-die access** from **permanent future ownership**. A current Draw uses an already owned die; it does not buy/add a die. A new die purchased to Discard increases the cycle length as well as quality. Existing Hand/Discard/Supply locations and lowest-sided-first order matter.

### First forecasting model: fixed, explainable behavior, not an optimizer

- Use a **scenario assumption** of opening three dice and **one extra normal Draw per future Cultivation round on average**, hence roughly four future dice drawn per Cultivation. Also calculate **three and five draws per Cultivation** as sensitivity bounds. Battle opening draws are modeled separately (three in the initial baseline), not as two optional Build Draws; special effects may override these counts when known.
- Compute each scenario by **walking the actual ordered Supply/Discard cycle** and applying a documented cleanup approximation. Track whether each physical die is seen and how many times; treat Dice Hand, purchased dice entering Discard, upgrading a Hand die to Discard, and Mulch storage correctly. Estimate future rolls without consuming RNG or changing game state.
- Keep unavailable future card/round effects out of the first *deterministic* baseline. Later add an explicit bounded/expected extra-Draw rate reflecting **currently owned/ready** effects (Queen's Blossom is a key regression) and available Main Actions; don't pretend the extra Draw is free when it consumes one of two Main Actions.
- Compare alternatives **from the same actual decision state**, applying exactly one proposed legal action to a *projection* (never mutate the game). Do not assume four future Draws for both choices and also add an extra Draw for the Draw choice. For current-round purchase or Battle threshold value, add separate transparent consequences or bounded proxies—do not double count the same purchased die in future inventory and a second arbitrary acquisition bonus.
- Output an explainable breakdown: `immediateRollPips`, `futureRawExpectedPips`, `futureRewardEquivalent`, `currentBuyOpportunity`, `futureActionOptionValue`, `personalityPreference`, `assumptionRange`, `legality/target`, `unsupportedConsequences`. Not all fields are measured in the same units until explicitly converted; **fail closed** or keep legacy for unsupported comparisons.
- For a small supply (e.g. three D4 + three D6) enumerate the next N future draws by rule order and assert exact die-size sequence and the resulting expected total. Test upgrading D4→D6 vs adding a purchased D8, placement in Hand vs Discard, empty zones, late purchase, and D12→D20 before projecting full-game outcomes.

### Resource personalities: coherent behavior, not learned strategic optimization

Introduce an immutable `HumanBaselineResourcePersonality` or similarly named value object configured **per player**. It expresses **independent** Water, Sunlight, and Mulch preferences so combinations such as **likes Water and Mulch; avoids Sunlight** are possible. A useful starting schema:

| Property | Meaning | Initial policy decision (provisional) |
| --- | --- | --- |
| `water` | `AVOID`, `NEUTRAL`, `LIKE`; soft target inventory (e.g. 0 / 1 / 1–2) | Encourages acquiring first/second Water and preserving a useful reserve; does not force impossible token gains |
| `sunlight` | Same three preferences and 0 / 1 / 1–2 target | A preference for **having** a Battle Main Action available, not an obsolete Raise +3 |
| `mulch` | Same three preferences and 0 / 1 / 1–2 prepared dice target | Acquisition/target/spending preference must respect the committed Hand die and pending token rules |
| `personalityId` | Stable explicit label for reports | Neutral default plus a small, **fixed** matrix of distinct profiles; no optimization by win rate |

- Prefer **soft reserve / tie-and-near-tie influence**, **diminishing marginal desire** after target, and hard checks for legal actions and availability. `AVOID` means *rarely proactively acquire*, not *refuse an overwhelmingly good free resource*; `LIKE` must not force a useless activation/stockpile forever. How near is "near" is an **explainable behavior rule** or modest preference band, not a value learned to maximize win share.
- Distinguish **acquisition**, **holding** and **spending**. Water can reroll or Refresh; Sunlight must have a legal worthwhile Battle Main Action; Mulch stores an actual die and needs a future legal use. Personalities describe the desired stock/propensity, while **shared math** judges obvious consequences (e.g. no future Battle means acquiring Sunlight has no useful option). Keep current reserve guards, timing, and target alignment unless deliberately replaced and tested.
- Start with four exemplar configurations: `NEUTRAL`, `WATER_KEEPER`, `BATTLE_PREPARER` (Mulch + Sunlight), and `WATER_MULCH_NO_SUNLIGHT` (the user's explicit mixed preference). Add resource-minimalist only if it adds testable variety. All profiles use the **same** common valuation engine, never different game rules.
- Seed and attach personality in `HumanBaselineDecisionDirector`/`HumanBaselinePolicy` (or a new immutable companion configuration), scoped to one player's director. No global mutable profile, CSV edits, pretrained Buy weights, or side effects on learned/frozen-policy decisions. Record `personalityId` and assumption version in research provenance.
- **Do not** search for personality weights with 1,000-game win-rate optimization. A personality acceptance test asks whether it **acts like its profile**, not whether it wins. The learned/expert policies independently optimize for winning.

## 3. Current Plant-effect valuation audit (36 official definitions)

This table classifies what can reasonably share the new valuation; it is **not** a claim that each card's existing conditional scorer or target selection is correct. All IDs/names are from the **current official November 16 CSV**, stripping only the historical `z` sorting prefix from Flower IDs. `E` = early shared mathematical migration; `M` = mixed/resource/conditional, migrate after base; `L` = later strategic/complex. All currently have a registered scorer with **legacy bases**, even when the scorer adds valid math.

| Current card ID | Current title | Build valuation direction | Priority |
| --- | --- | --- | --- |
| Root_05_01 | Root Double Down | Actual capped doubled-die gain vs next Draw | E |
| Root_05_02 | Root Four More | Actual capped +4, not base 58; opportunity cost vs Draw | E |
| Root_05_03 | Root on a Roll | Expected reroll-until-3+ (no rewards) | E |
| Root_05_04 | Root Well | Gain Water under personality; special Battle reroll later | M |
| Root_07_01 | Root Awakening | Upgrade/use-now expected roll plus **future lifetime** replacement | M |
| Root_07_02 | Root Appreciation | Worm/reward bonus and free Battle support; non-pip option | M |
| Root_07_03 | Root Down Payment | Printed VP from showing 1s, not raw pip gain | L |
| Root_07_04 | Root Loot | Move die from Discard to Mulch; timing/access forecast | M |
| Root_09_01 | Root & Scoot | +1 immediate in Cultivation; special Battle withdrawal separately | E / L Battle |
| Root_09_02 | Root Cause | Actual opposite-face gain, possibly negative | E |
| Root_09_03 | Root Kindred | Best legal match target and signed face-value change | E |
| Root_09_04 | Root Recall | Net expected Discard/redraw in Cultivation; Battle reroll separately | M |
| Vine_07_01 | Shift Happens | Plant facing/Refresh; Battle opponent denial separately | L |
| Vine_07_02 | Vine and Again | Reuse spent Root/Vine's projected consequence (recursive guard) | L |
| Vine_07_03 | Vine and Punishment | Expected reroll plus conditional Draw on 3; Battle row denial separately | M |
| Vine_07_04 | Vine Yield | +1 per Flower, best legal die and cap | E |
| Vine_09_01 | Low & Behold | Lowest-showing die set to maximum | E |
| Vine_09_02 | Parting Thorn | Flip/Refresh opportunity; Battle wound separate | L |
| Vine_09_03 | Saplink Trellis | Distinct legal Raise targets; sum of actual capped gains | E |
| Vine_09_04 | Vine and Dine | +5 capped Raise minus spent Critter option value | M |
| Vine_11_01 | Reap What You Roll | Discard one showing die, Draw two in order; reward eligibility | M |
| Vine_11_02 | Berry Tasty | All D8s +2, actual headroom | E |
| Vine_11_03 | Rise and Vine | All eligible dice +2, actual headroom | E |
| Vine_11_04 | Vine's the Limit | Best eligible die ≤D12 set to maximum | E |
| Flower_11_01 | Alluring Nectar | Butterfly acquisition/refresh and Battle free-Support option | M |
| Flower_11_02 | Bloom Backbone | All dice +1 per Vine/Flower; cap/eligibility | E |
| Flower_11_03 | Sapping Snapdragon | +2 in Cultivation; opposing-dice Battle effect separate | E / L Battle |
| Flower_11_04 | Someday Sunflower | Gain **two** Sunlight tokens, if Grove stock allows; personality | M |
| Flower_14_01 | Bee-loved Bloom | Bee acquisition/boost and free Battle Support | M |
| Flower_14_02 | Bloom Backflip | Flip own die in Cultivation; opponent in Battle | E / L Battle |
| Flower_14_03 | Gust of Petals | Expected reroll; Battle opponent consequences separately | E / L Battle |
| Flower_14_04 | Petal To Die 4 | Gain/use D4, then choose D4 discard/draw cycles; current/future split | M |
| Flower_17_01 | Bursting Blossom | +1 Raise plus conditional Draw for dice showing max | M |
| Flower_17_02 | Forget-Me-Not | Advance specific Discard die use; forecast access timing | M |
| Flower_17_03 | O Edelweiss | Two other Plant plays/flips; compounding choice/Refresh | L |
| Flower_17_04 | Queen's Blossom | Two consecutive next-available Draws; action-slot advantage | E |

**Order:** migrate the `E` **Cultivation** effects first, starting with Root Four More + Queen's Blossom + a grouped Raise and a reroll, then cover the remaining `E` set. `M` gets explicit resource, future-time, and target-cost extensions; `L` stays in its scoped legacy scorer until separately designed. Battle scores are **not** automatically the same as Cultivation pip scores. Where an effect is partially shared, **split the common pip component from phase-specific consequences**. No card should be declared "pips only" based on the table without checking its actual current legal/target/reward semantics.

## 4. Test and measurement architecture

### Mathematical tests: deterministic, no games needed

- Die-roll mean, reward eligibility/availability/cap, upgraded mean (+1 / +4), signed cap/headroom, actual `Supply -> Discard -> Supply` ordering, exact future-draw sequence and new-die latency.
- Forecast strict **future-round-only** boundary; prove a current action isn't added twice. Verify expected life of a new purchased D20 can be zero when too late, and reward-adjusted raw vs pip-equivalent totals stay separate.
- One source of truth for target/outcome: predicted highest-value target corresponds to **actual** `EffectStrategy` target/branch. Verify behavior when ideal target is unavailable.

### Decision-contract tests: small legal states, no numeric oracle

- Test legal candidate selection in `HumanBaselineCultivationStrategy` with `DecisionContextFactory` or exact legal fixtures. Compare a known near-capped Raise against a next D8 Draw; test an early Compost versus a late one, and Queen's Blossom Draw2 vs ordinary Draw1. Assert **selected choice**, not score constants; distinguish current-round pip wins from future-time tradeoffs.
- Test all three resource axes as **independent preferences**: Water keeper vs neutral; Sunlight lover vs avoider; Mulch lover vs avoider; `WATER_MULCH_NO_SUNLIGHT` with the same legal alternatives. A profile can favor near-ties but must reject an illegal, strictly valueless or wildly inferior candidate.
- Test both Round-effect positions, paired offered choices, no possible Mulch die, empty token supply, Water cap, Sunlight with no remaining Battle, last-round/low-hand cases, and target choice consistency. Test two different Player Directors simultaneously: no profile cross-contamination. Test the optional modern-scoring path separately or make its interaction explicit.
- Former scorer-specific/legacy test assertions should not simply be deleted. Keep legality/mechanics invariants; explicitly revise documented *policy* expectations when the new approved choice contract differs. Never choose based on actual future random rolls.

### Behavior cohorts: focused repeatable games, NOT win-rate optimization

- Run a small seeded v38 cohort per profile, alternating seats and using representative legal Groves, player counts and round-pattern conditions as resources allow. Start with a short `simulationTest`/direct-Java **behavioral diagnostic**; a longer opt-in cohort is allowed only as a behavior check, not as learned expert training.
- Per resource and profile report **opportunities offered**, chosen acquisitions, acquisition rate *conditioned on offered+legal opportunities*, sources (Round / Plant / Wisp), peak/mean inventory, uses, useful uses, idle tokens at end, and forgone alternative actions; report no-exposure games separately. Profile labels should be visible in every report, tied to seed/catalog/ruleset fingerprints.
- Acceptance is **relative behavior** at comparable opportunities: e.g. `WATER_KEEPER` takes legal worthwhile Water opportunities more frequently than `NEUTRAL`; `WATER_MULCH_NO_SUNLIGHT` has comparatively more Water/Mulch and fewer Sunlight acquisitions, *subject to actual offers and utility guardrails*. Do **not** assert exactly 1–2 tokens in every game or that a favored profile must win more.
- Keep the existing learned/frozen expert paths invariant; detect any effect of a personality setting on expert policies. Explicitly log candidate estimate, preference adjustment and *chosen alternative* to make Root Four More-type errors diagnosable without repeated giant review documents.

## 5. Sequenced implementation slices and acceptance gates

**November 16 implementation status:** The baseline-observation capture **passed on Ubuntu** (`leaf-human-baseline-build-20261116-214822.tgz`, four fixed-seed v38 games, 120 Build decisions). Shared immediate pip/reward arithmetic is implemented; its focused Ubuntu unit test is still to be confirmed. The future-only dice lifecycle projector is implemented in `FutureDiceForecast.kt`, with mathematical and real-`PlayerDice` parity tests pending Ubuntu execution. The read-only common comparison is now implemented in `CommonBuildActionValuation.kt`, including Draw, normal Compost upgrade, Root Four More, simple Raise and Queen's Blossom; its focused checks also await Ubuntu Gradle acceptance. See [Common Build Action Valuation](COMMON_BUILD_ACTION_VALUATION.md). None of these helpers is connected to the production Build choice selector. See [Immediate Pip and Roll Reward Values](IMMEDIATE_PIP_AND_ROLL_REWARD_VALUES.md), [Future Dice Lifetime Forecast](FUTURE_DICE_LIFETIME_FORECAST.md), and [Build Behavior Snapshot](BUILD_BEHAVIOR_SNAPSHOT.md). The original snapshot was collected **before** changing any Build valuation or personality.

Each line below is an independently reviewable changeset; use ZIP entries relative to repository root. In every implementation task, include a `TASKS.md` progress update. **No stage should rewrite official CSVs or v38 game mechanics.**

| Step | Task | Primary changes | Acceptance |
| --- | --- | --- | --- |
| 1 | Freeze old-vs-new Build diagnostic and effect inventory | Current CSV→scorer→effective `GameEffect` audit, chosen action + all legal alternatives, acquisition vs spend counters, baseline seeds | All 36 IDs mapped, no historical-name substitution; reproducible old behavior |
| 2 | Shared *immediate* pip/reward consequences | Extend `DieValueHeuristics`, `CultivationPipOutcome`, `ExpectedDrawProjection`, typed action-value explanations; reuse `SharedDieEffectOutcome` | Mean + eligible rewards; caps; Draw/roll vs Raise; repeatable unit tests |
| 3 | Future-only dice lifecycle projector | Read-only ordered dice-zone forecast for 3/4/5 Cultivation draws, 3 Battle opening draws and remaining public phase sequence | Exact small-pool tests, zero-use late D20, upgrade D12→D20 +4 per future appearance, no RNG use |
| 4 | Common Build comparison seam | `CurrentActionImpact` + `FutureStateValue` with explicit unit conversion and unsupported-fallback metadata, no double counting | Two legal choices ranked on the same scale; observed chosen targets agree with projected consequences |
| 5 | Immutable composable personality model | `HumanBaselineResourcePersonality` in per-player policy/director, neutral default; independently configure Water/Sunlight/Mulch | Names/combos serializable and reproducible; no changes to expert/other player |
| 6 | Personality-aware Round acquisition | Convert `WaterPriority`, `SunlightTokenPriority`, `MulchPriority` behind seam; existing timing and target constraints; soft goal/near-tie behavior | Clear Water/Sunlight/Mulch decision contracts incl. mixed profile, both Round slots and no-valid-opportunity cases |
| 7 | Resource spend/reserve consistency | Coordinate Cultivation Supports, Battle Sunlight use, Water Refresh/reroll and Mulch; policy reserve helpers | Resources preferred for holding are still used when worthwhile; idle stockpile guard; Battle rules unchanged |
| 8 | Compost lifetime valuation | Replace score's static future-round bonus with forecasted upgraded-die uses; retain legal Trash fallback and current Buy-power opportunity cost | Early vs late, 12→20, low Hand face, limited Graft Bed and target-selection tests |
| 9 | Migrate simple Plant activation effects | `E` family in table, staged and gated; replace migrated fixed base dominance with same Build scale; keep Battle-specific logic | Root Four More vs Draw, Queen's Blossom vs Draw, grouped/capped Raise, reroll cases; all 36 still registered |
| 10 | Extend hybrid Plants only where evidence supports it | `M` family: Water/Sunlight/Mulch sources, conditional extra Draw, upgrades/recycle; mark `L` explicitly unsupported | Exact effect/target and eligibility tests; no accidental generic default 45 for current Round actions |
| 11 | Behavioral verification and lightweight designer review | Conditioned resource-opportunity traces + named profile cohort and concise exception report | Profile behavior differences with credible exposure; no required designer numeric weights; no win-rate gate |
| 12 | Broader regression / research handoff | Full `test`, `integrationTest`, focused `simulationTest` + v38 behavior smoke; doc parity and provenance | Existing mechanical/NC coverage remains green; no learned-player policy drift; A2/A3 can use named Human Baseline profiles |

**Dependencies:** Capture original behavior (step 1) before any scoring changes. Build immediate math, future-only forecast, then common comparison (steps 2–4). Personality definitions (step 5) can proceed after the baseline; acquisition (step 6) requires common comparison and profile definitions. Resource spending (step 7) follows acquisition. Compost and measurable Plant effects (steps 8–9) follow the mathematical foundation; compound effects (step 10) follow those. Behavioral testing and broader regression (steps 11–12) finish the series. Pilot profiles and numerical targets are hypotheses; ask for designer approval only on **small unresolved principles**, never on rote pip arithmetic or a long row-by-row questionnaire.

## 6. Guardrails and explicitly excluded work

- This is an **intentional new Human Baseline workstream** prompted by observed Root Four More opportunity-cost pathology. It is not permission to reopen all deferred C04 historical tuning, rescore Battle holistically, or treat any old certified tests as balance certification.
- **Do not** adopt the previously proposed 58/45/35 Root Four More search or learn Water weights by selecting the profile with best win share. Personality is fixed descriptive variation; expert learning remains the optimization track.
- Use the game's existing `DecisionContext` (visible information only), legal choice requests, score diagnostics and game engine; do **not** introduce a second mutable simulation engine for valuation. Projection is an approximation, not a hidden-state oracle.
- No universal perfect pip scalar is promised: VP, guaranteed Buy tiers, critical Strike swings, Refresh, denial, and action options need separately explained terms. If those cannot yet be compared safely, retain transparent local heuristics and mark the comparison **partial**, rather than inventing precision.
- Preserve the current 3/2/2 v38 rules, official card CSV, player-specific strategy RNG, Graft Bed limits, physical tokens/caps, Roll Reward rules, Wisp restrictions, and available Main/Support counts. No optimization via stochastic policy fitting is required for the Human Baseline acceptance gate.
- A passing profile behavior cohort is evidence of **intentional human style**, not expert strength or card balance. All-card balance verdicts require the separate A2/A3/A4 research gates and actual adapted expert/counterstrategy evidence.
