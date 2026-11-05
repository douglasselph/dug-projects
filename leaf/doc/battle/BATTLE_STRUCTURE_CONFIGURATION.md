# Battle structure selection — v38-01

The adopted v38-01 Battle system is `THREE_ENGAGEMENTS`. The old pre-v38
whole-Battle system is retained as `LEGACY_WHOLE_BATTLE` for historical
reproduction. Both selectors use the same engine entry point with different
Battle coordinators; there is no automatic legacy fallback.

Optional configuration file:

```text
data/research/resync/game-rules-current.properties
```

```properties
battle.structure=THREE_ENGAGEMENTS
# To reproduce a pre-v38 Battle instead, set:
# battle.structure=LEGACY_WHOLE_BATTLE
```

If the file is missing, or the property is missing or empty, **Three Engagements
is selected**. `GameConfig(battleStructure = BattleStructure.LEGACY_WHOLE_BATTLE,
...)` overrides the file for an individual game. A setting change affects only
newly constructed `GameConfig` instances.

The old name `CANONICAL` is no longer a valid `battle.structure` value; a stale
properties file triggers a clear error instead of silently selecting the wrong
version. Other unrelated uses of `CANONICAL` (official Plant/Round definitions)
are unchanged.

**Runtime boundary (V38-6 patch, pending Ubuntu acceptance):** Three Engagements
scheduler and its Human decision adapter now compile in the `main` source set.
`GameModule` wires `ThreeEngagementRoundExecutor` directly; coordinators
constructed without explicit injection can construct it from a real
`BattleRound`. No ServiceLoader extension or simulation-only dependency is
necessary, and legacy is never used as a fallback. The original Kotlin
package names remain unchanged for source/binary compatibility with historical
research. The deprecated `BattleStructureProvider` interface is retained as a
compatibility marker, but no provider is registered. New main-only Kotlin
runtime tests enforce this independent source-set boundary.

**Historical research:** The dedicated v37-03 pacing A/B runner invokes its
comparison conditions directly; it ignores this settings file. Historical R3A
research is now pinned explicitly to `LEGACY_WHOLE_BATTLE` in its GameConfigs.
Research question examples created for the historical R3A investigation retain
that mode explicitly. Use `THREE_ENGAGEMENTS` for new v38 studies, and record
Battle structure in each study's inputs, manifests, and comparisons. Never
compare v37 and v38 studies as though they used identical rules.

Test commands:

```bash
./gradlew test --tests '*RoundCoordinatorTest'
./gradlew simulationTest --tests '*BattleStructureRoutingTest'
python3 -m unittest discover -s bin/tests -p 'test_plant_question_spec.py' -v
python3 -m unittest discover -s bin/tests -p 'test_plant_question_launcher.py' -v
```
