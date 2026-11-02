# C04 — pure Battle reroll consequence follow-up

This increment routes `REROLL_DIE_UNTIL_3_PLUS_IGNORE_ROLL_REWARDS`
through BattleOwnTotalChangeAnalyzer at activation time. Each permitted
face (3..sides) is projected separately through the Strike-row consequence
engine; resulting tactical utilities are averaged. This avoids substituting
a mean face before evaluating nonlinear ties, wins and Wounds.

An unavailable Battle board or legal target yields no projection, without
fabricating a tactical bonus. Compound rerolls that also modify opponents or
draw another die remain on their existing specialized paths; treating only
the friendly reroll would overstate their complete effect.

NOT C04 COMPLETE: Round effects, opponent-side transformations, compound
rerolls and universal within-Battle scale calibration still require review.
C05 unit harmonization also remains open. This patch intentionally preserves
legacy candidate ranking and canonical mechanics.

Verification:

    ./gradlew test --tests '*C04RerollActivationPriorityTest'
    ./gradlew test simulationTest
    python3 bin/a03_verify_boundaries.py

The Kotlin test suite must be run in an available local Gradle environment.
