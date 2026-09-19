package dugsolutions.leaf.v14.game.turn.handle

import dugsolutions.leaf.v14.chronicle.GameChronicle
import dugsolutions.leaf.v14.chronicle.domain.Moment
import dugsolutions.leaf.v14.player.Player
import dugsolutions.leaf.v14.player.components.DrawNewHand

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
