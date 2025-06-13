package dugsolutions.leaf.player.decisions.ui

import dugsolutions.leaf.player.decisions.core.DecisionFlowerSelect
import dugsolutions.leaf.player.decisions.ui.support.DecisionID
import dugsolutions.leaf.player.decisions.ui.support.DecisionMonitor
import dugsolutions.leaf.player.decisions.ui.support.DecisionSuspensionChannel

class DecisionFlowerSelectSuspend(
    monitor: DecisionMonitor
) : DecisionFlowerSelect {

    private val channel = DecisionSuspensionChannel<DecisionFlowerSelect.Result>(monitor)

    // region DecisionFlowerSelect

    override suspend fun invoke(): DecisionFlowerSelect.Result {
        onFlowerSelect()
        return channel.waitForDecision(DecisionID.FLOWER_SELECT)
    }

    // endregion DecisionFlowerSelect

    // region public

    var onFlowerSelect: () -> Unit = {}

    fun provide(result: DecisionFlowerSelect.Result) {
        channel.provideDecision(result)
    }

    // endregion public
}
