package dugsolutions.leaf.cards.cost

import dugsolutions.leaf.cards.domain.FlourishType

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
        override fun toString(): String = "${minimum}+"
    }
    
    /**
     * Requires the sum of all dice to be exactly [exact].
     * Match "M#"
     */
    data class TotalDiceExact(val exact: Int) : CostElement() {
        override fun toString(): String = "=${exact}"
    }
    
    /**
     * Requires the player to have at least one card of the specified [flourishType].
     * Match "R","C","V","B"
     */
    data class FlourishTypePresent(val flourishType: FlourishType) : CostElement() {
        override fun toString(): String = "$flourishType"
    }
}

/**
 * Represents a single cost alternative - a collection of cost elements that must ALL be satisfied.
 * This is essentially what the old Cost class represented.
 */
data class CostAlternative(val elements: List<CostElement> = emptyList()) {
    override fun toString(): String = elements.joinToString(" ")
}

/**
 * Represents a complete cost as a collection of alternative cost combinations.
 * ANY ONE of the alternatives must be satisfied to meet the full cost.
 * 
 * Examples:
 * - Simple cost: Cost([CostAlternative([ROOT, Die8+])])
 * - OR cost: Cost([CostAlternative([ROOT, Die8+]), CostAlternative([Die15+])])
 */
data class Cost(val alternatives: List<CostAlternative> = emptyList()) {

    companion object {
        fun from(elements: List<CostElement>): Cost {
            return Cost(listOf(CostAlternative(elements)))
        }
    }
    override fun toString(): String = when {
        alternatives.isEmpty() -> "Free"
        alternatives.size == 1 -> alternatives.first().toString()
        else -> alternatives.joinToString(" OR ") { "$it" }
    }
}