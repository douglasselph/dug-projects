package dugsolutions.leaf.common.domain.acquire

import dugsolutions.leaf.random.die.DieValues

data class UsingDice(
    val values: DieValues = DieValues(emptyList()),
    val addToTotal: Int = 0
) {

    override fun toString(): String {
        val buffer = StringBuffer()
        buffer.append("{")

        buffer.append(values.values)
        if (addToTotal > 0) {
            buffer.append("+$addToTotal")
        }
        buffer.append("}")
        return buffer.toString()
    }

    val totalValue: Int
        get() {
            val diceTotal = values.dice.sumOf { it.value }
            return diceTotal + addToTotal
        }
}
