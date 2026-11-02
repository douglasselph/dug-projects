# C04 — Battle consequence evaluator (increment 1)

This increment unifies *projected Battle consequence outputs* for directly
projectable row changes and new/placed dice. It reuses the existing
`BattleActionAnalyzer` / `BattleDiePlacementAnalyzer`, preserving the Strike-row
win/tie/wound/VP/capacity model and avoiding a competing formula. `BattleFirstMainPriority`
now consumes the same typed consequence projection for Draw.

**C04 is NOT completed in this increment.** Existing fixed intrinsic Plant and
Round priorities remain, and reroll, opponent-control, multi-row, and late Main
candidate selection have not been exhaustively migrated. The projected Strike
VP delta is diagnostic, not a second additive bonus (double counting).

Verification:

```
./gradlew test --tests '*C04BattleConsequenceEvaluatorTest' --tests '*BattleDiePlacementAnalyzerTest'
python3 bin/a03_verify_boundaries.py
```

These changes are restricted to Human Baseline source, new tests, and documentation.
