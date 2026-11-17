# Comparing Build actions using immediate and future pip value

**Status (November 16, 2026):** Read-only comparison implemented and independently checked; full Gradle verification on Ubuntu is pending. **Human Baseline still makes exactly the same decisions as before this change.** This is a foundation for the later personality and scorer migrations, not their completion.

## Why this exists

Historically, the Build evaluator assigned unrelated fixed priority bases to Draw, Compost, and individual Plant activations. Root Four More, for example, could receive a larger numerical priority than Draw before the actual benefit of either action was taken into account. The previous changes added two trustworthy arithmetic components:

- `ImmediatePipValuation` — the expected current-round spending pips, with provisional Critter and Wisp values kept separate.
- `FutureDiceForecast` — the *strictly future-round* expected rolls under the current lowest-sided-first Supply/Discard cycling and an explicit Draw schedule.

`CommonBuildActionValuation` now compares **alternatives from the same visible Cultivation decision**. It accepts the game's list of legal Main Actions and computes their pip-valued consequences without changing the game or touching strategy randomness. There are no arbitrary activation bases in the new results.

## How the value is calculated (in English)

1. Read today's Supply, Hand, Discard, the publicly known upcoming phase sequence, and the legal Main Action offers. Calculate a future-only reference forecast once.
2. Consider **one** offered action. For Draw, move the actual next lowest-sided die into a *projected* Hand. For Compost, move the chosen Hand die out and its available upgraded replacement into *projected* Discard. For a straightforward Raise, modify its current pip value without pretending it creates a die or rolls again. For Queen's Blossom, project two consecutive Draws in one Main Action.
3. Record the **current** change in expected spending pips. A Hand die sent to Compost costs its *actual showing value now*; a Draw gains the new die's mean; a Raise gains its actual capped pips. Current rolls can also have provisional Critter/Wisp value, but merely changing a face cannot trigger Roll Rewards.
4. Run the future-only forecast again from the projected zones using the *same* future Draw assumptions. Subtract the reference forecast. This delta counts the die's later opportunities and prevents counting a current Draw twice.
5. Report `current spending pips + current reward equivalents + change in future spending pips + change in future reward equivalents`. All four components are shown, not concealed in one score. The future forecast uses four Draws per future Cultivation by default (three and five are available), and three opening Draws per future Battle.
6. Report the **unpriced** consequences prominently. A pip-only comparison is useful evidence, but it is not a complete strategy recommendation when purchases, Plant Refresh, scarce resources, or tactical effects also matter.

### Three concrete comparisons

- **Root Four More versus Draw D8:** D8 showing 1 can be Raised +4. Draw of an available D8 gains 4.5 expected current spending pips. With no future rounds, the common pip measure is **4.0 vs. 4.5** (before eligible roll-reward equivalents). Root Four More's Flip/Refresh timing and purchase implications are not yet priced; the module must not silently claim that Draw always wins.
- **Compost D12→D20, showing 1, versus Draw D4:** One future Battle follows, and the upgraded die will actually be encountered. Compost costs **1 pip now** and improves one expected future roll by **4**, giving **+3 net**. Drawing the D4 gains **2.5 now** (and zero extra in that example's future forecast). If no future rounds remain, the same Compost instead gives **−1**. The live Compost target chooser and current purchasing opportunities are not migrated yet.
- **Queen's Blossom versus ordinary Draw:** If Supply offers D4 then D10, Draw gives 2.5 and the two-die effect gives **2.5 + 5.5 = 8.0** current expected pips (before eligible roll rewards). The Plant's consumed readiness remains a separate opportunity cost.

These are test fixtures, not observed whole-game performance or universal action preferences.

## Supported action families

| Legal Build action | Measured in this slice | Intentionally unresolved |
| --- | --- | --- |
| Draw one die | Actual next physical die, immediate expected roll/reward, projected future zone change | Probabilistic purchase thresholds, what is bought |
| Round Compost | Highest projected pip net among **available normal one-step upgrades**; actual Hand face removed; replacement into Discard | Optional Trash fallback, current Buy decisions, target-alignment with existing legacy chooser |
| Root Four More / a simple +1 Raise | Capped current pip gain on a candidate die; no extra Roll Reward | Whether the live effect target chooser picks the same die, Plant Refresh timing, threshold/purchase value |
| Queen's Blossom / Draw two dice | Two lowest-sided next Draws, reward equivalents, future zone changes | Plant Refresh timing, downstream purchases |
| Water, Sunlight, Mulch | **No numerical value yet** (explicit `UNMODELED`) | Future resource opportunity value and separate personality preference |
| All other Plant / Round effects | **No numerical value yet** (explicit `UNMODELED`) | Mixed, conditional, opponent, VP, recycling or future-activation effects |

The actual legal Main choices are supplied by `ChooseCultivationActionRequest`. The comparison does not invent additional actions, bypass legality checks, promise a Compost replacement when the Graft Bed lacks it, or treat a Trash fallback as an upgrade.

## Reporting and safe integration

- `BuildActionValue` records a selected **projection target** (where one exists), immediate pips and rewards, future differences, potential Buy-tier crossings, omissions and a textual explanation.
- `BuildActionComparison.pipOnlyOrder` ranks the modeled pip contributions, **not** the full action values.
- `readyToChoose` is false when any offered action is partial or unmodeled. The live Human Baseline does not call this module to choose its Main Action in this stage.
- Possible Buy thresholds are listed for investigation. A predicted Draw *mean* crossing cost 5 is **not** a calculated 50% purchase chance. The existing criterion for spending Critters is also policy-dependent. We do not assign arbitrary purchase bonuses or double-count a subsequent die purchase.
- Future dice rewards assume the current visible availability remains fixed. Future Round faces, additional acquired dice, purchases, future Compost, special Draws, Battle Doom and resource use are not predicted by this simple lifecycle model.
- The projection's best Compost/raise target is **not certified as identical** to the current downstream Effect policy's target. That alignment is a required separate migration and regression test.
- The existing `HumanBaselineCultivationStrategy`, learned-player strategies, Round card definitions and physical mechanics are unchanged. The old fixed scores are not supplemented with these estimates; that would repeat the original overvaluation error.

## Verification and next work

Run:

```bash
./gradlew test \
  --tests '*CommonBuildActionValuationTest' \
  --tests '*FutureDiceForecastTest' \
  --tests '*ImmediatePipValuationTest'
```

Eleven focused common-comparison checks passed in an isolated Kotlin build with the **real immediate and future forecast implementations** and simple substitutes for the game context; that is *not* the full project's Gradle test result. Ubuntu Gradle acceptance remains open.

The next stages will add fixed, per-player Water/Sunlight/Mulch personalities and later migrate actual Build selection and downstream targets **only for effects whose important consequences can be compared safely**. Do not derive personality settings from expert win-rate optimization. The unchanged-choice Build capture (`BUILD_BEHAVIOR_SNAPSHOT.md`) is the old-policy reference for later diagnostics.
