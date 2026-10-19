package dugsolutions.leaf.player.decisions.simple

import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.core.DecisionDrawCount

/**
 * Example showing how to use the simplified decision system.
 * 
 * This demonstrates the key benefits:
 * - No complex queue management
 * - Clear decision flow
 * - Easy to debug and test
 * - Direct communication between components
 */
class SimpleDecisionExample {
    
    private lateinit var player: Player
    private lateinit var decisionManager: SimpleDecisionManager
    private lateinit var mainDecisions: SimpleMainDecisions
    
    /**
     * Example of how a game turn would work with the simplified system.
     */
    suspend fun exampleGameTurn() {
        // 1. Player starts their turn
        println("Player ${player.id} starting turn")
        
        // 2. Game asks for draw count decision
        val drawCountResult = player.decisionDirector.drawCountDecision.invoke(player)
        println("Player decided to draw ${drawCountResult.count} cards")
        
        // 3. Game continues with other decisions...
        // Each decision is handled the same way - simple and direct
    }
    
    /**
     * Example of how UI interaction works with the simplified system.
     */
    fun exampleUIIntegration() {
        // 1. Game calls the decision (this suspends)
        // val result = player.decisionDirector.drawCountDecision.invoke(player)
        
        // 2. UI presents options to user
        // User selects "3 cards"
        
        // 3. UI calls the provide method directly
        val drawCountDecision = player.decisionDirector.drawCountDecision
        if (drawCountDecision is SimpleDecisionDrawCount) {
            drawCountDecision.provide(DecisionDrawCount.Result(3))
        }
        
        // 4. The suspended coroutine resumes with the result
        // No complex queue management needed!
    }
    
    /**
     * Example of debugging the simplified system.
     */
    fun exampleDebugging() {
        val drawCountDecision = player.decisionDirector.drawCountDecision
        if (drawCountDecision is SimpleDecisionDrawCount) {
            if (drawCountDecision.isWaiting()) {
                println("Draw count decision is waiting for UI input")
            } else {
                println("No draw count decision pending")
            }
        }
        
        // Check if any decisions are waiting
        if (decisionManager.hasWaitingDecisions()) {
            println("Waiting for decisions: ${decisionManager.getWaitingDecisions()}")
        }
    }
    
    /**
     * Example of cleanup and error handling.
     */
    fun exampleCleanup() {
        // Cancel any pending decisions
        val drawCountDecision = player.decisionDirector.drawCountDecision
        if (drawCountDecision is SimpleDecisionDrawCount) {
            drawCountDecision.cancel()
        }
        
        // Clear all waiting decisions
        decisionManager.clearWaitingDecisions()
    }
}
