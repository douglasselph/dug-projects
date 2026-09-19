package dugsolutions.leaf.v30.battle

import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.random.die.Die

class PlayerGridOrder(
    private val randomizer: Randomizer = Randomizer.create()
) {
    operator fun invoke(players: List<Player>): List<Player> {
        if (players.isEmpty()) return emptyList()
        return players
            .map { player -> PlayerOrderData(player, sortedDice(player), randomizer.nextInt(1, D20_MAX + 1)) }
            .sortedWith(orderComparator)
            .map { it.player }
    }

    private fun sortedDice(player: Player): List<Die> {
        return player.diceHand.dice.sortedWith(dieComparator)
    }

    private data class PlayerOrderData(
        val player: Player,
        val dice: List<Die>,
        val tieBreaker: Int
    )

    private companion object {
        const val D20_MAX = 20

        val dieComparator: Comparator<Die> = compareByDescending<Die> { it.value }
            .thenBy { it.sides }

        val orderComparator: Comparator<PlayerOrderData> = Comparator { left, right ->
            val maxSize = maxOf(left.dice.size, right.dice.size)
            for (index in 0 until maxSize) {
                val leftDie = left.dice.getOrNull(index)
                val rightDie = right.dice.getOrNull(index)
                val comparison = compareDice(leftDie, rightDie)
                if (comparison != 0) return@Comparator comparison
            }
            right.tieBreaker.compareTo(left.tieBreaker)
        }

        private fun compareDice(
            left: Die?,
            right: Die?
        ): Int {
            if (left == null && right == null) return 0
            if (left == null) return 1
            if (right == null) return -1
            if (left.value != right.value) return right.value.compareTo(left.value)
            return left.sides.compareTo(right.sides)
        }
    }
}
