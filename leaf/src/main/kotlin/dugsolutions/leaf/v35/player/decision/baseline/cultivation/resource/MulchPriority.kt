package dugsolutions.leaf.v35.player.decision.baseline.cultivation.resource

import dugsolutions.leaf.v35.player.decision.baseline.common.PurchaseThresholdHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext

object MulchPriority {
    fun score(context: DecisionContext): PriorityScore {
        val target = context.self.board.hand.maxByOrNull { die ->
            (if (die.value <= 2) 100 else 0) + die.sides - die.value * 4
        } ?: return PriorityScore(10)
        var score = PriorityScore(45)
        if (target.value <= 2) score = score.adjusted(20, "Low showing die is attractive to store")
        val stored = context.self.board.mulch.size + context.self.board.pendingMulch.size
        if (stored < 2) score = score.adjusted(10, "Fewer than two Mulched dice prepared")
        score = score.adjusted((target.sides / 4).coerceAtMost(5), "Higher-sided stored die has future value")

        val power = PurchaseThresholdHeuristics.purchasingPower(context.self.board)
        val after = (power - target.value).coerceAtLeast(0)
        score = score.adjusted(
            PurchaseThresholdHeuristics.thresholdBonus(power, after, PurchaseThresholdHeuristics.availableCostTiers(context.grove), 10),
            "Current-round Buy threshold impact"
        )
        return score
    }
}
