package dugsolutions.leaf.player.effect

import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.player.Player

class CardsEffectsProcessor(
    private val cardEffectsProcessor: CardEffectsProcessor
) {

    suspend operator fun invoke(cards: List<GameCard>, player: Player) {
        cards.forEach { card -> cardEffectsProcessor(card, player) }
    }

}
