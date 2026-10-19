package dugsolutions.leaf.v14.game.turn.effect

import dugsolutions.leaf.v14.chronicle.GameChronicle
import dugsolutions.leaf.v14.chronicle.domain.Moment
import dugsolutions.leaf.v14.player.Player

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
