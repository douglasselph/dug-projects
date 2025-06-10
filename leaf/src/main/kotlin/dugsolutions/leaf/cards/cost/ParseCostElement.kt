package dugsolutions.leaf.cards.cost

import dugsolutions.leaf.cards.domain.FlourishType

class ParseCostElement {

    companion object {
        private val FLOURISH_TYPE_MAP = mapOf(
            "R" to FlourishType.ROOT,
            "C" to FlourishType.CANOPY,
            "V" to FlourishType.VINE
        )
    }

    operator fun invoke(element: String): CostElement {
        return when {
            // Single Die Minimum (S#+): e.g., "D6+"
            element.matches(Regex("^S\\d+\\+$")) -> {
                val value = element.substring(1, element.length - 1).toInt()
                CostElement.SingleDieMinimum(value)
            }

            // Single Die Exact (S#): e.g., "D2"
            element.matches(Regex("^S\\d+$")) -> {
                val value = element.substring(1).toInt()
                CostElement.SingleDieExact(value)
            }

            // Total Dice Minimum (M#+): e.g., "M10+"
            element.matches(Regex("^M\\d+\\+$")) -> {
                val value = element.substring(1, element.length - 1).toInt()
                CostElement.TotalDiceMinimum(value)
            }

            // Total Dice Minimum (plain number): e.g., "8" (equivalent to "M8+")
            element.matches(Regex("^\\d+$")) -> {
                val value = element.toInt()
                CostElement.TotalDiceMinimum(value)
            }

            // Total Dice Exact (M#): e.g., "M10"
            element.matches(Regex("^M\\d+$")) -> {
                val value = element.substring(1).toInt()
                CostElement.TotalDiceExact(value)
            }

            // Flourish Type Present: e.g., "R", "C", "V"
            FLOURISH_TYPE_MAP.containsKey(element) -> {
                val flourishType = FLOURISH_TYPE_MAP[element]
                    ?: throw IllegalArgumentException("Unknown flourish type: $element")
                CostElement.FlourishTypePresent(flourishType)
            }

            // Can't parse
            else -> throw IllegalArgumentException("Invalid cost element: $element")
        }
    }
}
