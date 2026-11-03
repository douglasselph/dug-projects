# Weak Plant activation and Refresh judgment

The Human Baseline evaluates ordinary Plant effects and the short (one-/two-card) group-Refresh opportunity separately. In a Creature with many face-up Plants, it cannot perfectly foresee the full cycle. For a face-up `RESEARCH_NO_EFFECT` or `RAISE_ANY_DIE_PLUS_1` card when **at least one other Plant is already face down**, Cultivation Main now adds a provisional long-horizon nudge (+8 only with more than two face-up Plants), plus one **seeded strategy-RNG** sample in [-8, +8] to the candidate's priority. The sample is visible in the score explanation and is drawn only for eligible decisions. Near completion, existing `CreatureRefreshValue` scoring remains in force. This is human behavioral variability, **not** an objective value assigned to an effect; learned policies and mechanical RNG are unchanged.

## Worm Flip: bidirectional rule and Human Baseline judgment

A Worm flips **one grafted Plant card in either direction**: face down to face up (recover a spent ability), or face up to face down (help clear the remaining ready Plants so the whole Creature can naturally Refresh at Cleanup). The engine's `Creature.flip()` and Support executor already support both directions. The earlier description limiting Worm to face-down → face-up was incorrect.

The Human Baseline now considers the face-up → face-down option when it is the **last face-up card** and other cards are spent. In Cultivation this uses an explainable Refresh benefit and existing resource-reserve penalty; during Battle it uses a conservative end-of-Battle Cleanup benefit. It avoids spending a Worm on this option when it cannot complete the natural Refresh immediately. Existing face-down → face-up recovery scoring remains intact. This is a deliberately bounded heuristic, not perfect long-range planning. The previously added seeded uncertainty for activating weak Plants is retained.

## Research boundary

`RESEARCH_NO_EFFECT` remains a genuine suppression control, not an automatic Raise +1 substitute. A second `RAISE_ANY_DIE_PLUS_1` condition would ask a different question and needs its own explicit question definition. The new score adjustment can alter Human Baseline decisions and thus invalidates direct comparability with earlier *Human Baseline* cohorts unless source versions are recorded. Do not relabel historical 36-Plant results.

## Focused follow-up

Run existing deterministic Human Baseline Cultivation tests and one small paired research pilot; specifically inspect the selected weak-Plant activations, remaining face-up count, natural Cleanup Refreshes, and Water/Worm use. A broader 36-card rescreen should wait until the pilot confirms reasonable behavior. This is one bounded follow-up, not a new calibration program.
