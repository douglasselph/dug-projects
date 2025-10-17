package dugsolutions.leaf.player.decisions.ui

import dugsolutions.leaf.common.domain.acquire.ChoiceCard
import dugsolutions.leaf.common.domain.acquire.ChoiceDie
import dugsolutions.leaf.player.decisions.core.DecisionAcquireSelect

/**
 * Interactive version of DecisionAcquireSelect that uses DecisionTaskHandler.
 * 
 * This provides a suspend-based interface for UI interaction.
 * The decision is made directly through the handler, making the flow clear.
 */
class DecisionAcquireSelectSuspend : DecisionAcquireSelect {
    
    private val handler = DecisionTaskHandler<DecisionAcquireSelect.BuyItem>()
    
    override suspend fun invoke(
        possibleCards: List<ChoiceCard>,
        possibleDice: List<ChoiceDie>
    ): DecisionAcquireSelect.BuyItem {
        return handler.waitForDecision()
    }
    
    /**
     * Provide the decision result from the UI.
     */
    fun provide(result: DecisionAcquireSelect.BuyItem) {
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
