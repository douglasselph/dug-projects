package dugsolutions.leaf.player.effect

import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.player.Player

class CardsEffectsProcessor(
    private val cardEffectsProcessor: CardEffectsProcessor
) {

    suspend operator fun invoke(cards: List<GameCard>, player: Player) {
        cards.forEach { card -> cardEffectsProcessor(card, player) }
    }

}
