package dugsolutions.leaf.player.decisions

import dugsolutions.leaf.common.Commons
import dugsolutions.leaf.player.Player
import kotlin.math.max

/**
 * Return number of cards to draw, the remaining hand size will draw dice.
 * When supply is low (less than 4 total items), considers compost pile as available resources.
 */
class DecisionDrawCountCoreStrategy(
    private val player: Player
) : DecisionDrawCount {

    override operator fun invoke(): Int {
        val handSize = Commons.HAND_SIZE
        val cardSupplyCount = player.cardsInSupplyCount
        val diceSupplyCount = player.diceInSupplyCount
        val totalAvailable = cardSupplyCount + diceSupplyCount

        // If supply is low, include compost pile in calculations
        val (effectiveCardCount, effectiveDiceCount) = if (totalAvailable < handSize) {
            Pair(
                cardSupplyCount + player.cardsInCompostCount,
                diceSupplyCount + player.diceInCompostCount
            )
        } else {
            Pair(cardSupplyCount, diceSupplyCount)
        }

        val preferredCardCount = when {
            // No dice
            effectiveDiceCount == 0 -> handSize
            // No cards
            effectiveCardCount == 0 -> 0
            // Even distribution
            effectiveCardCount == effectiveDiceCount -> handSize / 2
            // More cards than dice
            effectiveCardCount > effectiveDiceCount -> handSize - 1
            // More dice than cards
            else -> 1
        }
        return max(
            0,
            preferredCardCount - player.cardsInHand.size
        )
    }
} 
