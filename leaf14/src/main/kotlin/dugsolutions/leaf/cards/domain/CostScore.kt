package dugsolutions.leaf.cards.domain

/**
 * Calculates the difficulty score for a Cost.
 * Higher scores indicate harder requirements to fulfill.
 * Right now the cost is just a simple value. I am trying to keep things simple.
 * But there very well could be another type of cost (a D10 for example), that I need
 * to be prepared for.
 */
class CostScore {

    operator fun invoke(cost: Cost): Int {
        return when(cost) {
            Cost.None -> 0
            is Cost.Value -> cost.amount
        }
    }

}
