package dugsolutions.leaf.simulation.v35.experiment.baseline

/**
 * Classifies only unambiguous scored Main/FinalMain labels emitted by
 * DecisionLabelFormatter. Other scored choices (including bare "Draw") are
 * deliberately excluded: they may be subordinate decisions.
 */
internal object MainActionFamily {
    fun classify(label: String): String? {
        val action = when {
            label.startsWith("Main(") -> label.removePrefix("Main(")
            label.startsWith("FinalMain(") -> label.removePrefix("FinalMain(")
            else -> return null
        }
        return when {
            action.startsWith("Draw)") || action.startsWith("Draw,") -> "Draw"
            action.startsWith("ActivatePlant(") -> "Plant"
            action.startsWith("RoundEffect1)") || action.startsWith("RoundEffect1,") ||
                action.startsWith("RoundEffect2)") || action.startsWith("RoundEffect2,") -> "Round effect"
            else -> "Unrecognized Main"
        }
    }
}
