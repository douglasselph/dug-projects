package dugsolutions.leaf.player.decisions.ui

import dugsolutions.leaf.player.decisions.core.DecisionFlowerSelect
import dugsolutions.leaf.player.decisions.local.monitor.DecisionID
import dugsolutions.leaf.player.decisions.local.monitor.DecisionMonitor
import dugsolutions.leaf.player.decisions.local.monitor.DecisionMonitorReport
import dugsolutions.leaf.player.decisions.local.monitor.DecisionTaskQueue

class DecisionFlowerSelectSuspend(
    monitor: DecisionMonitor,
    report: DecisionMonitorReport
) : DecisionFlowerSelect {

    private val taskQueue = DecisionTaskQueue<DecisionFlowerSelect.Result>(monitor, report)

    // region DecisionFlowerSelect

    override suspend fun invoke(): DecisionFlowerSelect.Result {
        return taskQueue.waitForDecision(DecisionID.FLOWER_SELECT)
    }

    // endregion DecisionFlowerSelect

    // region public

    fun provide(result: DecisionFlowerSelect.Result) {
        taskQueue.provideDecision(result)
    }

    // endregion public
}
