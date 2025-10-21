package dugsolutions.leaf.game.acquire.evaluator

import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.common.domain.acquire.ChoiceCard
import dugsolutions.leaf.game.acquire.domain.Combinations

class PossibleCards(
    private val canPurchaseCards: CanPurchaseCards
) {

    operator fun invoke(
        combinations: Combinations,
        marketCards: List<GameCard>
    ): List<ChoiceCard> {
        val choices = mutableListOf<ChoiceCard>()

        for (usingDice in combinations.choices) {
            val possibleCards = canPurchaseCards(marketCards, usingDice)
            for (card in possibleCards) {
                choices.add(ChoiceCard(card, usingDice))
            }
        }
        return choices
    }

}
