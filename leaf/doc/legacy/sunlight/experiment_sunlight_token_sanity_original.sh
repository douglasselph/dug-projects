#!/usr/bin/env bash
#
# Lightweight matched sanity experiment for the proposed Sunlight token rule.
#
# Compares the same frozen Buy policy, random-Grove schedule, provisional Plant
# resync baseline, mechanical seeds, and strategy seeds under:
#   A) canonical Sunlight Round effect (Raise +3)
#   B) Sunlight Round override (GAIN_SUNLIGHT_TOKEN)
#
# This is a behavioral sanity check, not a final balance/optimization run.
# Its primary question is whether Sunlight produces useful extra Battle Main
# Actions/Plant activations without pathological action inflation.
#
# Usage:
#   bin/experiment_sunlight_token_sanity
#   bin/experiment_sunlight_token_sanity --games 300
#   bin/experiment_sunlight_token_sanity --games 500 --players 4 --rounds 3/2/2
#   bin/experiment_sunlight_token_sanity --weights data/ai/4p/frozen-buy-first-game-default-v1.weights
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
source bin/lib/experiment_run_timing.sh

GAMES=300
PLAYERS=4
ROUNDS="3/2/2"
WEIGHTS="data/ai/4p/frozen-buy-first-game-default-v1.weights"
PLANT_OVERRIDES="data/research/resync/resync-current.csv"
ROUND_OVERRIDES="data/research/round-overrides/sunlight-token.csv"
MECHANICAL_SEED=263000
STRATEGY_SEED=273000
GROVE_SEED=283000

while [[ $# -gt 0 ]]; do
  case "$1" in
    --games) GAMES="$2"; shift 2 ;;
    --players) PLAYERS="$2"; shift 2 ;;
    --rounds) ROUNDS="$2"; shift 2 ;;
    --weights) WEIGHTS="$2"; shift 2 ;;
    --plant-overrides) PLANT_OVERRIDES="$2"; shift 2 ;;
    --seed) MECHANICAL_SEED="$2"; shift 2 ;;
    --strategy-seed) STRATEGY_SEED="$2"; shift 2 ;;
    --grove-seed) GROVE_SEED="$2"; shift 2 ;;
    --help|-h)
      sed -n '1,28p' "$0"
      exit 0
      ;;
    *) echo "Unknown argument: $1" >&2; exit 2 ;;
  esac
done

[[ "$GAMES" =~ ^[1-9][0-9]*$ ]] || { echo "--games must be a positive integer" >&2; exit 2; }
[[ "$PLAYERS" =~ ^[234]$ ]] || { echo "--players must be 2, 3, or 4" >&2; exit 2; }
[[ -f "$WEIGHTS" ]] || { echo "Weights file not found: $WEIGHTS" >&2; exit 2; }
[[ -f "$PLANT_OVERRIDES" ]] || { echo "Plant resync baseline not found: $PLANT_OVERRIDES" >&2; exit 2; }
[[ -f "$ROUND_OVERRIDES" ]] || { echo "Sunlight Round override not found: $ROUND_OVERRIDES" >&2; exit 2; }

STAMP="$(date +%Y%m%d-%H%M%S)"
RUN_DIR="output/experiments/sunlight-token-sanity/$STAMP"
mkdir -p "$RUN_DIR/config"

cp "$PLANT_OVERRIDES" "$RUN_DIR/config/plant-resync-baseline.csv"
cp "$ROUND_OVERRIDES" "$RUN_DIR/config/sunlight-token-round-overrides.csv"
cp "$WEIGHTS" "$RUN_DIR/config/policy.weights"

cat > "$RUN_DIR/manifest.txt" <<MANIFEST
experiment=sunlight-token-sanity
purpose=Compare canonical Sunlight Raise+3 against GAIN_SUNLIGHT_TOKEN as a Battle Support-funded extra Main Action.
games_per_condition=$GAMES
players=$PLAYERS
rounds=$ROUNDS
weights=$WEIGHTS
plant_overrides=$PLANT_OVERRIDES
round_override_condition_b=$ROUND_OVERRIDES
mechanical_seed_start=$MECHANICAL_SEED
strategy_seed_start=$STRATEGY_SEED
grove_seed_start=$GROVE_SEED
grove=random
matched_conditions=yes
condition_a=canonical Round effects
condition_b=sunlight-token Round override
primary_question=Does Sunlight increase meaningful Battle Plant-effect use without pathological action inflation?
MANIFEST

COMMON_ARGS=(
  --games "$GAMES"
  --players "$PLAYERS"
  --rounds "$ROUNDS"
  --weights "$WEIGHTS"
  --random-grove
  --grove-seed "$GROVE_SEED"
  --seed "$MECHANICAL_SEED"
  --strategy-seed "$STRATEGY_SEED"
  --plant-overrides "$PLANT_OVERRIDES"
)

TOTAL_RUNS=2
RUN=0
experiment_timing_init

experiment_timing_start "canonical Sunlight Raise +3 with provisional Plant resync baseline"
bin/evaluate_buy_policy "${COMMON_ARGS[@]}" |& tee "$RUN_DIR/canonical-sunlight.log"

experiment_timing_start "Sunlight token gain override with Battle Support extra Main Actions"
bin/evaluate_buy_policy "${COMMON_ARGS[@]}" --round-overrides "$ROUND_OVERRIDES" |& tee "$RUN_DIR/sunlight-token.log"

cat > "$RUN_DIR/README.txt" <<'README'
SUNLIGHT TOKEN SANITY EXPERIMENT

Compare canonical-sunlight.log with sunlight-token.log.

The primary design question is not whether win share increases. Inspect the
"Sunlight / Battle action research" section for:
- Sunlight gained/spent/retained
- Support opportunities and uses
- extra Main Actions by type
- Sunlight-funded Plant identities/effects
- total Battle Main and Support Actions
- total Battle Plant Main Actions
- maximum per-game action counts as an action-inflation warning

Also compare the standard evaluator sections for:
- Plant VP
- Battle VP
- final VP
- dice development
- wounds
- win share
- Plant/die acquisition substitutions

Both conditions use the exact same seeds, random-Grove schedule, policy, and
explicit provisional Plant resync baseline. The only intended condition change
is the Round effect override in the token condition.
README

ARCHIVE="output/experiments/sunlight-token-sanity/sunlight-token-sanity-$STAMP.tar.gz"
tar -czf "$ARCHIVE" -C "$(dirname "$RUN_DIR")" "$(basename "$RUN_DIR")"

echo
echo "Sunlight sanity experiment complete."
echo "Results directory: $RUN_DIR"
echo "Archive to send back: $ARCHIVE"
