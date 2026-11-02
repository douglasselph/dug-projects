# C04 next verification: real Plant/Draw/Round comparison

This adds regression coverage at `BattleMainPriority.score`, using a real `CreatureCard` for Root Four More instead of evaluating only the direct-effect helper. It verifies that a legal deterministic Plant projection reaches the actual Main scorer unchanged, exhausted Wisp acquisition cannot outrank that projection, and capped face changes do not earn the tactical reward of a winning Raise.

## Remaining C04 sign-off work

1. Run these new tests and the full `test simulationTest` suite on Ubuntu; investigate any failure rather than only editing assertions.
2. Align action and target projections for compound rerolls, Sapping/collateral, opponent effects, and Parting Thorn with genuinely legal targets and realistic state fixtures. A target selector must not contradict the action evaluator.
3. Finish contextual Plant effects and Round options (Worm, refresh, future dice, Wisp) with available action capacity and future usage; avoid valuing unprojectable benefits as calibrated tactical utility.
4. Replace the remaining provisional 35-point common origin and resource assumptions with an explicit Battle utility policy; verify each tactical component (Strike VP, wounds, denial, ties, capacity) is included once.
5. Add representative multi-row/multiplayer mixed candidate scenarios, including final Main and Sunlight, plus counterexamples where the correct preference reverses with the state.
6. Confirm complete green Kotlin suites and A03 PASS on the final archive before checking C04 off.

This patch does **not** assert that C04 is complete or that the calibration coefficients have empirical support. No production code or frozen policies are modified.
