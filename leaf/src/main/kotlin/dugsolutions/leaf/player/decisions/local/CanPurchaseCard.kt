package dugsolutions.leaf.player.decisions.local

import dugsolutions.leaf.cards.cost.CostElement
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.game.acquire.domain.Combination
import dugsolutions.leaf.game.acquire.domain.totalValue
import dugsolutions.leaf.player.domain.AppliedEffect
import kotlin.math.max

class CanPurchaseCard {

    operator fun invoke(
        card: GameCard,
        playerHasFlourishTypes: List<FlourishType>,
        combination: Combination,
        effectsList: List<AppliedEffect>
    ): Boolean {
        // If no cost elements, the card is free
        if (card.cost.alternatives.isEmpty()) {
            return true
        }
        for (alternative in card.cost.alternatives) {
            if (canBuy(card.type, alternative.elements, playerHasFlourishTypes, combination, effectsList)) {
                return true
            }
        }
        return false
    }

    private fun canBuy(
        cardType: FlourishType,
        elements: List<CostElement>,
        playerHasFlourishTypes: List<FlourishType>,
        combination: Combination,
        effectsList: List<AppliedEffect>
    ): Boolean {
        // If no cost elements, the card is free
        if (elements.isEmpty()) {
            return true
        }

        // Check all cost elements
        return elements.all { element ->
            when (element) {
                // Check if player has the required flourish type
                is CostElement.FlourishTypePresent -> {
                    playerHasFlourishTypes.contains(element.flourishType)
                }

                // Check if any die has the exact value required
                is CostElement.SingleDieExact -> {
                    combination.values.dice.any { die -> die.value == element.exact }
                }

                // Check if any die has at least the minimum value required
                is CostElement.SingleDieMinimum -> {
                    combination.values.dice.any { die -> die.value >= element.minimum }
                }

                // Check if the sum of all dice is exactly the required value
                is CostElement.TotalDiceExact -> {
                    val totalValue = combination.totalValue
                    totalValue == element.exact
                }

                // Check if the sum of all dice is at least the minimum required
                is CostElement.TotalDiceMinimum -> {
                    val totalValue = combination.totalValue
                    totalValue >= reduceFor(element.minimum, cardType, effectsList)
                }
            }
        }
    }

    private fun reduceFor(amount: Int, flourishType: FlourishType, effectsList: List<AppliedEffect>): Int {
        val reduce = effectsList.filterIsInstance<AppliedEffect.MarketBenefit>().sumOf { matchForValue(flourishType, it) }
        return max(0, amount - reduce)
    }

    private fun matchForValue(type: FlourishType, effect: AppliedEffect.MarketBenefit): Int {
        if (type == effect.type) {
            return effect.costReduction
        }
        return 0
    }
}
