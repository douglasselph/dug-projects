package dugsolutions.leaf.v35.player.decision.baseline.projection

import dugsolutions.leaf.v35.battle.domain.BattleSquare
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattleAnalysisMode
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattleDiePlacementAnalyzer
import dugsolutions.leaf.v35.player.decision.baseline.common.DieValueHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.common.PurchaseThresholdHeuristics
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import kotlin.math.roundToInt

/**
 * Deterministic, visible-information forecast of consecutive Draws.
 * Each unknown die is assigned its arithmetic mean, not a sampled outcome.
 * This projection neither mutates supply/discard nor awards speculative Roll Rewards.
 *
 * Results are a scoring aid, NOT legal game-state dice. In Battle, more than one
 * predicted placement is an approximation: rows are not mutated between Draws.
 */
object ExpectedDrawProjection {
    data class Forecast(
        val sides: List<Int>,
        val values: List<Double>,
        val battleCapacity: Int,
        val projectedPlacementCount: Int,
        val buyingPowerDelta: Double,
        val buyTiersCrossed: List<Int>,
        val tacticalEstimate: Double
    ) {
        val totalMean: Double get() = values.sum()
    }

    /** Pure helper for supply/refill ordering, independently testable without a game. */
    fun drawSides(supply: List<Int>, discard: List<Int>, count: Int): List<Int> {
        require(count >= 0)
        val available = supply.sorted().toMutableList()
        val recycled = discard.sorted().toMutableList()
        return buildList {
            repeat(count) {
                if (available.isEmpty()) {
                    available.addAll(recycled)
                    recycled.clear()
                }
                if (available.isNotEmpty()) add(available.removeAt(0))
            }
        }
    }

    fun forecast(context: DecisionContext, count: Int, removeHandDieIndex: Int? = null): Forecast {
        val board = context.self.board
        val sides = drawSides(board.supply.map { it.sides }, board.discard.map { it.sides }, count)
        val means = sides.map(CultivationPipOutcome::expectedNewDie)
        val removed = board.hand.firstOrNull { it.index == removeHandDieIndex }
        val delta = means.sum() - (removed?.value ?: 0)
        val battle = context.battle
        val capacity = if (battle == null) Int.MAX_VALUE else battle.rows.sumOf { row ->
            val actor = row.forPlayer(context.self.id)
            if (row.closed || actor == null || actor.withdrawn) 0
            else (BattleSquare.MAX_DICE - actor.dice.size).coerceAtLeast(0)
        } + if (battle != null && removed != null) 1 else 0
        val placeCount = if (battle == null) sides.size else minOf(capacity, sides.size)
        val power = PurchaseThresholdHeuristics.purchasingPower(board)
        val tiers = PurchaseThresholdHeuristics.availableCostTiers(context.grove)
        val crossed = if (battle == null) tiers.filter { it > power && it <= power + delta } else emptyList()
        val tactical = if (battle == null) 0.0 else {
            val analyzer = BattleDiePlacementAnalyzer()
            means.take(placeCount).sumOf { mean ->
                analyzer(context, mean, BattleAnalysisMode.EXPECTED)
                    .maxOfOrNull { it.tacticalValue } ?: 0.0
            }
        }
        return Forecast(sides, means, capacity, placeCount, delta, crossed, tactical)
    }

    /** Predict an Upgrade-and-use-now without assuming a particular new roll. */
    fun upgradeDelta(oldSides: Int, oldValue: Int, newSides: Int): Double {
        require(oldSides > 0 && newSides > oldSides)
        require(oldValue in 1..oldSides)
        return DieValueHeuristics.expectedRoll(newSides) - oldValue
    }

    /** Shared scoring scale for Draw, replacement, and extra-draw effects. */
    fun scoreAdjustment(forecast: Forecast, battle: Boolean): Int =
        if (battle) {
            (forecast.tacticalEstimate * 1.0).roundToInt()
        } else {
            (forecast.buyingPowerDelta * 3).roundToInt() + forecast.buyTiersCrossed.size * 10
        }
}
