package dugsolutions.leaf.v30.table

import dugsolutions.leaf.v30.common.Commons
import dugsolutions.leaf.v30.grove.Grove
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.round.RoundCardManager
import dugsolutions.leaf.v30.round.RoundCardRegistry
import dugsolutions.leaf.v30.round.RoundDeck
import dugsolutions.leaf.v30.round.di.RoundCardsFactory
import dugsolutions.leaf.v30.wisp.WispCardManager
import dugsolutions.leaf.v30.wisp.WispCardRegistry
import dugsolutions.leaf.v30.wisp.WispDeck
import dugsolutions.leaf.v30.wisp.di.WispCardsFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TableTest {

    private lateinit var grove: Grove
    private lateinit var roundDeck: RoundDeck
    private lateinit var SUT: Table

    @BeforeEach
    fun setup() {
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
