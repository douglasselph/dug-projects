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
        val playerCount = Integer.getInteger(PLAYERS_PROPERTY, PLAYER_COUNT)
        val commonThreshold = Integer.getInteger(COMMON_THRESHOLD_PROPERTY, DEFAULT_COMMON_THRESHOLD)
        val detail = java.lang.Boolean.getBoolean(DETAIL_PROPERTY)
        require(gameCount > 0) { "$GAMES_PROPERTY must be positive" }
        require(playerCount in 2..4) { "$PLAYERS_PROPERTY must be 2, 3, or 4" }
        require(commonThreshold > 0) { "$COMMON_THRESHOLD_PROPERTY must be positive" }

        val observations = mutableListOf<PurchaseObservation>()

        repeat(gameCount) { gameIndex ->
            val mechanicalSeed = MECHANICAL_SEED_BASE + gameIndex
            val strategySeed = STRATEGY_SEED_BASE + gameIndex
            val scenario = GameScenario(
                numPlayers = playerCount,
                selectedPlantNames = IntegrationCatalog.FIRST_GAME_PLANT_NAMES,
                roundSetup = GameRoundSetup.Ordered(cultivationRounds = 2, battleRounds = 0),
                seed = mechanicalSeed,
                strategySeed = strategySeed,
                decisionFactories = List(playerCount) { PlayerDecisionFactory.humanBaseline() }
            )

            IntegrationGameHarness(scenario).use { harness ->
                harness.runNextRound()
                harness.runNextRound()

                val entries = harness.chronicleEntries()
                for (roundNumber in 1..2) {
                    observations += observationsForRound(
                        gameNumber = gameIndex + 1,
                        mechanicalSeed = mechanicalSeed,
                        strategySeed = strategySeed,
                        roundNumber = roundNumber,
                        entries = ChronicleQueries.entriesForRound(entries, roundNumber)
                    )
                }
            }
        }

        // Every player contributes one observation in each of two Buy phases per game.
        assertEquals(gameCount * playerCount * 2, observations.size)
        assertTrue(observations.any { it.purchasingPower > 0 })

        val report = PurchaseVarietyReport.render(
            observations = observations,
            gameCount = gameCount,
            playerCount = playerCount,
            commonThreshold = commonThreshold,
            detail = detail
        )
        println("\n$report")

        val reportPath = Path.of("build", "reports", "human-baseline-early-purchase-variety.txt")
        Files.createDirectories(reportPath.parent)
        Files.writeString(reportPath, report)
        println("Purchase-variety report written to ${reportPath.toAbsolutePath()}")
    }

    private fun observationsForRound(
        gameNumber: Int,
        mechanicalSeed: Long,
        strategySeed: Long,
        roundNumber: Int,
        entries: List<GameEntry>
    ): List<PurchaseObservation> {
        val buyOrder = entries.filterIsInstance<GameEntry.BuyOrder>().single()
        val purchases = entries.filterIsInstance<GameEntry.Purchase>()

        return buyOrder.resources.map { resources ->
            val playerPurchases = purchases.filter { it.playerId == resources.playerId }
            PurchaseObservation(
                gameNumber = gameNumber,
                mechanicalSeed = mechanicalSeed,
                strategySeed = strategySeed,
                roundNumber = roundNumber,
                playerId = resources.playerId,
                purchasingPower = resources.dice.sumOf { it.value } + resources.critters.sumOf { it.value },
                hasCritters = resources.critters.isNotEmpty(),
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
        val mechanicalSeed: Long,
        val strategySeed: Long,
        val roundNumber: Int,
        val playerId: PlayerId,
        val purchasingPower: Int,
        val hasCritters: Boolean,
        val resourceShape: String,
        val outcome: String
    )

    private data class PowerBucket(
        val purchasingPower: Int,
        val hasCritters: Boolean
    ) {
        fun label(): String =
            "$purchasingPower purchasing power" + if (hasCritters) " with Critters" else ""
    }

    private object PurchaseVarietyReport {
        fun render(
            observations: List<PurchaseObservation>,
            gameCount: Int,
            playerCount: Int,
            commonThreshold: Int,
            detail: Boolean
        ): String = buildString {
            appendLine("HUMAN BASELINE — EARLY PURCHASE VARIETY")
            appendLine("Games: $gameCount")
            appendLine("Players per game: $playerCount")
            appendLine("Cultivation rounds observed: 1-2")
            appendLine("Buy observations: ${observations.size}")
            appendLine("Detailed-report threshold: $commonThreshold observations")
            appendLine()

            val groups = observations
                .groupBy { PowerBucket(it.purchasingPower, it.hasCritters) }
                .toSortedMap(compareBy<PowerBucket>({ it.purchasingPower }, { it.hasCritters }))
            val common = groups.filterValues { it.size >= commonThreshold }
            val uncommon = groups.filterValues { it.size < commonThreshold }

            appendLine("ALL PURCHASING POWERS")
            appendLine(groups.entries.joinToString(", ") { (bucket, rows) -> "${bucket.label()}=${rows.size}" })
            appendLine()

            common.forEach { (bucket, rows) ->
                appendLine("${bucket.label()}: ${rows.size} observations")
                val outcomeCounts = rows.groupingBy { it.outcome }.eachCount()
                    .entries
                    .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                val displayed = outcomeCounts.take(MAX_OUTCOMES_SHOWN)
                displayed.forEach { (outcome, count) ->
                    appendLine("  $outcome = ${percent(count, rows.size)}")
                    if (detail) {
                        appendLine("    ${exampleLine(rows.first { it.outcome == outcome })}")
                    }
                }
                val otherCount = outcomeCounts.drop(MAX_OUTCOMES_SHOWN).sumOf { it.value }
                if (otherCount > 0) {
                    appendLine("  other = ${percent(otherCount, rows.size)}")
                    if (detail) {
                        val otherOutcomes = outcomeCounts.drop(MAX_OUTCOMES_SHOWN).map { it.key }.toSet()
                        appendLine("    ${exampleLine(rows.first { it.outcome in otherOutcomes })}")
                    }
                }

                val dominantCount = outcomeCounts.firstOrNull()?.value ?: 0
                val dominantPercent = if (rows.isEmpty()) 0.0 else dominantCount * 100.0 / rows.size
                appendLine("  distinct purchase outcomes: ${outcomeCounts.size}")
                appendLine("  ${healthLine(outcomeCounts.size, dominantPercent)}")
                appendLine()
            }

            if (uncommon.isNotEmpty()) {
                appendLine("UNCOMMON PURCHASING POWERS (< $commonThreshold observations)")
                uncommon.forEach { (bucket, rows) ->
                    val outcomeCounts = rows.groupingBy { it.outcome }.eachCount()
                        .entries
                        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                    val distinct = outcomeCounts.size
                    appendLine("  ${bucket.label()}: ${rows.size} times; $distinct distinct purchase outcome${if (distinct == 1) "" else "s"}")
                    if (detail) {
                        outcomeCounts.forEach { (outcome, count) ->
                            appendLine("    $outcome = ${percent(count, rows.size)}")
                            appendLine("      ${exampleLine(rows.first { it.outcome == outcome })}")
                        }
                    }
                }
                appendLine()
            }

            appendLine("ROUND BREAKDOWN")
            for (round in 1..2) {
                val roundRows = observations.filter { it.roundNumber == round }
                val roundGroups = roundRows
                    .groupingBy { PowerBucket(it.purchasingPower, it.hasCritters) }
                    .eachCount()
                    .toSortedMap(compareBy<PowerBucket>({ it.purchasingPower }, { it.hasCritters }))
                appendLine("  Round $round: ${roundRows.size} observations; purchasing powers " +
                    roundGroups.entries.joinToString(", ") { (bucket, count) -> "${bucket.label()}=$count" })
            }
            appendLine()
            appendLine("NOTE: 'Looks healthy' is a diagnostic flag, not a game-balance assertion.")
            appendLine("A common bucket is flagged when it has only one observed purchase outcome or one outcome exceeds 95%.")
            appendLine("Purchasing power is total spendable value at Buy: dice plus Critters. A plain 'N purchasing power' bucket has no Critters; 'N purchasing power with Critters' includes Critter value in N and means Critters are part of the available purchasing power.")
            appendLine("Resource composition is retained in each observation for follow-up diagnostics.")
            if (detail) {
                appendLine("Detail examples show one reproducible mechanical/strategy seed pair for every reported outcome.")
            }
        }

        private fun exampleLine(row: PurchaseObservation): String =
            "example: mechanical=${row.mechanicalSeed} strategy=${row.strategySeed} " +
                "round=${row.roundNumber} player=${row.playerId} resources=${row.resourceShape}"

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
        private const val PLAYERS_PROPERTY = "leaf.purchaseVariety.players"
        private const val COMMON_THRESHOLD_PROPERTY = "leaf.purchaseVariety.commonThreshold"
        private const val DETAIL_PROPERTY = "leaf.purchaseVariety.detail"
    }
}
