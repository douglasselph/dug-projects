# B08 — Sunlight rule parity (post-B03)

## Source-of-truth conclusion

**Sunlight rule parity: PASS** for the submitted `leaf(20261026-140253).tgz` snapshot, according to the repeatable static source/data audit:

```bash
python3 bin/check_sunlight_rule_parity.py
```

The audit returns exit code **0** and `Sunlight rule parity: PASS` when consistent. Any failing check returns exit code **1**, details the mismatch, and prints `Sunlight rule parity: FAIL`.

## Checks

- Exactly three official Cultivation Round Sunlight entries: `Resource_Sunlight_Compost`, `Resource_Sunlight_Mulch`, and `Resource_Sunlight_Water` — each says `Gain Sunlight token`.
- The converter uses `GAIN_SUNLIGHT_TOKEN` and explicitly rejects obsolete Raise +3 printed inputs, rather than silently reinterpreting them.
- The default executor routes token gain to the resource handler and refuses direct execution of `RAISE_DIE_PLUS_3`.
- The resource handler checks Grove stock; the Sunlight resolver transfers a physical token from Grove to player and spending transfers it back.
- Human Baseline scores Sunlight **token acquisition** and rejects old Round effects; obsolete `SunlightPriority.kt` no longer exists.
- Research Round resync rejects `RAISE_DIE_PLUS_3`.
- Production/simulation references to the obsolete enum are restricted to an explicit compatibility constant and rejection sites. No new active handler, target selection, or Battle path may refer to it.

## Intentionally preserved history

- `GameEffect.RAISE_DIE_PLUS_3` remains an enum compatibility marker, not a legal executable effect.
- Historical B05 archive, retired launcher, older tests, and trained-weight feature labels may still contain Raise +3 references. These are *not active gameplay rules*; they must not be silently rewritten or removed.

## Limits

This check is **static**, not a substitute for Kotlin runtime tests. Continue running the A05, B02, B03, B06, B07 and complete test suites on Ubuntu. It does not prove Battle timing, token conservation during an entire game, or arbitrary historic experiments. The broad source scan detects exact enum references; dynamically constructed reflection or unrecognized textual aliases require additional runtime validation.

This increment modifies only `bin/check_sunlight_rule_parity.py`, `bin/tests/test_b08_sunlight_parity.py`, and this document. It does not change canonical mechanics, research weights or protected inputs.
