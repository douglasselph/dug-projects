package dugsolutions.leaf.v35.battle

import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.random.Randomizer

/**
 * Determines the complete left-to-right Battle order for one round.
 *
 * Unlike Buy order, Battle order ranks every player. Hands are compared
 * lexicographically by die value from highest to lowest. Only players whose
 * complete sorted value sequences tie roll D20s; equal D20 results reroll just
 * that tied subgroup until a strict order is produced.
 */
object BattleOrder {

    fun determine(
        players: List<Player>,
        randomizer: Randomizer
    ): List<Player> {
        if (players.isEmpty()) return emptyList()

        val byValues =
            players.groupBy { player ->
                player.dice.hand
                    .map { it.value }
                    .sortedDescending()
            }

        val orderedValueGroups =
            byValues.entries.sortedWith { left, right ->
                compareValueLists(
                    first = left.key,
                    second = right.key
                )
            }

        return orderedValueGroups.flatMap { entry ->
            if (entry.value.size == 1) {
                entry.value
            } else {
                breakExactTie(
                    tiedPlayers = entry.value,
                    randomizer = randomizer
                )
            }
        }
    }

    /** Comparator result suitable for ascending sort, while stronger Hands come first. */
    private fun compareValueLists(
        first: List<Int>,
        second: List<Int>
    ): Int {
        val length = maxOf(first.size, second.size)
        for (index in 0 until length) {
            val firstValue = first.getOrNull(index) ?: 0
            val secondValue = second.getOrNull(index) ?: 0
            val comparison = secondValue.compareTo(firstValue)
            if (comparison != 0) return comparison
        }
        return 0
    }

    private fun breakExactTie(
        tiedPlayers: List<Player>,
        randomizer: Randomizer
    ): List<Player> {
        if (tiedPlayers.size <= 1) return tiedPlayers

        val byRoll =
            tiedPlayers.groupBy {
                randomizer.nextInt(1, 21)
            }

        return byRoll.entries
            .sortedByDescending { it.key }
            .flatMap { entry ->
                if (entry.value.size == 1) {
                    entry.value
                } else {
                    breakExactTie(
                        tiedPlayers = entry.value,
                        randomizer = randomizer
                    )
                }
            }
    }
}
