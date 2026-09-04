package dugsolutions.leaf.v35.game.di

import dugsolutions.leaf.v35.chronicle.domain.Moment
import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.effect.GameEffectConverter
import dugsolutions.leaf.v35.game.GameConfig
import dugsolutions.leaf.v35.game.GameRoundSetup
import dugsolutions.leaf.v35.grove.di.GroveFactory
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.RoundCardRegistry
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameFactoryTest {

    private lateinit var factory: GameFactory
    private lateinit var selectedCards: List<PlantCard>

    @BeforeEach
    fun setup() {
        val converter =
            GameEffectConverter()

        val wispRegistry =
            WispCardRegistry(converter)

        wispRegistry.loadFromCsv(
            dataPath(CardDataFiles.WISP_LIST)
        )

        val wispManager =
            WispCardManager().apply {
                loadCards(wispRegistry)
            }

        val roundRegistry =
            RoundCardRegistry(converter)

        roundRegistry.loadFromCsv(
            dataPath(CardDataFiles.ROUND_CARD_LIST)
        )

        val roundManager =
            RoundCardManager().apply {
                loadCards(roundRegistry)
            }

        val plantRegistry =
            PlantCardRegistry(converter)

        plantRegistry.loadFromCsv(
            dataPath(CardDataFiles.ROOT_CARD_LIST),
            dataPath(CardDataFiles.VF_CARD_LIST)
        )

        val allPlants =
            plantRegistry.getAllCards()

        selectedCards =
            buildList {
                addAll(
                    allPlants
                        .filter {
                            it.type ==
                                PlantType.ROOT
                        }
                        .take(3)
                )
                addAll(
                    allPlants
                        .filter {
                            it.type ==
                                PlantType.VINE
                        }
                        .take(3)
                )
                addAll(
                    allPlants
                        .filter {
                            it.type ==
                                PlantType.FLOWER
                        }
                        .take(3)
                )
            }

        factory =
            GameFactory(
                groveFactory =
                    GroveFactory(
                        wispCardManager =
                            wispManager
                    ),
                roundCardManager =
                    roundManager
            )
    }

    @Test
    fun invoke_createsRequestedPlayersWithLocalIdsAndStartingDice() {
        val game =
            factory(
                GameConfig.baseline(
                    selectedPlantCards =
                        selectedCards,
                    numPlayers = 4,
                    seed = 100L
                )
            )

        assertEquals(
            listOf(1, 2, 3, 4),
            game.players.map {
                it.id.value
            }
        )

        game.players.forEach { player ->
            assertEquals(
                6,
                player.dice.supplySize
            )
            assertEquals(
                3,
                player.dice.supply.count {
                    it.sides == 4
                }
            )
            assertEquals(
                3,
                player.dice.supply.count {
                    it.sides == 6
                }
            )
            assertTrue(
                player.dice.hand.isEmpty()
            )
            assertTrue(
                player.dice.discard.isEmpty()
            )
        }
    }

    @Test
    fun invoke_setsUpConfiguredRoundDeck() {
        val game =
            factory(
                GameConfig.baseline(
                    selectedPlantCards =
                        selectedCards,
                    numPlayers = 2,
                    roundSetup =
                        GameRoundSetup.Ordered(
                            cultivationRounds = 5,
                            battleRounds = 3
                        ),
                    seed = 200L
                )
            )

        assertEquals(
            8,
            game.roundDeck.remaining
        )

        assertTrue(
            game.roundDeck.cards.cards
                .take(5)
                .all {
                    it.type ==
                        RoundCardType.CULTIVATION
                }
        )

        assertTrue(
            game.roundDeck.cards.cards
                .drop(5)
                .all {
                    it.type ==
                        RoundCardType.BATTLE
                }
        )
    }

    @Test
    fun sameReusableConfig_createsFreshDecisionDirectors() {
        val config =
            GameConfig.baseline(
                selectedPlantCards =
                    selectedCards,
                numPlayers = 2,
                roundSetup =
                    GameRoundSetup.Ordered(
                        cultivationRounds = 1,
                        battleRounds = 1
                    ),
                seed = 300L
            )

        val first =
            factory(config)

        val second =
            factory(config)

        assertFalse(
            first.players[0].decisions ===
                second.players[0].decisions
        )

        assertFalse(
            first.players[1].decisions ===
                second.players[1].decisions
        )
    }

    @Test
    fun sameSeed_producesSameInitialRoundAndWispDeckOrder() {
        val config =
            GameConfig.baseline(
                selectedPlantCards =
                    selectedCards,
                numPlayers = 2,
                roundSetup =
                    GameRoundSetup.Ordered(
                        cultivationRounds = 5,
                        battleRounds = 3
                    ),
                seed = 12345L
            )

        val first =
            factory(config)

        val second =
            factory(config)

        assertEquals(
            first.roundDeck.cards.cards.map {
                it.name
            },
            second.roundDeck.cards.cards.map {
                it.name
            }
        )

        assertEquals(
            first.grove.wispDeck.cards.cards.map {
                it.name
            },
            second.grove.wispDeck.cards.cards.map {
                it.name
            }
        )
    }

    @Test
    fun separateGames_doNotShareMutableGameState() {
        val config =
            GameConfig.baseline(
                selectedPlantCards =
                    selectedCards,
                numPlayers = 2,
                roundSetup =
                    GameRoundSetup.Ordered(
                        cultivationRounds = 2,
                        battleRounds = 1
                    ),
                seed = 999L
            )

        val first =
            factory(config)

        val second =
            factory(config)

        assertTrue(first !== second)
        assertTrue(
            first.randomizer !==
                second.randomizer
        )
        assertTrue(
            first.chronicle !==
                second.chronicle
        )
        assertTrue(
            first.grove !==
                second.grove
        )
        assertTrue(
            first.roundDeck !==
                second.roundDeck
        )
        assertTrue(
            first.players[0] !==
                second.players[0]
        )

        first.grove.graftBed.take(
            DieSides.D6
        )
        first.players[0].addVp(1)
        first.chronicle.record(
            Moment.Marker("first game")
        )
        first.roundDeck.next()

        assertEquals(
            8,
            first.grove.graftBed.count(
                DieSides.D6
            )
        )
        assertEquals(
            9,
            second.grove.graftBed.count(
                DieSides.D6
            )
        )

        assertEquals(
            1,
            first.players[0].vp
        )
        assertEquals(
            0,
            second.players[0].vp
        )

        assertEquals(
            1,
            first.chronicle.entries.size
        )
        assertTrue(
            second.chronicle.entries.isEmpty()
        )

        assertEquals(
            2,
            first.roundDeck.remaining
        )
        assertEquals(
            3,
            second.roundDeck.remaining
        )
    }

    private fun dataPath(
        fileName: String
    ): String =
        Path.of(
            "data",
            "v35",
            fileName
        ).toString()
}
