package dugsolutions.leaf.v30.battle

import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.random.die.Die
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlayerGridOrderTest {

    @Test
    fun invoke_whenNoPlayers_returnsEmptyList() {
        val result = PlayerGridOrder(FixedRollRandomizer()).invoke(emptyList())

        assertTrue(result.isEmpty())
    }

    @Test
    fun invoke_ordersPlayersByHighestDiceValues() {
        val player1 = player(1, FixedDie(20, 4), FixedDie(6, 2), FixedDie(8, 1))
        val player2 = player(2, FixedDie(6, 6), FixedDie(4, 1), FixedDie(8, 1))
        val player3 = player(3, FixedDie(8, 5), FixedDie(6, 3), FixedDie(20, 1))
        val player4 = player(4, FixedDie(4, 2), FixedDie(6, 2), FixedDie(8, 2))

        val result = PlayerGridOrder(FixedRollRandomizer())(listOf(player1, player2, player3, player4))

        assertEquals(listOf(2, 3, 1, 4), result.map { it.id })
    }

    @Test
    fun invoke_whenValuesTie_ordersLowerSidedDieFirst() {
        val player1 = player(1, FixedDie(20, 6), FixedDie(8, 1), FixedDie(6, 1))
        val player2 = player(2, FixedDie(6, 6), FixedDie(8, 1), FixedDie(20, 1))
        val player3 = player(3, FixedDie(8, 5), FixedDie(6, 4), FixedDie(4, 1))
        val player4 = player(4, FixedDie(4, 4), FixedDie(6, 4), FixedDie(8, 4))

        val result = PlayerGridOrder(FixedRollRandomizer())(listOf(player1, player2, player3, player4))

        assertEquals(listOf(2, 1, 3, 4), result.map { it.id })
    }

    @Test
    fun invoke_whenAllDiceTie_usesD20TieBreakerHighestRollFirst() {
        val players = listOf(
            player(1, FixedDie(6, 3), FixedDie(8, 2), FixedDie(10, 1)),
            player(2, FixedDie(6, 3), FixedDie(8, 2), FixedDie(10, 1)),
            player(3, FixedDie(6, 3), FixedDie(8, 2), FixedDie(10, 1)),
            player(4, FixedDie(6, 3), FixedDie(8, 2), FixedDie(10, 1))
        )
        val randomizer = FixedRollRandomizer(rolls = listOf(4, 18, 7, 12))

        val result = PlayerGridOrder(randomizer)(players)

        assertEquals(listOf(2, 4, 3, 1), result.map { it.id })
    }

    private fun player(
        id: Int,
        vararg dice: Die
    ): Player {
        return Player(id = id).apply {
            dice.forEach { addDieToSupply(it) }
            repeat(dice.size) { drawDie() }
        }
    }

    private class FixedDie(
        sides: Int,
        value: Int
    ) : Die(sides) {
        init {
            adjustTo(value)
        }

        override fun roll(): Die = this
    }

    private class FixedRollRandomizer(
        private val rolls: List<Int> = listOf(1, 2, 3, 4)
    ) : Randomizer {
        private var index = 0

        override fun nextBoolean(): Boolean = true

        override fun nextInt(from: Int, until: Int): Int {
            val roll = rolls.getOrElse(index++) { rolls.last() }
            return roll.coerceIn(from, until - 1)
        }

        override fun nextInt(until: Int): Int = nextInt(0, until)
        override fun <T> randomOrNull(list: List<T>): T? = list.firstOrNull()
        override fun <T> shuffled(list: List<T>): List<T> = list
    }
}
