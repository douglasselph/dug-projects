package dugsolutions.leaf.components

/**
 * Represents the different types of cost elements:
 * - Single die N+
 * - Single die =N
 * - Sum of dice is N+
 * - Sum of dice =N
 * - Flourish Type Present
 */
sealed class CostElement {
    abstract override fun toString(): String
    
    /**
     * Requires one die showing a value of at least [minimum].
     * Match "S#+".
     */
    data class SingleDieMinimum(val minimum: Int) : CostElement() {
        override fun toString(): String = "Die ${minimum}+"
    }
    
    /**
     * Requires one die showing exactly the value [exact].
     * Match "S#"
     */
    data class SingleDieExact(val exact: Int) : CostElement() {
        override fun toString(): String = "Die=${exact}"
    }
    
    /**
     * Requires the sum of all dice to be at least [minimum].
     * Match "M#+"
     */
    data class TotalDiceMinimum(val minimum: Int) : CostElement() {
        override fun toString(): String = "Sum ${minimum}+"
    }
    
    /**
     * Requires the sum of all dice to be exactly [exact].
     * Match "M#"
     */
    data class TotalDiceExact(val exact: Int) : CostElement() {
        override fun toString(): String = "Sum=${exact}"
    }
    
    /**
     * Requires the player to have at least one card of the specified [flourishType].
     * Match "R","C","V","B"
     */
    data class FlourishTypePresent(val flourishType: FlourishType) : CostElement() {
        override fun toString(): String = "Has $flourishType"
    }
}

/**
 * Represents a complete cost as a collection of cost elements.
 * All elements must be satisfied to meet the full cost.
 */
data class Cost(val elements: List<CostElement> = emptyList()) {
    override fun toString(): String = elements.joinToString(", ")
}
