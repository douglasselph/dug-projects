package dugsolutions.leaf.v14.game.turn.effect

import dugsolutions.leaf.v14.chronicle.GameChronicle
import dugsolutions.leaf.v14.chronicle.domain.Moment
import dugsolutions.leaf.v14.player.Player
import dugsolutions.leaf.v14.player.domain.HandItem

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
