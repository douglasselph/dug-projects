# Misplaced effect enhancements — integration audit

Source snapshot: `leaf(20261102-224410).tgz`.

## Conclusion

**No source reintegration is necessary.** The nested `leaf-effect-enhancements/` directory contains an older source patch whose effect additions are already present in the active `src/` tree. Overwriting the active files with the nested copies would remove later work (notably research calibration effects, the several more precise Petal To Die 4 variations, and discontinued Sunlight safeguards).

The misplaced directory contains 14 Kotlin files: 13 differ from their active counterparts, and one (`EffectStrategy.kt`) is byte-identical. It also contains a standalone readme. Every nested Kotlin path already exists in the active source tree. A function declaration inventory found one helper present only by its old name, `acquireD4ToDiscard`; the current Petal implementation uses `acquireD4`, which handles the more recent acquisition choices. This is a refactoring, not an absent research effect.

## Enhancements verified in the active source

| Enhancement in nested README | Present in active source |
|---|---|
| Root Down Payment: `GAIN_VP_PER_ONE_SHOWING` | Yes — enum, converter, handler, contracts |
| Berry Tasty: `RAISE_ALL_D8S_PLUS_1` | Yes — pre-existing/current functionality |
| Bee-loved Bloom: `GAIN_OR_STEAL_BEE_AND_BOOST_BEES_PLUS_2_THIS_ROUND` | Yes — enum and dedicated handler |
| Petal To Die 4: `GAIN_OR_STEAL_D4_THEN_DISCARD_D4S_AND_DRAW` | Yes — enum and extended dedicated handler |
| Vine and Punishment: `REROLL_DIE_ON_3_DRAW_ONE_CULTIVATION_OR_REDUCE_OPPOSING_STRIKE_ROW_BY_3` | Yes — enum and dedicated handler |
| Vine and Punishment combined variant: `REROLL_DIE_ON_3_DRAW_ONE_AND_REDUCE_OPPOSING_STRIKE_ROW_BY_3` | Yes — enum and dedicated handler |
| Sapping Snapdragon: `RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_AROUND_ANOTHER_DIE_IN_STRIKE_ROW` | Yes — enum, converter and active resolution path |

## Conflict review

The active `GameEffect.kt` additionally contains `RESEARCH_*` controls, several D4 acquisition variants, the retired `RAISE_DIE_PLUS_3` marker and an upgrade-without-rewards variant. The nested copy lacks these. The active Petal To Die 4 code handles additional destination/source choices and explicitly protects resolved Strike rows. The current integration `CardEffectContractTest.kt` validates current catalog mappings and ensures the optional Sapping research variant is not silently canonical. Other active files have newer changes; therefore copying any nested Kotlin file wholesale is unsafe.

## Verification limitations

Compared all nested source-file paths against the real tree and inspected effect-ID references, declaration differences, and relevant implementations. Did **not** run a complete byte-level or behavioral equivalence proof. Attempted `./gradlew --offline test --tests '*GameEffectConverterTest'`; the wrapper was not available locally and attempted to fetch a Gradle distribution (`UnknownHostException: services.gradle.org`). No source, CSV, weights, or simulator files were modified.

## Local verification and cleanup

Inside the repository root (`leaf/`):

```bash
./gradlew test --tests '*GameEffectConverterTest'
./gradlew integrationTest --tests '*CardEffectContractTest'
```

After confirming the tests on your Ubuntu machine and checking that `leaf-effect-enhancements/` contains no personal/uncommitted files beyond this archived patch, remove it:

```bash
rm -r -- leaf-effect-enhancements
```

`rm -r` is intentionally **not** included in the patch: cleanup remains an explicit user action. Keep the original `.tgz` as provenance if desired.
