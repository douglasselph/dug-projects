package dugsolutions.leaf.game.turn.effect

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.player.Player

class EffectDrawDie(
    private val chronicle: GameChronicle
) {

    data class DrawDieParams(
        val fromDiscard: Boolean = false,
        val drawHighest: Boolean = false,
        val thenDiscard: Boolean = false
    )

    operator fun invoke(player: Player, params: DrawDieParams) = with(params) {
        val result = if (fromDiscard) {
            if (drawHighest) {
                player.drawBestDieFromDiscard()
            } else {
                player.drawDieFromDiscard()
            }
        } else {
            if (drawHighest) {
                player.drawBestDie()
            } else {
                player.drawDie()
            }
        }
        result.die?.let { die ->
            chronicle(Moment.DRAW_DIE(player, die, result.reshuffleDone))
        }
    }

}
