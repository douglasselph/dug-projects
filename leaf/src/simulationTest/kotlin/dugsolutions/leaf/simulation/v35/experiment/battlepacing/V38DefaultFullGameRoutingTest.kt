package dugsolutions.leaf.simulation.v35.experiment.battlepacing

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
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Regression: a normal simulation runtime must execute the adopted v38 default end-to-end. */
class V38DefaultFullGameRoutingTest {
    @Test
    fun defaultSimulationRunnerCompletesCultivationAndThreeEngagementBattle() {
        val app = koinApplication { modules(appModules) }
        try {
            val koin = app.koin
            val plants = koin.get<PlantCardRegistry>().apply {
                loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROOT_CARD_LIST),
                    CardDataFiles.dataPath(CardDataFiles.VF_CARD_LIST))
            }
            val wisps = koin.get<WispCardRegistry>().apply {
                loadFromCsv(CardDataFiles.dataPath(CardDataFiles.WISP_LIST))
            }
            val rounds = koin.get<RoundCardRegistry>().apply {
                loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROUND_CARD_LIST))
            }
            koin.get<PlantCardManager>().loadCards(plants)
            koin.get<WispCardManager>().loadCards(wisps)
            koin.get<RoundCardManager>().loadCards(rounds)

            val selection = FirstGameDefault.PLANT_NAMES.map { requireNotNull(plants.getCard(it)) }
            // Do not specify battleStructure: the default itself is under test.
            val config = GameConfig(
                selectedPlantCards = selection,
                playerDecisionFactories = List(2) { PlayerDecisionFactory.mechanicalControl() },
                roundSetup = GameRoundSetup.Ordered(cultivationRounds = 1, battleRounds = 1),
                seed = 20261104L
            )
            assertEquals(BattleStructure.THREE_ENGAGEMENTS, config.battleStructure)
            val game = koin.get<GameFactory>()(config)
            val result = koin.get<GameRunner>().run(game)
            assertEquals(2, result.roundsCompleted)
            assertTrue(game.isComplete)
            assertTrue(result.finalScoring.winnerIds.isNotEmpty())
        } finally {
            app.close()
        }
    }
}
