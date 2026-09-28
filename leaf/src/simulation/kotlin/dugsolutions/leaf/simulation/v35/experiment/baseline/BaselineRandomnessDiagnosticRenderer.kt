package dugsolutions.leaf.simulation.v35.experiment.baseline

import dugsolutions.leaf.simulation.v35.analysis.OwnedDiceSignature
import dugsolutions.leaf.simulation.v35.analysis.PlantCreatureSignature

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
                        "Plant=[${player.plantSignature.reportText()}] " +
                        "Dice=[${player.diceSignature.reportText()}]"
                )
            }
        }
    }.trimEnd()
}

/** Compact composition-only view for the human-facing report; placement is intentionally omitted. */
private fun PlantCreatureSignature.reportText(): String {
    if (cards.isEmpty()) return "(none)"

    return cards
        .map { card -> compactPlantName(card.plantName) }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedWith(compareBy({ plantTypeOrder(it.key) }, { plantCost(it.key) }, { it.key }))
        .joinToString(", ") { (name, count) -> if (count == 1) name else "$count$name" }
}

private fun compactPlantName(plantName: String): String {
    val parts = plantName.split('_')
    val prefix = when (parts.firstOrNull()) {
        "Root" -> "R"
        "Vine" -> "V"
        "Flower" -> "F"
        else -> return plantName
    }
    val cost = parts.getOrNull(1)?.toIntOrNull() ?: return plantName
    return "$prefix$cost"
}

private fun plantTypeOrder(name: String): Int = when (name.firstOrNull()) {
    'R' -> 0
    'V' -> 1
    'F' -> 2
    else -> 3
}

private fun plantCost(name: String): Int = name.drop(1).toIntOrNull() ?: Int.MAX_VALUE

/** Compact owned-dice composition; zero-count die sizes are omitted. */
private fun OwnedDiceSignature.reportText(): String =
    listOf(
        d4 to "D4",
        d6 to "D6",
        d8 to "D8",
        d10 to "D10",
        d12 to "D12",
        d20 to "D20"
    )
        .filter { (count, _) -> count > 0 }
        .joinToString(", ") { (count, die) -> "$count$die" }
        .ifEmpty { "(none)" }
