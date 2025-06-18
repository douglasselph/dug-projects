package dugsolutions.leaf.player.decisions.ui

import dugsolutions.leaf.player.decisions.core.DecisionFlowerSelect
import dugsolutions.leaf.player.decisions.local.monitor.DecisionID
import dugsolutions.leaf.player.decisions.local.monitor.DecisionMonitor
import dugsolutions.leaf.player.decisions.local.monitor.DecisionMonitorReport
import dugsolutions.leaf.player.decisions.local.monitor.DecisionSuspensionChannel

class DecisionFlowerSelectSuspend(
    monitor: DecisionMonitor,
    report: DecisionMonitorReport
) : DecisionFlowerSelect {

    private val channel = DecisionSuspensionChannel<DecisionFlowerSelect.Result>(monitor, report)

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
