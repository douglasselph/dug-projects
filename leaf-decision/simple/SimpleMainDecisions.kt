package dugsolutions.leaf.player.decisions.simple

import dugsolutions.leaf.main.domain.CardInfo
import dugsolutions.leaf.main.domain.DieInfo
import dugsolutions.leaf.main.gather.MainGameManager
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.core.DecisionDamageAbsorption
import dugsolutions.leaf.player.decisions.core.DecisionDrawCount
import dugsolutions.leaf.player.decisions.core.DecisionFlowerSelect
import dugsolutions.leaf.player.decisions.core.DecisionShouldProcessTrashEffect
import dugsolutions.leaf.player.decisions.local.ShouldAskTrashEffect

/**
 * Simplified version of MainDecisions that eliminates the complex DecisionTaskQueue system.
 * 
 * Key improvements:
 * - Direct communication with decision handlers
 * - No complex notification chains
 * - Clear state management
 * - Easy to debug and test
 */
class SimpleMainDecisions(
    private val mainGameManager: MainGameManager,
    private val cardOperations: CardOperations,
    private val shouldAskTrashEffect: ShouldAskTrashEffect,
    private val decidingPlayerManager: DecidingPlayer
) {
    
    enum class Selecting {
        NONE,
        ITEMS,
        FLOWERS
    }
    
    var selecting = Selecting.NONE
    
    private var decidingPlayer: Player?
        get() = decidingPlayerManager.player
        set(value) { decidingPlayerManager.player = value }

    /**
     * Setup UI-based decisions for a player.
     */
    fun setup(player: Player) {
        // Switch to UI-based decisions
        player.decisionDirector.initialize(player, useUI = true)
        shouldAskTrashEffect.askTrashOkay = false
    }

    /**
     * Toggle between baseline and UI decisions.
     */
    fun onDecisionToggle(player: Player) {
        if (player.decisionDirector.usingBaselineDrawCount) {
            setup(player)
        } else {
            player.initialize()
        }
    }

    // region DrawCount

    /**
     * Handle draw count decision from UI.
     */
    fun onDrawCountChosen(player: Player, value: Int) {
        val drawCountDecision = player.decisionDirector.drawCountDecision
        if (drawCountDecision is SimpleDecisionDrawCount) {
            drawCountDecision.provide(DecisionDrawCount.Result(value))
        }
    }

    // endregion DrawCount

    // region GroveCard

    /**
     * Handle grove card selection.
     */
    fun onGroveCardSelected(cardInfo: CardInfo) {
        // TODO: Implement grove card selection logic
        // This would be similar to the original but without the complex queue system
    }

    /**
     * Handle grove die selection.
     */
    fun onGroveDieSelected(dieInfo: DieInfo) {
        // TODO: Implement grove die selection logic
    }

    // endregion GroveCard

    // region PlayerSelectItems

    /**
     * Handle player item selection completion.
     */
    private fun onPlayerItemSelectionComplete() {
        val selected = mainGameManager.gatherSelected()
        val player = decidingPlayer
        decidingPlayer = null
        require(player != null)
        
        val damageToAbsorb = player.incomingDamage
        val damageAbsorptionDecision = player.decisionDirector.damageAbsorptionDecision
        
        // TODO: Implement damage absorption decision handling
        // This would use a SimpleDecisionDamageAbsorption similar to SimpleDecisionDrawCount
    }

    // endregion PlayerSelectItems

    // region PlayerSelectFlowers

    /**
     * Handle player flower selection completion.
     */
    private fun onPlayerFlowerSelectionComplete() {
        val selected = mainGameManager.gatherSelected()
        val player = decidingPlayer
        decidingPlayer = null
        require(player != null)
        
        val flowerSelectDecision = player.decisionDirector.flowerSelectDecision
        
        // TODO: Implement flower selection decision handling
        // This would use a SimpleDecisionFlowerSelect similar to SimpleDecisionDrawCount
    }

    // endregion PlayerSelectFlowers

    // region TrashEffect

    /**
     * Handle trash effect decision.
     */
    fun onCardSelectedForEffect(value: Boolean) {
        // TODO: Implement trash effect decision handling
    }

    // endregion TrashEffect
}
