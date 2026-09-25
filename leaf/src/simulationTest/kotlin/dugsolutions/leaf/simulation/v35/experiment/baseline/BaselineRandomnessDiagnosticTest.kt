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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BaselineRandomnessDiagnosticTest {
    @Test
    fun fixedSeedsReproduceAndDifferentSeedCohortChangesCompleteGameDevelopment() {
        val application = koinApplication { modules(appModules) }
        try {
            val koin = application.koin
            loadCatalogs(koin.get(), koin.get(), koin.get(), koin.get(), koin.get(), koin.get())
            val plants = FIRST_GAME_PLANT_NAMES.map { requireNotNull(koin.get<PlantCardManager>().getCard(it)) }
            val matchup = Matchup("Human Baseline", List(4) { StrategyProfile.humanBaseline() })
            val runner = GameSummaryBatchRunner(
                gameFactory = koin.get<GameFactory>(),
                gameRunner = koin.get<GameRunner>(),
                selectedPlantCards = plants
            )

            fun run(baseSeed: Long, strategySeed: Long) = BaselineRandomnessDiagnostic.from(
                runner.run(
                    matchup,
                    BaselineCalibrationSpec(
                        selectedPlantCards = plants,
                        games = 4,
                        baseSeed = baseSeed,
                        strategyBaseSeed = strategySeed,
                        checkpoints = listOf(4)
                    ).experimentConfig()
                )
            )

            val first = run(baseSeed = 12_000L, strategySeed = 22_000L)
            val repeated = run(baseSeed = 12_000L, strategySeed = 22_000L)
            val different = run(baseSeed = 32_000L, strategySeed = 42_000L)

            assertEquals(first, repeated, "Same seed cohort must reproduce the complete diagnostic")
            assertEquals(first.fingerprint, repeated.fingerprint)
            assertNotEquals(
                first.fingerprint,
                different.fingerprint,
                "A different seed cohort should change winner/development outcomes across the complete batch"
            )

            first.games.forEach { game ->
                assertEquals(4, game.players.size)
                assertTrue(game.winnerSeats.isNotEmpty())
                game.players.forEach { player ->
                    assertTrue(player.plantSignature.cards.isNotEmpty(), "Completed game should expose final Plant development")
                    assertTrue(player.diceSignature.totalDice > 0, "Completed game should expose final owned dice")
                }
            }

            val text = BaselineRandomnessDiagnosticRenderer.render(first)
            assertTrue(text.contains("Games: 4"))
            assertTrue(text.contains("Plant=["))
            assertTrue(text.contains("Dice=[D4="))
            assertTrue(text.contains("diagnostic, not pass/fail"))
        } finally {
            application.close()
        }
    }

    @Test
    fun diagnosticSupportsFourEightAndTwelveGameCumulativeViewsWithoutDiversityAssertions() {
        val synthetic = BaselineRandomnessDiagnostic(
            games = (1..12).map { game ->
                BaselineDiagnosticGame(
                    gameNumber = game,
                    mechanicalSeed = 1000L + game,
                    strategySeed = 2000L + game,
                    winnerSeats = listOf((game - 1) % 4),
                    players = emptyList()
                )
            }
        )

        assertEquals(4, synthetic.prefix(4).games.size)
        assertEquals(8, synthetic.prefix(8).games.size)
        assertEquals(12, synthetic.prefix(12).games.size)
        assertTrue(BaselineRandomnessDiagnosticRenderer.render(synthetic.prefix(12)).contains("Games: 12"))
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
