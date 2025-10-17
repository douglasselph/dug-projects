package dugsolutions.leaf.player.decisions

import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.CardEffect
import dugsolutions.leaf.game.purchase.domain.Credits
import dugsolutions.leaf.game.turn.local.CardIsFree
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.DecisionAcquireCard.Result

// TODO: Unit test
class DecisionAcquireCardBaseStrategy(
    private val cardIsFree: CardIsFree
) : DecisionAcquireCard {

    override operator fun invoke(player: Player, possibleCards: List<GameCard>, credits: Credits): Result? {
        // Filter only cards with AddToTotal effect
        val baselineCards = possibleCards.filter { card -> 
            card.primaryEffect == CardEffect.ADD_TO_TOTAL 
        }
        
        if (baselineCards.isEmpty()) return null
        
        // Get the most expensive AddToTotal card the player can afford
        val selectedCard = baselineCards.maxByOrNull { card -> cardIsFree(card, player) }
        return selectedCard?.let { Result(it) }
    }

}
