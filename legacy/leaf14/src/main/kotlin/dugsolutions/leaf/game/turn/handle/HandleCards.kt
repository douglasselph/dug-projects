package dugsolutions.leaf.game.turn.handle

import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.game.effects.HandleCardEffect
import dugsolutions.leaf.player.Player

class HandleCards(
    private val handleCardEffect: HandleCardEffect
) {

    suspend operator fun invoke(player: Player, cards: List<GameCard>) {
        cards.forEach { card -> handleCardEffect(player, card) }
    }

}
