package dugsolutions.leaf.simulation.v35.strategy

/**
 * Coarse strategy sophistication levels used to label simulation experiments.
 *
 * A player's individual decision areas may sit at different levels. For
 * example, an experiment may use SIMPLE_HEURISTIC buying while every other
 * decision area remains MECHANICAL_BASELINE.
 */
enum class StrategyLevel(
    val number: Int,
    val displayName: String
) {
    MECHANICAL_BASELINE(0, "Mechanical Baseline"),
    SIMPLE_HEURISTIC(1, "Simple Heuristic"),
    TACTICAL(2, "Tactical"),
    STRATEGIC(3, "Strategic"),
    LEARNED_ADAPTIVE(4, "Learned/Adaptive");

    companion object {
        fun fromNumber(number: Int): StrategyLevel =
            entries.firstOrNull { it.number == number }
                ?: error("Unknown strategy level: $number")
    }
}
