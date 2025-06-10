package dugsolutions.leaf.game.turn.effect

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.game.turn.select.SelectCardToRetain
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.domain.HandItem

class EffectReuseDie(
    private val chronicle: GameChronicle
) {

    operator fun invoke(player: Player, rerollOkay: Boolean) {
        if (rerollOkay) {
            player.diceInHand.dice.maxByOrNull { it.sides }?.let { die ->
                die.roll()
                player.reused.add(HandItem.aDie(die))
                chronicle(Moment.REUSE_DIE(player, die))
            }
        } else {
            player.diceInHand.dice.maxByOrNull { it.value }?.let { die ->
                player.reused.add(HandItem.aDie(die))
                chronicle(Moment.REUSE_DIE(player, die))
            }
        }
    }
}
