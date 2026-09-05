package dugsolutions.leaf.simulation.v35.strategy

/** Central registry of named strategy layers and their current status. */
object StrategyCatalog {
    data class Family(
        val level: StrategyLevel,
        val implemented: Boolean,
        val purpose: String
    )

    val families: List<Family> = listOf(
        Family(
            StrategyLevel.MECHANICAL_CONTROL,
            implemented = true,
            purpose = "Stable deterministic engine/test control with almost no strategic intelligence."
        ),
        Family(
            StrategyLevel.HUMAN_BASELINE,
            implemented = true,
            purpose = "Canonical simulation baseline for simple, reasonable human play; " +
                "its distinct wiring exists now and contextual scoring is the next implementation stage."
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

    val mechanicalControl: StrategyProfile
        get() = StrategyProfile.mechanicalControl()

    val humanBaseline: StrategyProfile
        get() = StrategyProfile.humanBaseline()

    /** Canonical simulation baseline. */
    val baseline: StrategyProfile
        get() = humanBaseline

    /** Backward-compatible old Level-0 property name. */
    @Deprecated("Use mechanicalControl")
    val mechanicalBaseline: StrategyProfile
        get() = mechanicalControl
}
