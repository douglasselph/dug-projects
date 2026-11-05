package dugsolutions.leaf.simulation.v35.experiment.battlepacing

import dugsolutions.leaf.simulation.v35.analysis.GameSummary
import dugsolutions.leaf.simulation.v35.analysis.GameSummaryExtractor
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.chronicle.domain.ChroniclePhase
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.common.FirstGameDefault
import dugsolutions.leaf.v35.di.appModules
import dugsolutions.leaf.v35.game.BattleStructure
import dugsolutions.leaf.v35.game.GameConfig
import dugsolutions.leaf.v35.game.GameRoundSetup
import dugsolutions.leaf.v35.game.GameRunner
import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.game.di.GameFactory
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.RoundCardRegistry
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.koin.dsl.koinApplication
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * End-to-end seeded verification of both selectable Battle engines with the
 * adopted standard 3/2/2 (10-round) game, for every supported player count.
 *
 * These are rules-engine determinism checks, not statements of game balance.
 * No Wisp is excluded, no legacy scheduler is silently substituted, and no
 * fixed scores are assumed to be equal between the two rule sets.
 */
class V38BattleStructureFullGameRegressionTest {
    companion object {
        private val STRUCTURES = listOf(
            BattleStructure.THREE_ENGAGEMENTS,
            BattleStructure.LEGACY_WHOLE_BATTLE
        )
        private val PLAYER_COUNTS = listOf(2, 3, 4)
    }

    private data class CompletedRun(
        val summary: GameSummary,
        val chronicle: List<GameEntry>
    )

    @TestFactory
    fun standardFullGameMatrix(): List<DynamicTest> =
        PLAYER_COUNTS.flatMap { players ->
            STRUCTURES.map { structure ->
                DynamicTest.dynamicTest("$players players / $structure / standard 3-2-2") {
                    runScenario(players, structure)
                }
            }
        }

    private fun runScenario(players: Int, structure: BattleStructure) {
        val application = koinApplication { modules(appModules) }
        try {
            val koin = application.koin
            val plantRegistry = koin.get<PlantCardRegistry>().apply {
                loadFromCsv(
                    CardDataFiles.dataPath(CardDataFiles.ROOT_CARD_LIST),
                    CardDataFiles.dataPath(CardDataFiles.VF_CARD_LIST)
                )
            }
            val wispRegistry = koin.get<WispCardRegistry>().apply {
                loadFromCsv(CardDataFiles.dataPath(CardDataFiles.WISP_LIST))
            }
            val roundRegistry = koin.get<RoundCardRegistry>().apply {
                loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROUND_CARD_LIST))
            }
            koin.get<PlantCardManager>().loadCards(plantRegistry)
            koin.get<WispCardManager>().loadCards(wispRegistry)
            koin.get<RoundCardManager>().loadCards(roundRegistry)

            val firstGamePlants = FirstGameDefault.PLANT_NAMES.map { name ->
                requireNotNull(plantRegistry.getCard(name)) { "Missing official Plant: $name" }
            }
            val config = GameConfig(
                selectedPlantCards = firstGamePlants,
                playerDecisionFactories = List(players) { PlayerDecisionFactory.mechanicalControl() },
                roundSetup = GameRoundSetup.standard(),
                battleStructure = structure,
                seed = 20261104L + players * 1009L,
                strategySeed = 20261105L + players * 2003L
            )
            assertEquals(structure, config.battleStructure)
            assertEquals(10, config.roundSetup.totalRounds)
            assertEquals(3, config.roundSetup.battleRounds)

            val factory = koin.get<GameFactory>()
            val runner = koin.get<GameRunner>()
            val first = runAndCheck(factory, runner, config, players, structure)
            // A fresh game in the same application must not inherit dice,
            // Round, Wisp, shared-resource or strategy state from its predecessor.
            val second = runAndCheck(factory, runner, config, players, structure)

            assertEquals(first.summary, second.summary,
                "Final game summaries diverged: $players players / $structure")
            assertEquals(first.chronicle, second.chronicle,
                "Chronicle events diverged: $players players / $structure")
            println(
                "V38_FULL_GAME_PASS,players=$players,structure=$structure," +
                    "rounds=${first.summary.roundsCompleted}," +
                    "scores=${first.summary.players.joinToString("|") { it.totalVp.toString() }}," +
                    "events=${first.chronicle.size},replays=2"
            )
        } finally {
            application.close()
        }
    }

    private fun runAndCheck(
        factory: GameFactory,
        runner: GameRunner,
        config: GameConfig,
        players: Int,
        structure: BattleStructure
    ): CompletedRun {
        val game = factory(config)
        val result = runner.run(game)
        assertEquals(10, result.roundsCompleted, "$structure must finish the full 3/2/2 game")
        assertTrue(game.isComplete)
        assertEquals(players, result.finalScoring.scores.size)
        assertTrue(result.finalScoring.winnerIds.isNotEmpty())
        assertTrue(game.players.all { it.dice.hand.isEmpty() }, "Final Battle must clear all Hands")

        val chronicle = game.chronicle.entries
        assertEquals(10, chronicle.count { it is GameEntry.RoundRevealed })
        assertEquals(10, chronicle.count { it is GameEntry.RoundCompleted })
        assertEquals(3, chronicle.count {
            it is GameEntry.RoundRevealed && it.cardType == RoundCardType.BATTLE
        })
        assertEquals(9, chronicle.count { it is GameEntry.StrikeResolved })

        val battles = chronicle.withIndex().filter { (_, entry) ->
            entry is GameEntry.RoundRevealed && entry.cardType == RoundCardType.BATTLE
        }
        assertEquals(3, battles.size)
        battles.forEach { (start, revealed) ->
            val round = (revealed as GameEntry.RoundRevealed).roundNumber
            val end = chronicle.indexOfFirst { entry ->
                entry is GameEntry.RoundCompleted &&
                    entry.roundNumber == round && entry.cardType == RoundCardType.BATTLE
            }
            assertTrue(end > start, "Battle $round did not complete")
            val events = chronicle.subList(start, end + 1)
            val strikes = events.filterIsInstance<GameEntry.StrikeResolved>()
            assertEquals(StrikeRow.entries.toSet(), strikes.map { it.row }.toSet(),
                "Battle $round must resolve each Strike exactly once")
            assertEquals(3, strikes.size, "Battle $round must have exactly three Strike resolutions")

            val dooms = events.filterIsInstance<GameEntry.Doom>()
            val cleanups = events.filterIsInstance<GameEntry.Cleanup>().filter {
                it.phase == ChroniclePhase.BATTLE
            }
            when (structure) {
                BattleStructure.THREE_ENGAGEMENTS -> {
                    assertEquals(3, dooms.size, "v38 must Doom once for each Strike")
                    assertEquals(players * 4, cleanups.size,
                        "v38 must clean each Strike and then Refresh once per player")
                    // A Doom event with no dice has no row field, so pair it with
                    // its preceding StrikeResolved event chronologically. This
                    // also covers Wisps resolving a future row early.
                    val indexedStrikes = events.withIndex().filter {
                        it.value is GameEntry.StrikeResolved
                    }
                    indexedStrikes.forEachIndexed { ordinal, indexed ->
                        val resolved = indexed.value as GameEntry.StrikeResolved
                        val endOfStrike = indexedStrikes.getOrNull(ordinal + 1)?.index ?: events.size
                        val settled = events.subList(indexed.index + 1, endOfStrike)
                        val rowDooms = settled.filterIsInstance<GameEntry.Doom>()
                        assertEquals(1, rowDooms.size,
                            "v38 ${resolved.row} must Doom immediately after resolving")
                        val doom = rowDooms.single()
                        val dieValues = requireNotNull(resolved.rowSnapshot) {
                            "v38 ${resolved.row} must record a pre-Doom row snapshot"
                        }.squares.flatMap { square -> square.dice.map { it.value } }
                        val minimum = dieValues.minOrNull()
                        val expectedDoomedValues = minimum?.let { listOf(it) } ?: emptyList()
                        assertEquals(expectedDoomedValues, doom.valuesTrashed,
                            "v38 ${resolved.row} Doom must Trash only the minimum-value group")
                        assertEquals(dieValues.count { it == minimum }, doom.count,
                            "v38 ${resolved.row} Doom must Trash every die in the lowest-value group")
                        assertTrue(doom.dice.all { it.row == resolved.row },
                            "v38 ${resolved.row} Doom must not Trash dice from another Strike")
                        val rowCleanups = settled.filterIsInstance<GameEntry.Cleanup>()
                            .filter { it.phase == ChroniclePhase.BATTLE }
                        assertTrue(rowCleanups.size >= players,
                            "v38 ${resolved.row} must reclaim each player's row after Doom")
                        assertTrue(rowCleanups.take(players).all { !it.refreshed },
                            "v38 must not Refresh during a per-Strike Cleanup")
                        assertEquals(players, rowCleanups.take(players).map { it.playerId }.toSet().size,
                            "v38 must reclaim each player's row once")
                        val doomIndex = settled.indexOf(doom)
                        assertTrue(settled.indexOf(rowCleanups.first()) > doomIndex,
                            "v38 ${resolved.row} cleanup must follow its Doom")
                    }
                    val lastCleanups = cleanups.takeLast(players)
                    assertEquals(players, lastCleanups.map { it.playerId }.toSet().size,
                        "v38 must finish with one Refresh opportunity per player")
                    assertTrue(lastCleanups.all { it.discardedDice == 0 && it.returnedCritters == 0 },
                        "v38 final Refresh must not reclaim row pieces twice")
                }
                BattleStructure.LEGACY_WHOLE_BATTLE -> {
                    assertEquals(1, dooms.size, "Legacy must retain one whole-Battle Doom")
                    assertEquals(players, cleanups.size,
                        "Legacy must retain one cleanup per player")
                }
            }
        }

        val summary = GameSummaryExtractor.extract(game, result)
        assertEquals(players, summary.players.size)
        assertEquals(result.finalScoring.winnerIds, summary.winnerIds)
        assertEquals(10, summary.roundsCompleted)
        return CompletedRun(summary, chronicle)
    }
}
