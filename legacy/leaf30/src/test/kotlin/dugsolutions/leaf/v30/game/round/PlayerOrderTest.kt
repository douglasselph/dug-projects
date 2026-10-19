package dugsolutions.leaf.v30.game.round

import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.random.die.Die
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PlayerOrderTest {

    @Test
    fun invoke_whenOnePlayerHasHighestSingleDie_startsWithThatPlayerAndContinuesClockwise() {
        val players = listOf(
            playerWithDice(3),
            playerWithDice(6),
            playerWithDice(4)
        )

        val result = PlayerOrder(StaticRollRandomizer())(players)

        assertEquals(listOf(players[1], players[2], players[0]), result)
    }

    @Test
    fun invoke_whenHighestDieTied_usesNextHighestDie() {
        val players = listOf(
            playerWithDice(6, 2),
            playerWithDice(6, 5),
            playerWithDice(4, 4)
        )

        val result = PlayerOrder(StaticRollRandomizer())(players)

        assertEquals(listOf(players[1], players[2], players[0]), result)
    }

    @Test
    fun invoke_whenAllDiceTie_rollsD20ForFirstPlayerThenContinuesClockwise() {
        val players = listOf(
            playerWithDice(6, 4),
            playerWithDice(6, 4),
            playerWithDice(6, 4)
        )
        val randomizer = StaticRollRandomizer(rolls = listOf(7, 18, 12))

        val result = PlayerOrder(randomizer)(players)

        assertEquals(listOf(players[1], players[2], players[0]), result)
    }

    @Test
    fun invoke_whenPlayersEmpty_returnsEmptyList() {
        assertEquals(emptyList(), PlayerOrder(StaticRollRandomizer())(emptyList()))
    }

    private fun playerWithDice(vararg values: Int): Player {
        val player = Player()
        values.forEach { value ->
            player.addDieToSupply(FixedDie(20, value))
            player.drawDie()
        }
        return player
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

    private class StaticRollRandomizer(
        private val rolls: List<Int> = emptyList()
    ) : Randomizer {
        private var index = 0

        override fun nextBoolean(): Boolean = throw UnsupportedOperationException()
        override fun nextInt(from: Int, until: Int): Int = rolls[index++]
        override fun nextInt(until: Int): Int = throw UnsupportedOperationException()
        override fun <T> randomOrNull(list: List<T>): T? = throw UnsupportedOperationException()
        override fun <T> shuffled(list: List<T>): List<T> = list
    }
}