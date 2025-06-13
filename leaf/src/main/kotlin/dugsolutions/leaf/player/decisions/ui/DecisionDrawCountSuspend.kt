package dugsolutions.leaf.player.decisions.ui

import dugsolutions.leaf.player.decisions.core.DecisionDrawCount
import dugsolutions.leaf.player.decisions.ui.support.DecisionID
import dugsolutions.leaf.player.decisions.ui.support.DecisionMonitor
import dugsolutions.leaf.player.decisions.ui.support.DecisionSuspensionChannel

class DecisionDrawCountSuspend(
    monitor: DecisionMonitor
) : DecisionDrawCount {
    private val channel = DecisionSuspensionChannel<DecisionDrawCount.Result>(monitor)

    override suspend fun invoke(): DecisionDrawCount.Result {
        return channel.waitForDecision(DecisionID.DRAW_COUNT)
    }

    fun provide(result: DecisionDrawCount.Result) {
        channel.provideDecision(result)
    }
}
