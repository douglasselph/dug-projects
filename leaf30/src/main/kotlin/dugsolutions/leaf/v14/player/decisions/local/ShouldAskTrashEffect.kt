package dugsolutions.leaf.v14.player.decisions.local

import dugsolutions.leaf.v14.cards.domain.GameCard
import dugsolutions.leaf.v14.player.Player
import dugsolutions.leaf.v14.player.decisions.core.DecisionShouldProcessTrashEffect

class ShouldAskTrashEffect {

    var askTrashOkay: Boolean = true

    suspend operator fun invoke(player: Player, card: GameCard) : DecisionShouldProcessTrashEffect.Result {
        if (!askTrashOkay) {
            return DecisionShouldProcessTrashEffect.Result.DO_NOT_TRASH
        }
        return player.decisionDirector.shouldProcessTrashEffect(card)
    }

}
