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
                    dooms.forEach { doom ->
                        assertTrue(doom.valuesTrashed.distinct().size <= 1,
                            "v38 Doom must Trash only a single lowest-value group")
                        assertTrue(doom.dice.map { it.row }.distinct().size <= 1,
                            "v38 Doom must not span multiple Strikes")
                    }
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
