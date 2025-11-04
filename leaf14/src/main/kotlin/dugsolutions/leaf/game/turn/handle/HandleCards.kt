package dugsolutions.leaf.game.turn.handle

import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.player.Player

class HandleCards(
    private val handleCardActivation: HandleCardActivation
) {

    suspend operator fun invoke(player: Player, cards: List<GameCard>) {
        cards.forEach { card -> handleCardActivation(player, card) }
    }

}
