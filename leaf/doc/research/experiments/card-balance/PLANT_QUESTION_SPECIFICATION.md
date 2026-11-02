# Plant research question specification

## Purpose and boundary

The JSON question file is a **validated declaration**, not executable experiment instructions. Run `python3 bin/plant_question_spec.py <path-to-question.json>` at the top of the `leaf` repository to print a deterministic, expanded JSON record of the exact official card identity, nine-card Grove, rule inputs, effect conditions, policies, and seeds. Optional `--output /path/to/resolved.json` saves that record. Validation never starts Gradle, changes game data, trains a policy, or runs a game. The generic A1.3 resolver is `bin/plant_question_conditions.py`; A1.4 will consume a validated specification to plan execution.

Examples: `questions/root-four-more.json` and `questions/sapping-snapdragon.json`. Both deliberately use current official definitions and the explicit standard 3/2/2 research configuration. Example budgets are *small illustrative placeholders*, not sufficient for balance claims.

## Schema v1

All top-level keys are required. Unknown keys and unexpected condition fields fail closed.

| Key | Required content |
| --- | --- |
| `schema_version` | integer `1` |
| `question_id`, `question`, `hypothesis` | stable lowercase slug; human question and hypothesis |
| `target` | exact `id`, `title`, and `fingerprint` (SHA-256 of canonical JSON with `id,title,type,cost,effect_text,quantity`); all must agree with current official CSV |
| `grove` | exactly nine distinct existing card IDs, one per `Root 5/7/9`, `Vine 7/9/11`, `Flower 11/14/17`, including target |
| `player_counts` | nonempty, distinct values from 2, 3, 4 |
| `rules` | `round_pattern: [3,2,2]`, `battle_structure: CANONICAL` or `THREE_ENGAGEMENTS`, `plant_data`, `round_data`, `wisp_data`; v1 pins these paths to current research layers and official Wisp CSV |
| `conditions` | at least two named objects (`name`, `kind`, `scope`) with an `official` condition. Kinds: `official`, `suppressed`, `effect_override` (requires `effect` named in current Kotlin `GameEffect` enum), `plant_override` (requires `override_file`). Scope: `all_players` or `target_seat` |
| `stages` | selected from `targeted_discovery`, `unrestricted_discovery`, `frozen_comparison`, `adapted_comparison`, `heldout_evaluation`, `countersearch`. Held-out is required; countersearch requires targeted discovery; frozen comparison requires discovery or supplied weights |
| `policies` | `search_family: buy`, `companion: human_baseline`, `frozen_weights: []` or repo-relative existing paths, `seat_rotation: all` or `none` |
| `seeds` | nonnegative integer `market`, `mechanical`, `strategy`, `holdout`; holdout differs from mechanical and strategy |
| `budgets` | positive integer `generations`, `population`, `elites`, `train_games`, `holdout_games`; elites cannot exceed population |
| `thresholds` | `attention_pp` and `serious_pp`, positive percentage-point values with serious > attention |

The resolver fingerprints all four source CSVs, the current Wisp and research input files, the Kotlin GameEffect declaration, and any referenced weights. It prints all nine current canonical card definitions and per-card fingerprints. The JSON `status` explicitly says `VALIDATED_INPUT_ONLY_NOT_EXECUTED`.

**Important limitations:** A named `effect_override` proves a Kotlin enum exists, not that a particular override is mechanically supported by every target. The condition resolver verifies the declared effect constant and expresses target-role isolation; execution-level effect/target compatibility remains a required A1.4 gate. A `plant_override` CSV is checked for known IDs, duplicates, recognized columns, known effect/scoring names and basic value syntax; A1.3 now derives the effective Grove and rejects invalid nine-slot markets. Seat-scoped cost, type, scoring and availability interventions remain unsupported and are explicitly rejected. No first-run manifest should be called evidence of card balance. No canonical or frozen-weight file is modified by validation.

## Generate or update a target fingerprint

From within `leaf`:

```bash
python3 - <<'PY'
import sys
sys.path.insert(0, 'bin')
from plant_question_spec import catalog, fingerprint
for cid in ('Root_05_02', 'Flower_11_03'):
    card = catalog()[cid]
    print(cid, card['title'], fingerprint(card))
PY
```

If the official card CSV changes, intentionally update a question's target title/fingerprint only after reviewing the actual definition change; do not turn off stale-input validation.

## Test

```bash
python3 -m unittest discover -s bin/tests -p 'test_plant_question_spec.py' -v
python3 bin/plant_question_spec.py doc/research/experiments/card-balance/questions/root-four-more.json
python3 bin/plant_question_spec.py doc/research/experiments/card-balance/questions/sapping-snapdragon.json
```
