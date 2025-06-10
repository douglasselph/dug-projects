package dugsolutions.leaf.grove.local

import dugsolutions.leaf.grove.Grove
import dugsolutions.leaf.grove.domain.MarketStackID

/**
 * Return true if a combination of at least one set of 3 stacks add up to 4 or less cards.
 */
class GroveNearingTransition(
    private val grove: Grove
) {

    operator fun invoke(): Boolean {
        val counts = mutableMapOf<MarketStackID, Int>()
        for (stackType in MarketStackID.entries) {
            counts[stackType] = grove.getCardsFor(stackType)?.size ?: 0
        }
        val sortedCounts = counts.values.sorted()
        val sumOfThreeLowest = sortedCounts.take(3).sum()
        return sumOfThreeLowest <= 4
    }

}
