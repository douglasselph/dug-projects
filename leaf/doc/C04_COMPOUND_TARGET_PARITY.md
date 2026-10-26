# C04 compound collateral action-target parity (bounded increment)

The activation scorer for Bloom Backflip and Gust of Petals now exposes the same target-by-target tactical projections that produce the action maximum. Legal target indices can restrict these projections; no legal targets yield no projection. The selected opponent row for Gust remains an expected future branch, not an incorrectly committed target.

This does not change production mechanics or the downstream choice implementation, which already invokes the relevant target analyzers. It makes their common analysis testable and auditable and does not complete all compound-effect families.

Remaining C04 acceptance: prove the downstream strategy picks the maximizing legal target in real requests; add compound draw/refresh and forced opponent variants; calibrate Worm/refresh/future resource opportunity in actual states; eliminate provisional 35-point origins; matrix-test multiplayer multirow Draw/Plant/Round/Sunlight scenarios and complete full Kotlin test/simulationTest verification.
