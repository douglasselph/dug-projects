package dugsolutions.leaf.main.local

import dugsolutions.leaf.chronicle.GameChronicle
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
    private val cardOperations: CardOperations,
    private val chronicle: GameChronicle
) {

    private var decisionDrawCountSuspend: DecisionDrawCountSuspend = DecisionDrawCountSuspend()
    private var decisionBestCardPurchaseSuspend: DecisionBestCardPurchaseSuspend = DecisionBestCardPurchaseSuspend()
    private var decisionDamageAbsorptionSuspend: DecisionDamageAbsorptionSuspend = DecisionDamageAbsorptionSuspend()

    // region public

    fun setup(player: Player) {
        player.decisionDirector.drawCountDecision = createDecisionDrawCountSuspend(player)
        player.decisionDirector.bestCardPurchase = createDecisionBestCardPurchaseSuspend(player)
        player.decisionDirector.damageAbsorptionDecision = createDecisionDamageAbsorptionSuspend(player)
    }

    fun onDrawCountChosen(value: Int) {
        decisionDrawCountSuspend.provide(value)
        mainDomainManager.clearShowDrawCount()
    }

    fun onGroveCardSelected(cardInfo: CardInfo) {
        cardOperations.getCard(cardInfo)?.let { card ->
            decisionBestCardPurchaseSuspend.provide(card)
            mainDomainManager.clearGroveCardHighlights()
        }
    }

    // endregion public

    private fun createDecisionDrawCountSuspend(player: Player): DecisionDrawCount {
        val value = DecisionDrawCountSuspend()
        value.onDrawCountRequest = {
            mainDomainManager.setShowDrawCount(player, true)
            decisionDrawCountSuspend = value
        }
        return value
    }

    private fun createDecisionBestCardPurchaseSuspend(player: Player): DecisionBestCardPurchase {
        val value = DecisionBestCardPurchaseSuspend()
        value.onBestCardPurchase = { possibleCards ->
            mainDomainManager.setHighlightGroveCardsForSelection(possibleCards, player)
            decisionBestCardPurchaseSuspend = value
        }
        return value
    }

    private fun createDecisionDamageAbsorptionSuspend(player: Player): DecisionDamageAbsorption {
        val value = DecisionDamageAbsorptionSuspend()
        value.onDamageAbsorptionRequest = {
            mainDomainManager.setAllowPlayerItemSelect(player)
            decisionDamageAbsorptionSuspend = value
        }
        return value
    }

}
