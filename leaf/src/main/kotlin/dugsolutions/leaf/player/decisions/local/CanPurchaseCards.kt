package dugsolutions.leaf.player.decisions.local

import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.game.acquire.domain.Combination
import dugsolutions.leaf.player.domain.AppliedEffect

class CanPurchaseCards(
    private val canPurchaseCard: CanPurchaseCard
) {

    operator fun invoke(
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
