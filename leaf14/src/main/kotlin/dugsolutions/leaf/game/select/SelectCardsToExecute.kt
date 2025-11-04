package dugsolutions.leaf.game.select

import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.player.Player

class SelectCardsToExecute {

    suspend operator fun invoke(player: Player): List<GameCard> {
        return emptyList()
    }

}
