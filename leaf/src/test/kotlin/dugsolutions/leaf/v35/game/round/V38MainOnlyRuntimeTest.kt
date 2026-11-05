package dugsolutions.leaf.v35.game.round

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
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** This source set has no dependency on src/simulation: the adopted runtime must work from main. */
class V38MainOnlyRuntimeTest {
    @Test
    fun `main-only game runner executes adopted default with per Strike Doom`() {
        runBattle(null, expectedDooms = 3)
    }

    @Test
    fun `main-only game runner preserves explicit historical whole Battle`() {
        runBattle(BattleStructure.LEGACY_WHOLE_BATTLE, expectedDooms = 1)
    }

    private fun runBattle(structure: BattleStructure?, expectedDooms: Int) {
        val app = koinApplication { modules(appModules) }
        try {
            val koin = app.koin
            val plants = koin.get<PlantCardRegistry>().apply {
                clear()
                loadFromCsv(
                    CardDataFiles.dataPath(CardDataFiles.ROOT_CARD_LIST),
                    CardDataFiles.dataPath(CardDataFiles.VF_CARD_LIST)
                )
            }
            koin.get<PlantCardManager>().loadCards(plants)
            val wisps = koin.get<WispCardRegistry>().apply {
                clear()
                loadFromCsv(CardDataFiles.dataPath(CardDataFiles.WISP_LIST))
            }
            koin.get<WispCardManager>().loadCards(wisps)
            val rounds = koin.get<RoundCardRegistry>().apply {
                clear()
                loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROUND_CARD_LIST))
            }
            koin.get<RoundCardManager>().loadCards(rounds)

            val selection = FirstGameDefault.PLANT_NAMES.map { requireNotNull(plants.getCard(it)) }
            val decisions = List(2) { PlayerDecisionFactory.mechanicalControl() }
            val config = if (structure == null) GameConfig(
                selectedPlantCards = selection,
                playerDecisionFactories = decisions,
                roundSetup = GameRoundSetup.Ordered(cultivationRounds = 1, battleRounds = 1),
                seed = 20261105L,
                strategySeed = 20261106L
            ) else GameConfig(
                selectedPlantCards = selection,
                playerDecisionFactories = decisions,
                roundSetup = GameRoundSetup.Ordered(cultivationRounds = 1, battleRounds = 1),
                battleStructure = structure,
                seed = 20261105L,
                strategySeed = 20261106L
            )
            assertEquals(structure ?: BattleStructure.THREE_ENGAGEMENTS, config.battleStructure)
            val game = koin.get<GameFactory>()(config)
            val result = koin.get<GameRunner>().run(game)

            assertEquals(2, result.roundsCompleted)
            assertTrue(game.isComplete)
            assertEquals(3, game.chronicle.entries.count { it is GameEntry.StrikeResolved })
            assertEquals(expectedDooms, game.chronicle.entries.count { it is GameEntry.Doom })
            assertTrue(game.players.all { it.dice.hand.isEmpty() })
            assertTrue(result.finalScoring.winnerIds.isNotEmpty())
        } finally {
            app.close()
        }
    }
}
