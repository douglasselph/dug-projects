package dugsolutions.leaf.v35.player.decision.baseline.buy

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
 * 1. Continue buying while legal/affordable purchases remain. The BuyCoordinator
 *    re-enters this strategy after each committed purchase with fresh Hand,
 *    Creature, and Grove state.
 * 2. First decide whether development balance calls for a Plant or a die. Buy
 *    balance compares total owned die sides against grafted Plant count valued
 *    at 15 points per Plant. When both categories are affordable, an injected
 *    policy converts that visible imbalance into a probability.
 * 3. Within the die category, buy the most expensive affordable die.
 * 4. Within the Plant category, prefer expensive cost tiers exponentially rather
 *    than always choosing the maximum. A deliberately cheaper-than-maximum Plant
 *    is considered only when a minimum-overpay payment can leave at least the
 *    policy's remaining Hand-dice reserve (normally 5), preserving a plausible
 *    follow-up purchase. Otherwise Human Baseline falls back to the maximum tier.
 * 5. Within a selected Plant cost tier, ordinary card acquire scoring may break
 *    same-cost choices; duplicate Plants receive no generic penalty.
 * 6. Buy normally preserves the Critter reserve supplied by
 *    [HumanBaselinePolicy.protectedCritterReserve]. Surplus Critters are treated as available
 *    purchasing power probabilistically. For surplus > 0, the probability is
 *    [CRITTER_SPEND_STARTING_PERCENTAGE] +
 *    [CRITTER_SPEND_INCREMENT_PER_SURPLUS] * surplus, capped at 100%. Surplus
 *    0 therefore remains 0%; the starting percentage is an offset once surplus
 *    exists. There is no special D20/F17 reserve exception.
 * 7. Payment always minimizes overpay first. Among equal-overpay payments, the
 *    normal reserve/fewer-resources scoring applies.
 * 8. When a Critter is actually required and either type can make an equal-overpay
 *    payment, prefer a Bee [BEE_PREFERENCE_PERCENTAGE]% of the time because Worms
 *    have the additional Flip use.
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
    }

    private data class CritterSpendPlan(
        val allowSurplus: Boolean,
        val beeSurplus: Int,
        val wormSurplus: Int
    )

    private var pendingItem: BuyItem? = null
    private var pendingPlan: CritterSpendPlan? = null
    private var pendingMinimumRemainingDice: Int? = null

    override fun choosePurchase(request: ChoosePurchaseRequest): BuyChoice {
        if (request.context == DecisionContext.EMPTY) return delegate.choosePurchase(request)
        if (request.options.isEmpty()) {
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
        val selection = when {
            categoryCandidates.firstOrNull() is BuyItem.Die ->
                PurchaseSelection(
                    choice = chooseHighestCostTier(request.context, categoryCandidates),
                    minimumRemainingDice = null
                )

            else -> choosePlantPurchase(
                context = request.context,
                plants = categoryCandidates.filterIsInstance<BuyItem.Plant>(),
                plan = plan
            )
        }

        val purchased = (selection.choice as BuyChoice.Purchase).item
        pendingItem = purchased
        pendingPlan = plan
        pendingMinimumRemainingDice = selection.minimumRemainingDice
        return selection.choice
    }

    override fun choosePayment(request: ChoosePaymentRequest): BuyPayment {
        if (request.context == DecisionContext.EMPTY) return delegate.choosePayment(request)

        val matchedPendingItem = pendingItem == request.item
        val plan = if (matchedPendingItem) {
            pendingPlan ?: createCritterSpendPlan(request.context)
        } else {
            createCritterSpendPlan(request.context)
        }
        val minimumRemainingDice = if (matchedPendingItem) pendingMinimumRemainingDice else null
        clearPendingPlan()

        val permitted = enumeratePayments(request).filter { paymentAllowed(it, plan) }
        // Production Human Baseline should never need this fallback because choosePurchase
        // applies the same plan. It keeps direct/legacy callers legal rather than returning
        // an invalid payment if they invoke choosePayment in isolation.
        val payments = permitted.ifEmpty { enumeratePayments(request) }
        if (payments.isEmpty()) return BuyPayment()

        val minimumOverpay = payments.minOf { it.total - request.cost }
        val minimumOverpayPayments = payments.filter { it.total - request.cost == minimumOverpay }
        val reserveAwarePayments = minimumRemainingDice?.let { minimum ->
            minimumOverpayPayments.filter { payment ->
                remainingDicePower(request, payment) >= minimum
            }.ifEmpty { minimumOverpayPayments }
        } ?: minimumOverpayPayments

        val preferredPayments = preferCritterTypeWhenRequired(reserveAwarePayments)
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

    private data class PurchaseSelection(
        val choice: BuyChoice,
        val minimumRemainingDice: Int?
    )

    private fun choosePlantPurchase(
        context: DecisionContext,
        plants: List<BuyItem.Plant>,
        plan: CritterSpendPlan
    ): PurchaseSelection {
        require(plants.isNotEmpty()) { "Plant purchase selection requires candidates" }

        val maxCost = plants.maxOf { it.cost }
        val minimumRemainingDice = policy.buyCheaperPlantMinimumRemainingDice(context)
        val allCosts = plants.map { it.cost }.distinct().sorted()
        val weightedCost = chooseWeightedPlantCostTier(context, allCosts)
        val selectedCost = if (weightedCost < maxCost) {
            val representative = plants.first { it.cost == weightedCost }
            if (canLeaveMinimumDiceAfterPurchase(
                    context = context,
                    item = representative,
                    plan = plan,
                    minimumRemainingDice = minimumRemainingDice
                )
            ) weightedCost else maxCost
        } else {
            maxCost
        }
        val tier = plants.filter { it.cost == selectedCost }
        val choice = chooseHighestCostTier(context, tier)
        return PurchaseSelection(
            choice = choice,
            minimumRemainingDice = if (selectedCost < maxCost) minimumRemainingDice else null
        )
    }

    private fun chooseWeightedPlantCostTier(
        context: DecisionContext,
        costsAscending: List<Int>
    ): Int {
        require(costsAscending.isNotEmpty()) { "Plant cost-tier selection requires costs" }
        if (costsAscending.size == 1) return costsAscending.single()

        val weighted = costsAscending.mapIndexed { index, cost ->
            cost to policy.buyPlantCostTierWeight(context, index)
        }
        val totalWeight = weighted.sumOf { it.second }
        var roll = strategyRandomizer.nextInt(totalWeight)
        weighted.forEach { (cost, weight) ->
            if (roll < weight) return cost
            roll -= weight
        }
        return weighted.last().first
    }

    private fun chooseHighestCostTier(
        context: DecisionContext,
        items: List<BuyItem>
    ): BuyChoice {
        require(items.isNotEmpty()) { "Purchase selection requires candidates" }
        val highestCost = items.maxOf { it.cost }
        val tier = items.filter { it.cost == highestCost }
        return scoreEngine.chooseValue(
            context = context,
            candidates = tier.map { item ->
                val baselineScore = PurchasePriority.score(
                    context = context,
                    item = item,
                    cardScorers = cardScorers
                )
                DecisionCandidate<BuyChoice>(
                    choice = BuyChoice.Purchase(item),
                    score = purchaseScoreModifier.modify(
                        context = context,
                        item = item,
                        score = baselineScore
                    )
                )
            },
            influenceRegistry = influenceRegistry
        )
    }

    private fun canLeaveMinimumDiceAfterPurchase(
        context: DecisionContext,
        item: BuyItem.Plant,
        plan: CritterSpendPlan,
        minimumRemainingDice: Int
    ): Boolean {
        val request = ChoosePaymentRequest(
            item = item,
            availableDice = context.self.board.hand.map { BuyDieResource(it.sides, it.value) },
            availableCritters = buildList {
                repeat(context.self.board.bees) {
                    add(BuyCritterResource(Critter.BEE, context.self.board.beeValue))
                }
                repeat(context.self.board.worms) {
                    add(BuyCritterResource(Critter.WORM, context.self.board.wormValue))
                }
            },
            context = context
        )
        val permitted = enumeratePayments(request).filter { paymentAllowed(it, plan) }
        if (permitted.isEmpty()) return false
        val minimumOverpay = permitted.minOf { it.total - item.cost }
        return permitted
            .asSequence()
            .filter { it.total - item.cost == minimumOverpay }
            .any { remainingDicePower(request, it) >= minimumRemainingDice }
    }

    private fun remainingDicePower(request: ChoosePaymentRequest, payment: BuyPayment): Int =
        request.availableDice.sumOf { it.value } - payment.dice.sumOf { it.value }

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
        return dicePower + ordinaryCritterPower >= item.cost
    }

    private fun paymentAllowed(
        payment: BuyPayment,
        plan: CritterSpendPlan
    ): Boolean {
        if (payment.critters.isEmpty()) return true
        if (!plan.allowSurplus) return false
        val beesSpent = payment.critters.count { it.critter == Critter.BEE }
        val wormsSpent = payment.critters.count { it.critter == Critter.WORM }
        return beesSpent <= plan.beeSurplus && wormsSpent <= plan.wormSurplus
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
        pendingMinimumRemainingDice = null
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
