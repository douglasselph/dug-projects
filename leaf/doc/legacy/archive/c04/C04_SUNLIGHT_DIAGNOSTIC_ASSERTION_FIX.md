# C04 diagnostic test follow-up

The diagnostic fixture holds 2 Sunlight tokens and reports 2 Battles remaining.

Funded Main is 43. Base preservation is 39; timing adjustment 0; one surplus token discounts preservation by 4, giving 35. Net value is 43-35=8.

Only the assertions were corrected. No mechanics or valuation implementation changes.

Run: ./gradlew test --tests '*SunlightBattleSpendingValueTest'
Then: ./gradlew test simulationTest
Then: python3 bin/a03_verify_boundaries.py
