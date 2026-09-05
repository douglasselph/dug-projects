package dugsolutions.leaf.integration.v35.support.decision

import dugsolutions.leaf.integration.v35.support.DecisionScript
import dugsolutions.leaf.v35.player.decision.mechanical.buy.MechanicalBuyStrategy
import dugsolutions.leaf.v35.player.decision.buy.BuyChoice
import dugsolutions.leaf.v35.player.decision.buy.BuyPayment
import dugsolutions.leaf.v35.player.decision.buy.BuyStrategy
import dugsolutions.leaf.v35.player.decision.buy.ChoosePaymentRequest
import dugsolutions.leaf.v35.player.decision.buy.ChoosePurchaseRequest
import dugsolutions.leaf.v35.player.decision.buy.BuyItem
import dugsolutions.leaf.v35.random.die.DieSides

class ScriptedBuyStrategy(
    private val fallback: BuyStrategy = MechanicalBuyStrategy()
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


    fun thenDone(): ScriptedBuyStrategy =
        thenPurchase { BuyChoice.Done }

    fun thenPurchaseDie(sides: DieSides): ScriptedBuyStrategy =
        thenPurchase { request ->
            BuyChoice.Purchase(
                request.options.filterIsInstance<BuyItem.Die>()
                    .first { it.sides == sides }
            )
        }

    fun thenPurchasePlant(nameOrTitle: String): ScriptedBuyStrategy =
        thenPurchase { request ->
            BuyChoice.Purchase(
                request.options.filterIsInstance<BuyItem.Plant>()
                    .first { it.card.name == nameOrTitle || it.card.title == nameOrTitle }
            )
        }

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
