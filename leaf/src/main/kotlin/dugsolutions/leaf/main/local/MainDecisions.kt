package dugsolutions.leaf.main.local

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.main.domain.ActionButton
import dugsolutions.leaf.main.domain.CardInfo
import dugsolutions.leaf.main.gather.MainDomainManager
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.core.DecisionBestCardPurchase
import dugsolutions.leaf.player.decisions.core.DecisionDamageAbsorption
import dugsolutions.leaf.player.decisions.core.DecisionDrawCount
import dugsolutions.leaf.player.decisions.ui.DecisionBestCardPurchaseSuspend
import dugsolutions.leaf.player.decisions.ui.DecisionDamageAbsorptionSuspend
import dugsolutions.leaf.player.decisions.ui.DecisionDrawCountSuspend

class MainDecisions(
    private val mainDomainManager: MainDomainManager,
    private val cardOperations: CardOperations
) {

    private var decisionDrawCountSuspend: DecisionDrawCountSuspend = DecisionDrawCountSuspend()
    private var decisionBestCardPurchaseSuspend: DecisionBestCardPurchaseSuspend = DecisionBestCardPurchaseSuspend()
    private var decisionDamageAbsorptionSuspend: DecisionDamageAbsorptionSuspend = DecisionDamageAbsorptionSuspend()

    fun setup(player: Player) {
        player.decisionDirector.drawCountDecision = createDecisionDrawCountSuspend(player)
        player.decisionDirector.bestCardPurchase = createDecisionBestCardPurchaseSuspend(player)
        player.decisionDirector.damageAbsorptionDecision = createDecisionDamageAbsorptionSuspend(player)
    }

    // region DrawCount

    private fun createDecisionDrawCountSuspend(player: Player): DecisionDrawCount {
        val value = DecisionDrawCountSuspend()
        value.onDrawCountRequest = {
            mainDomainManager.setShowDrawCount(player, true)
            decisionDrawCountSuspend = value
        }
        return value
    }

    fun onDrawCountChosen(value: Int) {
        decisionDrawCountSuspend.provide(value)
        mainDomainManager.clearShowDrawCount()
    }

    // endregion DrawCount

    // region GroveCard

    private fun createDecisionBestCardPurchaseSuspend(player: Player): DecisionBestCardPurchase {
        val value = DecisionBestCardPurchaseSuspend()
        value.onBestCardPurchase = { possibleCards ->
            mainDomainManager.setHighlightGroveCardsForSelection(possibleCards, player)
            decisionBestCardPurchaseSuspend = value
        }
        return value
    }

    fun onGroveCardSelected(cardInfo: CardInfo) {
        cardOperations.getCard(cardInfo)?.let { card ->
            decisionBestCardPurchaseSuspend.provide(card)
            mainDomainManager.clearGroveCardHighlights()
        }
    }

    // endregion GroveCard

    // region PlayerSelect

    private fun createDecisionDamageAbsorptionSuspend(player: Player): DecisionDamageAbsorption {
        val value = DecisionDamageAbsorptionSuspend()
        value.onDamageAbsorptionRequest = {
            mainDomainManager.setAllowPlayerItemSelect(player)
            val amount = player.incomingDamage
            mainDomainManager.setActionButton(ActionButton.DONE, "Select cards and/or dice to absorb $amount damage.")
            decisionDamageAbsorptionSuspend = value
        }
        return value
    }

    fun onPlayerSelectionComplete() {
        val selected = mainDomainManager.gatherSelected()
        decisionDamageAbsorptionSuspend.provide(
            DecisionDamageAbsorption.Result(
                cards = selected.cards,
                floralCards = selected.floralCards,
                dice = selected.dice
            )
        )
        mainDomainManager.clearAllowPlayerItemSelect()
        mainDomainManager.setActionButton(ActionButton.NONE)
    }

    // endregion PlayerSelect

}
