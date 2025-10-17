package dugsolutions.leaf.player.decisions.ui

import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.core.DecisionDrawCount

/**
 * Interactive version of DecisionDrawCount that uses DecisionTaskHandler.
 * 
 * This provides a suspend-based interface for UI interaction.
 * The decision is made directly through the handler, making the flow clear.
 */
class DecisionDrawCountSuspend : DecisionDrawCount {
    
    private val handler = DecisionTaskHandler<DecisionDrawCount.Result>()
    
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
