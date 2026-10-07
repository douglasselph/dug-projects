# Provisional Resync Baseline

`resync-current.csv` is the explicit working Plant-market baseline for ongoing
resynchronization experiments. It is **not** loaded automatically by the core
simulator. Research scripts that opt into the resync baseline merge this file
with their experiment-specific Plant overrides and pass the resulting effective
CSV explicitly through `--plant-overrides`.

Current provisional interventions:

- `Vine_07_01` represents the working **Berry Tasty** package for research:
  cost 14, Fixed 3 VP, `RAISE_D8_PLUS_1`.
- `Vine_07_04` remains unavailable until a provisional replacement for
  canonical Vine Yield is established.

Important limitation: `Vine_07_01` still retains its canonical simulator card
identity/type/Grove slot. The current Plant override system changes cost,
availability, scoring, and effect, but not Plant type. Therefore the current
Berry Tasty baseline is an economic/effect proxy for the proposed Flower, not a
full simulation of Flower grafting/type/slot behavior.

For a preserved research checkpoint, copy `resync-current.csv` to a numbered
snapshot such as `resync-v01.csv` before changing the current baseline.

Core commands such as `bin/train_buy_policy` and `bin/evaluate_buy_policy` do
not discover this file implicitly. Pass an override explicitly for ad-hoc runs,
or use the resync-aware experiment scripts.
