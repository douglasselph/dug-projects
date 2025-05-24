package dugsolutions.leaf.game.acquire.evaluator

import dugsolutions.leaf.components.CostElement
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.game.acquire.domain.Combination
import dugsolutions.leaf.game.acquire.domain.totalValue

class EvaluateCardPurchases {

    operator fun invoke(
        marketCards: List<GameCard>,
        playerHasFlourishTypes: List<FlourishType>,
        combination: Combination
    ): List<GameCard> {
        return marketCards.filter { card ->
            canPurchaseCard(card, playerHasFlourishTypes, combination)
        }
    }

    private fun canPurchaseCard(
        card: GameCard,
        playerHasFlourishTypes: List<FlourishType>,
        combination: Combination
    ): Boolean {
        // If no cost elements, the card is free
        if (card.cost.elements.isEmpty()) {
            return true
        }

        // Check all cost elements
        return card.cost.elements.all { element ->
            when (element) {
                // Check if player has the required flourish type
                is CostElement.FlourishTypePresent -> {
                    playerHasFlourishTypes.contains(element.flourishType)
                }

                // Check if any die has the exact value required
                is CostElement.SingleDieExact -> {
                    combination.values.dice.any { it.value == element.exact }
                }

                // Check if any die has at least the minimum value required
                is CostElement.SingleDieMinimum -> {
                    combination.values.dice.any { it.value >= element.minimum }
                }

                // Check if the sum of all dice is exactly the required value
                is CostElement.TotalDiceExact -> {
                    val totalValue = combination.totalValue
                    totalValue == element.exact
                }

                // Check if the sum of all dice is at least the minimum required
                is CostElement.TotalDiceMinimum -> {
                    val totalValue = combination.totalValue
                    totalValue >= element.minimum
                }
            }
        }
    }
}
