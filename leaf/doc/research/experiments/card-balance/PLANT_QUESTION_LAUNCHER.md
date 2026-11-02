# Reusable Plant question launcher

## Scope

`bin/plant_question` reads and revalidates schema-v1 questions and A1.3 condition resolutions. Its `--plan` output is stable JSON (`plant-question-plan-v1`) with the effective nine-card Grove, inputs/fingerprints, roles, selected stages, conditions, player counts, seeds, policy inputs, budgets, and intended output locations. This **does not execute games** and is never evidence of balance. Plan output paths designate logical stage folders; historical R3A physical output layout differs.

## Usage (run from inside `leaf/`)

```bash
python3 bin/plant_question doc/research/experiments/card-balance/questions/root-four-more.json --plan
python3 bin/plant_question doc/research/experiments/card-balance/questions/sapping-snapdragon.json --plan
python3 -m unittest discover -s bin/tests -p 'test_plant_question*.py' -v
```

## Execution adapter and critical limitations

`--run-r3a-official --condition official --players 2` invokes the preserved R3A script **only when** the question's selected stages are exactly `targeted_discovery`, `unrestricted_discovery`, `heldout_evaluation`; rules are CANONICAL, policies are Buy/Human with full seat rotation and no frozen weights; and the explicitly declared Grove exactly equals the legacy R3A context-zero Grove. The caller must use an intentionally restricted question file satisfying these conditions; the two full research example files do **not** qualify because they request additional stages and player counts. Failing the gate produces an error before any build or game. No hidden stages or alternative Groves are substituted.

The legacy R3A adapter owns the compile-once/direct-Java process model, its separate training/evaluation JVMs, locks, checkpoint/resume validation, manifests, progress/ETA, and verified `.tgz` archive. The launcher verifies held-out evidence in that archive and copies it to `~/Downloads`, printing `UPLOAD THIS FILE:`. Do not claim this adapter executes target-seat effect isolation, cost changes, noncanonical Battle, multiplayer, frozen comparisons, adapted comparisons or countersearch. Those require explicit implementations and verification in A1.5. A1.4 remains **partially complete** until the full planned execution path works; status is deliberately not overstated.

Potential research interpretation issue: the R3A training and evaluation seed derivation is preserved, but its historical CLI seeds do not natively distinguish the specification's held-out seed from the evolutionary training seed. Mapping the declared holdout value to R3A's evolution seed is merely a compatibility mapping, not independent held-out RNG isolation certification. Verify that before making inferential claims.

## Safety

Do not edit canonical CSVs or frozen weights. Never silently omit a failed cohort or unsupported condition. Do not run all stages or player counts from a full question spec through the narrow official R3A adapter. Historical launchers and their output schemas remain unchanged.
