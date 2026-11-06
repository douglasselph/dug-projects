# Authoritative card catalogs and Round CSV source of truth

**Baseline supplied:** `leaf(20261105-235751).tgz` (November 5, 2026).
**Status:** Catalog validation, Round CSV source-path migration, and explicit recognition of all authoritative effect texts are complete. **New effect execution remains incomplete**; these audits do not establish full-game compatibility.

## Source-of-truth contract

Exactly four official card catalogs are read from `data/`:

- `Cultivation_Cards - Root.csv`
- `Cultivation_Cards - VF.csv`
- `Wisp_Cards.csv`
- `Round_Cards.csv` **(sole official Round catalog)**

The user removed `Turn_Cards.csv`. `CardDataFiles.ROUND_CARD_LIST` now points exclusively to `Round_Cards.csv`. There is **no fallback**. The audit fails if `Turn_Cards.csv` reappears in `data/`, so a future experiment cannot accidentally choose it alongside the official file. Research overlays remain opt-in; no archived data or legacy/protected-baseline manifests were rewritten.

Active reference consumers migrated: Kotlin `CardDataFiles`/`RoundCardRegistry`; current input audit and research provenance; Sunlight parity scripts and tests; Battle-pacing input fingerprinting; Buy/Round research scripts; Battle regression evidence tools; and current README/data override documentation. Historical protected-baseline JSON and historical experiment documents retain old paths/hashes intentionally.

## Reproduce the exact catalog manifest

From the repository top level on Ubuntu:

```bash
python3 bin/audit_authoritative_cards.py --out ~/Downloads/leaf-cards-manifest.json
python3 -m unittest discover -s bin/tests -p 'test_authoritative_card_inventory.py' -v
python3 -m unittest discover -s bin/tests -p 'test_v38_research_provenance.py' -v
python3 bin/r0_audit_inputs.py --out ~/Downloads/leaf-r0-input-audit.json
python3 bin/check_sunlight_rule_parity.py
```

The dated `CARD_CATALOG_SNAPSHOT_2026-11-05.json` is a **historical, immutable snapshot**, not a live input or a current-catalog guarantee. It records a deterministic snapshot of all **60** definition rows with raw fields, normalized IDs, per-definition fingerprints, per-file SHA-256 values, and a composite catalog fingerprint. It records `git.head = null` and `git.dirty = null` **only because the supplied `.tgz` archive did not contain a Git checkout**. The executable audit records the actual HEAD and dirty state when run inside a Git checkout. Do not infer the source revision from archive date alone.

Verified structural invariants:

| Group | Definitions | Physical copies | Additional checks |
|---|---:|---:|---|
| Roots | 12 | 72 | 4 each in costs 5, 7, 9 |
| Vines and Flowers | 24 | 144 | 4 each in six legal cost/type slots |
| Wisps | 12 | 32 | `count` matches `quantity`; Wispquake has quantity 0 |
| Round cards | 12 | 18 | 6 Battle ×1; 6 Cultivation ×2; each has 2 effects |

**Round behavior source:** `Battle_Bloom_Surge` has `Gain up to a D12. Place in Discard.`; `Battle_Scatter_Swell` has `Gain up to a D8. Use it now.` The old Round CSV is not read. The converter recognizes both with **dedicated, currently pending** effect identities. **Do not replace them with previous D12/D10 simple effects or interpret a successful catalog audit as successful game execution.** Implement Surge/Swell mechanics, then run the full Kotlin regressions.

## Art-only discrepancy for later review

The incoming Swell definition says **up to D8**, but its `effect_2_image` references `turn_surge_D12` and its `effect_2_icon` references `D10`. The CSV is the source of truth; the catalog audit did not change art references. Review these fields before final artwork and Round-effect implementation. This has no bearing on filename migration or source hashing.

## Evidence limits

`bin/audit_authoritative_cards.py` validates **catalog structure and identity**, not whether all textual effects have Kotlin implementations. All 72 official effect fields now convert to known identities. Sixteen new semantics intentionally lack executable handlers. The game runner and effect dispatcher fail closed instead of silently treating them as harmless no-ops. Previously collected Battle regression evidence belongs to the earlier card definitions; it must not be misattributed to this new fingerprint.

## Documentation naming convention

Keep ongoing guides, source files, tests, and generated-output tools named for their purpose (for example, `CARD_CATALOG_SOURCE_OF_TRUTH.md`). Task IDs are appropriate in the active `TASKS.md` checklist and historical research records, but should not be required to locate or invoke normal tools. The dated snapshot is historical; run the audit again when card CSVs change.
