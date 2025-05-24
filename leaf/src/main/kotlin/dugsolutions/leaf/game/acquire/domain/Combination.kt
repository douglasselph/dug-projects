package dugsolutions.leaf.game.acquire.domain

import dugsolutions.leaf.components.die.DieValue
import dugsolutions.leaf.components.die.DieValues

sealed class Adjusted {
    data class ByAmount(val die: DieValue, val amount: Int) : Adjusted()
    data class ToMax(val die: DieValue) : Adjusted()
}

data class Combination(
    val values: DieValues,
    val addToTotal: Int,
    val adjusted: List<Adjusted> = emptyList()
) {

    override fun toString(): String {
        val buffer = StringBuffer()
        buffer.append("Combination(")
        if (addToTotal > 0) {
            buffer.append("addToTotal=$addToTotal, ")
        }
        if (adjusted.isNotEmpty()) {
            buffer.append("adjusted:$adjusted, ")
        }
        buffer.append("values:$values)")
        return buffer.toString()
    }
}

data class Combinations(
    val list: List<Combination>
) : Iterable<Combination> {
    override fun iterator(): Iterator<Combination> {
        return list.iterator()
    }
}

val Combination.totalValue: Int
    get() {
        // Sum all dice values
        val diceTotal = values.dice.sumOf { it.value }

        // Add any additional total from effects
        return diceTotal + addToTotal
    }
