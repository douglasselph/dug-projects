package dugsolutions.leaf.v35.game

import dugsolutions.leaf.v35.chronicle.GameChronicle
import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectConverter
import dugsolutions.leaf.v35.grove.Grove
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.random.Randomizer
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.RoundCardRegistry
import dugsolutions.leaf.v35.round.RoundDeck
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispDeck
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameTest {

    @Test
    fun newGame_isReadyAndNotComplete() {
        val game = fixture(listOf(player(1), player(2))).game

        assertEquals(GameStatus.READY, game.status)
        assertFalse(game.isComplete)
    }

    @Test
    fun newGame_exposesOwnedStateAndDefensivePlayerList() {
        val incoming =
            mutableListOf(
                player(1),
                player(2)
            )

        val fixture =
            fixture(incoming)

        incoming.clear()

        assertEquals(
            2,
            fixture.game.players.size
        )
        assertTrue(
            fixture.game.grove ===
                fixture.grove
        )
        assertTrue(
            fixture.game.chronicle ===
                fixture.chronicle
        )
        assertTrue(
            fixture.game.roundDeck ===
                fixture.roundDeck
        )
        assertTrue(
            fixture.game.randomizer ===
                fixture.randomizer
        )
    }

    @Test
    fun beforeFirstReveal_currentRoundIsNullAndRoundNumberIsZero() {
        val fixture =
            fixture(
                listOf(
                    player(1),
                    player(2)
                )
            )

        assertNull(
            fixture.game.currentRound
        )
        assertEquals(
            0,
            fixture.game.roundNumber
        )
        assertFalse(
            fixture.game.hasRevealedFinalRound
        )
    }

    @Test
    fun currentRoundAndRoundNumber_deriveFromRoundDeck() {
        val fixture =
            fixture(
                listOf(
                    player(1),
                    player(2)
                )
            )

        val revealed =
            fixture.roundDeck.next()

        assertEquals(
            revealed,
            fixture.game.currentRound
        )
        assertEquals(
            1,
            fixture.game.roundNumber
        )
    }

    @Test
    fun afterFinalCardReveal_hasRevealedFinalRoundIsTrue() {
        val fixture =
            fixture(
                listOf(
                    player(1),
                    player(2)
                )
            )

        repeat(
            fixture.config.roundSetup.totalRounds
        ) {
            fixture.roundDeck.next()
        }

        assertTrue(
            fixture.game.hasRevealedFinalRound
        )
        assertEquals(
            fixture.config.roundSetup.totalRounds,
            fixture.game.roundNumber
        )
    }

    private fun fixture(
        players: List<Player>
    ): Fixture {
        val randomizer =
            Randomizer.create(seed = 555L)

        val config =
            GameConfig(
                selectedPlantCards =
                    selectedCards(),
                playerDecisionFactories =
                    players.map { player ->
                        PlayerDecisionFactory {
                            player.decisions
                        }
                    },
                roundSetup =
                    GameRoundSetup.Ordered(
                        cultivationRounds = 1,
                        battleRounds = 1
                    ),
                seed = 555L
            )

        val roundDeck =
            RoundDeck(
                roundCardManager =
                    roundManager(),
                randomizer =
                    randomizer
            ).apply {
                setup(
                    numBattle = 1,
                    numCultivation = 1
                )
            }

        val grove =
            Grove(
                selectedPlantCards =
                    selectedCards(),
                wispDeck =
                    WispDeck(
                        wispCardManager =
                            WispCardManager(),
                        randomizer =
                            randomizer
                    )
            )

        val chronicle =
            GameChronicle()

        return Fixture(
            config = config,
            grove = grove,
            roundDeck = roundDeck,
            chronicle = chronicle,
            randomizer = randomizer,
            game = Game(
                config = config,
                grove = grove,
                players = players,
                chronicle = chronicle,
                roundDeck = roundDeck,
                randomizer = randomizer
            )
        )
    }

    private fun roundManager(): RoundCardManager {
        val registry =
            RoundCardRegistry(
                GameEffectConverter()
            )

        registry.loadFromCsv(
            dataPath(
                CardDataFiles.ROUND_CARD_LIST
            )
        )

        return RoundCardManager().apply {
            loadCards(registry)
        }
    }

    private fun player(
        id: Int
    ): Player =
        Player(
            id = PlayerId(id),
            decisions =
                DecisionDirector.baseline()
        )

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

    private fun dataPath(
        fileName: String
    ): String =
        Path.of(
            "data",
            "v35",
            fileName
        ).toString()

    private data class Fixture(
        val config: GameConfig,
        val grove: Grove,
        val roundDeck: RoundDeck,
        val chronicle: GameChronicle,
        val randomizer: Randomizer,
        val game: Game
    )
}
