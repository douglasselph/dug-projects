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

**Runtime boundary:** Three Engagements currently lives in the `simulation`
source set and is discovered via `BattleStructureProvider`. Ordinary simulation
launchers include this source set. A main-only runtime that does not include
`simulation` fails explicitly when asked to execute Three Engagements; it will
**not** silently substitute the legacy engine. Main-source Kotlin unit tests
and historical integration fixtures intentionally request `LEGACY_WHOLE_BATTLE`.

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
