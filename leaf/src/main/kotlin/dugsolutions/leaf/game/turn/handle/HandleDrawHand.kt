package dugsolutions.leaf.game.turn.handle

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.components.DrawNewHand

// TODO: Unit test
class HandleDrawHand(
    private val drawNewHand: DrawNewHand,
    private val chronicle: GameChronicle
) {

    suspend operator fun invoke(player: Player) {
        val preferredCardCount = player.decisionDirector.drawCountDecision(player).count
        val result = drawNewHand(player, preferredCardCount)
        for (item in result) {
            when(item) {
                is DrawNewHand.ResultInstance.WasCard -> item.result.cardId?.let {
                    chronicle(Moment.DRAW_CARD(player, item.result.cardId, item.result.reshuffleDone))
                }
                is DrawNewHand.ResultInstance.WasDie -> item.result.die?.let {
                    chronicle(Moment.DRAW_DIE(player, item.result.die, item.result.reshuffleDone))
                }
            }
        }
    }

    operator fun invoke(player: Player, incomingCardCount: Int) {
        drawNewHand(player, incomingCardCount)
    }

}
