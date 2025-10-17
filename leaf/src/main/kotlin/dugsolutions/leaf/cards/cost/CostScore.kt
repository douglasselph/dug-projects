package dugsolutions.leaf.cards.cost

/**
 * Calculates the difficulty score for a Cost.
 * Higher scores indicate harder requirements to fulfill.
 */
class CostScore {

    /**
     * Calculate a difficulty score for the given cost.
     * Higher scores indicate harder requirements to fulfill.
     * 
     * For costs with alternatives (OR conditions), returns the minimum score among all alternatives,
     * as this represents the easiest path to fulfill the cost.
     * Each alternative's score is calculated as the sum of its element difficulties.
     * 
     * @param cost The cost to evaluate
     * @return An integer score representing the cost difficulty
     */
    operator fun invoke(cost: Cost): Int {
        if (cost.alternatives.isEmpty()) return 0
        
        // Find the minimum score among all alternatives
        return cost.alternatives.minOf { alternative ->
            alternative.elements.sumOf { difficulty(it) }
        }
    }

    /**
     * Returns a difficulty score for a cost element.
     * Higher scores indicate harder requirements.
     * 
     * @param element The cost element to evaluate
     * @return An integer representing the element's difficulty
     */
    private fun difficulty(element: CostElement): Int {
        return when (element) {
            is CostElement.SingleDieMinimum -> element.minimum + 5
            is CostElement.SingleDieExact -> element.exact * 3
            is CostElement.TotalDiceMinimum -> element.minimum
            is CostElement.TotalDiceExact -> element.exact * 3
            is CostElement.FlourishTypePresent -> 7
        }
    }
}
