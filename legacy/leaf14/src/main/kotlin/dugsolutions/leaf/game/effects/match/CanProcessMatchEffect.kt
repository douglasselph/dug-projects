package dugsolutions.leaf.game.effects.match

import dugsolutions.leaf.cards.domain.MatchWith
import dugsolutions.leaf.common.domain.Token
import dugsolutions.leaf.player.Player

/**
 * Determines if a match effect can be processed based on the card's match conditions
 */
class CanProcessMatchEffect {

    operator fun invoke(player: Player, matchWith: MatchWith, using: Token): Boolean {
        return when (matchWith) {
            MatchWith.None -> false
            MatchWith.PulledGraft -> player.hasGraftedDice // TODO: Doesn't work
            MatchWith.WormOrSap -> {
                if (using == Token.Sap) {
                    player.hasSap
                } else if (player.hasBug(Token.Worm)) {
                    player.hasBug(using)
                } else false
            }
            MatchWith.Sap -> {
                player.hasSap && (using == Token.Sap)
            }
            MatchWith.Bee -> player.hasBug(Token.Bee) && (using == Token.Bee)
            MatchWith.End -> false
        }
    }

} 
