package dugsolutions.leaf.game.turn.select

import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.game.acquire.ManageAcquiredFloralTypes
import dugsolutions.leaf.grove.Grove

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
