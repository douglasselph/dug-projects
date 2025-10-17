package dugsolutions.leaf.player.decisions.baseline

import dugsolutions.leaf.common.Commons
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.core.DecisionDrawCount
import kotlin.math.max

/**
 * Return number of cards to draw, the remaining hand size will draw dice.
 * When supply is low (less than 4 total items), considers compost pile as available resources.
 */
class DecisionDrawCountBaseline : DecisionDrawCount {

    override suspend operator fun invoke(player: Player): DecisionDrawCount.Result {
        val handSize = Commons.HAND_SIZE
        val cardSupplyCount = player.cardsInSupplyCount
        val diceSupplyCount = player.diceInSupplyCount
        val totalAvailable = cardSupplyCount + diceSupplyCount

        // If supply is low, include compost pile in calculations
        val (effectiveCardCount, effectiveDiceCount) = if (totalAvailable < handSize) {
            Pair(
                cardSupplyCount + player.cardsInDiscardCount,
                diceSupplyCount + player.diceInDiscardCount
            )
        } else {
            Pair(cardSupplyCount, diceSupplyCount)
        }
        val preferredCardCount = when {
            // No dice
            effectiveDiceCount == 0 -> handSize
            // No cards
            effectiveCardCount <= 1 -> 1
            // Even distribution
            effectiveCardCount == effectiveDiceCount -> handSize / 2
            // More cards than dice
            effectiveCardCount > effectiveDiceCount -> handSize - 1
            // More dice than cards
            else -> 1
        }
        return DecisionDrawCount.Result(
            max(
                1,
                preferredCardCount - player.cardsInHand.size
            )
        )
    }
} 
