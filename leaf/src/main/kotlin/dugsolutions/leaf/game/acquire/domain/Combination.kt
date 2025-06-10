package dugsolutions.leaf.game.acquire.domain

import dugsolutions.leaf.random.die.DieValue
import dugsolutions.leaf.random.die.DieValues

sealed class Adjusted {
    data class ByAmount(val die: DieValue, val amount: Int) : Adjusted()
    data class ToMax(val die: DieValue) : Adjusted()
}

data class Combination(
    val values: DieValues = DieValues(emptyList()),
    val addToTotal: Int = 0
) {

    override fun toString(): String {
        val buffer = StringBuffer()
        buffer.append("Comb(")
        if (addToTotal > 0) {
            buffer.append("addToTotal=$addToTotal, ")
        }
        buffer.append("values:${values.values})")
        return buffer.toString()
    }

    val simplicityScore: Int
        get() {
            return values.dice.size
        }

    val totalValue: Int
        get() {
            val diceTotal = values.dice.sumOf { it.value }
            return diceTotal + addToTotal
        }
}

data class Combinations(
    val list: List<Combination>
) : Iterable<Combination> {
    override fun iterator(): Iterator<Combination> {
        return list.iterator()
    }

    override fun toString(): String {
        return "Combs(" + list.map { it.toString() }.joinToString(",") +")"
    }
}

val Combination.totalValue: Int
    get() {
        // Sum all dice values
        val diceTotal = values.dice.sumOf { it.value }

        // Add any additional total from effects
        return diceTotal + addToTotal
    }
