package dugsolutions.leaf.player.effect

import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.cards.domain.MatchWith
import dugsolutions.leaf.random.die.Die
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

    operator fun invoke(card: GameCard, player: Player): Result {
        return when (card.matchWith) {
            is MatchWith.None -> Result(possible = false)
            is MatchWith.OnRoll -> canProcessByRoll(card.matchWith.value, player, card.matchWith.discardDie)
            is MatchWith.OnFlourishType -> canProcessByFlourishType(card.matchWith.type, player)
            is MatchWith.Flower -> Result(possible = player.floralCards.isNotEmpty())
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
