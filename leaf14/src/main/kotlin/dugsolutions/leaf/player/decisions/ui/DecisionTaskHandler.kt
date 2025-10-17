package dugsolutions.leaf.player.decisions.ui

import kotlinx.coroutines.CompletableDeferred

/**
 * Decision task handler that manages interactive decision making.
 * 
 * Key benefits:
 * - No queue complexity - only one decision at a time
 * - Clear state - easy to see what's waiting
 * - Simple debugging - direct call stack
 * - No race conditions - only one decision active
 * - Direct communication - no complex notification chains
 */
class DecisionTaskHandler<T> {
    
    private var currentDecision: CompletableDeferred<T>? = null
    
    /**
     * Wait for a decision to be made. This suspends until provideDecision() is called.
     */
    suspend fun waitForDecision(): T {
        val deferred = CompletableDeferred<T>()
        currentDecision = deferred
        return deferred.await()
    }
    
    /**
     * Provide the decision result. This will complete the waiting coroutine.
     */
    fun provideDecision(value: T) {
        currentDecision?.complete(value)
        currentDecision = null
    }
    
    /**
     * Check if currently waiting for a decision.
     */
    fun isWaiting(): Boolean = currentDecision != null
    
    /**
     * Cancel any pending decision (useful for cleanup).
     */
    fun cancelDecision() {
        currentDecision?.cancel()
        currentDecision = null
    }
}
