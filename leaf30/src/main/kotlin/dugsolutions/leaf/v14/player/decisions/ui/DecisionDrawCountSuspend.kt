package dugsolutions.leaf.v14.player.decisions.ui

import dugsolutions.leaf.v14.player.Player
import dugsolutions.leaf.v14.player.decisions.core.DecisionDrawCount
import dugsolutions.leaf.v14.player.decisions.local.monitor.DecisionID
import dugsolutions.leaf.v14.player.decisions.local.monitor.DecisionMonitor
import dugsolutions.leaf.v14.player.decisions.local.monitor.DecisionMonitorReport
import dugsolutions.leaf.v14.player.decisions.local.monitor.DecisionTaskQueue

class DecisionDrawCountSuspend(
    monitor: DecisionMonitor,
    report: DecisionMonitorReport
) : DecisionDrawCount {

    private val taskQueue = DecisionTaskQueue<DecisionDrawCount.Result>(monitor, report)

    override suspend fun invoke(player: Player): DecisionDrawCount.Result {
        return taskQueue.waitForDecision(DecisionID.DRAW_COUNT(player))
    }

    fun provide(result: DecisionDrawCount.Result) {
        taskQueue.provideDecision(result)
    }
}
