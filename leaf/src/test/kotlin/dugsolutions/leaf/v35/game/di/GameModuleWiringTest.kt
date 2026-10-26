package dugsolutions.leaf.v35.game.di

import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.common.FirstGameDefault
import dugsolutions.leaf.v35.di.appModules
import dugsolutions.leaf.v35.game.GameConfig
import dugsolutions.leaf.v35.game.GameRoundSetup
import dugsolutions.leaf.v35.game.GameRunner
import dugsolutions.leaf.v35.game.round.RoundCoordinator
import dugsolutions.leaf.v35.game.round.battle.BattleRound
import dugsolutions.leaf.v35.game.round.cultivation.CultivationRound
import dugsolutions.leaf.v35.game.scoring.FinalScorer
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.RoundCardRegistry
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import org.junit.jupiter.api.Test
import org.koin.dsl.koinApplication
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class GameModuleWiringTest {

    @Test
    fun appModules_resolveCompleteProductionGameEngine() {
        val app = koinApplication { modules(appModules) }
        try {
            val koin = app.koin
            assertNotNull(koin.get<GameFactory>())
            assertNotNull(koin.get<CultivationRound>())
            assertNotNull(koin.get<BattleRound>())
            assertNotNull(koin.get<RoundCoordinator>())
            assertNotNull(koin.get<FinalScorer>())
            assertNotNull(koin.get<GameRunner>())
        } finally {
            app.close()
        }
    }

    @Test
    fun productionGraph_runsCultivationAndBattleThroughFinalScoring() {
        val app = koinApplication { modules(appModules) }
        try {
            val koin = app.koin
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

            val selectedPlants = FirstGameDefault.PLANT_NAMES.map { name ->
                requireNotNull(plantRegistry.getCard(name))
            }

            val game = koin.get<GameFactory>()(
                GameConfig.baseline(
                    selectedPlantCards = selectedPlants,
                    numPlayers = 2,
                    roundSetup = GameRoundSetup.Ordered(
                        cultivationRounds = 1,
                        battleRounds = 1
                    ),
                    seed = 12345L
                )
            )

            val result = koin.get<GameRunner>().run(game)

            assertEquals(2, result.roundsCompleted)
            assertEquals(2, result.finalScoring.scores.size)
            assertTrue(result.finalScoring.winnerIds.isNotEmpty())
            assertTrue(game.isComplete)
        } finally {
            app.close()
        }
    }

    @Test
    fun closedProductionApplicationReleasesSharedCatalogSingletons() {
        val first = koinApplication { modules(appModules) }
        val firstRegistry: PlantCardRegistry
        try {
            firstRegistry = first.koin.get()
            firstRegistry.loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROOT_CARD_LIST))
            assertEquals(12, firstRegistry.getAllCards().size)
        } finally {
            first.close()
        }

        val second = koinApplication { modules(appModules) }
        try {
            val secondRegistry = second.koin.get<PlantCardRegistry>()
            assertNotSame(firstRegistry, secondRegistry)
            assertTrue(secondRegistry.getAllCards().isEmpty())
            secondRegistry.loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROOT_CARD_LIST))
            assertEquals(12, secondRegistry.getAllCards().size)
        } finally {
            second.close()
        }
    }

}
