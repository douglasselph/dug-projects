# C04 Gust legacy regression reconciliation

The `BattleEnabledPlantAnalyzerTest` Gust assertion expected precisely one
`BattleSwing` and called `rowSwings.single()` at line 156. The new Gust
projector enumerates every face of the selected D20 and retains one weighted
row swing per first-reroll outcome. Its result therefore contains 20 weighted
swings (all TOP in this deliberately one-open-row fixture).

The assertion is replaced with tests of the 20 branches, valid row attribution,
finite contributions, presence of a winning branch, and exact consistency
between weighted swing sum and exposed tactical utility. No production code or
card mechanics have changed.

To run:

```bash
./gradlew test --tests '*BattleEnabledPlantAnalyzerTest' --tests '*C04GustOutcomeEnumerationTest'
./gradlew test simulationTest
python3 bin/a03_verify_boundaries.py
```

This does not certify every Gust target choice; it corrects a regression that
assumed the former single-average projection output shape.
