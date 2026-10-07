# Human Baseline — Sunlight Battle Spending

## Status

**PASS — calibrated first Human Baseline model.**

This document describes the ordinary-human control used when a player already
owns Sunlight during Battle. It is not an optimal-play model and it is not a
claim about the objective strength of Sunlight.

## Decision being modeled

During Battle, spending one Sunlight is a Support Action that immediately funds
one otherwise-normal Battle Main Action. The Human Baseline asks:

> Given the visible Battle state right now, is the best extra Main Action worth
> consuming one banked Sunlight?

The funded Main Action continues to use the normal `BattleMainPriority` path.
Sunlight does not duplicate Draw, Plant, or Round-effect valuation.

## Preservation value

The calibrated preservation value is intentionally shallow and interpretable.

- Base preservation of the last token: **55**.
- Early Battle (3 or more Battles including the current one remain): **+7**.
- Middle Battle: no timing adjustment.
- Final Battle: **-40**, because a token kept past the final Battle has no
  ordinary future use.
- Each additional stored Sunlight beyond the first: **-10** marginal
  preservation pressure.
- Preservation has a small floor of **5**.

Therefore representative preservation values are:

| Situation | Preservation |
| --- | ---: |
| Early Battle, 1 Sunlight | 62 |
| Early Battle, 2 Sunlight | 52 |
| Middle Battle, 1 Sunlight | 55 |
| Final Battle, 1 Sunlight | 15 |

This produces the intended qualitative behavior: a player may spend one of two
stored tokens on a useful extra action while preserving the last token for a
better later opportunity.

## Funded action value

The benefit side is simply the existing Battle Main score:

- Draw uses the existing Draw score plus the established Battle-extra-die
  adjustment.
- Plant activation uses the card's existing Battle Human Baseline scorer and
  any tactical analysis already attached to that effect.
- Round effects use the existing Battle Round-effect Main valuation.

The Sunlight Support is worthwhile when:

`funded Main value - preservation value >= 0`

The normal Battle candidate chooser still compares the surviving Support
alternatives. Passing the Sunlight preservation gate does not force Sunlight to
be chosen over a better Bee, Water, Wisp, or other Support.

## Why Battle timing matters

During an early Battle, keeping the last Sunlight has genuine option value: a
later Battle may present a much stronger tactical state.

During the final Battle there is no comparable future opportunity. The model
therefore spends a token much more freely rather than hoarding a resource that
has no ordinary end-game conversion.

## Multiple Sunlight tokens

Surplus tokens lower marginal preservation pressure rather than increasing the
value of the funded action. This distinction is intentional.

For example, in an early Battle an extra Main worth 60 is:

- not worth consuming the player's last Sunlight (60 < 62);
- worth consuming one of two Sunlights (60 >= 52);
- after that spend, the remaining token is again protected at 62 if only the
  same marginal actions remain.

## Information used

The scorer uses only ordinary visible state:

- the exact legal Main Action being funded;
- its existing Human Baseline Battle value;
- current Sunlight count;
- Battles remaining;
- whether this is the final Battle.

It does not predict hidden cards, future purchases, unknown rolls, or future
Strike layouts.

## Targeted calibration observations

Sunlight Support candidates expose diagnostic observations through the existing
DecisionReasoning system:

- `fundedMainValue`
- `sunlightHeld`
- `battlesRemaining`
- `finalBattle`
- `sunlightBasePreservation`
- `sunlightBattleTimingAdjustment`
- `sunlightAdditionalHoldingAdjustment`
- `sunlightPreservationValue`
- `sunlightNetSupportValue`

The targeted decision calibration runner recognizes target
`sunlight-spend` / `sunlight-battle-spend`. When the Sunlight Round override is
needed to create tokens naturally, it can be supplied explicitly, for example:

```bash
bin/targeted-decision-calibration \
  --target=sunlight-spend \
  --games=200 \
  --round-overrides=data/research/4p/round-overrides/sunlight-token.csv
```

The report selects the best Sunlight-funded candidate at each opportunity and
shows the selected action and nearby rejected alternatives.

## Certification cases

The deterministic calibration suite covers the intended qualitative cases:

- weak extra action in an early Battle -> preserve;
- clearly strong extra action -> spend;
- useful but marginal action -> preserve early, become spendable later;
- two Sunlights -> first marginal spend can be worthwhile while the last is
  protected;
- final Battle -> preservation pressure falls sharply;
- a high-value Draw can justify Sunlight;
- Battle coordinator still preserves normal Support/final-Main structure.

Existing Battle coordinator tests continue to cover Support continuation and
that Sunlight-funded actions do not consume the normal final Main.

## Deliberate limitations

This is a Human Baseline, not a learned tactical optimizer. In particular:

- it does not forecast exact future Battle states;
- it does not learn an empirical resource shadow price;
- it does not claim every card's Battle Main valuation is perfectly calibrated;
- indirect provenance such as Sunlight -> Draw -> later die contribution remains
  a separate research question.

If later targeted evidence shows a concrete misvaluation, tune the shared Main
scorer or this preservation model based on that evidence rather than maximizing
Human Baseline win rate.
