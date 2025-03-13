package dugsolutions.leaf.player.decisions

import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.game.purchase.domain.Credits
import dugsolutions.leaf.player.Player

interface DecisionAcquireCard {

    data class Result(val card: GameCard)
    operator fun invoke(player: Player, possibleCards: List<GameCard>, credits: Credits): Result?

}
