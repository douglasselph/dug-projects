package dugsolutions.leaf.game.turn.effect

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.game.turn.select.SelectCardToRetain
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.domain.HandItem

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
