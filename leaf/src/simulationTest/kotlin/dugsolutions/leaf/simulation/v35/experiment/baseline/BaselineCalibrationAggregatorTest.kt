package dugsolutions.leaf.simulation.v35.experiment.baseline

import dugsolutions.leaf.simulation.v35.experiment.GameSummaryBatchRunner
import dugsolutions.leaf.simulation.v35.experiment.Matchup
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
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BaselineCalibrationAggregatorTest {
    @Test
    fun aggregatesCumulativeFourSeatCheckpointsForExactGrove() {
        val application = koinApplication { modules(appModules) }
        try {
            val koin = application.koin
            loadCatalogs(koin.get(), koin.get(), koin.get(), koin.get(), koin.get(), koin.get())
            val plantManager = koin.get<PlantCardManager>()
            val plants = FIRST_GAME_PLANT_NAMES.map { requireNotNull(plantManager.getCard(it)) }
            val spec = BaselineCalibrationSpec(
                selectedPlantCards = plants.reversed(),
                games = 4,
                baseSeed = 4100L,
                strategyBaseSeed = 8100L,
                checkpoints = listOf(2, 4)
            )
            val matchup = Matchup("Human Baseline", List(4) { StrategyProfile.humanBaseline() })
            val batch = GameSummaryBatchRunner(
                gameFactory = koin.get<GameFactory>(),
                gameRunner = koin.get<GameRunner>(),
                selectedPlantCards = plants
            ).run(matchup, spec.experimentConfig())

            val report = BaselineCalibrationAggregator.aggregate(spec, batch)

            assertEquals(FIRST_GAME_PLANT_NAMES.sorted().joinToString("|"), report.groveFingerprint)
            assertEquals(4, report.totalGamesAvailable)
            assertEquals(listOf(2, 4), report.checkpoints.map { it.games })

            report.checkpoints.forEach { checkpoint ->
                assertEquals(listOf(0, 1, 2, 3), checkpoint.seats.map { it.seat })
                assertEquals(1.0, checkpoint.seats.sumOf { it.winShare }, absoluteTolerance = 1e-9)
                assertEquals(
                    checkpoint.seats.maxOf { abs(it.winShare - 0.25) },
                    checkpoint.maxAbsoluteWinShareDeviation,
                    absoluteTolerance = 1e-9
                )
            }

            val firstTwoExpected = batch.summaries.take(2).sumOf { it.players.single { player -> player.seat == 0 }.winShare } / 2.0
            assertEquals(firstTwoExpected, report.checkpoints.first().seats[0].winShare, absoluteTolerance = 1e-9)

            val text = BaselineCalibrationReportRenderer.render(report)
            assertTrue(text.contains("Grove: ${report.groveFingerprint}"))
            assertTrue(text.contains("Sampling reference is context only"))
        } finally {
            application.close()
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
