package dugsolutions.leaf.player.decisions.ui

import dugsolutions.leaf.player.decisions.core.DecisionDamageAbsorption

/**
 * Interactive version of DecisionDamageAbsorption that uses DecisionTaskHandler.
 * 
 * This provides a suspend-based interface for UI interaction.
 * The decision is made directly through the handler, making the flow clear.
 */
class DecisionDamageAbsorptionSuspend : DecisionDamageAbsorption {
    
    private val handler = DecisionTaskHandler<DecisionDamageAbsorption.Result>()
    
    override suspend fun invoke(): DecisionDamageAbsorption.Result {
        return handler.waitForDecision()
    }
    
    /**
     * Provide the decision result from the UI.
     */
    fun provide(result: DecisionDamageAbsorption.Result) {
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
