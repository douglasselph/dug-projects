package dugsolutions.leaf.game.turn.handle

import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.components.DrawNewHand

// TODO: Unit test
class HandleDrawHand(
    private val drawNewHand: DrawNewHand,
    private val chronicle: GameChronicle
) {

    suspend operator fun invoke(player: Player) {
        val preferredCardCount = player.decisionDirector.drawCountDecision().count
        val result = drawNewHand(player, preferredCardCount)
        if (result.reshuffleNeeded) {
            val cultivationCards = player.cardsInDiscard().filter { it.type != FlourishType.RESOURCE }
            val cardToGraft = player.decisionDirector.graftCard(cultivationCards)
            cardToGraft?.let {
                player.removeCardFromDiscard(it.id)
                player.addCardToCreature(it.id)
            }
            player.resupply()
            drawNewHand(player, preferredCardCount - player.cardsInHand.size)
            // TODO: Add chronicle
        }
    }

    operator fun invoke(player: Player, incomingCardCount: Int) {
        drawNewHand(player, incomingCardCount)
    }

}
