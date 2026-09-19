package dugsolutions.leaf.v14.player.decisions.local

import dugsolutions.leaf.v14.cards.domain.FlourishType
import dugsolutions.leaf.v14.cards.domain.GameCard
import dugsolutions.leaf.v14.game.acquire.domain.Combination
import dugsolutions.leaf.v14.player.domain.AppliedEffect

class CanPurchaseCards(
    private val canPurchaseCard: CanPurchaseCard
) {

    operator fun  invoke(
        marketCards: List<GameCard>,
        playerHasFlourishTypes: List<FlourishType>,
        combination: Combination,
        effectsList: List<AppliedEffect>
    ): List<GameCard> {
        return marketCards.filter { card ->
            canPurchaseCard(card, playerHasFlourishTypes, combination, effectsList)
        }
    }

}
