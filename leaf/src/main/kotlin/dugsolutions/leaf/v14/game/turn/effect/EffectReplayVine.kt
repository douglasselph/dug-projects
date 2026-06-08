package dugsolutions.leaf.v14.game.turn.effect

import dugsolutions.leaf.v14.cards.domain.CardEffect
import dugsolutions.leaf.v14.cards.domain.FlourishType
import dugsolutions.leaf.v14.chronicle.GameChronicle
import dugsolutions.leaf.v14.chronicle.domain.Moment
import dugsolutions.leaf.v14.player.Player

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
