package dugsolutions.leaf.v35.player.decision.baseline.cultivation.resource

import dugsolutions.leaf.v35.player.decision.baseline.common.DieValueHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.common.PurchaseThresholdHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext

object SunlightPriority {
    fun score(context: DecisionContext): PriorityScore {
        val best = context.self.board.hand.maxOfOrNull {
            DieValueHeuristics.actualRaiseGain(it, 3)
        } ?: 0
        var score = PriorityScore(35 + 5 * best)
        val power = PurchaseThresholdHeuristics.purchasingPower(context.self.board)
        score = score.adjusted(
            PurchaseThresholdHeuristics.thresholdBonus(power, power + best, PurchaseThresholdHeuristics.availableCostTiers(context.grove), 10),
            "Buy threshold gained by Sunlight"
        )
        return score
    }
}
