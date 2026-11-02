# Plant research condition resolution

## Purpose

`bin/plant_question_conditions.py` is the stable, card-independent condition adapter between the validated JSON question specification and existing Kotlin `PlantExperimentConfig`/seat-scoped research effect interfaces. It **does not run simulations** or modify canonical CSVs. Existing R3A/R3B/R3B.1 launchers and output contracts remain untouched.

Run from the repository root:

```bash
python3 bin/plant_question_conditions.py doc/research/experiments/card-balance/questions/root-four-more.json
python3 -m unittest discover -s bin/tests -p 'test_plant_question_conditions.py' -v
```

Each condition has a reproducible fingerprint, effective Grove, `global_plant_overrides` (to become a `PlantExperimentConfig`), and `target_role_effects` (for `researchPlantEffectsBySeat` on the designated role/seat). A1.4 must serialize the first into an actual Kotlin configuration and map the second to the actual targeted player seat at evaluation time. No seat should be inferred from fixed table position.

| Kind | Resolution |
| --- | --- |
| `official` | No overrides; uses active research catalog unchanged |
| `suppressed` | Explicit `RESEARCH_NO_EFFECT` on target card |
| `effect_override` | Exact declared, executable `GameEffect` enum constant; never inferred from card name |
| `plant_override` | Explicit override-file columns `type,cost,available,scoring,effect` indexed by existing Grove card IDs; blank means no change |

Global interventions can change a Plant's effect, cost, structured scoring, type, or availability. Seat-scoped interventions currently support **effect only**, because the underlying game infrastructure cannot safely isolate the other attributes per player. Attempts to apply price/type/scoring/availability per seat are errors, not silently global changes. Overrides outside the actual nine-card market are also rejected.

The effective Grove must continue to have exactly nine available entries with the original legal `(type,cost)` slots. Moving one Plant to another cost slot without compensating valid changes fails. The resolver preserves canonical definitions and never edits them. A1.4 should validate any alternative market roster explicitly before dispatch and should never infer new market contents.

### Limits / next integration gate

- The resolver proves the referenced `GameEffect` exists, not that the underlying effect can legally execute on all target types/states. A1.4 must exercise the selected effect in Kotlin and fail clearly for unsupported combinations.
- Target-seat isolation must be proved with a Kotlin integration test asserting unaffected opponents keep their official effect; existing R3B.1 seat-scoped research machinery is the intended implementation.
- For non-effect overrides, A1.4 must convert the resolved data through `PlantExperimentConfigLoader` or equivalent typed Kotlin bridge; an effect-only seat map must not be used for cost/scoring/type overrides.
- This is an immutable planning contract, not an achieved experimental outcome or balance result. Historical scripts, frozen weights and archived artifacts remain unchanged.
