# C04 positive Worm target fixture correction

The previous positive test placed one D4 showing 3 for each player, leaving MIDDLE and BOTTOM at 0-0. Worm placement there could also create a Strike win, so choosing MIDDLE did not prove a strategy failure.

Place opponent dice of values 3, 2, 1 in TOP, MIDDLE, BOTTOM respectively while retaining the actor's sole TOP die of 3. One Worm flips TOP from tie to win; MIDDLE still loses (1 vs 2); BOTTOM only ties (1 vs 1). The original action and timing assertions remain.

No production changes or scoring coefficients. Run `./gradlew test --tests '*BattleActionCoordinatorTest'` and the full test suite.
