package dugsolutions.leaf.player.decisions.ui

import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.player.decisions.core.DecisionShouldProcessTrashEffect

// TODO: Unit test
class DecisionShouldProcessTrashEffectSuspend : DecisionShouldProcessTrashEffect {

    private val channel = DecisionSuspensionChannel<Boolean>()
    var askTrashOkay: Boolean = true

    // region DecisionDrawCount

    override suspend fun invoke(card: GameCard): DecisionShouldProcessTrashEffect.Result {
        if (!askTrashOkay) {
            return DecisionShouldProcessTrashEffect.Result.DO_NOT_TRASH
        }
        onShouldProcessTrashEffect(card)
        if (channel.waitForDecision()) {
            return DecisionShouldProcessTrashEffect.Result.TRASH
        }
        return DecisionShouldProcessTrashEffect.Result.DO_NOT_TRASH
    }

    override fun reset() {
    }

    // endregion DecisionDrawCount

    // region public

    var onShouldProcessTrashEffect: (card: GameCard) -> Unit = {}

    fun provide(decision: Boolean) {
        channel.provideDecision(decision)
    }

    // endregion public
}
