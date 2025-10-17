package dugsolutions.leaf.player.decisions.local

import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.core.DecisionShouldProcessTrashEffect

class ShouldAskTrashEffect {

    var askTrashOkay: Boolean = true

    suspend operator fun invoke(player: Player, card: GameCard) : DecisionShouldProcessTrashEffect.Result {
        if (!askTrashOkay) {
            return DecisionShouldProcessTrashEffect.Result.DO_NOT_TRASH
        }
        return player.decisionDirector.shouldProcessTrashEffect(card)
    }

}
