package dugsolutions.leaf.player.effect

import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.MatchWith
import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.player.Player

/**
 * Determines if a match effect can be processed based on the card's match conditions
 */
class CanProcessMatchEffect(
    private val hasDieValue: HasDieValue,
    private val hasFlourishType: HasFlourishType
) {

    data class Result(
        val possible: Boolean,
        val dieCost: Die? = null
    )

    /**
     * Determine if a match effect should be processed based on the card's match conditions
     */
    operator fun invoke(card: GameCard, player: Player): Result {
        return when (card.matchWith) {
            is MatchWith.None -> Result(false)
            is MatchWith.OnRoll -> canProcessByRoll(card.matchWith.value, player, card.matchWith.discardDie)
            is MatchWith.OnFlourishType -> canProcessByFlourishType(card.matchWith.type, player)
            is MatchWith.Flower -> Result(player.flowerCount(card) > 0)
        }
    }

    private fun canProcessByRoll(value: Int, player: Player, discardDie: Boolean): Result {
        val handItems = player.getItemsInHand()
        val useDie = hasDieValue(handItems, value)
        if (discardDie) {
            return Result(useDie != null, dieCost = useDie)
        }
        return Result(useDie != null, dieCost = null)
    }

    private fun canProcessByFlourishType(type: FlourishType, player: Player): Result {
        val handItems = player.getItemsInHand()
        return Result(hasFlourishType(handItems, type))
    }
} 
