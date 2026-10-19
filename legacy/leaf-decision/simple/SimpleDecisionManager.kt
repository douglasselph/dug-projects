package dugsolutions.leaf.player.decisions.simple

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.game.domain.GameTime
import dugsolutions.leaf.grove.Grove
import dugsolutions.leaf.grove.local.GroveNearingTransition
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.baseline.DecisionAcquireSelectBaseline
import dugsolutions.leaf.player.decisions.baseline.DecisionBestBloomAcquisitionCardBaseline
import dugsolutions.leaf.player.decisions.baseline.DecisionDamageAbsorptionBaseline
import dugsolutions.leaf.player.decisions.baseline.DecisionDrawCountBaseline
import dugsolutions.leaf.player.decisions.baseline.DecisionFlowerSelectBaseline
import dugsolutions.leaf.player.decisions.baseline.DecisionRerollOneDieBaseline
import dugsolutions.leaf.player.decisions.baseline.DecisionShouldProcessTrashEffectBaseline
import dugsolutions.leaf.player.decisions.baseline.DecisionShouldTargetPlayerBaseline
import dugsolutions.leaf.player.decisions.core.DecisionAcquireSelect
import dugsolutions.leaf.player.decisions.core.DecisionBestBloomAcquisitionCard
import dugsolutions.leaf.player.decisions.core.DecisionDamageAbsorption
import dugsolutions.leaf.player.decisions.core.DecisionDrawCount
import dugsolutions.leaf.player.decisions.core.DecisionFlowerSelect
import dugsolutions.leaf.player.decisions.core.DecisionRerollOneDie
import dugsolutions.leaf.player.decisions.core.DecisionShouldProcessTrashEffect
import dugsolutions.leaf.player.decisions.core.DecisionShouldTargetPlayer
import dugsolutions.leaf.player.decisions.local.AcquireCardEvaluator
import dugsolutions.leaf.player.decisions.local.AcquireDieEvaluator
import dugsolutions.leaf.player.di.CardEffectBattleScoreFactory

/**
 * Simplified decision manager that replaces the complex DecisionDirector.
 * 
 * Key improvements:
 * - No complex DecisionTaskQueue dependencies
 * - Clear decision state management
 * - Easy to switch between baseline and UI decisions
 * - Simple debugging and testing
 */
class SimpleDecisionManager(
    private val cardEffectBattleScoreFactory: CardEffectBattleScoreFactory,
    private val cardManager: CardManager,
    private val acquireCardEvaluator: AcquireCardEvaluator,
    private val acquireDieEvaluator: AcquireDieEvaluator,
    private val groveNearingTransition: GroveNearingTransition
) {

    // Decision instances - can be baseline or UI versions
    lateinit var drawCountDecision: DecisionDrawCount
    lateinit var acquireSelectDecision: DecisionAcquireSelect
    lateinit var damageAbsorptionDecision: DecisionDamageAbsorption
    lateinit var shouldProcessTrashEffect: DecisionShouldProcessTrashEffect
    lateinit var shouldTargetPlayer: DecisionShouldTargetPlayer
    lateinit var rerollOneDie: DecisionRerollOneDie
    lateinit var bestBloomCardAcquisition: DecisionBestBloomAcquisitionCard
    lateinit var flowerSelectDecision: DecisionFlowerSelect

    // Track which decisions are currently waiting for UI input
    private val waitingDecisions = mutableSetOf<String>()

    fun initialize(player: Player, useUI: Boolean = false) {
        if (useUI) {
            // Use UI-based decisions
            drawCountDecision = SimpleDecisionDrawCount()
            // TODO: Add other UI decision implementations
            acquireSelectDecision = DecisionAcquireSelectBaseline(player, acquireCardEvaluator, acquireDieEvaluator)
            damageAbsorptionDecision = DecisionDamageAbsorptionBaseline(player, cardEffectBattleScoreFactory, cardManager)
            shouldProcessTrashEffect = DecisionShouldProcessTrashEffectBaseline(groveNearingTransition)
            shouldTargetPlayer = DecisionShouldTargetPlayerBaseline(player)
            rerollOneDie = DecisionRerollOneDieBaseline(player)
            bestBloomCardAcquisition = DecisionBestBloomAcquisitionCardBaseline()
            flowerSelectDecision = DecisionFlowerSelectBaseline(player)
        } else {
            // Use baseline decisions
            drawCountDecision = DecisionDrawCountBaseline()
            acquireSelectDecision = DecisionAcquireSelectBaseline(player, acquireCardEvaluator, acquireDieEvaluator)
            damageAbsorptionDecision = DecisionDamageAbsorptionBaseline(player, cardEffectBattleScoreFactory, cardManager)
            shouldProcessTrashEffect = DecisionShouldProcessTrashEffectBaseline(groveNearingTransition)
            shouldTargetPlayer = DecisionShouldTargetPlayerBaseline(player)
            rerollOneDie = DecisionRerollOneDieBaseline(player)
            bestBloomCardAcquisition = DecisionBestBloomAcquisitionCardBaseline()
            flowerSelectDecision = DecisionFlowerSelectBaseline(player)
        }
    }

    val usingUIDecisions: Boolean
        get() = drawCountDecision is SimpleDecisionDrawCount

    /**
     * Check if any decisions are currently waiting for UI input.
     */
    fun hasWaitingDecisions(): Boolean = waitingDecisions.isNotEmpty()

    /**
     * Get the list of decisions currently waiting.
     */
    fun getWaitingDecisions(): List<String> = waitingDecisions.toList()

    /**
     * Mark a decision as waiting.
     */
    private fun markWaiting(decisionName: String) {
        waitingDecisions.add(decisionName)
    }

    /**
     * Mark a decision as completed.
     */
    private fun markCompleted(decisionName: String) {
        waitingDecisions.remove(decisionName)
    }

    /**
     * Clear all waiting decisions (useful for cleanup).
     */
    fun clearWaitingDecisions() {
        waitingDecisions.clear()
    }
}
