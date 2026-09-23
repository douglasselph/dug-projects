package dugsolutions.leaf.integration.v35.tool

import dugsolutions.leaf.integration.v35.support.GameScenario
import dugsolutions.leaf.integration.v35.support.IntegrationCatalog
import dugsolutions.leaf.integration.v35.support.IntegrationGameHarness
import dugsolutions.leaf.v35.chronicle.ChronicleTextRenderer
import dugsolutions.leaf.v35.game.GameRoundSetup
import java.nio.file.Files
import java.nio.file.Path

/**
 * Runs one complete deterministic Mechanical Control game for human inspection.
 *
 * This is not a balance experiment. It is a visual smoke test: make sure the
 * real game reaches completion, then read the Chronicle to see what happened.
 */
fun main(args: Array<String>) {
    val seed = args.firstOrNull()?.toLongOrNull() ?: DEFAULT_SEED
    val roundSetup = GameRoundSetup.firstGame()
    val scenario = GameScenario(
        numPlayers = 4,
        selectedPlantNames = IntegrationCatalog.FIRST_GAME_PLANT_NAMES,
        roundSetup = roundSetup,
        seed = seed
        // Empty decisionFactories is intentional: GameScenario defaults every
        // player to deterministic Mechanical Control.
    )

    IntegrationGameHarness(scenario).use { harness ->
        val result = harness.runGame()
        val entries = harness.chronicleEntries()
        val outputDir = outputDirectory(seed)
        Files.createDirectories(outputDir)

        val chroniclePath = outputDir.resolve("chronicle.txt")
        Files.writeString(
            chroniclePath,
            ChronicleTextRenderer.render(entries)
        )

        val summaryPath = outputDir.resolve("summary.txt")
        Files.writeString(
            summaryPath,
            buildSummary(
                seed = seed,
                roundSetup = roundSetup,
                entryCount = entries.size,
                result = result
            )
        )

        println("Mechanical Control smoke game completed successfully.")
        println("Chronicle entries: ${entries.size}")
        println("Summary:   ${summaryPath.toAbsolutePath()}")
        println("Chronicle: ${chroniclePath.toAbsolutePath()}")
    }
}

private fun outputDirectory(seed: Long): Path =
    Path.of(
        "output",
        "smoke",
        "mechanical-control",
        "seed-$seed"
    )

private fun buildSummary(
    seed: Long,
    roundSetup: GameRoundSetup,
    entryCount: Int,
    result: dugsolutions.leaf.v35.game.GameRunResult
): String =
    buildString {
        appendLine("Leaf & Let Die — Mechanical Control smoke run")
        appendLine("Purpose: full-game engine/Chronicle sanity check; not a balance result")
        appendLine("Seed: $seed")
        appendLine("Players: 4")
        appendLine("Strategy: Mechanical Control for every player")
        appendLine(
            "Rounds: ${roundSetup.cultivationRounds} Cultivation + " +
                "${roundSetup.battleRounds} Battle (${roundSetup.totalRounds} total)"
        )
        appendLine("Rounds completed: ${result.roundsCompleted}")
        appendLine("Chronicle entries: $entryCount")
        appendLine()
        appendLine("Final scores:")
        result.finalScoring.scores.forEach { score ->
            appendLine(
                "  P${score.playerId.value}: ${score.totalVp} VP " +
                    "(existing=${score.existingVp}, plants=${score.plantVp}, " +
                    "wisps=${score.unplayedWispVp}, graftedPlants=${score.graftedPlantCount})"
            )
        }
        appendLine(
            "Winner(s): " +
                result.finalScoring.winnerIds.joinToString(", ") { "P${it.value}" }
        )
    }

private const val DEFAULT_SEED: Long = 13_579L
