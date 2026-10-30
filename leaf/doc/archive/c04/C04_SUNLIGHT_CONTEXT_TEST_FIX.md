# C04 Sunlight-spend tests after contextual Round scoring

Updated the SunlightBattleSpendingValueTest fixtures, not production policy.

* The `GAIN_ANY_DIE_TO_DISCARD` test now supplies two future rounds, so its
  provisional future-draw estimate is 35 + int(10.5 * 2) = 56 rather than
  zero when no future rounds exist.
* The opponent-Wisp-steal test supplies three opponents with one Wisp each,
  allowing `STEAL_RANDOM_WISP_FROM_ALL_OPPONENTS` to score 35 + 3 * 12 = 71.
* Existing preservation tests still check the intended early/middle/final Battle
  timing and one-versus-two Sunlight token effects.
* Existing empty-resource regression tests (C04RoundEffectContextTest) remain
  responsible for asserting zero value when nothing can be acquired.

No production mechanics, scoring values, frozen weights, or policies changed.
