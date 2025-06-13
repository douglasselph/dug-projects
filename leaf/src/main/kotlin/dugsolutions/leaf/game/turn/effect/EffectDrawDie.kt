package dugsolutions.leaf.game.turn.effect

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.player.Player

class EffectDrawDie(
    private val chronicle: GameChronicle
) {

    data class DrawDieParams(
        val fromCompost: Boolean = false,
        val drawHighest: Boolean = false,
        val thenDiscard: Boolean = false
    )

    operator fun invoke(player: Player, params: DrawDieParams) = with(params) {
        val die = if (fromCompost) {
            if (drawHighest) {
                player.drawBestDieFromBed()
            } else {
                player.drawDieFromBed()
            }
        } else {
            if (drawHighest) {
                player.drawBestDie()
            } else {
                player.drawDie()
            }
        }
        die?.let {
            chronicle(Moment.DRAW_DIE(player, die))
        }
    }

}
