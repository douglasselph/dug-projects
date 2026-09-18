package dugsolutions.leaf.simulation.v35.experiment.card

/**
 * Compact per-game observation retained by the aggregator.
 *
 * The experiment intentionally stores scalar metrics rather than the mutable
 * Game or full Chronicle so thousands of games can be summarized cheaply.
 */
data class CardFocusGameObservation(
    val focusSeat: Int,
    val focusWon: Boolean,
    val focusWinShare: Double,
    val baselineAverageWinShare: Double,
    val focusVp: Int,
    val baselineAverageVp: Double,
    val targetPurchasedCopies: Int,
    val targetGraftedCopiesAtEnd: Int,
    val targetActivations: Int,
    val targetPlantVp: Int,
    val battleStrikeVp: Int
) {
    init {
        require(focusSeat >= 0) { "Focus seat cannot be negative: $focusSeat" }
        require(focusWinShare in 0.0..1.0) { "Invalid focus win share: $focusWinShare" }
        require(baselineAverageWinShare in 0.0..1.0) {
            "Invalid baseline win share: $baselineAverageWinShare"
        }
        require(targetPurchasedCopies >= 0)
        require(targetGraftedCopiesAtEnd >= 0)
        require(targetActivations >= 0)
        require(targetPlantVp >= 0)
        require(battleStrikeVp >= 0)
    }

    val vpDeltaVsBaseline: Double
        get() = focusVp - baselineAverageVp

    val winShareDeltaVsBaseline: Double
        get() = focusWinShare - baselineAverageWinShare
}
