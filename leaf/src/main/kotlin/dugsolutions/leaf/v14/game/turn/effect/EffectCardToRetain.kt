package dugsolutions.leaf.v14.game.turn.effect

import dugsolutions.leaf.v14.chronicle.GameChronicle
import dugsolutions.leaf.v14.chronicle.domain.Moment
import dugsolutions.leaf.v14.game.turn.select.SelectCardToRetain
import dugsolutions.leaf.v14.player.Player

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
