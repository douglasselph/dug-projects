# NC-01 / NC-02 — Authoritative card inventory and Round CSV migration

**Baseline supplied:** `leaf(20261105-235751).tgz` (November 5, 2026).
**Status:** CSV inventory + source-path migration complete. **Effect conversion/execution remains pending** (NC-03 and NC-05–NC-22); this is **not** a full-game regression PASS.

## Source-of-truth contract

Exactly four official card catalogs are read from `data/`:

- `Cultivation_Cards - Root.csv`
- `Cultivation_Cards - VF.csv`
- `Wisp_Cards.csv`
- `Round_Cards.csv` **(sole official Round catalog)**

The user removed `Turn_Cards.csv`. `CardDataFiles.ROUND_CARD_LIST` now points exclusively to `Round_Cards.csv`. There is **no fallback**. The audit fails if `Turn_Cards.csv` reappears in `data/`, so a future experiment cannot accidentally choose it alongside the official file. Research overlays remain opt-in; no archived data or legacy/protected-baseline manifests were rewritten.

Active reference consumers migrated: Kotlin `CardDataFiles`/`RoundCardRegistry` description; R0 input audit; v38 ruleset provenance and associated tests; Sunlight A05/B08 static parity scripts/tests; current Battle-pacing input fingerprinting; current joint Buy/Round study captions; v38 regression/main-runtime evidence scripts; current README/data override documentation. Historical protected-baseline JSON and historical experiment documents retain old paths/hashes intentionally.

## Reproduce the exact catalog manifest

From the repository top level on Ubuntu:

```bash
python3 bin/audit_authoritative_cards.py --out ~/Downloads/leaf-cards-manifest.json
python3 -m unittest discover -s bin/tests -p 'test_authoritative_card_inventory.py' -v
python3 -m unittest discover -s bin/tests -p 'test_v38_research_provenance.py' -v
python3 bin/r0_audit_inputs.py --out ~/Downloads/leaf-r0-input-audit.json
python3 bin/check_sunlight_rule_parity.py
```

The attached versioned `NC01_02_MANIFEST_2026-11-05.json` is a deterministic snapshot of all **60** definition rows with raw fields, normalized IDs, per-definition fingerprints, per-file SHA-256 values, and a composite catalog fingerprint. It records `git.head = null` and `git.dirty = null` **only because the supplied `.tgz` archive did not contain a Git checkout**. The executable audit records the actual HEAD and dirty state when run inside a Git checkout. Do not infer the source revision from archive date alone.

Verified structural invariants:

| Group | Definitions | Physical copies | Additional checks |
|---|---:|---:|---|
| Roots | 12 | 72 | 4 each in costs 5, 7, 9 |
| Vines and Flowers | 24 | 144 | 4 each in six legal cost/type slots |
| Wisps | 12 | 32 | `count` matches `quantity`; Wispquake has quantity 0 |
| Round cards | 12 | 18 | 6 Battle ×1; 6 Cultivation ×2; each has 2 effects |

**Round behavior source:** `Battle_Bloom_Surge` has `Gain up to a D12. Place in Discard.`; `Battle_Scatter_Swell` has `Gain up to a D8. Use it now.` The old Round CSV is not read. The current `GameEffectConverter` does not yet recognize these texts. **Do not replace them with previous D12/D10 simple effects or interpret a successful catalog audit as successful game execution.** Finish NC-03 and NC-21/NC-22, then run full Kotlin regressions.

## Art-only discrepancy for later review

The incoming Swell definition says **up to D8**, but its `effect_2_image` references `turn_surge_D12` and its `effect_2_icon` references `D10`. The CSV is the source of truth; no art references were changed in NC-01/02. Please review these source fields during NC-22/production art alignment. This has no bearing on filename migration or source hashing.

## Evidence limits

`bin/audit_authoritative_cards.py` validates **catalog structure and identity**, not whether all textual effects have Kotlin implementations. An unsupported effect is still an explicit, expected failure at card-registry load time until NC-03. Existing v38-4 through v38-7 regression evidence belongs to the earlier card definitions; it must not be misattributed to this new fingerprint.
