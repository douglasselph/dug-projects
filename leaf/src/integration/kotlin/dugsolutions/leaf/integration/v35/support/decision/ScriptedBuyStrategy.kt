package dugsolutions.leaf.integration.v35.support.decision

import dugsolutions.leaf.integration.v35.support.DecisionScript
import dugsolutions.leaf.v35.player.decision.buy.BaselineBuyStrategy
import dugsolutions.leaf.v35.player.decision.buy.BuyChoice
import dugsolutions.leaf.v35.player.decision.buy.BuyPayment
import dugsolutions.leaf.v35.player.decision.buy.BuyStrategy
import dugsolutions.leaf.v35.player.decision.buy.ChoosePaymentRequest
import dugsolutions.leaf.v35.player.decision.buy.ChoosePurchaseRequest

class ScriptedBuyStrategy(
    private val fallback: BuyStrategy = BaselineBuyStrategy()
) : BuyStrategy {
    private val purchases =
        DecisionScript<ChoosePurchaseRequest, BuyChoice>("Buy choices")
    private val payments =
        DecisionScript<ChoosePaymentRequest, BuyPayment>("Buy payments")

    fun thenPurchase(
        selector: (ChoosePurchaseRequest) -> BuyChoice
    ): ScriptedBuyStrategy = apply { purchases.then(selector) }

    fun thenPayment(
        selector: (ChoosePaymentRequest) -> BuyPayment
    ): ScriptedBuyStrategy = apply { payments.then(selector) }

    override fun choosePurchase(request: ChoosePurchaseRequest): BuyChoice {
        val chosen = purchases.nextOrElse(request, fallback::choosePurchase)
        if (chosen is BuyChoice.Purchase) {
            require(chosen.item in request.options) {
                "Scripted purchase is not legal: ${chosen.item}; legal=${request.options}"
            }
        }
        return chosen
    }

    override fun choosePayment(request: ChoosePaymentRequest): BuyPayment {
        val chosen = payments.nextOrElse(request, fallback::choosePayment)
        require(chosen.total >= request.cost) {
            "Scripted payment ${chosen.total} does not meet cost ${request.cost}"
        }
        require(chosen.dice.all { it in request.availableDice }) {
            "Scripted payment contains an unavailable die: ${chosen.dice}"
        }
        require(chosen.critters.all { it in request.availableCritters }) {
            "Scripted payment contains an unavailable Critter: ${chosen.critters}"
        }
        return chosen
    }

    fun assertExhausted() {
        purchases.assertExhausted()
        payments.assertExhausted()
    }
}
