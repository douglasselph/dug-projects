package dugsolutions.leaf.integration.v35.tool

import dugsolutions.leaf.integration.v35.support.HumanBaselineSmokeScenario
import dugsolutions.leaf.integration.v35.support.IntegrationGameHarness
import dugsolutions.leaf.v35.chronicle.ChronicleTextRenderer
import dugsolutions.leaf.v35.game.GameRunResult
import java.nio.file.Files
import java.nio.file.Path

/**
 * Runs one complete deterministic four-player Human Baseline game for human inspection.
 *
 * The game uses the recommended first-game Plant set and a 3/2/2 Round cadence:
 * 3 Cultivation, Battle, 2 Cultivation, Battle, 2 Cultivation, Battle.
 * Decision reasoning is recorded in the Chronicle so the smoke output can be
 * inspected while Human Baseline certification is still in progress.
 *
 * This is not a balance experiment.
 */
fun main(args: Array<String>) {
    val seed = args.firstOrNull()?.toLongOrNull()
        ?: HumanBaselineSmokeScenario.DEFAULT_SEED
    val scenario = HumanBaselineSmokeScenario.scenario(
        seed = seed,
        recordDecisionReasoning = true
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
                entryCount = entries.size,
                result = result
            )
        )

        println("Human Baseline smoke game completed successfully.")
        println("Chronicle entries: ${entries.size}")
        println("Summary:   ${summaryPath.toAbsolutePath()}")
        println("Chronicle: ${chroniclePath.toAbsolutePath()}")
    }
}

private fun outputDirectory(seed: Long): Path =
    Path.of(
        "output",
        "smoke",
        "human-baseline",
        "seed-$seed"
    )

private fun buildSummary(
    seed: Long,
    entryCount: Int,
    result: GameRunResult
): String =
    buildString {
        appendLine("Leaf & Let Die — Human Baseline smoke run")
        appendLine("Purpose: full-game Human Baseline/Chronicle sanity check; not a balance result")
        appendLine("Seed: $seed")
        appendLine("Players: ${HumanBaselineSmokeScenario.NUM_PLAYERS}")
        appendLine("Strategy: Human Baseline for every player")
        appendLine("Plants: recommended first-game Plant set")
        appendLine("Round cadence: 3 Cultivation / Battle / 2 Cultivation / Battle / 2 Cultivation / Battle (3/2/2)")
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
