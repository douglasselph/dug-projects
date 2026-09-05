package dugsolutions.leaf.simulation.v35.strategy

/**
 * Strategy sophistication labels used in simulation metadata.
 *
 * Level 0 is deterministic Mechanical Control. Level 1 is the canonical Human
 * Baseline. Tactical and higher layers belong to simulation experimentation.
 */
enum class StrategyLevel(
    val number: Int,
    val displayName: String
) {
    MECHANICAL_CONTROL(0, "Mechanical Control"),
    HUMAN_BASELINE(1, "Human Baseline"),
    TACTICAL(2, "Tactical"),
    STRATEGIC(3, "Strategic"),
    LEARNED_ADAPTIVE(4, "Learned/Adaptive");

    companion object {
        fun fromNumber(number: Int): StrategyLevel =
            entries.firstOrNull { it.number == number }
                ?: error("Unknown strategy level: $number")

        /** Backward-compatible old Level-0 name. */
        @Deprecated("Use MECHANICAL_CONTROL")
        val MECHANICAL_BASELINE: StrategyLevel
            get() = MECHANICAL_CONTROL

        /** Backward-compatible old Level-1 name. */
        @Deprecated("Use HUMAN_BASELINE")
        val SIMPLE_HEURISTIC: StrategyLevel
            get() = HUMAN_BASELINE
    }
}
