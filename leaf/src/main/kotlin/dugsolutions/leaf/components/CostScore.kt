package dugsolutions.leaf.components

/**
 * Calculates the difficulty score for a Cost.
 * Higher scores indicate harder requirements to fulfill.
 */
class CostScore {

    /**
     * Calculate a difficulty score for the given cost.
     * Higher scores indicate harder requirements to fulfill.
     * 
     * The score is calculated based on the sum of individual element difficulties.
     * 
     * @param cost The cost to evaluate
     * @return An integer score representing the cost difficulty
     */
    operator fun invoke(cost: Cost): Int {
        if (cost.elements.isEmpty()) return 0
        
        // Calculate score from sum of individual difficulties
        return cost.elements.sumOf { difficulty(it) }
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
