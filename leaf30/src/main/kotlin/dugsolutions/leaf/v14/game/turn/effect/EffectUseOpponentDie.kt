package dugsolutions.leaf.v14.game.turn.effect

import dugsolutions.leaf.v14.chronicle.GameChronicle
import dugsolutions.leaf.v14.chronicle.domain.Moment
import dugsolutions.leaf.v14.player.Player

class EffectUseOpponentDie(
    private val chronicle: GameChronicle
) {

    operator fun invoke(player: Player, target: Player) {
        target.diceInHand.dice.maxByOrNull { it.value }?.let { die ->
            player.pipModifier += die.value
            chronicle(Moment.USE_OPPONENT_DIE(player, die))
        }
    }
}
