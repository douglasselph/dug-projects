package dugsolutions.leaf.game.turn.effect

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.game.turn.select.SelectCardToRetain
import dugsolutions.leaf.player.Player

class EffectCardToRetain(
    private val selectCardToRetain: SelectCardToRetain,
    private val chronicle: GameChronicle
) {

    operator fun invoke(player: Player) {
        selectCardToRetain(player.cardsToPlay, null)?.let { card ->
            player.retainCard(card)
            player.cardsToPlay.remove(card)
            chronicle(Moment.RETAIN_CARD(player, card))
        }
    }

}
