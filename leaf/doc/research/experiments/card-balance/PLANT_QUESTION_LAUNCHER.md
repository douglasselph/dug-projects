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

## Generic one-sided effect suppression slice (A1.4/A1.5)

`bin/plant_question --run-paired-frozen QUESTION.json` invokes `bin/plant_question_pair.py`.
The adapter runs the existing R3A targeted/unrestricted **official Buy discovery** and
held-out evaluation, then evaluates its frozen Buy champions a second time with
`RESEARCH_NO_EFFECT` applied **only to the targeted seat**. This is not full execution
of all declared research stages: multiplayer, adapted comparisons, countersearch,
price/scoring overrides and other candidate conditions remain pending. The adapter
explicitly requires the question to declare official/all-players and
suppressed/target-seat conditions, a two-player cohort, discovery and frozen
held-out stages, and a Grove exactly reproduced by legacy R3A's market seed.

The frozen comparison reuses the same held-out seed schedule, seats and policy
weights. The independent unrestricted-versus-baseline matchup must be identical,
and the suppressed target must produce zero direct mechanical effect telemetry.
It records the observed paired difference in win share and VP, but **cannot yet
issue a balance verdict**. The source is compiled once by R3A; evaluation uses a
separate direct-Java process; checkpoints validate outputs and inputs. A full
archive containing manifest, question plan, frozen weights and both held-out TSVs
is copied to `~/Downloads` and printed as `UPLOAD THIS FILE:` after success.

```bash
python3 bin/plant_question doc/research/experiments/card-balance/questions/root-four-more.json --run-paired-frozen
python3 bin/plant_question doc/research/experiments/card-balance/questions/sapping-snapdragon.json --run-paired-frozen
```

The generic `effect:RESEARCH_*` executable selection in R3A is deliberately
restricted to research-only enum names. The historical `--effect-variant`
aliases remain unchanged; canonical card data is not modified. **Execution
has not been exercised with a Kotlin JVM in the packaging environment** because
the Gradle wrapper distribution could not be downloaded. The Ubuntu run is the
required integration acceptance check, not a claim of completed A1.5.
