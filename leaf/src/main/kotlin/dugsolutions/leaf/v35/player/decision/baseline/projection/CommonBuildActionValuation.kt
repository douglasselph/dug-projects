package dugsolutions.leaf.v35.player.decision.baseline.projection

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.common.PurchaseThresholdHeuristics
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.cultivation.ChooseCultivationActionRequest
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationAction
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationMainAction
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.round.domain.RoundCardType

/**
 * A comparison of *pip-equivalent consequences*, not a decision policy.
 *
 * Only spending pips can buy a Plant or win a Strike. Critter/Wisp values are
 * provisional strategic equivalents, and purchase thresholds/Refresh options
 * cannot be inferred from a simple pip total. A partial result is informative,
 * but MUST NOT silently replace the live Human Baseline scorer.
 */
enum class BuildValueCoverage {
    /** This action's modeled effect is wholly represented within the chosen pip model. */
    PIP_MODEL,
    /** Pip components are measured; relevant non-pip consequences are not priced. */
    PARTIAL,
    /** No honest common-scale estimate is currently available. */
    UNMODELED
}

data class BuildActionValue(
    val choice: CultivationAction.Main,
    val description: String,
    val targetHandIndex: Int?,
    val immediate: ImmediatePipValue?,
    val futureSpendingPipChange: Double?,
    val futureRewardEquivalentChange: Double?,
    val currentBuyThresholdsPossiblyCrossed: List<Int>,
    val coverage: BuildValueCoverage,
    val unmodeledConsequences: List<String>
) {
    /** Null is deliberate: an unmodeled action must never look like a zero-value action. */
    val totalPipEquivalentChange: Double?
        get() = if (immediate == null || futureSpendingPipChange == null || futureRewardEquivalentChange == null) {
            null
        } else {
            immediate.pipEquivalentTotal + futureSpendingPipChange + futureRewardEquivalentChange
        }

    val explanation: String
        get() = if (coverage == BuildValueCoverage.UNMODELED) {
            "$description: not modeled; ${unmodeledConsequences.joinToString("; ")}"
        } else {
            buildString {
                append("$description: now ${immediate!!.spendingPips} spending pips")
                append(" + ${immediate.critterOptionValue + immediate.wispOptionValue} reward equivalents")
                append("; future ${futureSpendingPipChange} spending pips")
                append(" + ${futureRewardEquivalentChange} reward equivalents")
                append("; combined $totalPipEquivalentChange pip-equivalents")
                if (currentBuyThresholdsPossiblyCrossed.isNotEmpty()) {
                    append("; possible Buy thresholds ${currentBuyThresholdsPossiblyCrossed.joinToString()}")
                }
                if (unmodeledConsequences.isNotEmpty()) {
                    append("; NOT PRICED: ${unmodeledConsequences.joinToString("; ")}")
                }
            }
        }
}

data class BuildActionComparison(
    val actions: List<BuildActionValue>,
    val futureDrawAssumption: CultivationDrawAssumption
) {
    /** Numerical ordering only: NOT an instruction to choose an action. */
    val pipOnlyOrder: List<BuildActionValue>
        get() = actions.filter { it.totalPipEquivalentChange != null }
            .sortedByDescending { it.totalPipEquivalentChange }

    /** Full-policy selection requires explicit valuation of all relevant consequences. */
    val readyToChoose: Boolean
        get() = actions.isNotEmpty() && actions.all { it.coverage == BuildValueCoverage.PIP_MODEL }
}

/**
 * Read-only bridge from actual legal Cultivation Main choices to shared expected
 * immediate benefit plus the CHANGE in the strictly-future dice forecast.
 *
 * Draws move physical dice into a projected Hand; Compost removes a physical
 * Hand die and puts the next-size replacement in projected Discard; a Raise
 * changes only the showing current face. The same future Draw budget is used
 * on both sides of every comparison. No live dice, policy scores or RNG change.
 *
 * In this stage resource opportunity value, purchases, Plant Refresh, and
 * conditional tactical effects are intentionally unmodeled. Those are explicit
 * omissions rather than unjustified bonuses or arbitrary fixed bases.
 */
object CommonBuildActionValuation {
    private val upgradeSteps = mapOf(4 to 6, 6 to 8, 8 to 10, 10 to 12, 12 to 20)

    fun inspect(
        request: ChooseCultivationActionRequest,
        assumption: CultivationDrawAssumption = CultivationDrawAssumption.TYPICAL_ONE_EXTRA
    ): BuildActionComparison {
        val context = request.context
        require(context != DecisionContext.EMPTY && context.phase == RoundCardType.CULTIVATION) {
            "Common Build projection requires a visible Cultivation decision context"
        }
        val actions = request.legalChoices.mapNotNull { (it as? CultivationAction.Main) }
        val zones = FutureDiceZones.fromBoard(context.self.board)
        val rewards = RollRewardOpportunities.fromVisibleContext(context)
        val futureRounds = context.progress.upcomingRoundTypes
        val baseFuture = FutureDiceForecast.project(zones, futureRounds, assumption, rewards).expectedValue
        val buyPower = PurchaseThresholdHeuristics.purchasingPower(context.self.board)
        val availableCosts = PurchaseThresholdHeuristics.availableCostTiers(context.grove)
        return BuildActionComparison(actions.map { choice ->
            val action = choice.action
            val effect = when (action) {
                CultivationMainAction.Draw -> null
                is CultivationMainAction.ActivatePlant -> action.card.card.effect
                CultivationMainAction.RoundEffect1 -> request.roundCard.firstEffect.effect
                CultivationMainAction.RoundEffect2 -> request.roundCard.secondEffect.effect
            }
            val sourceIsPlant = action is CultivationMainAction.ActivatePlant
            when {
                action == CultivationMainAction.Draw -> measureDraw(
                    choice, "Draw one die", zones, rewards, futureRounds, assumption, baseFuture, buyPower, availableCosts
                )
                effect == GameEffect.UPGRADE_DIE_FROM_HAND -> measureCompost(
                    choice, context, zones, rewards, futureRounds, assumption, baseFuture, buyPower, availableCosts
                )
                effect == GameEffect.RAISE_DIE_PLUS_4 || effect == GameEffect.RAISE_ANY_DIE_PLUS_1 -> measureRaise(
                    choice, context, effect, sourceIsPlant, zones, rewards, futureRounds, assumption, baseFuture, buyPower, availableCosts
                )
                effect == GameEffect.DRAW_TWO_DICE -> measureDraw(
                    choice, "Draw two dice", zones, rewards, futureRounds, assumption, baseFuture, buyPower, availableCosts,
                    count = 2, plantActivation = sourceIsPlant
                )
                else -> unmodeled(choice, "${effect ?: action} requires resource, target, or strategic evaluation")
            }
        }, assumption)
    }

    private fun measureDraw(
        choice: CultivationAction.Main,
        description: String,
        zones: FutureDiceZones,
        rewards: RollRewardOpportunities,
        futureRounds: List<RoundCardType>,
        assumption: CultivationDrawAssumption,
        baseFuture: ImmediatePipValue,
        buyPower: Int,
        availableCosts: List<Int>,
        count: Int = 1,
        plantActivation: Boolean = false
    ): BuildActionValue {
        val outcome = drawIntoProjectedHand(zones, count)
        if (outcome.second.isEmpty()) return unmodeled(choice, "$description has no drawable die")
        // In normal rules an effect that says Draw 2 may draw fewer when the pool
        // is empty. The projected Hand and counted rolls must agree exactly.
        val immediate = outcome.second.fold(ImmediatePipValue(0.0)) { sum, die ->
            sum + ImmediatePipValuation.roll(die.sides, rewards)
        }
        val omissions = mutableListOf("Current Buy threshold probabilities and purchase consequences")
        if (plantActivation) omissions += "Plant facing/Refresh-cycle opportunity"
        return measured(choice, description, null, immediate, outcome.first,
            futureRounds, assumption, rewards, baseFuture, buyPower, availableCosts, omissions)
    }

    private fun measureCompost(
        choice: CultivationAction.Main,
        context: DecisionContext,
        zones: FutureDiceZones,
        rewards: RollRewardOpportunities,
        futureRounds: List<RoundCardType>,
        assumption: CultivationDrawAssumption,
        baseFuture: ImmediatePipValue,
        buyPower: Int,
        availableCosts: List<Int>
    ): BuildActionValue {
        val candidates = context.self.board.hand.mapNotNull { die ->
            val upgradedSides = upgradeSteps[die.sides] ?: return@mapNotNull null
            if ((context.grove.graftBed[DieSides.from(upgradedSides)] ?: 0) <= 0) return@mapNotNull null
            val diePosition = context.self.board.hand.indexOfFirst { it.index == die.index }
            val changed = zones.withHandUpgradeIntoDiscard("h$diePosition", upgradedSides)
            measured(choice, "Compost D${die.sides} to D$upgradedSides", die.index,
                ImmediatePipValue(-die.value.toDouble()), changed,
                futureRounds, assumption, rewards, baseFuture, buyPower, availableCosts,
                listOf("Current Buy power/threshold after losing showing die",
                    "Later Buy opportunities after physical replacement",
                    "The live Compost target also applies a separate affordable-Buy-tier cost"))
        }
        // The official Round effect can also Trash when no next die is available.
        // Never claim the Trash fallback is an Upgrade worth zero pips.
        return candidates.maxByOrNull { it.totalPipEquivalentChange ?: Double.NEGATIVE_INFINITY }
            ?: unmodeled(choice, "Compost has no available next-size Upgrade; optional Trash not modeled")
    }

    private fun measureRaise(
        choice: CultivationAction.Main,
        context: DecisionContext,
        effect: GameEffect,
        sourceIsPlant: Boolean,
        zones: FutureDiceZones,
        rewards: RollRewardOpportunities,
        futureRounds: List<RoundCardType>,
        assumption: CultivationDrawAssumption,
        baseFuture: ImmediatePipValue,
        buyPower: Int,
        availableCosts: List<Int>
    ): BuildActionValue {
        val amount = if (effect == GameEffect.RAISE_DIE_PLUS_4) 4 else 1
        val best = context.self.board.hand.map { die ->
            die to ImmediatePipValuation.raise(die.sides, die.value, amount)
        }.maxByOrNull { it.second.spendingPips }
            ?: return unmodeled(choice, "Raise has no legal Hand die to target")
        val omissions = mutableListOf("Current Buy tier / purchasing-power consequences")
        if (sourceIsPlant) omissions += "Plant facing/Refresh-cycle opportunity"
        omissions += "The live Raise target chooser may differ from the pip-best target"
        return measured(choice, "Raise D${best.first.sides} by $amount", best.first.index,
            best.second, zones, futureRounds, assumption, rewards, baseFuture, buyPower, availableCosts, omissions)
    }

    private fun measured(
        choice: CultivationAction.Main,
        description: String,
        target: Int?,
        immediate: ImmediatePipValue,
        changed: FutureDiceZones,
        futureRounds: List<RoundCardType>,
        assumption: CultivationDrawAssumption,
        rewards: RollRewardOpportunities,
        baseFuture: ImmediatePipValue,
        buyPower: Int,
        availableCosts: List<Int>,
        omissions: List<String>
    ): BuildActionValue {
        val afterFuture = FutureDiceForecast.project(changed, futureRounds, assumption, rewards).expectedValue
        val delta = afterFuture.spendingPips - baseFuture.spendingPips
        val futureRewards = (afterFuture.critterOptionValue + afterFuture.wispOptionValue) -
            (baseFuture.critterOptionValue + baseFuture.wispOptionValue)
        // Thresholds are informational. A predicted Draw mean is NOT the
        // probability of reaching a Buy tier; protected Critters and spending
        // choices need a separate model. Never add a flat tier bonus here.
        val expectedBuyPower = buyPower + immediate.spendingPips
        val crossed = availableCosts.filter { cost ->
            (cost > buyPower && cost <= expectedBuyPower) ||
                (cost <= buyPower && cost > expectedBuyPower)
        }
        return BuildActionValue(
            choice = choice, description = description, targetHandIndex = target,
            immediate = immediate, futureSpendingPipChange = delta,
            futureRewardEquivalentChange = futureRewards,
            currentBuyThresholdsPossiblyCrossed = crossed,
            coverage = if (omissions.isEmpty()) BuildValueCoverage.PIP_MODEL else BuildValueCoverage.PARTIAL,
            unmodeledConsequences = omissions
        )
    }

    private fun unmodeled(choice: CultivationAction.Main, reason: String): BuildActionValue = BuildActionValue(
        choice, "${choice.action}", null, null, null, null, emptyList(),
        BuildValueCoverage.UNMODELED, listOf(reason)
    )

    /** Same lowest-sided-first Draw and Discard refill as the game, without RNG. */
    private fun drawIntoProjectedHand(zones: FutureDiceZones, count: Int): Pair<FutureDiceZones, List<FutureDie>> {
        val supply = zones.supply.toMutableList()
        val discard = zones.discard.toMutableList()
        val hand = zones.hand.toMutableList()
        val drawn = mutableListOf<FutureDie>()
        repeat(count) {
            if (supply.isEmpty()) {
                supply.addAll(discard)
                discard.clear()
            }
            val first = supply.indices.minByOrNull { supply[it].sides } ?: return@repeat
            val die = supply.removeAt(first)
            hand.add(die)
            drawn.add(die)
        }
        return FutureDiceZones(supply, hand, discard) to drawn
    }
}
