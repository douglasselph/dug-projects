package dugsolutions.leaf.v35.game.buy

import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.dice.PlayerDice
import dugsolutions.leaf.v35.random.Randomizer
import dugsolutions.leaf.v35.random.die.Die
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BuyOrderTest {

    @Test
    fun determine_highestDieStartsAndOrderContinuesClockwiseWithoutReranking() {
        val players = listOf(player(1, 15), player(2, 2), player(3, 20), player(4, 3))

        val result = BuyOrder.determine(players, FixedRandomizer())

        assertEquals(listOf(3, 4, 1, 2), result.map { it.id.value })
    }

    @Test
    fun determine_secondThenThirdHighestBreakTiesLexicographically() {
        val second = listOf(player(1, 9, 5), player(2, 9, 7), player(3, 8))
        val third = listOf(player(1, 9, 7, 3), player(2, 9, 7, 4), player(3, 8))

        assertEquals(2, BuyOrder.determine(second, FixedRandomizer()).first().id.value)
        assertEquals(2, BuyOrder.determine(third, FixedRandomizer()).first().id.value)
    }

    @Test
    fun determine_differentHandSizesTreatMissingValueAsZero() {
        val players = listOf(player(1, 8), player(2, 8, 1), player(3))

        assertEquals(2, BuyOrder.determine(players, FixedRandomizer()).first().id.value)
    }

    @Test
    fun determine_exactTieUsesD20AndRerollsOnlyTiedLeaders() {
        val randomizer = FixedRandomizer(10, 10, 5, 12, 19)
        val players = listOf(player(1, 8), player(2, 8), player(3, 8))
        val handsBefore = players.map { it.dice.hand.map(Die::value) }

        val result = BuyOrder.determine(players, randomizer)

        assertEquals(2, result.first().id.value)
        assertEquals(5, randomizer.calls)
        assertEquals(handsBefore, players.map { it.dice.hand.map(Die::value) })
    }

    @Test
    fun determine_doesNotMutateInputList() {
        val players = mutableListOf(player(1, 2), player(2, 10), player(3, 4))
        val before = players.toList()

        BuyOrder.determine(players, FixedRandomizer())

        assertEquals(before, players)
    }

    private fun player(id: Int, vararg values: Int): Player = Player(
        id = PlayerId(id),
        decisions = DecisionDirector.baseline(),
        dice = PlayerDice(hand = values.map { FixedDie(20, it) })
    )

    private class FixedDie(sides: Int, value: Int) : Die(sides) {
        init { adjustTo(value) }
        override fun roll(): Die = this
    }

    private class FixedRandomizer(vararg rolls: Int) : Randomizer {
        private val rolls = ArrayDeque(rolls.toList())
        var calls = 0
        override fun nextInt(from: Int, until: Int): Int {
            calls++
            return rolls.removeFirstOrNull() ?: from
        }
        override fun nextBoolean() = false
        override fun nextInt(until: Int) = nextInt(0, until)
        override fun <T> randomOrNull(list: List<T>) = list.firstOrNull()
        override fun <T> shuffled(list: List<T>) = list
    }
}
