package dugsolutions.leaf.random.die


class DieValues(val dice: List<DieValue>) : Iterable<DieValue> {

    companion object {
        fun from(incoming: List<Die>) = DieValues(incoming.map { it.copy })
    }

    val copy: DieValues
        get() = DieValues(dice.map { DieValue(it.sides, it.value) })

    override fun iterator(): Iterator<DieValue> {
        return dice.iterator()
    }

    override fun toString(): String {
        if (dice.isEmpty()) return ""

        // Group dice by number of sides
        val diceBySides = dice.groupBy { it.sides }
        val totalPips = dice.sumOf { it.value }

        // Format each group as "countDsides" (e.g., "2D4")
        val base = diceBySides.entries
            .sortedBy { it.key } // Sort by number of sides
            .filter { it.value.isNotEmpty() }
            .joinToString(",") { (sides, diceList) -> "${diceList.size}D$sides" }
        return "$base=$totalPips"
    }

    val values: String
        get() {
            if (dice.isEmpty()) return ""

            // Sort the dice by sides for consistent output
            val sortedDice = dice.sortedBy { it.sides }

            // Format each die as "Dn(value)"
            return sortedDice.joinToString(",") { die -> "D${die.sides}(${die.value})" }

        }

}
