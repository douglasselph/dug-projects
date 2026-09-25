package dugsolutions.leaf.simulation.v35.experiment.baseline

/** Human-readable 4/8/12-style diagnostic output for manual diversity review. */
object BaselineRandomnessDiagnosticRenderer {
    fun render(diagnostic: BaselineRandomnessDiagnostic): String = buildString {
        appendLine("Human Baseline small-run randomness diagnostic")
        appendLine("Games: ${diagnostic.games.size}")
        appendLine("NOTE: repeated winners or development shapes are legal; this output is diagnostic, not pass/fail.")

        diagnostic.games.forEach { game ->
            appendLine()
            appendLine(
                "Game ${game.gameNumber}  mechanicalSeed=${game.mechanicalSeed ?: "random"} " +
                    "strategySeed=${game.strategySeed ?: "random"}  " +
                    "winnerSeat(s)=${game.winnerSeats.joinToString(",") { (it + 1).toString() }}"
            )
            game.players.forEach { player ->
                appendLine(
                    "  Seat ${player.seat + 1} VP=${player.totalVp} " +
                        "Plant=[${player.plantSignature.canonicalText()}] " +
                        "Dice=[${player.diceSignature.canonicalText()}]"
                )
            }
        }
    }.trimEnd()
}
