package dugsolutions.leaf.main.gather

import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.grove.Grove
import dugsolutions.leaf.grove.domain.MarketStackID
import dugsolutions.leaf.main.domain.GroveInfo
import dugsolutions.leaf.main.domain.StackInfo
import dugsolutions.leaf.player.Player

class GatherGroveInfo(
    private val grove: Grove,
    private val gatherCardInfo: GatherCardInfo
) {

    operator fun invoke(
        highlight: List<GameCard> = emptyList(),
        selectForPlayer: Player? = null
    ): GroveInfo {
        return GroveInfo(
            stacks = MarketStackID.entries.map { stack -> get(stack, highlight) },
            selectText = selectForPlayer?.let { it.name + " PIPS " + it.pipTotal }
        )
    }

    private fun get(stack: MarketStackID, highlight: List<GameCard> = emptyList()): StackInfo {
        val card = grove.getCardsFor(stack)?.getCard(0)
        val highlightCard = highlight.any { it.id == card?.id }
        return StackInfo(
            stack = stack,
            topCard = card?.let { gatherCardInfo(it) },
            numCards = grove.getCardsFor(stack)?.size ?: 0,
            highlight = highlightCard
        )
    }
}
