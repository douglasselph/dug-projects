package dugsolutions.leaf.v35.battle

import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.dice.PlayerDice
import dugsolutions.leaf.v35.random.Randomizer
import dugsolutions.leaf.v35.random.die.Die

object BattleTestFixture {
    fun player(
        id: Int,
        vararg dice: Die
    ): Player =
        Player(
            id = PlayerId(id),
            decisions = DecisionDirector.baseline(),
            dice = PlayerDice(hand = dice.toList())
        )

    fun die(
        sides: Int,
        value: Int
    ): Die = FixedDie(sides, value)

    class FixedRandomizer(
        vararg rolls: Int
    ) : Randomizer {
        private val rolls = ArrayDeque(rolls.toList())
        var calls: Int = 0
            private set

        override fun nextInt(
            from: Int,
            until: Int
        ): Int {
            calls++
            return rolls.removeFirstOrNull() ?: from
        }

        override fun nextBoolean(): Boolean = false

        override fun nextInt(until: Int): Int =
            nextInt(0, until)

        override fun <T> randomOrNull(list: List<T>): T? =
            list.firstOrNull()

        override fun <T> shuffled(list: List<T>): List<T> =
            list.toList()
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
}
