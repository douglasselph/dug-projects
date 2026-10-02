package dugsolutions.leaf.integration.v35.tool

import dugsolutions.leaf.integration.v35.support.HumanBaselineSmokeScenario
import dugsolutions.leaf.integration.v35.support.IntegrationGameHarness
import dugsolutions.leaf.v35.chronicle.ChronicleTextRenderer
import dugsolutions.leaf.v35.game.GameRunResult
import dugsolutions.leaf.v35.plant.GrovePlantCode
import java.nio.file.Files
import java.nio.file.Path

/**
 * Runs one complete deterministic Human Baseline game for human inspection.
 *
 * The game uses the recommended first-game Plant set and a 3/2/2 Round cadence:
 * 3 Cultivation, Battle, 2 Cultivation, Battle, 2 Cultivation, Battle.
 * Compact Chronicle output is the default. Pass `--detail` to restore
 * line-by-line opening rolls and scored decision reasoning.
 *
 * This is not a balance experiment.
 */
fun main(args: Array<String>) {
    val detail = args.any { it.equals("--detail", ignoreCase = true) }
    val groveArg = args.firstOrNull { it.startsWith("--grove=") }?.substringAfter('=')
    val players = args.firstOrNull { it.startsWith("--players=") }?.substringAfter('=')?.toInt() ?: HumanBaselineSmokeScenario.NUM_PLAYERS
    require(players in 2..4) { "--players must be 2, 3, or 4" }
    val positional = args.filterNot { it.equals("--detail", ignoreCase = true) || it.startsWith("--grove=") || it.startsWith("--players=") }
    val seed = positional.getOrNull(0)?.toLongOrNull()
        ?: HumanBaselineSmokeScenario.DEFAULT_SEED
    val strategySeed = positional.getOrNull(1)?.toLongOrNull() ?: seed
    val selectedNames = groveArg?.let(GrovePlantCode::overrideNames)
    val scenario = HumanBaselineSmokeScenario.scenario(
        seed = seed,
        strategySeed = strategySeed,
        chronicleDetail = detail,
        selectedPlantNames = selectedNames ?: dugsolutions.leaf.integration.v35.support.IntegrationCatalog.FIRST_GAME_PLANT_NAMES,
        numPlayers = players
    )

    IntegrationGameHarness(scenario).use { harness ->
        val resolvedPlants = harness.game.grove.plantMarket.stacks.map { it.card }
        val resolvedGroveCode = GrovePlantCode.encode(resolvedPlants)
        val result = harness.runGame()
        val entries = harness.chronicleEntries()
        val outputDir = outputDirectory(seed, strategySeed, players)
        Files.createDirectories(outputDir)

        val chroniclePath = outputDir.resolve("chronicle.txt")
        Files.writeString(
            chroniclePath,
            ChronicleTextRenderer.render(
                entries = entries,
                detail = detail,
                selectedPlantCards = resolvedPlants
            )
        )

        val summaryPath = outputDir.resolve("summary.txt")
        Files.writeString(
            summaryPath,
            buildSummary(
                seed = seed,
                strategySeed = strategySeed,
                detail = detail,
                players = players,
                groveCode = resolvedGroveCode,
                plantNames = resolvedPlants.map { it.name },
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

private fun outputDirectory(seed: Long, strategySeed: Long, players: Int): Path =
    Path.of(
        "output",
        "smoke",
        "human-baseline",
        "${players}-player-mechanical-$seed-strategy-$strategySeed"
    )

private fun buildSummary(
    seed: Long,
    strategySeed: Long,
    detail: Boolean,
    players: Int,
    groveCode: String,
    plantNames: List<String>,
    entryCount: Int,
    result: GameRunResult
): String =
    buildString {
        appendLine("Leaf & Let Die — Human Baseline smoke run")
        appendLine("Purpose: full-game Human Baseline/Chronicle sanity check; not a balance result")
        appendLine("Mechanical seed: $seed")
        appendLine("Strategy seed: $strategySeed")
        appendLine("Chronicle detail: $detail")
        appendLine("Players: $players")
        appendLine("Strategy: Human Baseline for every player")
        appendLine("Grove code: $groveCode")
        appendLine("Plants: ${plantNames.joinToString(", ")}")
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
