package dugsolutions.leaf.player.decisions.ui

import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.core.DecisionDrawCount
import dugsolutions.leaf.player.decisions.local.monitor.DecisionID
import dugsolutions.leaf.player.decisions.local.monitor.DecisionMonitor
import dugsolutions.leaf.player.decisions.local.monitor.DecisionMonitorReport
import dugsolutions.leaf.player.decisions.local.monitor.DecisionSuspensionChannel

class DecisionDrawCountSuspend(
    monitor: DecisionMonitor,
    report: DecisionMonitorReport
) : DecisionDrawCount {
    private val channel = DecisionSuspensionChannel<DecisionDrawCount.Result>(monitor, report)

    override suspend fun invoke(player: Player): DecisionDrawCount.Result {
        return channel.waitForDecision(DecisionID.DRAW_COUNT(player))
    }

    fun provide(result: DecisionDrawCount.Result) {
        channel.provideDecision(result)
    }
}
