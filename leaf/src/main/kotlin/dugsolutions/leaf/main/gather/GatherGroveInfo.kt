package dugsolutions.leaf.main.gather

import dugsolutions.leaf.grove.Grove
import dugsolutions.leaf.grove.domain.MarketStackID
import dugsolutions.leaf.main.domain.GroveInfo
import dugsolutions.leaf.main.domain.StackInfo

class GatherGroveInfo(
    private val grove: Grove,
    private val gatherCardInfo: GatherCardInfo
) {

    operator fun invoke(): GroveInfo {
        return GroveInfo(
            stacks = MarketStackID.entries.map { stack -> get(stack) }
        )
    }

    private fun get(stack: MarketStackID): StackInfo {
        val card = grove.getCardsFor(stack)?.getCard(0)
        return StackInfo(
            stack = stack,
            topCard = card?.let { gatherCardInfo(it) },
            numCards = grove.getCardsFor(stack)?.size ?: 0
        )
    }
}
