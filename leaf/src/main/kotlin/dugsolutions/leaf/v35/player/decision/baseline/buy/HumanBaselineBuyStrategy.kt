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
 * Human Baseline Buy behavior: recognizable ordinary shopping with short
 * multi-buy look-ahead rather than a one-item greedy rule.
 *
 * Purchase contract:
 * 1. Continue buying while a legal purchase remains. BuyCoordinator re-enters
 *    the strategy after every committed purchase with fresh state.
 * 2. Plant-vs-die category choice still comes from [HumanBaselinePolicy]: the
 *    low-Plant safety tendency first, then current Plant-power vs owned-dice
 *    power. It is independent of round number.
 * 3. [HumanBaselineBuyPlanner] partitions the current Hand into plausible Buy
 *    groups and looks ahead at no more than two purchases at a time. Whole
 *    plans are preferred by low total overpay penalty, then by total value
 *    purchased. Overpay concern grows exponentially from 2 upward.
 * 4. For a die group, the normal item is the most expensive die that group can
 *    buy. Equal-quality complete plans prefer the larger first die.
 * 5. Plant tiers retain the exponential expensive-card preference, but the
 *    recursive grouping can make a cheaper Plant attractive when it creates a
 *    much cleaner multi-buy sequence.
 * 6. A Bee may be spent even from inside the normal two-Bee reserve when it
 *    bridges one otherwise-limited purchase to the next useful tier. Die
 *    upgrade odds rise with die size: spending a lone Bee on D4 -> D6 is rare,
 *    while D10 -> D12 is much more tempting. Plant bridges use a lower flat
 *    starting chance. More owned Bees multiply the odds.
 * 7. Two or more Worms may very occasionally supply a one-point Buy bridge.
 *    Worm willingness is much lower than Bee willingness and never bridges a
 *    gap larger than one.
 * 8. One Bee-willingness roll and one Worm-willingness roll are shared by all
 *    candidate groupings in the same planner pass. Candidates do not receive
 *    separate random lottery tickets.
 * 9. The planner projects Grove supply and graft topology, so buying a Vine can
 *    make a Flower a legal later purchase in the same Buy phase. Every cached
 *    projected step is revalidated against the real coordinator options before
 *    use; any mismatch discards the remainder and replans.
 * 10. Legacy minimum-overpay/Critter logic remains as a fallback for unusual
 *    states the dice-partition planner intentionally does not optimize (for
 *    example a Critter-only purchase).
 *
 * All Human Baseline randomness uses [StrategyRandomizer], never the mechanical
 * RNG used for dice, decks, or other game state.
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

    private val buyPlanner = HumanBaselineBuyPlanner(
        policy = policy,
        cardScorers = cardScorers,
        purchaseScoreModifier = purchaseScoreModifier,
        strategyRandomizer = strategyRandomizer
    )
    private val plannedSteps = ArrayDeque<HumanBaselineBuyPlanner.PlannedStep>()
    private var pendingPlannedStep: HumanBaselineBuyPlanner.PlannedStep? = null

    override fun choosePurchase(request: ChoosePurchaseRequest): BuyChoice {
        if (request.context == DecisionContext.EMPTY) return delegate.choosePurchase(request)
        if (request.purchasesMadeThisBuy == 0) clearPlannedSequence()
        if (request.options.isEmpty()) {
            clearPendingPlan()
            clearPlannedSequence()
            return BuyChoice.Done
        }

        val cached = plannedSteps.firstOrNull()
        if (cached != null && plannedStepStillAvailable(cached, request)) {
            pendingPlannedStep = cached
            pendingItem = cached.item
            pendingPlan = null
            pendingMinimumRemainingDice = null
            return BuyChoice.Purchase(cached.item)
        }
        if (cached != null) clearPlannedSequence()

        val projected = buyPlanner.plan(request)
        if (projected.isNotEmpty()) {
            plannedSteps.addAll(projected)
            val first = plannedSteps.first()
            pendingPlannedStep = first
            pendingItem = first.item
            pendingPlan = null
            pendingMinimumRemainingDice = null
            return BuyChoice.Purchase(first.item)
        }

        // Fallback retains legacy Critter-only/surplus behavior for unusual
        // states the dice-partition planner intentionally does not optimize.
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

        val planned = pendingPlannedStep
        if (planned != null && planned.item == request.item && plannedPaymentStillAvailable(planned.payment, request)) {
            pendingPlannedStep = null
            if (plannedSteps.firstOrNull() == planned) plannedSteps.removeFirst()
            clearPendingPlan()
            return planned.payment
        }
        if (planned != null) clearPlannedSequence()

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

    private fun clearPlannedSequence() {
        plannedSteps.clear()
        pendingPlannedStep = null
    }

    private fun plannedStepStillAvailable(
        step: HumanBaselineBuyPlanner.PlannedStep,
        request: ChoosePurchaseRequest
    ): Boolean =
        step.item in request.options &&
            containsDiceResources(request.context.self.board.hand.map { BuyDieResource(it.sides, it.value) }, step.payment.dice) &&
            request.context.self.board.bees >= step.payment.critters.count { it.critter == Critter.BEE } &&
            request.context.self.board.worms >= step.payment.critters.count { it.critter == Critter.WORM }

    private fun plannedPaymentStillAvailable(
        payment: BuyPayment,
        request: ChoosePaymentRequest
    ): Boolean =
        containsDiceResources(request.availableDice, payment.dice) &&
            containsCritterResources(request.availableCritters, payment.critters) &&
            payment.total >= request.cost

    private fun containsDiceResources(
        available: List<BuyDieResource>,
        required: List<BuyDieResource>
    ): Boolean {
        val remaining = available.toMutableList()
        return required.all { remaining.remove(it) }
    }

    private fun containsCritterResources(
        available: List<BuyCritterResource>,
        required: List<BuyCritterResource>
    ): Boolean {
        val remaining = available.toMutableList()
        return required.all { remaining.remove(it) }
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
