package dugsolutions.leaf.player.decisions.ui

import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.player.decisions.core.DecisionShouldProcessTrashEffect
import dugsolutions.leaf.player.decisions.local.monitor.DecisionID
import dugsolutions.leaf.player.decisions.local.monitor.DecisionMonitor
import dugsolutions.leaf.player.decisions.local.monitor.DecisionMonitorReport
import dugsolutions.leaf.player.decisions.local.monitor.DecisionSuspensionChannel

// TODO: Unit test
class DecisionShouldProcessTrashEffectSuspend(
    monitor: DecisionMonitor,
    report: DecisionMonitorReport
) : DecisionShouldProcessTrashEffect {

    private val channel = DecisionSuspensionChannel<DecisionShouldProcessTrashEffect.Result>(monitor, report)

    // region DecisionDrawCount

    override suspend fun invoke(card: GameCard): DecisionShouldProcessTrashEffect.Result {
        return channel.waitForDecision(DecisionID.SHOULD_PROCESS_TRASH_EFFECT(card))
    }

    override fun reset() {
    }

    // endregion DecisionDrawCount

    // region public

    fun provide(result: DecisionShouldProcessTrashEffect.Result) {
        channel.provideDecision(result)
    }

    // endregion public
}
