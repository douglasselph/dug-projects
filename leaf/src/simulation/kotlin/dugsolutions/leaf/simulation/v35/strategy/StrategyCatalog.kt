package dugsolutions.leaf.simulation.v35.strategy

/**
 * Central registry of strategy families and their implementation status.
 *
 * Only the Mechanical Baseline is implemented today. Higher levels are named
 * and reserved deliberately so new work is added as explicit strategy code
 * rather than silently making Level 0 smarter.
 */
object StrategyCatalog {
    data class Family(
        val level: StrategyLevel,
        val implemented: Boolean,
        val purpose: String
    )

    val families: List<Family> = listOf(
        Family(
            StrategyLevel.MECHANICAL_BASELINE,
            implemented = true,
            purpose = "Stable deterministic control group with almost no strategic intelligence."
        ),
        Family(
            StrategyLevel.SIMPLE_HEURISTIC,
            implemented = false,
            purpose = "Small local heuristics that improve one decision without planning ahead."
        ),
        Family(
            StrategyLevel.TACTICAL,
            implemented = false,
            purpose = "Short-horizon evaluation of immediate combinations, threats, and payoffs."
        ),
        Family(
            StrategyLevel.STRATEGIC,
            implemented = false,
            purpose = "Whole-game planning using broader game-state and opponent information."
        ),
        Family(
            StrategyLevel.LEARNED_ADAPTIVE,
            implemented = false,
            purpose = "Persisted behavior trained from simulation outcomes and decision traces."
        )
    )

    val mechanicalBaseline: StrategyProfile
        get() = StrategyProfile.mechanicalBaseline()
}
