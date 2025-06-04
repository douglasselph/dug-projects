package dugsolutions.leaf.main.local

import dugsolutions.leaf.game.Game
import dugsolutions.leaf.main.domain.ActionButton
import dugsolutions.leaf.main.domain.CardInfo
import dugsolutions.leaf.main.domain.DieInfo
import dugsolutions.leaf.main.gather.MainDomainManager
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.core.DecisionAcquireSelect
import dugsolutions.leaf.player.decisions.core.DecisionDamageAbsorption
import dugsolutions.leaf.player.decisions.core.DecisionDrawCount
import dugsolutions.leaf.player.decisions.core.DecisionShouldProcessTrashEffect
import dugsolutions.leaf.player.decisions.ui.DecisionAcquireSelectSuspend
import dugsolutions.leaf.player.decisions.ui.DecisionDamageAbsorptionSuspend
import dugsolutions.leaf.player.decisions.ui.DecisionDrawCountSuspend
import dugsolutions.leaf.player.decisions.ui.DecisionShouldProcessTrashEffectSuspend

class MainDecisions(
    private val mainDomainManager: MainDomainManager,
    private val cardOperations: CardOperations
) {

    var decidingPlayer: Player? = null // Made public for the sake of unit tests.

    fun setup(player: Player) = with(player.decisionDirector) {
        drawCountDecision = createDecisionDrawCountSuspend(player)
        acquireSelectDecision = createDecisionAcquireSelectSuspend(player)
        damageAbsorptionDecision = createDecisionDamageAbsorptionSuspend(player)
        shouldProcessTrashEffect = createDecisionShouldProcessTrashEffectSuspend(player)
    }

    // region DrawCount

    private fun createDecisionDrawCountSuspend(player: Player): DecisionDrawCount {
        val value = DecisionDrawCountSuspend()
        value.onDrawCountRequest = {
            mainDomainManager.updateData()
            mainDomainManager.setShowDrawCount(player, true)
        }
        return value
    }

    fun onDrawCountChosen(player: Player, value: Int) {
        val drawCountDecision = player.decisionDirector.drawCountDecision
        if (drawCountDecision is DecisionDrawCountSuspend) {
            drawCountDecision.provide(value)
        }
        mainDomainManager.clearShowDrawCount()
    }

    // endregion DrawCount

    // region GroveCard

    private fun createDecisionAcquireSelectSuspend(player: Player): DecisionAcquireSelect {
        val value = DecisionAcquireSelectSuspend()
        value.onBestPurchase = { possibleCards, possibleDice ->
            mainDomainManager.updateData()
            mainDomainManager.setHighlightGroveItemsForSelection(possibleCards, possibleDice, player)
            decidingPlayer = player
        }
        return value
    }

    fun onGroveCardSelected(cardInfo: CardInfo) {
        cardOperations.getCard(cardInfo)?.let { card ->
            val player = decidingPlayer
            require(player != null)
            val acquireSelectDecision = player.decisionDirector.acquireSelectDecision
            if (acquireSelectDecision is DecisionAcquireSelectSuspend) {
                acquireSelectDecision.provide(card)
            }
            mainDomainManager.clearGroveCardHighlights()
        }
    }

    fun onGroveDieSelected(dieInfo: DieInfo) {
        require(dieInfo.backingDie != null)
        val player = decidingPlayer
        require(player != null)
        val acquireSelectDecision = player.decisionDirector.acquireSelectDecision
        if (acquireSelectDecision is DecisionAcquireSelectSuspend) {
            acquireSelectDecision.provide(dieInfo.backingDie)
        }
        mainDomainManager.clearGroveCardHighlights()
    }

    // endregion GroveCard

    // region PlayerSelect

    private fun createDecisionDamageAbsorptionSuspend(player: Player): DecisionDamageAbsorption {
        val value = DecisionDamageAbsorptionSuspend()
        value.onDamageAbsorptionRequest = {
            mainDomainManager.updateData()
            mainDomainManager.setAllowPlayerItemSelect(player)
            val amount = player.incomingDamage
            mainDomainManager.setActionButton(ActionButton.DONE, "Select cards and/or dice to absorb $amount damage.")
            decidingPlayer = player
        }
        return value
    }

    fun onPlayerSelectionComplete() {
        val selected = mainDomainManager.gatherSelected()
        val player = decidingPlayer
        require(player != null)
        val damageAbsorptionDecision = player.decisionDirector.damageAbsorptionDecision
        if (damageAbsorptionDecision is DecisionDamageAbsorptionSuspend) {
            damageAbsorptionDecision.provide(
                DecisionDamageAbsorption.Result(
                    cards = selected.cards,
                    floralCards = selected.floralCards,
                    dice = selected.dice
                )
            )
        }
        mainDomainManager.clearPlayerSelect()
        mainDomainManager.setActionButton(ActionButton.NONE)
    }

    // endregion PlayerSelect

    // region TrashEffect

    private fun createDecisionShouldProcessTrashEffectSuspend(player: Player): DecisionShouldProcessTrashEffect {
        val value = DecisionShouldProcessTrashEffectSuspend()
        value.onShouldProcessTrashEffect = { card ->
            mainDomainManager.setHighlightPlayerCard(player, card)
            mainDomainManager.setShowBooleanInstruction("Trash card for effect?")
            decidingPlayer = player
        }
        value.askTrashOkay = false
        return value
    }

    fun onCardSelectedForEffect(value: Boolean) {
        val player = decidingPlayer
        require(player != null)
        val shouldTrashDecision = player.decisionDirector.shouldProcessTrashEffect
        if (shouldTrashDecision is DecisionShouldProcessTrashEffectSuspend) {
            shouldTrashDecision.provide(value)
        }
        mainDomainManager.clearBooleanInstruction()
        mainDomainManager.clearPlayerSelect()
    }

    fun setAskTrash(value: Boolean) {
        mainDomainManager.setAskTrash(value)
    }

    // endregion TrashEffect

}
