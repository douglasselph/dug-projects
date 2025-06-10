package dugsolutions.leaf.game.turn.effect

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.player.Player

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
