package dugsolutions.leaf.player.effect

import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.MatchWith
import dugsolutions.leaf.player.Player

/**
 * Determines if a match effect should be processed based on the card's match conditions
 * and the current game state.
 */
class ShouldProcessMatchEffect(
    private val hasDieValue: HasDieValue,
    private val hasFlourishType: HasFlourishType
) {
    /**
     * Determine if a match effect should be processed based on the card's match conditions
     */
    operator fun invoke(card: GameCard, player: Player): Boolean {
        return when (card.matchWith) {
            is MatchWith.None -> false
            is MatchWith.OnRoll -> shouldProcessOnRoll(card.matchWith.value, player)
            is MatchWith.OnFlourishType -> shouldProcessOnFlourishType(card.matchWith.type, player)
        }
    }

    private fun shouldProcessOnRoll(value: Int, player: Player): Boolean {
        val handItems = player.getItemsInHand()
        return hasDieValue(handItems, value)
    }

    private fun shouldProcessOnFlourishType(type: FlourishType, player: Player): Boolean {
        val handItems = player.getItemsInHand()
        return hasFlourishType(handItems, type)
    }
} 