# C04 — Round scale and mixed-candidate follow-up

This increment removes ungrounded fixed 60/55/45 priority totals for non-Strike Round actions and adds tests against the actual Battle Main action scorer.

- `GAIN_ONE_VP`: common Battle origin 35 plus 3 decision points for one immediate *non-Strike* VP. Strike award benefits already modeled by tactical projectors must not be added again.
- `REFRESH_CREATURE`, `GAIN_TWO_WORMS`, and unhandled Round effects: neutral origin 35 with **explicit unprojected** diagnostic, rather than a fabricated +10/+20 benefit. This is an interim fallback, not a claim those effects are worthless. Further contextual tactical and Support value projection is needed.
- Existing Wisp/steal/die-to-discard contextual paths are unchanged, including their provisional legacy scale.

C04 is **not** complete. Draw still carries 35 + 4× expected pips, Wisp and future-draw Round preferences remain provisional, and generic Plant effects can still carry old card-specific base priorities. Full common Battle tactical calibration requires migrating these competing paths, realistic mixed candidate ranking acceptance tests, and the full Ubuntu Kotlin suite.

Verification:

```
./gradlew test --tests '*C04RoundScaleMixedSelectionTest' --tests '*C04MixedCandidateSelectionTest'
./gradlew test simulationTest
python3 bin/a03_verify_boundaries.py
```

The local environment cannot download Gradle 8.13, so Kotlin compilation/tests are not verified here.
