package dugsolutions.leaf.v35.player.decision.baseline.projection

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattleActionSwing
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattleSwingChange
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattleSwingEvaluator
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattleSwingState
import dugsolutions.leaf.v35.player.decision.baseline.common.DieValueHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.common.PurchaseThresholdHeuristics
import dugsolutions.leaf.v35.player.decision.context.DecisionContext

/**
 * Task 5B.3: an immutable, deterministic *prediction* model, not a rules-engine state.
 * All quantities may be fractional: an unknown D6 roll is represented by 3.5.
 * Neither this layer nor its operations roll dice, resolve Roll Rewards, or mutate a DecisionContext.
 *
 * Callers must establish legal targets and actions using the real decision machinery first.
 * In particular, a fractional prediction is never a physical die face or a legal target.
 */
data class ProjectedDie(val key: String, val sides: Int, val value: Double) {
    init {
        require(key.isNotBlank())
        require(sides > 0)
        require(value.isFinite() && value >= 0.0)
    }
}

/** Snapshot of one Strike from the actor's perspective. Opponent totals include players
 * who have finished their turns; liveOpponentKeys determines which opponents can respond.
 * Every map/list is treated as read-only input; projection operations replace collections.
 */
data class ProjectedStrike(
    val row: StrikeRow,
    val ownTotal: Double,
    val opponentTotals: Map<String, Double>,
    val liveOpponentKeys: Set<String> = opponentTotals.keys,
    val available: Boolean = true
) {
    init {
        require(ownTotal.isFinite() && ownTotal >= 0.0)
        require(opponentTotals.values.all { it.isFinite() && it >= 0.0 })
        require(liveOpponentKeys.all { it in opponentTotals })
    }
}

data class ProjectedState(
    val dice: List<ProjectedDie> = emptyList(),
    val buyingPower: Double = 0.0,
    val availableCosts: List<Int> = emptyList(),
    val strikes: List<ProjectedStrike> = emptyList()
) {
    init {
        require(buyingPower.isFinite() && buyingPower >= 0.0)
        require(dice.map { it.key }.distinct().size == dice.size) { "Projected dice must have unique keys" }
        require(strikes.map { it.row }.distinct().size == strikes.size) { "Projected rows must be unique" }
    }

    fun die(key: String): ProjectedDie = dice.single { it.key == key }
    fun strike(row: StrikeRow): ProjectedStrike = strikes.single { it.row == row }
}

/** Hypothetical, explicitly chosen transformations. This is not an action legality engine. */
sealed interface ProjectionStep {
    data class ExpectedDraw(val key: String, val sides: Int, val addToBuyPower: Boolean = true) : ProjectionStep
    data class RaiseDie(val key: String, val amount: Double, val countTowardBuyPower: Boolean = true) : ProjectionStep
    data class AddBuyingPower(val amount: Double) : ProjectionStep
    data class OwnStrikeDelta(val row: StrikeRow, val amount: Double) : ProjectionStep
    data class OpponentStrikeDelta(val row: StrikeRow, val opponentKey: String, val amount: Double) : ProjectionStep
}

/**
 * Functional projection: every step returns a new state, and sequential steps observe the
 * prior projected result. State values are prediction-only; never feed them to the game engine.
 */
object HumanStateProjection {
    fun apply(initial: ProjectedState, steps: Iterable<ProjectionStep>): ProjectedState =
        steps.fold(initial, ::apply)

    fun apply(state: ProjectedState, step: ProjectionStep): ProjectedState = when (step) {
        is ProjectionStep.ExpectedDraw -> {
            require(state.dice.none { it.key == step.key }) { "Duplicate projected die key ${step.key}" }
            val mean = DieValueHeuristics.expectedRoll(step.sides)
            state.copy(
                dice = state.dice + ProjectedDie(step.key, step.sides, mean),
                buyingPower = state.buyingPower + if (step.addToBuyPower) mean else 0.0
            )
        }
        is ProjectionStep.RaiseDie -> {
            require(step.amount.isFinite() && step.amount >= 0.0)
            val die = state.die(step.key)
            val newValue = (die.value + step.amount).coerceAtMost(die.sides.toDouble())
            state.copy(
                dice = state.dice.map { if (it.key == step.key) it.copy(value = newValue) else it },
                buyingPower = state.buyingPower + if (step.countTowardBuyPower) newValue - die.value else 0.0
            )
        }
        is ProjectionStep.AddBuyingPower -> {
            require(step.amount.isFinite())
            state.copy(buyingPower = (state.buyingPower + step.amount).also { require(it >= 0.0) })
        }
        is ProjectionStep.OwnStrikeDelta -> {
            require(step.amount.isFinite())
            state.copy(strikes = state.strikes.map { strike ->
                if (strike.row != step.row) strike else strike.copy(
                    ownTotal = (strike.ownTotal + step.amount).also { require(it >= 0.0) }
                )
            }.also { require(state.strikes.any { it.row == step.row }) })
        }
        is ProjectionStep.OpponentStrikeDelta -> {
            require(step.amount.isFinite())
            state.copy(strikes = state.strikes.map { strike ->
                if (strike.row != step.row) strike else {
                    val old = requireNotNull(strike.opponentTotals[step.opponentKey])
                    val updated = (old + step.amount).also { require(it >= 0.0) }
                    strike.copy(opponentTotals = strike.opponentTotals + (step.opponentKey to updated))
                }
            }.also { require(state.strikes.any { it.row == step.row }) })
        }
    }
}

/** One, non-overlapping assessment of before/after outcomes. */
data class ProjectedOutcome(
    val battle: BattleActionSwing?,
    val crossedBuyTiersUp: List<Int>,
    val crossedBuyTiersDown: List<Int>,
    val buyingPowerDelta: Double
)

/** Adapters to the existing Battle tactical scoring and Cultivation purchase-tier machinery.
 * Battle value counts one swing per changed row; no extra raw-die Raise bonus is added.
 * Purchase tiers use doubles and are never rounded to simulated physical faces.
 */
class HumanProjectionEvaluator(
    private val policy: HumanBaselinePolicy = HumanBaselinePolicy(),
    private val battleSwing: BattleSwingEvaluator = BattleSwingEvaluator(policy)
) {
    fun evaluate(context: DecisionContext, before: ProjectedState, after: ProjectedState): ProjectedOutcome {
        require(before.availableCosts == after.availableCosts) { "Cost catalog changes require a separate projection comparison" }
        val costs = PurchaseThresholdHeuristics.tiers(before.availableCosts)
        val up = costs.filter { it > before.buyingPower && it <= after.buyingPower }
        val down = costs.filter { it > after.buyingPower && it <= before.buyingPower }.asReversed()
        val changes = before.strikes.mapNotNull { old ->
            val newer = after.strikes.singleOrNull { it.row == old.row }
                ?: error("Missing projected row ${old.row}")
            if (old == newer || !old.available || !newer.available) null
            else BattleSwingChange(toSwingState(context, old), toSwingState(context, newer))
        }
        require(after.strikes.size == before.strikes.size) { "Projected row sets must match" }
        return ProjectedOutcome(
            battle = if (changes.isEmpty()) null else battleSwing(context, changes),
            crossedBuyTiersUp = up,
            crossedBuyTiersDown = down,
            buyingPowerDelta = after.buyingPower - before.buyingPower
        )
    }

    private fun toSwingState(context: DecisionContext, strike: ProjectedStrike): BattleSwingState {
        val benchmark = strike.opponentTotals.values.maxOrNull()
        val margin = benchmark?.let { strike.ownTotal - it }
        val live = strike.opponentTotals.filterKeys { it in strike.liveOpponentKeys }.values.maxOrNull()
        val liveMargin = live?.let { strike.ownTotal - it }
        val totals = listOf(strike.ownTotal) + strike.opponentTotals.values
        val high = totals.maxOrNull() ?: strike.ownTotal
        val everyoneTied = totals.size > 1 && totals.all { kotlin.math.abs(it - high) < EPSILON }
        val winning = !everyoneTied && strike.ownTotal >= high - EPSILON
        return BattleSwingState(
            row = strike.row,
            available = strike.available,
            currentlyWinning = winning,
            woundRisk = !winning && high - strike.ownTotal >= 5.0 - EPSILON,
            securedForNow = winning && (live == null || requireNotNull(liveMargin) >= policy.battleSecuredLead(context)),
            scoreMargin = margin,
            liveThreatMargin = liveMargin
        )
    }

    private companion object { const val EPSILON = 1e-9 }
}
