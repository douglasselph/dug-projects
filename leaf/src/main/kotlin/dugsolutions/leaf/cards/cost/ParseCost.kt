package dugsolutions.leaf.cards.cost

class ParseCost(
    private val parseCostElement: ParseCostElement
) {

    operator fun invoke(incoming: String): Cost {
        // Handle special cases
        if (incoming.equals("Free", ignoreCase = true)) {
            return Cost(emptyList())
        }
        if (incoming.isEmpty() || incoming == "-" || incoming == "0") {
            return Cost(emptyList())
        }
        // Split by "|" to handle OR alternatives
        val alternativeStrings = incoming.split("|").map { it.trim() }
        val alternatives = alternativeStrings.map { alternativeString ->
            parseAlternative(alternativeString)
        }
        return Cost(alternatives)
    }
    
    private fun parseAlternative(alternativeString: String): CostAlternative {
        // Parse comma-separated cost elements
        val costElements = mutableListOf<CostElement>()
        val elements = alternativeString.split(" ").map { it.trim() }

        for (element in elements) {
            costElements.add(parseCostElement(element))
        }
        return CostAlternative(costElements)
    }
}
