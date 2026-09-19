package dugsolutions.leaf.v14.game.turn.select

import dugsolutions.leaf.v14.cards.domain.FlourishType
import dugsolutions.leaf.v14.cards.domain.GameCard
import dugsolutions.leaf.v14.game.acquire.ManageAcquiredFloralTypes
import dugsolutions.leaf.v14.grove.Grove

class SelectPossibleCards(
    private val grove: Grove,
    private val manageAcquiredFloralTypes: ManageAcquiredFloralTypes
) {
    operator fun invoke(): List<GameCard> {
        return grove.getTopShowingCards()
            .filter { card -> card.type != FlourishType.BLOOM && card.type != FlourishType.SEEDLING }
            .filter { card -> !manageAcquiredFloralTypes.has(card.type) }
    }
}
