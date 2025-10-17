package dugsolutions.leaf.game.turn.effect

import dugsolutions.leaf.cards.domain.CardEffect
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.player.Player

class EffectReplayVine(
    private val chronicle: GameChronicle
) {

    operator fun invoke(player: Player) {
        val possibleCards = player.cardsInHand.filter {
            it.type == FlourishType.VINE &&
                    it.primaryEffect != CardEffect.REPLAY_VINE &&
                    it.matchEffect != CardEffect.REPLAY_VINE
        }
        if (possibleCards.isNotEmpty()) {
            val selectedCard = possibleCards[0]
            player.cardsToPlay.add(selectedCard)
            chronicle(Moment.REPLAY_VINE(player, selectedCard))
        }
    }

}
