package dugsolutions.leaf.v35.game.di

import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.di.appModules
import dugsolutions.leaf.v35.game.GameConfig
import dugsolutions.leaf.v35.game.GameRoundSetup
import dugsolutions.leaf.v35.game.GameRunner
import dugsolutions.leaf.v35.game.round.RoundCoordinator
import dugsolutions.leaf.v35.game.round.battle.BattleRound
import dugsolutions.leaf.v35.game.round.cultivation.CultivationRound
import dugsolutions.leaf.v35.game.scoring.FinalScorer
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.RoundCardRegistry
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import org.junit.jupiter.api.Test
import org.koin.dsl.koinApplication
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GameModuleWiringTest {

    @Test
    fun appModules_resolveCompleteProductionGameEngine() {
        val koin = koinApplication {
            modules(appModules)
        }.koin

        assertNotNull(koin.get<GameFactory>())
        assertNotNull(koin.get<CultivationRound>())
        assertNotNull(koin.get<BattleRound>())
        assertNotNull(koin.get<RoundCoordinator>())
        assertNotNull(koin.get<FinalScorer>())
        assertNotNull(koin.get<GameRunner>())
    }

    @Test
    fun productionGraph_runsCultivationAndBattleThroughFinalScoring() {
        val koin = koinApplication {
            modules(appModules)
        }.koin

        val plantRegistry = koin.get<PlantCardRegistry>().apply {
            loadFromCsv(
                dataPath(CardDataFiles.ROOT_CARD_LIST),
                dataPath(CardDataFiles.VF_CARD_LIST)
            )
        }
        val wispRegistry = koin.get<WispCardRegistry>().apply {
            loadFromCsv(dataPath(CardDataFiles.WISP_LIST))
        }
        val roundRegistry = koin.get<RoundCardRegistry>().apply {
            loadFromCsv(dataPath(CardDataFiles.ROUND_CARD_LIST))
        }

        koin.get<WispCardManager>().loadCards(wispRegistry)
        koin.get<RoundCardManager>().loadCards(roundRegistry)

        val selectedPlants = PlantType.entries.flatMap { type ->
            plantRegistry.getAllCards()
                .filter { it.type == type }
                .take(3)
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
    }

    private fun dataPath(fileName: String): String =
        Path.of("data", "v35", fileName).toString()
}
