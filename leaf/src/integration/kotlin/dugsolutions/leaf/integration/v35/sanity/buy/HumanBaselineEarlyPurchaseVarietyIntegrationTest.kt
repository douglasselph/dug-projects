package dugsolutions.leaf.integration.v35.sanity.buy

import dugsolutions.leaf.integration.v35.support.ChronicleQueries
import dugsolutions.leaf.integration.v35.support.GameScenario
import dugsolutions.leaf.integration.v35.support.IntegrationCatalog
import dugsolutions.leaf.integration.v35.support.IntegrationGameHarness
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.chronicle.domain.PurchaseKind
import dugsolutions.leaf.v35.game.GameRoundSetup
import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.player.PlayerId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale

/**
 * Statistical integration diagnostic for the Human Baseline's first two Buy phases.
 *
 * This deliberately runs the real production engine, Human Baseline decisions,
 * Chronicle, Grove, and Buy machinery.  It is not a balance invariant: the report
 * is intended to reveal suspicious convergence (for example, purchasing power 8
 * producing the same purchase almost every time) without baking an arbitrary
 * game-design percentage into CI.
 *
 * Override the sample size with:
 *   -Dleaf.purchaseVariety.games=5000
 */
class HumanBaselineEarlyPurchaseVarietyIntegrationTest {

    @Test
    fun `early Human Baseline purchases show their variety by purchasing power`() {
        val gameCount = Integer.getInteger(GAMES_PROPERTY, DEFAULT_GAMES)
        val commonThreshold = Integer.getInteger(COMMON_THRESHOLD_PROPERTY, DEFAULT_COMMON_THRESHOLD)
        require(gameCount > 0) { "$GAMES_PROPERTY must be positive" }
        require(commonThreshold > 0) { "$COMMON_THRESHOLD_PROPERTY must be positive" }

        val observations = mutableListOf<PurchaseObservation>()

        repeat(gameCount) { gameIndex ->
            val mechanicalSeed = MECHANICAL_SEED_BASE + gameIndex
            val strategySeed = STRATEGY_SEED_BASE + gameIndex
            val scenario = GameScenario(
                numPlayers = PLAYER_COUNT,
                selectedPlantNames = IntegrationCatalog.FIRST_GAME_PLANT_NAMES,
                roundSetup = GameRoundSetup.Ordered(cultivationRounds = 2, battleRounds = 0),
                seed = mechanicalSeed,
                strategySeed = strategySeed,
                decisionFactories = List(PLAYER_COUNT) { PlayerDecisionFactory.humanBaseline() }
            )

            IntegrationGameHarness(scenario).use { harness ->
                harness.runNextRound()
                harness.runNextRound()

                val entries = harness.chronicleEntries()
                for (roundNumber in 1..2) {
                    observations += observationsForRound(
                        gameNumber = gameIndex + 1,
                        roundNumber = roundNumber,
                        entries = ChronicleQueries.entriesForRound(entries, roundNumber)
                    )
                }
            }
        }

        // Four players x two Buy phases must yield one observation each per game.
        assertEquals(gameCount * PLAYER_COUNT * 2, observations.size)
        assertTrue(observations.any { it.purchasingPower > 0 })

        val report = PurchaseVarietyReport.render(
            observations = observations,
            gameCount = gameCount,
            commonThreshold = commonThreshold
        )
        println("\n$report")

        val reportPath = Path.of("build", "reports", "human-baseline-early-purchase-variety.txt")
        Files.createDirectories(reportPath.parent)
        Files.writeString(reportPath, report)
        println("Purchase-variety report written to ${reportPath.toAbsolutePath()}")
    }

    private fun observationsForRound(
        gameNumber: Int,
        roundNumber: Int,
        entries: List<GameEntry>
    ): List<PurchaseObservation> {
        val buyOrder = entries.filterIsInstance<GameEntry.BuyOrder>().single()
        val purchases = entries.filterIsInstance<GameEntry.Purchase>()

        return buyOrder.resources.map { resources ->
            val playerPurchases = purchases.filter { it.playerId == resources.playerId }
            PurchaseObservation(
                gameNumber = gameNumber,
                roundNumber = roundNumber,
                playerId = resources.playerId,
                purchasingPower = resources.total,
                resourceShape = resourceShape(resources),
                outcome = purchaseOutcome(playerPurchases)
            )
        }
    }

    private fun resourceShape(resources: dugsolutions.leaf.v35.chronicle.domain.BuyOrderResourceSnapshot): String {
        val dice = resources.dice
            .sortedWith(compareBy({ it.sides.value }, { it.value }))
            .joinToString("+") { "D${it.sides.value}(${it.value})" }
        val critters = resources.critters
            .sortedBy { it.critter.name }
            .joinToString("+") { "${it.critter.name}(${it.value})" }
        return listOf(dice, critters).filter { it.isNotEmpty() }.joinToString("+").ifEmpty { "none" }
    }

    private fun purchaseOutcome(purchases: List<GameEntry.Purchase>): String =
        if (purchases.isEmpty()) {
            "Nothing"
        } else {
            purchases.joinToString(" + ") { purchase ->
                when (purchase.kind) {
                    PurchaseKind.DIE -> purchase.itemName
                    PurchaseKind.PLANT -> plantLabel(purchase.itemName, purchase.cost)
                }
            }
        }

    private fun plantLabel(itemName: String, cost: Int): String {
        val prefix = when {
            itemName.startsWith("Root_", ignoreCase = true) -> "R"
            itemName.startsWith("Vine_", ignoreCase = true) -> "V"
            itemName.startsWith("Flower_", ignoreCase = true) -> "F"
            else -> "Plant"
        }
        return "$prefix$cost"
    }

    private data class PurchaseObservation(
        val gameNumber: Int,
        val roundNumber: Int,
        val playerId: PlayerId,
        val purchasingPower: Int,
        val resourceShape: String,
        val outcome: String
    )

    private object PurchaseVarietyReport {
        fun render(
            observations: List<PurchaseObservation>,
            gameCount: Int,
            commonThreshold: Int
        ): String = buildString {
            appendLine("HUMAN BASELINE — EARLY PURCHASE VARIETY")
            appendLine("Games: $gameCount")
            appendLine("Players per game: $PLAYER_COUNT")
            appendLine("Cultivation rounds observed: 1-2")
            appendLine("Buy observations: ${observations.size}")
            appendLine("Detailed-report threshold: $commonThreshold observations")
            appendLine()

            val byPower = observations.groupBy { it.purchasingPower }.toSortedMap()
            val common = byPower.filterValues { it.size >= commonThreshold }
            val uncommon = byPower.filterValues { it.size < commonThreshold }

            appendLine("ALL PURCHASING POWERS")
            appendLine(byPower.entries.joinToString(", ") { (power, rows) -> "$power=${rows.size}" })
            appendLine()

            common.forEach { (power, rows) ->
                appendLine("$power purchasing power: ${rows.size} observations")
                val outcomeCounts = rows.groupingBy { it.outcome }.eachCount()
                    .entries
                    .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                val displayed = outcomeCounts.take(MAX_OUTCOMES_SHOWN)
                displayed.forEach { (outcome, count) ->
                    appendLine("  $outcome = ${percent(count, rows.size)}")
                }
                val otherCount = outcomeCounts.drop(MAX_OUTCOMES_SHOWN).sumOf { it.value }
                if (otherCount > 0) {
                    appendLine("  other = ${percent(otherCount, rows.size)}")
                }

                val dominantCount = outcomeCounts.firstOrNull()?.value ?: 0
                val dominantPercent = if (rows.isEmpty()) 0.0 else dominantCount * 100.0 / rows.size
                appendLine("  distinct purchase outcomes: ${outcomeCounts.size}")
                appendLine("  ${healthLine(outcomeCounts.size, dominantPercent)}")
                appendLine()
            }

            if (uncommon.isNotEmpty()) {
                appendLine("UNCOMMON PURCHASING POWERS (< $commonThreshold observations)")
                uncommon.forEach { (power, rows) ->
                    val distinct = rows.map { it.outcome }.toSet().size
                    appendLine("  $power purchasing power: ${rows.size} times; $distinct distinct purchase outcome${if (distinct == 1) "" else "s"}")
                }
                appendLine()
            }

            appendLine("ROUND BREAKDOWN")
            for (round in 1..2) {
                val roundRows = observations.filter { it.roundNumber == round }
                appendLine("  Round $round: ${roundRows.size} observations; purchasing powers " +
                    roundRows.groupingBy { it.purchasingPower }.eachCount().toSortedMap()
                        .entries.joinToString(", ") { (power, count) -> "$power=$count" })
            }
            appendLine()
            appendLine("NOTE: 'Looks healthy' is a diagnostic flag, not a game-balance assertion.")
            appendLine("A common bucket is flagged when it has only one observed purchase outcome or one outcome exceeds 95%.")
            appendLine("Resource composition is retained in each observation for follow-up diagnostics even though this report groups by total purchasing power.")
        }

        private fun healthLine(distinctOutcomes: Int, dominantPercent: Double): String = when {
            distinctOutcomes <= 1 -> "CHECK: only one purchase outcome observed."
            dominantPercent > DOMINANCE_WARNING_PERCENT ->
                "CHECK: one purchase outcome accounts for ${formatOneDecimal(dominantPercent)}%."
            else -> "Looks healthy."
        }

        private fun percent(count: Int, total: Int): String =
            "${formatOneDecimal(count * 100.0 / total)}% ($count)"

        private fun formatOneDecimal(value: Double): String =
            String.format(Locale.US, "%.1f", value)
    }

    companion object {
        private const val PLAYER_COUNT = 4
        private const val DEFAULT_GAMES = 1_000
        private const val DEFAULT_COMMON_THRESHOLD = 100
        private const val MAX_OUTCOMES_SHOWN = 5
        private const val DOMINANCE_WARNING_PERCENT = 95.0
        private const val MECHANICAL_SEED_BASE = 12000L
        private const val STRATEGY_SEED_BASE = 22000L
        private const val GAMES_PROPERTY = "leaf.purchaseVariety.games"
        private const val COMMON_THRESHOLD_PROPERTY = "leaf.purchaseVariety.commonThreshold"
    }
}
