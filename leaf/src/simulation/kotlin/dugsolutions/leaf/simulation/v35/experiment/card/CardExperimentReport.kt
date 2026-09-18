package dugsolutions.leaf.simulation.v35.experiment.card

/** Small text renderer suitable for console/debug experiment output. */
object CardExperimentReport {
    fun render(result: CardExperimentResult): String = buildString {
        appendLine("Card focus: ${result.targetCard.cardName} × ${result.targetCount}")
        appendLine("Games: ${result.gamesCompleted} (${result.numPlayers} seats)")
        appendLine("Focused winner rate: ${pct(result.focusedWinnerRate)}")
        appendLine("Focused win share: ${pct(result.focusedWinShare)}")
        appendLine("Baseline avg win share: ${pct(result.baselineAverageWinShare)}")
        appendLine("Win-share delta: ${signedPct(result.winShareDeltaVsBaseline)}")
        appendLine("Focused avg VP: ${fmt(result.focusedAverageVp)}")
        appendLine("Baseline avg VP: ${fmt(result.baselineAverageVp)}")
        appendLine("VP delta: ${signed(result.averageVpDeltaVsBaseline)}")
        appendLine("Acquisition game rate: ${pct(result.acquisitionGameRate)}")
        appendLine("Avg copies purchased: ${fmt(result.averageTargetCopiesPurchased)}")
        appendLine("Avg copies surviving: ${fmt(result.averageTargetCopiesAtEnd)}")
        appendLine("Avg activations: ${fmt(result.averageTargetActivations)}")
        appendLine("Avg target Plant VP: ${fmt(result.averageTargetPlantVp)}")
        appendLine("Avg Battle Strike VP: ${fmt(result.averageBattleStrikeVp)}")
    }.trimEnd()

    private fun pct(value: Double): String = "%.2f%%".format(value * 100.0)
    private fun signedPct(value: Double): String = "%+.2f%%".format(value * 100.0)
    private fun fmt(value: Double): String = "%.3f".format(value)
    private fun signed(value: Double): String = "%+.3f".format(value)
}
