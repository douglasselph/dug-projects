package dugsolutions.leaf.player.decisions.ui

import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.core.DecisionDamageAbsorption
import dugsolutions.leaf.player.decisions.local.monitor.DecisionID
import dugsolutions.leaf.player.decisions.local.monitor.DecisionMonitor
import dugsolutions.leaf.player.decisions.local.monitor.DecisionMonitorReport
import dugsolutions.leaf.player.decisions.local.monitor.DecisionTaskQueue

class DecisionDamageAbsorptionSuspend(
    private val player: Player,
    monitor: DecisionMonitor,
    report: DecisionMonitorReport
) : DecisionDamageAbsorption {

    private val taskQueue = DecisionTaskQueue<DecisionDamageAbsorption.Result>(monitor, report)

    // region DecisionDamageAbsorption

    override suspend fun invoke(): DecisionDamageAbsorption.Result {
        return taskQueue.waitForDecision(DecisionID.DAMAGE_ABSORPTION(player.incomingDamage))
    }

    // endregion DecisionDamageAbsorption

    // region public

    fun provide(result: DecisionDamageAbsorption.Result) {
        taskQueue.provideDecision(result)
    }

    // endregion public
}
