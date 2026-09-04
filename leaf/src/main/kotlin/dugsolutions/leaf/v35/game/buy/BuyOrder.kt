package dugsolutions.leaf.v35.game.buy

import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.random.Randomizer

object BuyOrder {
    fun determine(
        players: List<Player>,
        randomizer: Randomizer
    ): List<Player> {
        if (players.isEmpty()) return emptyList()

        val values = players.associateWith {
            it.dice.hand.map { die -> die.value }.sortedDescending()
        }
        var leaders = players.filter { candidate ->
            players.none { other -> compare(values.getValue(other), values.getValue(candidate)) > 0 }
        }

        while (leaders.size > 1) {
            val rolls = leaders.associateWith { randomizer.nextInt(1, 21) }
            val highest = rolls.values.max()
            leaders = leaders.filter { rolls.getValue(it) == highest }
        }

        val firstIndex = players.indexOf(leaders.single())
        return (players.drop(firstIndex) + players.take(firstIndex)).toList()
    }

    private fun compare(first: List<Int>, second: List<Int>): Int {
        val length = maxOf(first.size, second.size)
        for (index in 0 until length) {
            val comparison = (first.getOrNull(index) ?: 0)
                .compareTo(second.getOrNull(index) ?: 0)
            if (comparison != 0) return comparison
        }
        return 0
    }
}
