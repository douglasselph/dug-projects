package dugsolutions.leaf.simulation.v35.experiment

import dugsolutions.leaf.simulation.v35.strategy.StrategyProfile
import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.di.appModules
import dugsolutions.leaf.v35.game.GameRunner
import dugsolutions.leaf.v35.game.di.GameFactory
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.RoundCardRegistry
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameSummaryBatchRunnerTest {
    @Test
    fun batchRetainsCompactSummariesWithDeterministicIndependentSeeds() {
        val application = koinApplication { modules(appModules) }
        try {
            val koin = application.koin
            loadCatalogs(koin.get(), koin.get(), koin.get(), koin.get(), koin.get(), koin.get())
            val plantManager = koin.get<PlantCardManager>()
            val plants = FIRST_GAME_PLANT_NAMES.map { requireNotNull(plantManager.getCard(it)) }
            val matchup = Matchup("Human Baseline", List(4) { StrategyProfile.humanBaseline() })
            val config = ExperimentConfig(games = 2, baseSeed = 100L, strategyBaseSeed = 900L)

            val result = GameSummaryBatchRunner(
                gameFactory = koin.get<GameFactory>(),
                gameRunner = koin.get<GameRunner>(),
                selectedPlantCards = plants
            ).run(matchup, config)

            assertEquals(2, result.gamesCompleted)
            assertEquals(listOf(100L, 101L), result.summaries.map { it.mechanicalSeed })
            assertEquals(listOf(900L, 901L), result.summaries.map { it.strategySeed })
            assertTrue(result.summaries.all { it.players.size == 4 })

            val retainedTypes = BatchRunResult::class.java.declaredFields.map { it.type.name }
            assertFalse(retainedTypes.any { it.contains("GameChronicle") || it.endsWith(".Game") })

            val report = BatchReport.render(result)
            val csv = BatchReport.renderCsv(result)
            assertTrue(report.contains("Games: 2"))
            assertEquals(1 + 2 * 4, csv.lineSequence().count())
        } finally {
            application.close()
        }
    }

    @Test
    fun seatRotationsPutEveryProfileInEverySeat() {
        val profiles = listOf(
            StrategyProfile.humanBaseline(),
            StrategyProfile.mechanicalControl(),
            StrategyProfile.humanBaseline(),
            StrategyProfile.mechanicalControl()
        )
        val rotations = Matchup("rotation", profiles).seatRotations()

        assertEquals(4, rotations.size)
        profiles.indices.forEach { originalIndex ->
            val profile = profiles[originalIndex]
            assertEquals(4, rotations.count { rotation -> rotation.players.any { it === profile } })
            assertEquals((0..3).toSet(), rotations.mapIndexedNotNull { _, rotation ->
                rotation.players.indexOfFirst { it === profile }.takeIf { it >= 0 }
            }.toSet())
        }
    }

    private fun loadCatalogs(
        plantRegistry: PlantCardRegistry,
        plantManager: PlantCardManager,
        wispRegistry: WispCardRegistry,
        wispManager: WispCardManager,
        roundRegistry: RoundCardRegistry,
        roundManager: RoundCardManager
    ) {
        val root = CardDataFiles.dataDirectory()
        plantRegistry.clear()
        plantRegistry.loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROOT_CARD_LIST, root), CardDataFiles.dataPath(CardDataFiles.VF_CARD_LIST, root))
        plantManager.loadCards(plantRegistry)
        wispRegistry.clear()
        wispRegistry.loadFromCsv(CardDataFiles.dataPath(CardDataFiles.WISP_LIST, root))
        wispManager.loadCards(wispRegistry)
        roundRegistry.clear()
        roundRegistry.loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROUND_CARD_LIST, root))
        roundManager.loadCards(roundRegistry)
    }

    companion object {
        private val FIRST_GAME_PLANT_NAMES = listOf(
            "Root_05_02", "Root_07_04", "Root_09_03",
            "Vine_07_01", "Vine_09_01", "Vine_11_04",
            "Flower_11_03", "Flower_14_02", "Flower_17_04"
        )
    }
}
