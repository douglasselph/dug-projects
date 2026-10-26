C04 behavior contract fixture correction

Two HumanBaselineBattleStrategyTest scenarios assumed that Parting Thorn was a strong tactical Plant merely because its obsolete Battle base was 80.

Neither fixture includes a spent friendly Plant to refresh or an opponent Plant to wound, so no positive legal target benefit is established. With C04 normalized fallback, available Draw should beat the unsupported Plant action in both cases. These tests now assert Draw and are renamed accordingly.

No production code changes. This does not prove Parting Thorn is correctly valued when a real legal target exists; add separate concrete target-selection tests before C04 sign-off.
