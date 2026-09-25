package dugsolutions.leaf.simulation.v35.experiment.baseline

import dugsolutions.leaf.simulation.v35.analysis.GameSummary
import dugsolutions.leaf.simulation.v35.analysis.OwnedDiceSignature
import dugsolutions.leaf.simulation.v35.analysis.PlantCreatureSignature
import dugsolutions.leaf.simulation.v35.experiment.BatchRunResult

/**
 * Small-run diagnostic view of complete Human Baseline games.
 *
 * This is intentionally descriptive rather than a balance gate. Repeated
 * winners or repeated development shapes are legal outcomes; the diagnostic
 * exists to make gross sameness visible and to support deterministic
 * reproducibility/seed-sensitivity checks.
 */
data class BaselineRandomnessDiagnostic(
    val games: List<BaselineDiagnosticGame>
) {
    init {
        require(games.isNotEmpty()) { "Randomness diagnostic requires at least one game" }
    }

    /** Stable value-only fingerprint suitable for same-seed/different-seed comparisons. */
    val fingerprint: String = games.joinToString("||") { game ->
        buildString {
            append(game.winnerSeats.joinToString(","))
            append("::")
            append(game.players.joinToString(";") { player ->
                "${player.seat}:${player.plantSignature.canonicalText()}:${player.diceSignature.canonicalText()}"
            })
        }
    }

    fun prefix(gameCount: Int): BaselineRandomnessDiagnostic {
        require(gameCount in 1..games.size) {
            "Diagnostic prefix must be between 1 and ${games.size}: $gameCount"
        }
        return BaselineRandomnessDiagnostic(games.take(gameCount))
    }

    companion object {
        fun from(batch: BatchRunResult): BaselineRandomnessDiagnostic =
            BaselineRandomnessDiagnostic(
                batch.summaries.mapIndexed { index, summary -> summary.toDiagnosticGame(index + 1) }
            )
    }
}

data class BaselineDiagnosticGame(
    val gameNumber: Int,
    val mechanicalSeed: Long?,
    val strategySeed: Long?,
    val winnerSeats: List<Int>,
    val players: List<BaselineDiagnosticPlayer>
)

data class BaselineDiagnosticPlayer(
    val seat: Int,
    val totalVp: Int,
    val plantSignature: PlantCreatureSignature,
    val diceSignature: OwnedDiceSignature
)

private fun GameSummary.toDiagnosticGame(gameNumber: Int): BaselineDiagnosticGame =
    BaselineDiagnosticGame(
        gameNumber = gameNumber,
        mechanicalSeed = mechanicalSeed,
        strategySeed = strategySeed,
        winnerSeats = players.filter { it.won }.map { it.seat }.sorted(),
        players = players.sortedBy { it.seat }.map { player ->
            BaselineDiagnosticPlayer(
                seat = player.seat,
                totalVp = player.totalVp,
                plantSignature = player.plantCreatureSignature,
                diceSignature = player.ownedDiceSignature
            )
        }
    )

internal fun PlantCreatureSignature.canonicalText(): String =
    if (cards.isEmpty()) "(none)" else cards.joinToString(",") { card ->
        "${card.plantName}@${card.side.name}(${card.x},${card.y})"
    }

internal fun OwnedDiceSignature.canonicalText(): String =
    "D4=$d4,D6=$d6,D8=$d8,D10=$d10,D12=$d12,D20=$d20"
