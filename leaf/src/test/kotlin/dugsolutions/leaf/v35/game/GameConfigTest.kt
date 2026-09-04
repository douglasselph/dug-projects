package dugsolutions.leaf.v35.game

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.random.die.di.DieFactory
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class GameConfigTest {

    @Test
    fun baseline_createsRequestedNumberOfPlayerDecisionFactories() {
        val config =
            GameConfig.baseline(
                selectedPlantCards =
                    selectedCards(),
                numPlayers = 4
            )

        assertEquals(
            4,
            config.numPlayers
        )
        assertEquals(
            4,
            config.playerDecisionFactories.size
        )
    }

    @Test
    fun baselineDecisionFactory_createsFreshDirectorEachTime() {
        val config =
            GameConfig.baseline(
                selectedPlantCards =
                    selectedCards(),
                numPlayers = 2
            )

        val factory =
            config.playerDecisionFactories.first()

        val first =
            factory.create()

        val second =
            factory.create()

        assertFalse(first === second)
    }

    @Test
    fun constructor_preservesDecisionFactoryOrder() {
        val first =
            PlayerDecisionFactory {
                DecisionDirector.baseline()
            }

        val second =
            PlayerDecisionFactory {
                DecisionDirector.baseline()
            }

        val config =
            GameConfig(
                selectedPlantCards =
                    selectedCards(),
                playerDecisionFactories =
                    listOf(first, second)
            )

        kotlin.test.assertTrue(
            config.playerDecisionFactories[0] === first
        )
        kotlin.test.assertTrue(
            config.playerDecisionFactories[1] === second
        )
    }

    @Test
    fun constructor_defensivelyCopiesIncomingLists() {
        val cards =
            selectedCards().toMutableList()

        val factories =
            mutableListOf(
                PlayerDecisionFactory.baseline(),
                PlayerDecisionFactory.baseline()
            )

        val config =
            GameConfig(
                selectedPlantCards = cards,
                playerDecisionFactories =
                    factories
            )

        cards.clear()
        factories.clear()

        assertEquals(
            9,
            config.selectedPlantCards.size
        )
        assertEquals(
            2,
            config.playerDecisionFactories.size
        )
    }

    @Test
    fun constructor_rejectsUnsupportedPlayerCounts() {
        assertFailsWith<IllegalArgumentException> {
            GameConfig(
                selectedPlantCards =
                    selectedCards(),
                playerDecisionFactories =
                    listOf(
                        PlayerDecisionFactory.baseline()
                    )
            )
        }

        assertFailsWith<IllegalArgumentException> {
            GameConfig(
                selectedPlantCards =
                    selectedCards(),
                playerDecisionFactories =
                    List(5) {
                        PlayerDecisionFactory.baseline()
                    }
            )
        }
    }

    @Test
    fun standardRoundSetup_isEightCultivationAndFourBattle() {
        val setup =
            GameRoundSetup.standard()
                as GameRoundSetup.Ordered

        assertEquals(
            8,
            setup.cultivationRounds
        )
        assertEquals(
            4,
            setup.battleRounds
        )
        assertEquals(
            12,
            setup.totalRounds
        )
    }

    @Test
    fun firstGameRoundSetup_isSixCultivationAndThreeBattle() {
        val setup =
            GameRoundSetup.firstGame()
                as GameRoundSetup.Ordered

        assertEquals(
            6,
            setup.cultivationRounds
        )
        assertEquals(
            3,
            setup.battleRounds
        )
        assertEquals(
            9,
            setup.totalRounds
        )
    }

    @Test
    fun extendedRoundSetup_isNineCultivationAndFiveBattle() {
        val setup =
            GameRoundSetup.extended()
                as GameRoundSetup.Ordered

        assertEquals(
            9,
            setup.cultivationRounds
        )
        assertEquals(
            5,
            setup.battleRounds
        )
        assertEquals(
            14,
            setup.totalRounds
        )
    }

    @Test
    fun orderedRoundSetup_rejectsNegativeCountsAndEmptyGame() {
        assertFailsWith<IllegalArgumentException> {
            GameRoundSetup.Ordered(
                cultivationRounds = -1,
                battleRounds = 4
            )
        }

        assertFailsWith<IllegalArgumentException> {
            GameRoundSetup.Ordered(
                cultivationRounds = 8,
                battleRounds = -1
            )
        }

        assertFailsWith<IllegalArgumentException> {
            GameRoundSetup.Ordered(
                cultivationRounds = 0,
                battleRounds = 0
            )
        }
    }

    @Test
    fun constructor_preservesSeedAndDieConfiguration() {
        val config =
            GameConfig.baseline(
                selectedPlantCards =
                    selectedCards(),
                numPlayers = 2,
                seed = 12345L,
                dieConfig =
                    DieFactory.Config.UNIFORM
            )

        assertEquals(
            12345L,
            config.seed
        )
        assertEquals(
            DieFactory.Config.UNIFORM,
            config.dieConfig
        )
    }

    private fun selectedCards(): List<PlantCard> =
        listOf(
            card("Root_1", PlantType.ROOT, 5),
            card("Root_2", PlantType.ROOT, 7),
            card("Root_3", PlantType.ROOT, 9),
            card("Vine_1", PlantType.VINE, 7),
            card("Vine_2", PlantType.VINE, 9),
            card("Vine_3", PlantType.VINE, 11),
            card("Flower_1", PlantType.FLOWER, 11),
            card("Flower_2", PlantType.FLOWER, 14),
            card("Flower_3", PlantType.FLOWER, 17)
        )

    private fun card(
        name: String,
        type: PlantType,
        cost: Int
    ): PlantCard =
        PlantCard(
            quantity = 4,
            name = name,
            title = name,
            type = type,
            cost = cost,
            lineIcon = null,
            vpIcon = "",
            typeIcon = "",
            fgColor = "",
            textColor = "",
            fullImage = "",
            backgroundImage = "",
            cardBackgroundImage = "",
            effect = GameEffect.UNKNOWN
        )
}
