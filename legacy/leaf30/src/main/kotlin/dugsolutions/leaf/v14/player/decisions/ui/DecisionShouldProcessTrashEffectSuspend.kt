package dugsolutions.leaf.v14.player.decisions.ui

import dugsolutions.leaf.v14.cards.domain.GameCard
import dugsolutions.leaf.v14.player.decisions.core.DecisionShouldProcessTrashEffect
import dugsolutions.leaf.v14.player.decisions.local.monitor.DecisionID
import dugsolutions.leaf.v14.player.decisions.local.monitor.DecisionMonitor
import dugsolutions.leaf.v14.player.decisions.local.monitor.DecisionMonitorReport
import dugsolutions.leaf.v14.player.decisions.local.monitor.DecisionTaskQueue

class DecisionShouldProcessTrashEffectSuspend(
    monitor: DecisionMonitor,
    report: DecisionMonitorReport
) : DecisionShouldProcessTrashEffect {

    private val taskQueue = DecisionTaskQueue<DecisionShouldProcessTrashEffect.Result>(monitor, report)

    // region DecisionDrawCount

    override suspend fun invoke(card: GameCard): DecisionShouldProcessTrashEffect.Result {
        return taskQueue.waitForDecision(DecisionID.SHOULD_PROCESS_TRASH_EFFECT(card))
    }

    override fun reset() {
    }

    // endregion DecisionDrawCount

    // region public

    fun provide(result: DecisionShouldProcessTrashEffect.Result) {
        taskQueue.provideDecision(result)
    }

    // endregion public
}
