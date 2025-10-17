package dugsolutions.leaf.player.decisions.ui

import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.core.DecisionShouldTargetPlayer

/**
 * Interactive version of DecisionShouldTargetPlayer that uses DecisionTaskHandler.
 * 
 * This provides a suspend-based interface for UI interaction.
 * The decision is made directly through the handler, making the flow clear.
 */
class DecisionShouldTargetPlayerSuspend : DecisionShouldTargetPlayer {
    
    private val handler = DecisionTaskHandler<Boolean>()
    
    override fun invoke(target: Player, amount: Int): Boolean {
        // This is a synchronous interface, but we need to handle it asynchronously
        // In practice, this would be called from a suspend context
        throw UnsupportedOperationException("Use invokeSuspend() instead of invoke() for suspend-based decisions")
    }
    
    /**
     * Suspend-based version of the decision.
     */
    suspend fun invokeSuspend(target: Player, amount: Int): Boolean {
        return handler.waitForDecision()
    }
    
    /**
     * Provide the decision result from the UI.
     */
    fun provide(result: Boolean) {
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
