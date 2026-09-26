package dugsolutions.leaf.v35.player.decision.baseline.buy

import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import dugsolutions.leaf.v35.player.decision.baseline.common.GraftTopologyEvaluator
import dugsolutions.leaf.v35.player.decision.buy.BuyChoice
import dugsolutions.leaf.v35.player.decision.buy.BuyCritterResource
import dugsolutions.leaf.v35.player.decision.buy.BuyDieResource
import dugsolutions.leaf.v35.player.decision.buy.BuyItem
import dugsolutions.leaf.v35.player.decision.buy.BuyPayment
import dugsolutions.leaf.v35.player.decision.buy.ChoosePurchaseRequest
import dugsolutions.leaf.v35.player.decision.context.CreatureCardView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.tokens.Critter

/**
 * Short-horizon Human Baseline Buy look-ahead.
 *
 * The planner deliberately reasons about the entire current Hand instead of
 * greedily committing every die to one expensive purchase. Each look-ahead step:
 *
 * 1. chooses Plant-vs-die with the normal Human Baseline balance policy;
 * 2. tries every unique non-empty group of remaining Hand dice;
 * 3. assigns that group a plausible purchase in the chosen category;
 * 4. optionally spends one Bee, or more rarely one surplus Worm, to bridge
 *    that purchase up one useful tier;
 * 5. plans at most one more purchase from the remaining dice.
 *
 * One Bee-willingness roll and one Worm-willingness roll are sampled once for
 * the whole planning pass. Every candidate grouping sees those same rolls, so
 * alternatives are compared against one coherent level of Critter willingness
 * rather than receiving independent lottery tickets.
 *
 * Plans minimize an exponential overpay penalty first, then maximize purchased
 * cost. This makes e.g. `5+5+5 -> V11` (overpay 4) naturally lose to
 * `5 -> R5; 5+5 -> D10` (overpay 0) without hard-coding that example.
 *
 * Bought dice are projected into Discard and bought Plants into a hypothetical
 * Creature, so the existing Plant-vs-dice balance and future graft legality can
 * change during look-ahead. The mutable Game/Grove is never exposed here.
 */
internal class HumanBaselineBuyPlanner(
    private val policy: HumanBaselinePolicy,
    private val cardScorers: HumanBaselineCardScorerRegistry,
    private val purchaseScoreModifier: PurchaseScoreModifier,
    private val strategyRandomizer: StrategyRandomizer
) {
    data class PlannedStep(
        val item: BuyItem,
        val payment: BuyPayment
    )

    private data class Plan(
        val steps: List<PlannedStep>,
        val penalty: Int,
        val purchasedCost: Int,
        val beesSpent: Int,
        val wormsSpent: Int
    )

    private data class Market(
        val plantsByName: Map<String, PlantCard>,
        val plantRemaining: Map<String, Int>,
        val dieRemaining: Map<DieSides, Int>
    )

    private data class State(
        val context: DecisionContext,
        val hand: List<DieView>,
        val market: Market
    )

    private enum class Category { PLANT, DIE }

    private val categoryRolls = mutableMapOf<Int, Int>()
    private val plantTierRolls = mutableMapOf<Int, Int>()
    private var sharedBeeWillingnessRoll: Int? = null
    private var sharedWormWillingnessRoll: Int? = null

    fun plan(request: ChoosePurchaseRequest): List<PlannedStep> {
        categoryRolls.clear()
        plantTierRolls.clear()
        sharedBeeWillingnessRoll = null
        sharedWormWillingnessRoll = null

        if (request.context == DecisionContext.EMPTY || request.context.self.board.hand.isEmpty()) {
            return emptyList()
        }
        // Critter willingness is sampled once before candidate groupings are
        // evaluated. Every branch in this planning pass reuses the same roll.
        if (request.context.self.board.bees > 0) {
            sharedBeeWillingnessRoll = strategyRandomizer.nextInt(100)
        }
        if (request.context.self.board.worms >= 2) {
            sharedWormWillingnessRoll = strategyRandomizer.nextInt(100)
        }
        // Normal games are far below this. The guard prevents an accidental
        // pathological Hand from turning exhaustive subset look-ahead into an
        // exponential simulation bottleneck.
        if (request.context.self.board.hand.size > MAX_PLANNED_HAND_DICE) return emptyList()

        val market = initialMarket(request)
        val state = State(
            context = request.context,
            hand = request.context.self.board.hand,
            market = market
        )
        val plan = bestPlan(state, depth = 0)
        if (plan.steps.isEmpty()) return emptyList()

        // The first projected purchase must still be one the rules engine
        // actually offered right now. Future projected purchases are rechecked
        // against fresh coordinator options before they are executed.
        return if (plan.steps.first().item in request.options) plan.steps else emptyList()
    }

    private fun bestPlan(state: State, depth: Int): Plan {
        if (state.hand.isEmpty() || depth >= MAX_PLANNED_PURCHASES) {
            return Plan(emptyList(), 0, 0, 0, 0)
        }

        val fullBudget = state.hand.sumOf { it.value }
        val category = chooseCategory(state, depth, fullBudget)
            ?: return Plan(
                steps = emptyList(),
                penalty = state.hand.sumOf { it.value },
                purchasedCost = 0,
                beesSpent = 0,
                wormsSpent = 0
            )

        val candidates = mutableListOf<Plan>()
        val seenGroups = mutableSetOf<String>()
        val n = state.hand.size
        val maskLimit = 1 shl n
        for (mask in 1 until maskLimit) {
            val group = buildList {
                for (index in 0 until n) {
                    if ((mask and (1 shl index)) != 0) add(state.hand[index])
                }
            }
            // Identical dice are interchangeable for Buy planning. Deduping by
            // their visible sides/value multiset dramatically cuts symmetric branches.
            val signature = group
                .map { it.sides to it.value }
                .sortedWith(compareBy<Pair<Int, Int>> { it.first }.thenBy { it.second })
                .joinToString("|") { "${it.first}:${it.second}" }
            if (!seenGroups.add(signature)) continue

            val budget = group.sumOf { it.value }
            val step = chooseStepForGroup(state, category, group, budget, depth) ?: continue
            val nextState = applyStep(state, group, step) ?: continue
            val remainder = bestPlan(nextState, depth + 1)
            val overpay = step.payment.total - step.item.cost
            val candidate = Plan(
                steps = listOf(step) + remainder.steps,
                penalty = policy.buyOverpayPenalty(state.context, overpay) + remainder.penalty,
                purchasedCost = step.item.cost + remainder.purchasedCost,
                beesSpent = step.payment.critters.count { it.critter == Critter.BEE } + remainder.beesSpent,
                wormsSpent = step.payment.critters.count { it.critter == Critter.WORM } + remainder.wormsSpent
            )
            candidates += candidate
        }

        if (candidates.isEmpty()) {
            return Plan(
                steps = emptyList(),
                penalty = state.hand.sumOf { it.value },
                purchasedCost = 0,
                beesSpent = 0,
                wormsSpent = 0
            )
        }

        return candidates.minWithOrNull(
            compareBy<Plan> { it.penalty }
                .thenByDescending { it.purchasedCost }
                .thenByDescending { plan ->
                    if (category == Category.DIE) {
                        (plan.steps.firstOrNull()?.item as? BuyItem.Die)?.cost ?: 0
                    } else {
                        0
                    }
                }
                .thenBy { it.wormsSpent }
                .thenBy { it.beesSpent }
                .thenByDescending { it.steps.size }
        )!!
    }

    private fun chooseCategory(state: State, depth: Int, budget: Int): Category? {
        val plants = legalPlants(state).filter { it.cost <= budget }
        val dice = availableDice(state).filter { it.cost <= budget }
        if (plants.isEmpty() && dice.isEmpty()) return null
        if (plants.isEmpty()) return Category.DIE
        if (dice.isEmpty()) return Category.PLANT

        val roll = categoryRoll(depth)
        val lowPlantPercentage = policy.lowPlantPriorityPercentage(state.context)
        if (lowPlantPercentage > 0) {
            return if (roll < lowPlantPercentage) Category.PLANT else Category.DIE
        }

        val plantPercentage = policy.buyPlantPriorityPercentage(state.context)
        return when {
            plantPercentage <= 0 -> Category.DIE
            plantPercentage >= 100 -> Category.PLANT
            roll < plantPercentage -> Category.PLANT
            else -> Category.DIE
        }
    }

    private fun chooseStepForGroup(
        state: State,
        category: Category,
        group: List<DieView>,
        budget: Int,
        depth: Int
    ): PlannedStep? = when (category) {
        Category.DIE -> chooseDieStep(state, group, budget, depth)
        Category.PLANT -> choosePlantStep(state, group, budget, depth)
    }

    private fun chooseDieStep(
        state: State,
        group: List<DieView>,
        budget: Int,
        depth: Int
    ): PlannedStep? {
        val affordable = availableDice(state).filter { it.cost <= budget }
        if (affordable.isEmpty()) return null
        var item = affordable.maxBy { it.cost }
        var critter: BuyCritterResource? = null

        val next = nextDie(item.sides)
        if (next != null && (state.market.dieRemaining[next] ?: 0) > 0 && next.value > budget) {
            val beeCount = state.context.self.board.bees
            val beeGapReachable = beeCount > 0 &&
                next.value <= budget + state.context.self.board.beeValue
            if (beeGapReachable) {
                val chance = policy.buyBeeUpgradeDiePercentage(state.context, item.sides, beeCount)
                if (chance > 0 && beeWillingnessRoll() < chance) {
                    item = BuyItem.Die(next)
                    critter = BuyCritterResource(Critter.BEE, state.context.self.board.beeValue)
                }
            }

            if (critter == null &&
                next.value == budget + 1 &&
                state.context.self.board.worms >= 2
            ) {
                val chance = policy.buyWormBridgePercentage(
                    state.context,
                    state.context.self.board.worms
                )
                if (chance > 0 && wormWillingnessRoll() < chance) {
                    item = BuyItem.Die(next)
                    critter = BuyCritterResource(Critter.WORM, state.context.self.board.wormValue)
                }
            }
        }

        return PlannedStep(
            item = item,
            payment = BuyPayment(
                dice = group.map { BuyDieResource(it.sides, it.value) },
                critters = listOfNotNull(critter)
            )
        )
    }

    private fun choosePlantStep(
        state: State,
        group: List<DieView>,
        budget: Int,
        depth: Int
    ): PlannedStep? {
        val legal = legalPlants(state)
        val affordable = legal.filter { it.cost <= budget }
        if (affordable.isEmpty()) return null

        val costs = affordable.map { it.cost }.distinct().sorted()
        val selectedCost = chooseWeightedPlantCost(state.context, costs, depth)
        var item = bestPlantAtCost(state.context, affordable.filter { it.cost == selectedCost })
        var critter: BuyCritterResource? = null

        // Critters bridge a genuine budget gap; they do not upgrade a deliberately
        // cheap Plant selected by the cost-tier randomizer. Thus Critter use is
        // considered only when the selected Plant is already the best tier the
        // dice group can naturally afford.
        val maxAffordableCost = affordable.maxOf { it.cost }
        if (selectedCost == maxAffordableCost) {
            val beeCount = state.context.self.board.bees
            if (beeCount > 0) {
                val beeTargetCost = legal
                    .asSequence()
                    .map { it.cost }
                    .distinct()
                    .filter { it > budget && it <= budget + state.context.self.board.beeValue }
                    .minOrNull()
                if (beeTargetCost != null) {
                    val chance = policy.buyBeeUpgradePlantPercentage(state.context, beeCount)
                    if (chance > 0 && beeWillingnessRoll() < chance) {
                        item = bestPlantAtCost(
                            state.context,
                            legal.filter { it.cost == beeTargetCost }
                        )
                        critter = BuyCritterResource(Critter.BEE, state.context.self.board.beeValue)
                    }
                }
            }

            if (critter == null && state.context.self.board.worms >= 2) {
                val wormTargetCost = budget + 1
                val wormTier = legal.filter { it.cost == wormTargetCost }
                if (wormTier.isNotEmpty()) {
                    val chance = policy.buyWormBridgePercentage(
                        state.context,
                        state.context.self.board.worms
                    )
                    if (chance > 0 && wormWillingnessRoll() < chance) {
                        item = bestPlantAtCost(state.context, wormTier)
                        critter = BuyCritterResource(Critter.WORM, state.context.self.board.wormValue)
                    }
                }
            }
        }

        return PlannedStep(
            item = item,
            payment = BuyPayment(
                dice = group.map { BuyDieResource(it.sides, it.value) },
                critters = listOfNotNull(critter)
            )
        )
    }

    private fun chooseWeightedPlantCost(
        context: DecisionContext,
        costsAscending: List<Int>,
        depth: Int
    ): Int {
        if (costsAscending.size == 1) return costsAscending.single()
        val weights = costsAscending.mapIndexed { index, cost ->
            cost to policy.buyPlantCostTierWeight(context, index)
        }
        val total = weights.sumOf { it.second }
        var roll = ((plantTierRoll(depth).toLong() * total) / PLANT_TIER_ROLL_SCALE)
            .toInt()
            .coerceAtMost(total - 1)
        weights.forEach { (cost, weight) ->
            if (roll < weight) return cost
            roll -= weight
        }
        return weights.last().first
    }

    private fun bestPlantAtCost(context: DecisionContext, plants: List<BuyItem.Plant>): BuyItem.Plant {
        require(plants.isNotEmpty()) { "Plant selection requires candidates" }
        return plants.maxWithOrNull(
            compareBy<BuyItem.Plant> { plant ->
                val baseline = PurchasePriority.score(context, plant, cardScorers)
                purchaseScoreModifier.modify(context, plant, baseline).total
            }.thenBy { it.card.name }
        )!!
    }

    private fun legalPlants(state: State): List<BuyItem.Plant> =
        state.market.plantsByName.values
            .asSequence()
            .filter { (state.market.plantRemaining[it.name] ?: 0) > 0 }
            .filter {
                GraftTopologyEvaluator.legalPlacements(
                    state.context.self.board.creature,
                    it.type
                ).isNotEmpty()
            }
            .map { BuyItem.Plant(it) }
            .toList()

    private fun availableDice(state: State): List<BuyItem.Die> =
        DieSides.entries
            .filter { (state.market.dieRemaining[it] ?: 0) > 0 }
            .map { BuyItem.Die(it) }

    private fun applyStep(
        state: State,
        paidDice: List<DieView>,
        step: PlannedStep
    ): State? {
        val paidIndices = paidDice.map { it.index }.toSet()
        val remainingHand = state.hand.filterNot { it.index in paidIndices }
        var board = state.context.self.board.copy(
            hand = remainingHand,
            discard = state.context.self.board.discard + paidDice,
            bees = state.context.self.board.bees - step.payment.critters.count { it.critter == Critter.BEE },
            worms = state.context.self.board.worms - step.payment.critters.count { it.critter == Critter.WORM }
        )
        var market = state.market
        var grove = state.context.grove

        when (val item = step.item) {
            is BuyItem.Die -> {
                val remaining = (market.dieRemaining[item.sides] ?: 0) - 1
                if (remaining < 0) return null
                market = market.copy(dieRemaining = market.dieRemaining + (item.sides to remaining))
                grove = grove.copy(graftBed = grove.graftBed + (item.sides to remaining))
                val nextIndex = (board.supply + board.hand + board.discard)
                    .maxOfOrNull { it.index }
                    ?.plus(1) ?: 0
                board = board.copy(
                    discard = board.discard + DieView(nextIndex, item.sides.value, 1)
                )
            }

            is BuyItem.Plant -> {
                val remaining = (market.plantRemaining[item.card.name] ?: 0) - 1
                if (remaining < 0) return null
                val creature = graftHypothetically(board.creature, item.card) ?: return null
                board = board.copy(creature = creature)
                market = market.copy(plantRemaining = market.plantRemaining + (item.card.name to remaining))
                grove = grove.copy(
                    plantStacks = grove.plantStacks.map { stack ->
                        if (stack.name == item.card.name) stack.copy(remaining = remaining) else stack
                    }
                )
            }
        }

        val spentBees = step.payment.critters.count { it.critter == Critter.BEE }
        val spentWorms = step.payment.critters.count { it.critter == Critter.WORM }
        if (spentBees > 0 || spentWorms > 0) {
            grove = grove.copy(
                bees = grove.bees + spentBees,
                worms = grove.worms + spentWorms
            )
        }

        return State(
            context = state.context.copy(
                self = state.context.self.copy(board = board),
                grove = grove
            ),
            hand = remainingHand,
            market = market
        )
    }

    private fun graftHypothetically(
        creature: List<CreatureCardView>,
        card: PlantCard
    ): List<CreatureCardView>? {
        val placements = GraftTopologyEvaluator.legalPlacements(creature, card.type)
        if (placements.isEmpty()) return null
        val placement = placements.maxByOrNull { candidate ->
            GraftTopologyEvaluator.futureGrowthSlotsAfter(creature, card.type, candidate)
        } ?: return null
        val nextId = (creature.maxOfOrNull { it.id.value } ?: 0) + 1
        return creature + CreatureCardView(
            id = CreatureCardId(nextId),
            name = card.name,
            title = card.title,
            type = card.type,
            cost = card.cost,
            effect = card.effect,
            scoringRule = card.scoringRule,
            side = placement.side,
            position = placement.position,
            facing = CreatureCard.Facing.FACE_DOWN,
            isSnippable = true
        )
    }

    private fun initialMarket(request: ChoosePurchaseRequest): Market {
        val plants = request.marketOptions.filterIsInstance<BuyItem.Plant>()
        val dice = request.marketOptions.filterIsInstance<BuyItem.Die>()
        val stackCounts = request.context.grove.plantStacks.associate { it.name to it.remaining }
        val dieCounts = request.context.grove.graftBed
        return Market(
            plantsByName = plants.associate { it.card.name to it.card },
            plantRemaining = plants.associate { plant ->
                plant.card.name to (stackCounts[plant.card.name] ?: 1)
            },
            dieRemaining = dice.associate { die ->
                die.sides to (dieCounts[die.sides] ?: 1)
            }
        )
    }

    private fun nextDie(sides: DieSides): DieSides? = when (sides) {
        DieSides.D4 -> DieSides.D6
        DieSides.D6 -> DieSides.D8
        DieSides.D8 -> DieSides.D10
        DieSides.D10 -> DieSides.D12
        DieSides.D12, DieSides.D20 -> null
    }

    private fun categoryRoll(depth: Int): Int =
        categoryRolls.getOrPut(depth) { strategyRandomizer.nextInt(100) }

    private fun plantTierRoll(depth: Int): Int =
        plantTierRolls.getOrPut(depth) { strategyRandomizer.nextInt(PLANT_TIER_ROLL_SCALE) }

    private fun beeWillingnessRoll(): Int =
        sharedBeeWillingnessRoll ?: 100

    private fun wormWillingnessRoll(): Int =
        sharedWormWillingnessRoll ?: 100

    companion object {
        private const val MAX_PLANNED_HAND_DICE = 9
        private const val MAX_PLANNED_PURCHASES = 2
        private const val PLANT_TIER_ROLL_SCALE = 1_000_000
    }
}
