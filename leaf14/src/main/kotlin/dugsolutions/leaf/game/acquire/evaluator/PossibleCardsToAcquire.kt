package dugsolutions.leaf.game.acquire.evaluator

import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.common.domain.acquire.ChoiceCard
import dugsolutions.leaf.random.die.DieValues

class PossibleCardsToAcquire(
    private val canPurchaseCards: CanPurchaseCards
) {

    operator fun invoke(
        combinations: List<DieValues>,
        marketCards: List<GameCard>
    ): List<ChoiceCard> {
        val choices = mutableListOf<ChoiceCard>()

        for (combination in combinations) {
            val possibleCards = canPurchaseCards(marketCards, combination)
            for (card in possibleCards) {
                choices.add(ChoiceCard(card, combination))
            }
        }
        return choices
    }

}
