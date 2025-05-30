package dugsolutions.leaf.tool

import dugsolutions.leaf.components.Cost
import dugsolutions.leaf.components.CostElement
import dugsolutions.leaf.components.FlourishType

class ParseCost {

    companion object {
        private val FLOURISH_TYPE_MAP = mapOf(
            "R" to FlourishType.ROOT,
            "C" to FlourishType.CANOPY,
            "V" to FlourishType.VINE
        )
    }

    operator fun invoke(value: String): Cost {
        // Handle special cases
        if (value.equals("Free", ignoreCase = true)) {
            return Cost(emptyList())
        }
        if (value.isEmpty() || value == "-" || value == "0") {
            return Cost(emptyList())
        }
        // Parse comma-separated cost elements
        val costElements = mutableListOf<CostElement>()
        val elements = value.split(" ").map { it.trim() }

        for (element in elements) {
            when {
                // Single Die Minimum (S#+): e.g., "D6+"
                element.matches(Regex("^S\\d+\\+$")) -> {
                    val value = element.substring(1, element.length - 1).toInt()
                    costElements.add(CostElement.SingleDieMinimum(value))
                }

                // Single Die Exact (S#): e.g., "D2"
                element.matches(Regex("^S\\d+$")) -> {
                    val value = element.substring(1).toInt()
                    costElements.add(CostElement.SingleDieExact(value))
                }

                // Total Dice Minimum (M#+): e.g., "M10+"
                element.matches(Regex("^M\\d+\\+$")) -> {
                    val value = element.substring(1, element.length - 1).toInt()
                    costElements.add(CostElement.TotalDiceMinimum(value))
                }

                // Total Dice Minimum (plain number): e.g., "8" (equivalent to "M8+")
                element.matches(Regex("^\\d+$")) -> {
                    val value = element.toInt()
                    costElements.add(CostElement.TotalDiceMinimum(value))
                }

                // Total Dice Exact (M#): e.g., "M10"
                element.matches(Regex("^M\\d+$")) -> {
                    val value = element.substring(1).toInt()
                    costElements.add(CostElement.TotalDiceExact(value))
                }

                // Flourish Type Present: e.g., "R", "C", "V"
                FLOURISH_TYPE_MAP.containsKey(element) -> {
                    val flourishType = FLOURISH_TYPE_MAP[element]
                        ?: throw IllegalArgumentException("Unknown flourish type: $element")
                    costElements.add(CostElement.FlourishTypePresent(flourishType))
                }

                // Can't parse
                else -> throw IllegalArgumentException("Invalid cost element: $element")
            }
        }
        return Cost(costElements)
    }
}
