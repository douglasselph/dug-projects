package dugsolutions.leaf.v35.player.decision.baseline.buy

import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import dugsolutions.leaf.v35.player.decision.baseline.influence.BaselineInfluenceRegistry
import dugsolutions.leaf.v35.player.decision.baseline.scoring.BaselineScoreEngine
import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionCandidate
import dugsolutions.leaf.v35.player.decision.baseline.scoring.ScoredChoice
import dugsolutions.leaf.v35.player.decision.buy.*
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.mechanical.buy.MechanicalBuyStrategy

class HumanBaselineBuyStrategy(
    private val delegate: BuyStrategy = MechanicalBuyStrategy(),
    internal val scoreEngine: BaselineScoreEngine = BaselineScoreEngine(),
    internal val cardScorers: HumanBaselineCardScorerRegistry = HumanBaselineCardScorerRegistry(),
    internal val influenceRegistry: BaselineInfluenceRegistry = BaselineInfluenceRegistry(cardScorers)
) : BuyStrategy {
    override fun choosePurchase(request: ChoosePurchaseRequest): BuyChoice {
        if (request.context == DecisionContext.EMPTY || request.options.isEmpty()) {
            return delegate.choosePurchase(request)
        }
        return scoreEngine.chooseValue(
            request.options.map { item ->
                ScoredChoice(
                    BuyChoice.Purchase(item),
                    PurchasePriority.score(request.context, item, cardScorers = cardScorers)
                )
            }
        )
    }

    override fun choosePayment(request: ChoosePaymentRequest): BuyPayment {
        if (request.context == DecisionContext.EMPTY) return delegate.choosePayment(request)

        val resources = request.availableDice.map { Resource(die = it, value = it.value) } +
            request.availableCritters.map { Resource(critter = it, value = it.value) }
        val payments = mutableListOf<BuyPayment>()

        fun search(index: Int, selected: MutableList<Resource>, total: Int) {
            if (total >= request.cost) {
                payments += BuyPayment(
                    dice = selected.mapNotNull { it.die },
                    critters = selected.mapNotNull { it.critter }
                )
                return
            }
            if (index >= resources.size) return
            selected += resources[index]
            search(index + 1, selected, total + resources[index].value)
            selected.removeAt(selected.lastIndex)
            search(index + 1, selected, total)
        }
        search(0, mutableListOf(), 0)
        if (payments.isEmpty()) return BuyPayment()

        return scoreEngine.chooseValue(
            context = request.context,
            candidates = payments.map { payment ->
                DecisionCandidate(
                    choice = payment,
                    score = PaymentPriority.score(request.context, payment, request.cost),
                    tags = PaymentPriority.tags(payment)
                )
            },
            influenceRegistry = influenceRegistry
        )
    }

    private data class Resource(
        val die: BuyDieResource? = null,
        val critter: BuyCritterResource? = null,
        val value: Int
    )
}
