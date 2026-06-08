package dugsolutions.leaf.v14.game.turn.effect

import dugsolutions.leaf.v14.chronicle.GameChronicle
import dugsolutions.leaf.v14.chronicle.domain.Moment
import dugsolutions.leaf.v14.game.turn.select.SelectCardToRetain
import dugsolutions.leaf.v14.player.Player
import dugsolutions.leaf.v14.player.domain.HandItem

class EffectReuseCard(
    private val selectCardToRetain: SelectCardToRetain,
    private val chronicle: GameChronicle
) {

    operator fun invoke(player: Player) {
        selectCardToRetain(player.cardsInHand)?.let { card ->
            player.reused.add(HandItem.aCard(card))
            chronicle(Moment.REUSE_CARD(player, card))
        }
    }
}
