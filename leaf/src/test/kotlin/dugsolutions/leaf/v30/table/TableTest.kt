package dugsolutions.leaf.v30.table

import dugsolutions.leaf.v30.grove.Grove
import dugsolutions.leaf.v30.player.Player
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TableTest {

    private lateinit var grove: Grove
    private lateinit var SUT: Table

    @BeforeEach
    fun setup() {
        grove = Grove()
        SUT = Table(grove)
    }

    @Test
    fun constructor_holdsInjectedGrove() {
        assertSame(grove, SUT.grove)
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
}
