package dugsolutions.leaf.main.local

import dugsolutions.leaf.main.domain.ActionButton
import dugsolutions.leaf.main.domain.CardInfo
import dugsolutions.leaf.main.domain.DieInfo
import dugsolutions.leaf.main.gather.MainGameManager
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.core.DecisionDamageAbsorption
import dugsolutions.leaf.player.decisions.core.DecisionDrawCount
import dugsolutions.leaf.player.decisions.core.DecisionFlowerSelect
import dugsolutions.leaf.player.decisions.core.DecisionShouldProcessTrashEffect
import dugsolutions.leaf.player.decisions.local.ShouldAskTrashEffect
import dugsolutions.leaf.player.decisions.ui.DecisionAcquireSelectSuspend
import dugsolutions.leaf.player.decisions.ui.DecisionDamageAbsorptionSuspend
import dugsolutions.leaf.player.decisions.ui.DecisionDrawCountSuspend
import dugsolutions.leaf.player.decisions.ui.DecisionFlowerSelectSuspend
import dugsolutions.leaf.player.decisions.ui.DecisionShouldProcessTrashEffectSuspend
import dugsolutions.leaf.player.decisions.ui.support.DecisionID
import dugsolutions.leaf.player.decisions.ui.support.DecisionMonitor

class MainDecisions(
    private val mainGameManager: MainGameManager,
    private val cardOperations: CardOperations,
    private val decisionMonitor: DecisionMonitor,
    private val shouldAskTrashEffect: ShouldAskTrashEffect
) {
    enum class Selecting {
        NONE,
        ITEMS,
        FLOWERS
    }

    var selecting = Selecting.NONE
    var decidingPlayer: Player? = null // Made public for the sake of unit tests.

    fun setup(player: Player) = with(player.decisionDirector) {
        decisionMonitor.observe { id -> applyDecisionId(player, id) }
        drawCountDecision = DecisionDrawCountSuspend(decisionMonitor)
        acquireSelectDecision = DecisionAcquireSelectSuspend(decisionMonitor)
        damageAbsorptionDecision = DecisionDamageAbsorptionSuspend(decisionMonitor)
        shouldProcessTrashEffect = DecisionShouldProcessTrashEffectSuspend(decisionMonitor)
        flowerSelectDecision = DecisionFlowerSelectSuspend(decisionMonitor)
        shouldAskTrashEffect.askTrashOkay = false
    }

    private fun applyDecisionId(player: Player, id: DecisionID?) {
        when (id) {
            is DecisionID.ACQUIRE_SELECT -> {
                val possibleCards = id.possibleCards
                val possibleDice = id.possibleDice
                mainGameManager.resetData(player)
                mainGameManager.setHighlightGroveItemsForSelection(possibleCards, possibleDice, player)
                decidingPlayer = player
            }

            DecisionID.DAMAGE_ABSORPTION -> {
                val amount = player.incomingDamage
                mainGameManager.resetData(player)
                mainGameManager.setAllowPlayerItemSelect(player)
                mainGameManager.setActionButton(ActionButton.DONE, "Select cards and/or dice to absorb $amount damage.")
                decidingPlayer = player
                selecting = Selecting.ITEMS
            }

            DecisionID.DRAW_COUNT -> {
                mainGameManager.setShowDrawCount(player)
            }

            DecisionID.FLOWER_SELECT -> {
                mainGameManager.resetData(player)
                mainGameManager.setAllowPlayerFlowerSelect(player)
                mainGameManager.setActionButton(ActionButton.DONE, "Select flower cards to contribute toward played Bloom card.")
                decidingPlayer = player
                selecting = Selecting.FLOWERS
            }

            is DecisionID.SHOULD_PROCESS_TRASH_EFFECT -> {
                val card = id.card
                mainGameManager.setHighlightPlayerCard(player, card)
                mainGameManager.setShowBooleanInstruction("Trash card for effect?")
                decidingPlayer = player
            }

            else -> {
            }
        }
    }

    fun reapplyDecisionId() {
        decidingPlayer?.let {
            applyDecisionId(it, decisionMonitor.currentlyWaitingFor)
        }
    }

    // region DrawCount

    fun onDrawCountChosen(player: Player, value: Int) {
        val drawCountDecision = player.decisionDirector.drawCountDecision
        if (drawCountDecision is DecisionDrawCountSuspend) {
            drawCountDecision.provide(DecisionDrawCount.Result(value))
        }
        mainGameManager.clearShowDrawCount()
    }

    // endregion DrawCount

    // region GroveCard

    fun onGroveCardSelected(cardInfo: CardInfo) {
        cardOperations.getCard(cardInfo)?.let { card ->
            val player = decidingPlayer
            require(player != null)
            val acquireSelectDecision = player.decisionDirector.acquireSelectDecision
            if (acquireSelectDecision is DecisionAcquireSelectSuspend) {
                acquireSelectDecision.provide(card)
            }
            mainGameManager.clearGroveCardHighlights()
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
        mainGameManager.clearGroveCardHighlights()
    }

    // endregion GroveCard

    // TODO: Unit test
    fun onPlayerSelectionComplete() {
        when (selecting) {
            Selecting.NONE -> {}
            Selecting.ITEMS -> onPlayerItemSelectionComplete()
            Selecting.FLOWERS -> onPlayerFlowerSelectionComplete()
        }
        selecting = Selecting.NONE
    }

    // region PlayerSelectItems

    private fun onPlayerItemSelectionComplete() {
        val selected = mainGameManager.gatherSelected()
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
        mainGameManager.clearPlayerSelect()
        mainGameManager.setActionButton(ActionButton.NONE)
    }

    // endregion PlayerSelectItems

    // region PlayerSelectFlowers

    // TODO: Unit test
    private fun onPlayerFlowerSelectionComplete() {
        val selected = mainGameManager.gatherSelected()
        val player = decidingPlayer
        require(player != null)
        val flowerSelectDecision = player.decisionDirector.flowerSelectDecision
        if (flowerSelectDecision is DecisionFlowerSelectSuspend) {
            flowerSelectDecision.provide(DecisionFlowerSelect.Result(selected.floralCards))
        }
        mainGameManager.clearPlayerSelect()
        mainGameManager.setActionButton(ActionButton.NONE)
    }

    // endregion PlayerSelectFlowers

    // region TrashEffect

    fun onCardSelectedForEffect(value: Boolean) {
        val player = decidingPlayer
        require(player != null)
        val shouldTrashDecision = player.decisionDirector.shouldProcessTrashEffect
        if (shouldTrashDecision is DecisionShouldProcessTrashEffectSuspend) {
            shouldTrashDecision.provide(
                if (value) DecisionShouldProcessTrashEffect.Result.TRASH
                else DecisionShouldProcessTrashEffect.Result.DO_NOT_TRASH
            )
        }
        mainGameManager.clearBooleanInstruction()
        mainGameManager.clearPlayerSelect()
    }

    fun setAskTrash(value: Boolean) {
        mainGameManager.setAskTrash(value)
        shouldAskTrashEffect.askTrashOkay = value
    }

    // endregion TrashEffect

}
