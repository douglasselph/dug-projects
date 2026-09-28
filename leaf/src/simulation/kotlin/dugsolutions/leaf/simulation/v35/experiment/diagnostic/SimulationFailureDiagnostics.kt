package dugsolutions.leaf.simulation.v35.experiment.diagnostic

import dugsolutions.leaf.v35.chronicle.ChronicleTextRenderer
import dugsolutions.leaf.v35.game.Game

/** Context that makes one experiment game reproducible after a rare batch failure. */
data class SimulationRunContext(
    val experiment: String,
    val sample: Int,
    val variant: String,
    val affectedSeat: Int?,
    val mechanicalSeed: Long,
    val strategySeed: Long,
    val grove: String? = null,
    val roundStructure: String? = null
)

/**
 * Runs one already-created Game and enriches RuntimeExceptions with deterministic
 * reproduction coordinates plus a snapshot of the live Game and recent Chronicle.
 *
 * The original exception remains the cause, so its type/message/stack are retained.
 */
inline fun <T> withSimulationFailureDiagnostics(
    game: Game,
    context: SimulationRunContext,
    block: () -> T
): T = try {
    block()
} catch (failure: RuntimeException) {
    throw IllegalStateException(buildSimulationFailureReport(game, context, failure), failure)
}

fun buildSimulationFailureReport(
    game: Game,
    context: SimulationRunContext,
    failure: RuntimeException,
    chronicleTailLines: Int = 120
): String = buildString {
    appendLine("SIMULATION GAME FAILED")
    appendLine("experiment=${context.experiment}")
    appendLine("sample=${context.sample}")
    appendLine("variant=${context.variant}")
    context.affectedSeat?.let { appendLine("affectedSeat=${it + 1}") }
    appendLine("mechanicalSeed=${context.mechanicalSeed}")
    appendLine("strategySeed=${context.strategySeed}")
    context.grove?.let { appendLine("grove=$it") }
    context.roundStructure?.let { appendLine("roundStructure=$it") }
    appendLine("gameStatus=${game.status}")
    appendLine("roundNumber=${game.roundNumber}/${game.config.roundSetup.totalRounds}")
    appendLine("currentRound=${game.currentRound}")
    appendLine("failure=${failure::class.qualifiedName}: ${failure.message}")
    appendLine()
    appendLine("PLAYER STATE AT FAILURE")
    game.players.forEachIndexed { index, player ->
        val dice = player.dice
        val plants = player.creature.cards.joinToString(", ") { card ->
            "${card.card.name}:${card.facing}"
        }.ifEmpty { "none" }
        appendLine(
            "seat=${index + 1} id=${player.id} vp=${player.vp} " +
                "dice[supply=${dice.supply}, hand=${dice.hand}, discard=${dice.discard}] " +
                "critters=${player.critters.all} wisps=${player.wisps.size} plants=[$plants]"
        )
    }
    appendLine()
    appendLine("RECENT CHRONICLE (last $chronicleTailLines rendered lines)")
    val rendered = ChronicleTextRenderer.render(game.chronicle.entries, detail = true)
    rendered.lineSequence().filter { it.isNotBlank() }.toList().takeLast(chronicleTailLines).forEach(::appendLine)
}
