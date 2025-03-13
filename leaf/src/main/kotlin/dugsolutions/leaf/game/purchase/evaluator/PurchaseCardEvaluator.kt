package dugsolutions.leaf.game.purchase.evaluator

import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.getFlourishTypes
import dugsolutions.leaf.game.purchase.domain.Combination
import dugsolutions.leaf.game.purchase.domain.Combinations
import dugsolutions.leaf.player.Player

class PurchaseCardEvaluator(
    private val evaluateCardPurchases: EvaluateCardPurchases
) {

    data class BestChoice(
        val card: GameCard,
        val combination: Combination
    )

    operator fun invoke(
        player: Player,
        combinations: Combinations,
        marketCards: List<GameCard>
    ): BestChoice? {
        val flourishTypesHeld = player.cardsInHand.getFlourishTypes()
        val bestChoices = mutableListOf<BestChoice>()
        
        for (combination in combinations) {
            val possibleCards = evaluateCardPurchases(marketCards, flourishTypesHeld, combination)
            if (possibleCards.isNotEmpty()) {
                val bestCardOf = player.decisionDirector.bestCardPurchase(possibleCards)
                bestChoices.add(
                    BestChoice(
                        bestCardOf,
                        combination
                    )
               )
            }
        }
        if (bestChoices.isEmpty()) {
            return null
        }
        val bestCards = bestChoices.map { it.card }
        val bestCardOf = player.decisionDirector.bestCardPurchase(bestCards)
        val winningChoice = bestChoices.firstOrNull { it.card == bestCardOf } ?: return null
        return BestChoice(
            bestCardOf,
            winningChoice.combination
        )
    }
}
