package dugsolutions.leaf.player.decisions.ui

import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.player.decisions.core.DecisionFlowerSelect

class DecisionFlowerSelectSuspend : DecisionFlowerSelect {

    private val channel = DecisionSuspensionChannel<List<GameCard>>()

    // region DecisionFlowerSelect

    override suspend fun invoke(): List<GameCard> {
        onFlowerSelect()
        return channel.waitForDecision()
    }

    // endregion DecisionFlowerSelect

    // region public

    var onFlowerSelect: () -> Unit = {}

    fun provide(result: List<GameCard>) {
        channel.provideDecision(result)
    }

    // endregion public
}
