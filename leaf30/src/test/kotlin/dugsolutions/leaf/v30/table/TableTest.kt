package dugsolutions.leaf.v30.table

import dugsolutions.leaf.v30.common.Commons
import dugsolutions.leaf.v30.cards.GameCardRegistry
import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.cards.domain.GameCards
import dugsolutions.leaf.v30.game.domain.CurrentRoundNotSetException
import dugsolutions.leaf.v30.grove.Grove
import dugsolutions.leaf.v30.grove.domain.GroveCardStackID
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.round.RoundCardManager
import dugsolutions.leaf.v30.round.RoundCardRegistry
import dugsolutions.leaf.v30.round.RoundDeck
import dugsolutions.leaf.v30.round.di.RoundCardsFactory
import dugsolutions.leaf.v30.table.domain.TableConfig
import dugsolutions.leaf.v30.wisp.WispCardManager
import dugsolutions.leaf.v30.wisp.WispCardRegistry
import dugsolutions.leaf.v30.wisp.WispDeck
import dugsolutions.leaf.v30.wisp.di.WispCardsFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TableTest {

    private lateinit var grove: Grove
    private lateinit var roundDeck: RoundDeck
    private lateinit var cards: List<GameCard>
    private lateinit var SUT: Table

    @BeforeEach
    fun setup() {
        val cardRegistry = GameCardRegistry()
        cardRegistry.loadFromCsv(Commons.CARD_LIST)
        cards = cardRegistry.getAllCards()
        grove = Grove(createWispDeck())
        roundDeck = createRoundDeck()
        SUT = Table(grove, roundDeck)
    }

    @Test
    fun players_whenNew_isEmpty() {
        assertTrue(SUT.players.isEmpty())
    }

    @Test
    fun add_addsPlayerAndReturnsSameTable() {
        val player = Player()

        val result = SUT.add(player)

        assertSame(SUT, result)
        assertEquals(listOf(player), SUT.players)
    }

    @Test
    fun add_withMultiplePlayers_preservesOrder() {
        val player1 = Player()
        val player2 = Player()

        SUT.add(player1)
        SUT.add(player2)

        assertEquals(listOf(player1, player2), SUT.players)
    }

    @Test
    fun players_whenSnapshotIsChanged_doesNotChangeTablePlayers() {
        val player = Player()
        SUT.add(player)
        val snapshot = SUT.players.toMutableList()

        snapshot.clear()

        assertEquals(listOf(player), SUT.players)
    }

    @Test
    fun setup_withConfig_setsUpGroveAndRoundDeck() {
        val card = requireNotNull(cards.firstOrNull { it.name == "Root_05_01" })
        val config = TableConfig(
            cards = GameCards(listOf(card)),
            numPlayers = 2,
            numBattle = 1,
            numCultivation = 2
        )

        SUT.setup(config)

        assertEquals(0, grove.diceStacks.getCount(dugsolutions.leaf.v30.random.die.DieSides.D4))
        assertEquals(7, grove.diceStacks.getCount(dugsolutions.leaf.v30.random.die.DieSides.D6))
        assertEquals(card, grove.cardStacks.getCard(GroveCardStackID.ROOT_5))
        assertEquals(8, grove.cardStacks.getCount(GroveCardStackID.ROOT_5))
        assertEquals(30, grove.wispDeck.remaining)
        assertEquals(3, roundDeck.remaining)
    }

    @Test
    fun currentRoundType_whenNoTopRoundCard_throwsException() {
        assertThrows<CurrentRoundNotSetException> {
            SUT.currentRoundType
        }
    }

    @Test
    fun currentRoundType_whenRoundDeckHasTopCard_returnsTopCardType() {
        roundDeck.setup(numBattle = 0, numCultivation = 1)
        val card = roundDeck.next()

        assertEquals(card!!.cardType, SUT.currentRoundType)
    }

    private fun createWispDeck(): WispDeck {
        val registry = WispCardRegistry()
        registry.loadFromCsv(Commons.WISP_LIST)
        val manager = WispCardManager(WispCardsFactory())
        manager.loadCards(registry)
        return WispDeck(manager, IdentityRandomizer())
    }

    private fun createRoundDeck(): RoundDeck {
        val registry = RoundCardRegistry()
        registry.loadFromCsv(Commons.ROUND_CARD_LIST)
        val manager = RoundCardManager(RoundCardsFactory())
        manager.loadCards(registry)
        return RoundDeck(manager, IdentityRandomizer())
    }

    private class IdentityRandomizer : Randomizer {
        override fun nextBoolean(): Boolean = throw UnsupportedOperationException()
        override fun nextInt(from: Int, until: Int): Int = throw UnsupportedOperationException()
        override fun nextInt(until: Int): Int = throw UnsupportedOperationException()
        override fun <T> randomOrNull(list: List<T>): T? = throw UnsupportedOperationException()
        override fun <T> shuffled(list: List<T>): List<T> = list
    }
}
