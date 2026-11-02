# A03 — Human Baseline decision-valuation boundary (2026-10-25)

**Status:** guardrails implemented, protected baseline verified; no gameplay or Human Baseline algorithm changed in A03.

## Why A03 exists

The upcoming master calibration changes how **Human Baseline** values choices during Cultivation **and** Battle. Those valuation changes must not accidentally change what a card does, how a turn works, its cost, the legal 4×9 market, trained/frozen policy files, or the learned policy families. The later Root Four More Raise +20, Queen's Blossom Draw 4, disabled-effect and Gain 20 VP *research controls* remain separate, deliberately labeled experiments.

A03 is a structural change detector, not a mechanical proof or a substitute for scenario/integration tests. It will fail if an existing protected file is changed or deleted or if a new file is created in a protected area.

## Verified input

- Basis: `leaf(20261025-203429).tgz`, SHA-256 is recorded in `doc/A03_PROTECTED_BASELINE.json`.
- The upload includes the eight A02 changes. The preceding `leaf(20261025-201731).tgz` differs from the latest archive by precisely those eight paths (including tests and documentation).
- Neither archive has `.git`, so **this upload cannot establish the user's current Git HEAD**. The earlier A01 local report recorded `104c7b6104c9dd2f36bf025aacd064e13e3632b7` **before A02**; do not assert it is still current.
- All 478 protected **source/data files** were SHA-256 fingerprinted from the latest uploaded archive. Python bytecode (`__pycache__`, `.pyc`) is intentionally excluded.

## Allowed without boundary exception

- `src/main/kotlin/dugsolutions/leaf/v35/player/decision/baseline/**` — Human Baseline scoring and target valuation, including its Cultivation/Battle subpackages.
- `src/test/**`, `bin/tests/**`, `doc/**` — tests and documentation (still subject to human review).

## Protected by default

- All other `src/main/**` — canonical effects and handlers, engine mechanics, decision context, learned policy classes, shared domain models, rule scheduling, evaluation/research support.
- All `data/**` — Plant and Round CSVs, costs, 36-card/9-slot market inputs, research-only override/resync datasets, and `data/ai/**` trained/frozen weights.
- Existing and newly added `bin/**` scripts except `bin/tests/**`, including experiment launchers. `bin/a03_verify_boundaries.py` itself is protected.
- Gradle build files and wrapper configuration.

**This is intentionally conservative.** A later change may legitimately require a shared helper, decision-context interface, or removal of obsolete Sunlight Raise +3 *mechanics*. A03 does not forbid the owner from authorizing such a change; it makes it visible rather than accidental. The intended active Sunlight effect remains **Gain 1 Sunlight token**. A03 does not change or remove legacy `RAISE_DIE_PLUS_3` code.

## How to run

From the `leaf` project root after installing this patch:

```bash
python3 bin/a03_verify_boundaries.py --verbose
python3 -m unittest discover -s bin/tests -p 'test_a03_boundaries.py' -v
```

- Exit 0 / `A03 PASS`: no protected change (or every change explicitly named/approved). It is **not** proof that new scoring is good or gameplay is correct.
- Exit 1 / `A03 FAIL`: a protected file was modified, added or removed without an explicit exception.
- Exit 2 / `A03 ERROR`: bad manifest, invalid approval, protected symlink, or I/O failure.

The manifest records baseline file-content hashes, not mtime, ZIP/TGZ byte-for-byte identity, or current Git HEAD. It can be run offline with Python 3's standard library. No Gradle dependency is needed to run this boundary check.

## Intentional exception procedure

When an explicitly approved task really requires changing a protected source file (e.g., **separately** approved removal of legacy Sunlight code):

1. Make the change and inspect the exact Git diff.
2. Name **each** approved path (no wildcards), give a meaningful rationale, and run the checker. It will continue to block any unrelated modifications.

```bash
python3 bin/a03_verify_boundaries.py \
  --approve-path src/main/kotlin/dugsolutions/leaf/v35/effect/handler/ExampleEffectHandler.kt \
  --reason 'Explicitly approved canonical handler correction'
```

3. If the protected change is to become the new reference, request a proposed replacement manifest, **not an automatic overwrite**:

```bash
python3 bin/a03_verify_boundaries.py \
  --approve-path src/main/kotlin/dugsolutions/leaf/v35/effect/handler/ExampleEffectHandler.kt \
  --reason 'Explicitly approved canonical handler correction' \
  --propose-baseline /tmp/A03_proposed.json
```

4. Review `git diff` and the proposed manifest. Commit the reviewed changes, including the updated `doc/A03_PROTECTED_BASELINE.json` if deliberately accepted. An exception alone does not change the recorded baseline, so unreviewed changes still fail next time.

## Required next steps and explicit non-goals

- Implement the master expected-pip valuation in **Human Baseline**, with Battle-scoring calibration alongside Cultivation scoring.
- Preserve R3B.1 mechanics and research-only effect overrides (Root Four More Raise +20, Queen's Blossom Draw 4, Gain 20 VP and disabled controls).
- Independently test game mechanics and source/cost/market invariants; A03 does not replace A02 or future Sunlight behavior tests.
- Build user-reviewed realistic market/hand/Battle scenario integration tests and eventually run the complete 36-card balance investigation.
- Do **not** touch canonical card costs, CSVs, weights, or learner behavior under the guise of this A03 guard task.
