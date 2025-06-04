package dugsolutions.leaf.player.decisions.local

import dugsolutions.leaf.components.CostElement
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.game.acquire.domain.Combination
import dugsolutions.leaf.game.acquire.domain.totalValue
import dugsolutions.leaf.player.domain.AppliedEffect
import dugsolutions.leaf.player.effect.EffectsList
import kotlin.math.max

class EvaluateCardPurchases {

    operator fun invoke(
        marketCards: List<GameCard>,
        playerHasFlourishTypes: List<FlourishType>,
        combination: Combination,
        effectsList: EffectsList
    ): List<GameCard> {
        return marketCards.filter { card ->
            canPurchaseCard(card, playerHasFlourishTypes, combination, effectsList)
        }
    }

    private fun canPurchaseCard(
        card: GameCard,
        playerHasFlourishTypes: List<FlourishType>,
        combination: Combination,
        effectsList: EffectsList
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
                    totalValue >= reduceFor(element.minimum, card.type, effectsList)
                }
            }
        }
    }

    private fun reduceFor(amount: Int, flourishType: FlourishType, effectsList: EffectsList): Int {
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
