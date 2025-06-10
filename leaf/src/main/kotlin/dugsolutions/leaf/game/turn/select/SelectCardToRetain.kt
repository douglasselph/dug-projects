package dugsolutions.leaf.game.turn.select

import dugsolutions.leaf.cards.cost.CostScore
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.GameCard

class SelectCardToRetain(
    private val costScore: CostScore
) {

    private val priorityMap = mapOf(
        FlourishType.BLOOM to 5,
        FlourishType.VINE to 4,
        FlourishType.CANOPY to 3,
        FlourishType.ROOT to 2,
        FlourishType.SEEDLING to 1,
        FlourishType.NONE to 0
    )

    // Choose BLOOM card, then VINE, then CANOPY, then ROOT
    // Choose card with the largest COST.
    operator fun invoke(cards: List<GameCard>, flourishType: FlourishType? = null): GameCard? {
        // If flourishType is specified, only consider cards of that type
        val eligibleCards = if (flourishType != null) {
            cards.filter { it.type == flourishType }
        } else {
            cards
        }
        return eligibleCards
            .sortedWith(compareByDescending<GameCard> {
                priorityMap[it.type] ?: Int.MIN_VALUE
            }.thenByDescending {
                costScore(it.cost)
            })
            .firstOrNull()
    }
}
