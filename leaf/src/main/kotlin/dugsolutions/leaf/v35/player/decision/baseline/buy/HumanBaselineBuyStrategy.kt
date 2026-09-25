package dugsolutions.leaf.v35.player.decision.baseline.buy

import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import dugsolutions.leaf.v35.player.decision.baseline.influence.BaselineInfluenceRegistry
import dugsolutions.leaf.v35.player.decision.baseline.scoring.BaselineScoreEngine
import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionCandidate
import dugsolutions.leaf.v35.player.decision.buy.*
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.mechanical.buy.MechanicalBuyStrategy
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import dugsolutions.leaf.v35.tokens.Critter

/**
 * Human Baseline Buy behavior: simple, recognizable ordinary play rather than
 * optimized shopping.
 *
 * Purchase contract:
 * 1. Normally make one principal purchase, then stop. Deliberately splitting
 *    purchasing power across several buys belongs to more advanced strategy.
 * 2. First decide whether development balance calls for a Plant or a die. Buy
 *    balance compares total owned die sides against grafted Plant count valued
 *    at 15 points per Plant. When both categories are affordable, an injected
 *    policy converts that visible imbalance into a probability; equal
 *    development is 50/50 and a three-point difference is about 85/15 toward
 *    the weaker side. This comparison is independent of round number.
 * 3. Within the selected category, buy from the most expensive affordable cost
 *    tier. Card-specific value only breaks choices within that tier; it does
 *    not turn Human Baseline into an efficiency/combo optimizer.
 * 4. Duplicate Plants receive no generic penalty.
 * 5. Buy normally preserves the Critter reserve supplied by
 *    [HumanBaselinePolicy.protectedCritterReserve]. Surplus Critters are treated as available
 *    purchasing power probabilistically. For surplus > 0, the probability is
 *    [CRITTER_SPEND_STARTING_PERCENTAGE] +
 *    [CRITTER_SPEND_INCREMENT_PER_SURPLUS] * surplus, capped at 100%. Surplus
 *    0 therefore remains 0%; the starting percentage is an offset once surplus
 *    exists.
 * 6. A D20 (cost 20) or cost-17 Flower is a premium threshold. Human Baseline
 *    may spend protected Critters to reach one of those purchases.
 * 7. When a Critter is actually required and either type can make the payment,
 *    prefer a Bee [BEE_PREFERENCE_PERCENTAGE]% of the time because Worms have
 *    the additional Flip use.
 *
 * Every probability above consumes StrategyRandomizer only. Mechanical dice,
 * deck, and other game randomness must never be consumed by this strategy.
 */
class HumanBaselineBuyStrategy(
    private val delegate: BuyStrategy = MechanicalBuyStrategy(),
    internal val scoreEngine: BaselineScoreEngine = BaselineScoreEngine(),
    internal val cardScorers: HumanBaselineCardScorerRegistry = HumanBaselineCardScorerRegistry(),
    internal val influenceRegistry: BaselineInfluenceRegistry = BaselineInfluenceRegistry(cardScorers),
    private val purchaseScoreModifier: PurchaseScoreModifier = PurchaseScoreModifier.NONE,
    private val strategyRandomizer: StrategyRandomizer = StrategyRandomizer.create(),
    internal val policy: HumanBaselinePolicy = HumanBaselinePolicy()
) : BuyStrategy {

    companion object {
        const val CRITTER_SPEND_STARTING_PERCENTAGE: Int = 5
        const val CRITTER_SPEND_INCREMENT_PER_SURPLUS: Int = 15
        const val BEE_PREFERENCE_PERCENTAGE: Int = 67
        const val PREMIUM_DIE_COST: Int = 20
        const val PREMIUM_FLOWER_COST: Int = 17
    }

    private data class CritterSpendPlan(
        val allowSurplus: Boolean,
        val beeSurplus: Int,
        val wormSurplus: Int
    )

    private var pendingItem: BuyItem? = null
    private var pendingPlan: CritterSpendPlan? = null

    override fun choosePurchase(request: ChoosePurchaseRequest): BuyChoice {
        if (request.context == DecisionContext.EMPTY) return delegate.choosePurchase(request)
        if (request.purchasesMadeThisBuy > 0 || request.options.isEmpty()) {
            clearPendingPlan()
            return BuyChoice.Done
        }

        val plan = createCritterSpendPlan(request.context)
        val affordable = request.options.filter { isOrdinarilyAffordable(request.context, it, plan) }
        if (affordable.isEmpty()) {
            clearPendingPlan()
            return BuyChoice.Done
        }

        val affordablePlants = affordable.filterIsInstance<BuyItem.Plant>()
        val affordableDice = affordable.filterIsInstance<BuyItem.Die>()
        val categoryCandidates = when {
            affordablePlants.isEmpty() -> affordableDice
            affordableDice.isEmpty() -> affordablePlants
            else -> chooseDevelopmentCategory(
                context = request.context,
                plants = affordablePlants,
                dice = affordableDice
            )
        }
        val highestCost = categoryCandidates.maxOf { it.cost }
        val tier = categoryCandidates.filter { it.cost == highestCost }

        val selected = scoreEngine.chooseValue(
            context = request.context,
            candidates = tier.map { item ->
                val baselineScore = PurchasePriority.score(
                    context = request.context,
                    item = item,
                    cardScorers = cardScorers
                )
                DecisionCandidate<BuyChoice>(
                    choice = BuyChoice.Purchase(item),
                    score = purchaseScoreModifier.modify(
                        context = request.context,
                        item = item,
                        score = baselineScore
                    )
                )
            },
            influenceRegistry = influenceRegistry
        )

        val purchased = (selected as BuyChoice.Purchase).item
        pendingItem = purchased
        pendingPlan = plan
        return selected
    }

    override fun choosePayment(request: ChoosePaymentRequest): BuyPayment {
        if (request.context == DecisionContext.EMPTY) return delegate.choosePayment(request)

        val plan = if (pendingItem == request.item) {
            pendingPlan ?: createCritterSpendPlan(request.context)
        } else {
            createCritterSpendPlan(request.context)
        }
        clearPendingPlan()

        val permitted = enumeratePayments(request).filter { paymentAllowed(request, it, plan) }
        // Production Human Baseline should never need this fallback because choosePurchase
        // applies the same plan. It keeps direct/legacy callers legal rather than returning
        // an invalid payment if they invoke choosePayment in isolation.
        val payments = permitted.ifEmpty { enumeratePayments(request) }
        if (payments.isEmpty()) return BuyPayment()

        val preferredPayments = preferCritterTypeWhenRequired(payments)
        return scoreEngine.chooseValue(
            context = request.context,
            candidates = preferredPayments.map { payment ->
                DecisionCandidate(
                    choice = payment,
                    score = PaymentPriority.score(request.context, payment, request.cost, policy),
                    tags = PaymentPriority.tags(payment)
                )
            },
            influenceRegistry = influenceRegistry
        )
    }

    private fun chooseDevelopmentCategory(
        context: DecisionContext,
        plants: List<BuyItem.Plant>,
        dice: List<BuyItem.Die>
    ): List<BuyItem> {
        val lowPlantPercentage = policy.lowPlantPriorityPercentage(context)
        if (lowPlantPercentage > 0) {
            return if (strategyRandomizer.nextInt(100) < lowPlantPercentage) {
                plants
            } else {
                dice
            }
        }

        val plantPercentage = policy.buyPlantPriorityPercentage(context)
        return when {
            plantPercentage <= 0 -> dice
            plantPercentage >= 100 -> plants
            strategyRandomizer.nextInt(100) < plantPercentage -> plants
            else -> dice
        }
    }

    internal fun critterSpendPercentage(surplus: Int): Int {
        require(surplus >= 0) { "Critter surplus cannot be negative: $surplus" }
        if (surplus == 0) return 0
        return minOf(
            100,
            CRITTER_SPEND_STARTING_PERCENTAGE + CRITTER_SPEND_INCREMENT_PER_SURPLUS * surplus
        )
    }

    private fun createCritterSpendPlan(context: DecisionContext): CritterSpendPlan {
        val bees = context.self.board.bees
        val worms = context.self.board.worms
        val reserve = policy.protectedCritterReserve(context)
        val beeSurplus = (bees - reserve.bees).coerceAtLeast(0)
        val wormSurplus = (worms - reserve.worms).coerceAtLeast(0)
        val surplus = beeSurplus + wormSurplus
        val chance = critterSpendPercentage(surplus)
        val allowSurplus = chance > 0 && strategyRandomizer.nextInt(100) < chance
        return CritterSpendPlan(allowSurplus, beeSurplus, wormSurplus)
    }

    private fun isOrdinarilyAffordable(
        context: DecisionContext,
        item: BuyItem,
        plan: CritterSpendPlan
    ): Boolean {
        val dicePower = context.self.board.hand.sumOf { it.value }
        if (dicePower >= item.cost) return true

        val beeValue = context.self.board.beeValue
        val wormValue = context.self.board.wormValue
        val ordinaryCritterPower = if (plan.allowSurplus) {
            plan.beeSurplus * beeValue + plan.wormSurplus * wormValue
        } else 0
        if (dicePower + ordinaryCritterPower >= item.cost) return true

        if (!isPremium(item)) return false
        val allCritterPower = context.self.board.bees * beeValue + context.self.board.worms * wormValue
        return dicePower + allCritterPower >= item.cost
    }

    private fun paymentAllowed(
        request: ChoosePaymentRequest,
        payment: BuyPayment,
        plan: CritterSpendPlan
    ): Boolean {
        if (payment.critters.isEmpty()) return true
        if (isPremium(request.item)) return true
        if (!plan.allowSurplus) return false
        val beesSpent = payment.critters.count { it.critter == Critter.BEE }
        val wormsSpent = payment.critters.count { it.critter == Critter.WORM }
        return beesSpent <= plan.beeSurplus && wormsSpent <= plan.wormSurplus
    }

    private fun isPremium(item: BuyItem): Boolean =
        when (item) {
            is BuyItem.Die -> item.cost == PREMIUM_DIE_COST
            is BuyItem.Plant -> item.card.type == PlantType.FLOWER && item.cost == PREMIUM_FLOWER_COST
        }

    private fun preferCritterTypeWhenRequired(payments: List<BuyPayment>): List<BuyPayment> {
        if (payments.any { it.critters.isEmpty() }) return payments
        val beePayments = payments.filter { p -> p.critters.any { it.critter == Critter.BEE } }
        val wormPayments = payments.filter { p -> p.critters.any { it.critter == Critter.WORM } }
        if (beePayments.isEmpty() || wormPayments.isEmpty()) return payments

        val preferBee = strategyRandomizer.nextInt(100) < BEE_PREFERENCE_PERCENTAGE
        return if (preferBee) beePayments else wormPayments
    }

    private fun clearPendingPlan() {
        pendingItem = null
        pendingPlan = null
    }

    /** Enumerate complete sufficient payments; never add resources after a payment is already sufficient. */
    private fun enumeratePayments(request: ChoosePaymentRequest): List<BuyPayment> {
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
        return payments
    }

    private data class Resource(
        val die: BuyDieResource? = null,
        val critter: BuyCritterResource? = null,
        val value: Int
    )
}
