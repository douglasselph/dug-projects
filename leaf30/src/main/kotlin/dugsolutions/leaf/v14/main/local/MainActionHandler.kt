package dugsolutions.leaf.v14.main.local

import dugsolutions.leaf.v14.main.domain.ActionButton
import dugsolutions.leaf.v14.player.decisions.local.monitor.DecisionID
import dugsolutions.leaf.v14.player.decisions.local.monitor.DecisionMonitor

class MainActionHandler(
    private val decisionMonitor: DecisionMonitor
) {

    fun setActionActive(action: ActionButton?) {
        when (action) {
            ActionButton.RUN -> decisionMonitor.setWaitingFor(DecisionID.START_GAME)
            ActionButton.NEXT -> decisionMonitor.setWaitingFor(DecisionID.NEXT_STEP)
            else -> return
        }
    }

    fun clearAction() {
        decisionMonitor.setWaitingFor(DecisionID.NONE)
    }

}
