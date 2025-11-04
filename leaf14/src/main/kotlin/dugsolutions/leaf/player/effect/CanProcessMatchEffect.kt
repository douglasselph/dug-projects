package dugsolutions.leaf.player.effect

import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.cards.domain.MatchWith
import dugsolutions.leaf.common.domain.Token
import dugsolutions.leaf.player.Player

/**
 * Determines if a match effect can be processed based on the card's match conditions
 */
class CanProcessMatchEffect {

    operator fun invoke(card: GameCard, player: Player): Boolean {
        return when (card.matchWith) {
            MatchWith.None -> true
            MatchWith.PulledGraft -> player.hasGraftedDice
            MatchWith.WormOrSap -> player.hasSap || player.hasInsect(Token.Worm)
            MatchWith.Sap -> player.hasSap
            MatchWith.Bee -> player.hasInsect(Token.Bee)
            MatchWith.End -> true
        }
    }

} 
