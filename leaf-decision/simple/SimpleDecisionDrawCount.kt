package dugsolutions.leaf.player.decisions.simple

import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.core.DecisionDrawCount

/**
 * Simplified version of DecisionDrawCountSuspend that uses SimpleDecisionHandler.
 * 
 * This eliminates the complex DecisionTaskQueue and DecisionMonitor dependencies.
 * The decision is made directly through the handler, making the flow much clearer.
 */
class SimpleDecisionDrawCount : DecisionDrawCount {
    
    private val handler = SimpleDecisionHandler<DecisionDrawCount.Result>()
    
    override suspend fun invoke(player: Player): DecisionDrawCount.Result {
        return handler.waitForDecision()
    }
    
    /**
     * Provide the decision result from the UI.
     */
    fun provide(result: DecisionDrawCount.Result) {
        handler.provideDecision(result)
    }
    
    /**
     * Check if currently waiting for a decision.
     */
    fun isWaiting(): Boolean = handler.isWaiting()
    
    /**
     * Cancel any pending decision.
     */
    fun cancel() {
        handler.cancelDecision()
    }
}
