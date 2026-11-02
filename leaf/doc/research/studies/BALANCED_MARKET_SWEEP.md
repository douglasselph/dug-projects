# Exact-4x9 Balanced Market Sweep

Run:

```bash
bin/experiment_balanced_market_sweep
```

The default run is intentionally deep: 15 complete market configurations, 5 fresh Buy learners per market, and 1,000 held-out matched samples per learner. With train + evaluation stages, that is 150 resumable stages.

Before any training starts, every selected market is merged onto `data/research/resync/resync-current.csv` and checked by `bin/validate_plant_slot_market`. The run aborts unless every legal Grove slot has exactly four available candidates:

- Root 5 / 7 / 9
- Vine 7 / 9 / 11
- Flower 11 / 14 / 17

Use `PREFLIGHT_ONLY=1 bin/experiment_balanced_market_sweep` to verify all market maps without training. Use `CONFIGS="m01-... m05-..."` to run a subset. `RESUME=1` is the default.

All configurations use identical learner/evaluation seed schedules so paired market comparisons remain as controlled as practical while still retraining the Buy policy for each complete market.

## Common provisional changes

Every market includes the current resync plus:

- Root Awakening: Root 9 -> Root 7, balancing Root & Scoot's Root 7 -> Root 9 move.
- Petal To Die 4: redesigned effect `Gain or Steal 1 D4. Then discard any number of your D4s; Draw that many dice.` and `PER_OWNED_D4` scoring. Steal is legal only from an opponent's Hand.
- Bee-loved Bloom: research stacking effect `Gain or Steal a Bee; Bees are worth +2 more this round` unless a configuration explicitly moves its cost.
- Root Down Payment: simplified `GAIN_VP_PER_ONE_SHOWING` unless the configuration is an original-effect comparator.

## Markets

- `m01-berry7-down-simple7-bee14`: Berry Vine 7; simplified Down Payment Root 7; Bee-loved Flower 14.
- `m02-berry7-down-original7-bee14`: matched original Down Payment comparator at Root 7.
- `m03-berry7-down-simple9-bee14`: simplified Down Payment Root 9; Root Cause moves 9 -> 7 as the counter-move.
- `m04-berry7-down-original9-bee14`: matched original Down Payment comparator at Root 9.
- `m05-berry9-down-simple7-bee14`: Berry Vine 9; Parting Thorn moves Vine 9 -> 7.
- `m06-berry11-punish7-cult-down-simple7-bee14`: Berry Vine 11; Vine and Punishment Vine 7, reroll/on-3 Draw in Cultivation only.
- `m07-berry11-punish7-both-down-simple7-bee14`: same, but reroll/on-3 Draw is available in both phases while Battle also keeps the reduce-row-by-3 effect.
- `m08-berry11-punish9-cult-down-simple7-bee14`: Vine and Punishment Vine 9 Cultivation-only; Parting Thorn moves 9 -> 7.
- `m09-berry11-punish9-both-down-simple7-bee14`: Vine and Punishment Vine 9 both-phases; Parting Thorn moves 9 -> 7.
- `m10-berry7-bee11-sapping14-current-down-simple7`: Bee-loved Flower 11; unchanged Sapping Snapdragon package moves 11 -> 14.
- `m11-berry7-bee11-sapping14-enhanced-down-simple7`: same slot swap, with enhanced Sapping Snapdragon using a different Battle die as the row anchor.
- `m12-combined-punish7-cult-sapping14-current`: combined plausible final market: Berry 11 / Punishment 7 Cultivation-only plus Bee 11 / Sapping 14 unchanged.
- `m13-combined-punish7-both-sapping14-current`: combined market with Punishment's reroll/on-3 Draw available in both phases.
- `m14-combined-punish7-cult-sapping14-enhanced`: combined market with enhanced Sapping at 14.
- `m15-gone-with-the-vine7-berry14-comparator`: Gust of Petals moves Flower 14 -> Vine 7 (working Vine name: **Gone with the Vine**); Berry remains Flower 14 as a comparator.

Berry uses Fixed 3 VP + `RAISE_ALL_D8S_PLUS_1` in all of these research markets.

## Output

Each market directory records:

- the market layer,
- the fully merged effective override,
- an exact slot map showing all four candidates in each slot,
- fresh learned weights,
- training logs,
- held-out evaluation logs,
- a focus-card grep index.

After all selected markets complete, the script creates `balanced-market-sweep-results.tar.gz` outside the traversed directory first, then atomically moves it into the result directory. This avoids the GNU tar self-modification failure seen in an earlier overnight sweep.
