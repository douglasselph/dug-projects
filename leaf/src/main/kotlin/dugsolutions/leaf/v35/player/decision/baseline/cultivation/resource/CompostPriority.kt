package dugsolutions.leaf.v35.player.decision.baseline.cultivation.resource

import dugsolutions.leaf.v35.player.decision.baseline.common.PurchaseThresholdHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.random.die.DieSides

object CompostPriority {
    fun score(context: DecisionContext): PriorityScore {
        val hand = context.self.board.hand
        val available = context.grove.graftBed.filterValues { it > 0 }.keys
        val candidates = hand.mapNotNull { die ->
            val current = DieSides.entries.firstOrNull { it.value == die.sides } ?: return@mapNotNull null
            val next = DieSides.entries.dropWhile { it != current }.drop(1).firstOrNull { it in available }
                ?: return@mapNotNull null
            Triple(die, current, next)
        }
        val best = candidates.maxByOrNull { (_, current, next) -> next.value - current.value }
            ?: return PriorityScore(15)

        val (die, current, next) = best
        val upgrade = next.value - current.value
        val power = PurchaseThresholdHeuristics.purchasingPower(context.self.board)
        val tiers = PurchaseThresholdHeuristics.availableCostTiers(context.grove)
        val thresholdLoss = PurchaseThresholdHeuristics.thresholdBonus(power, (power - die.value).coerceAtLeast(0), tiers, 10)
        return PriorityScore(75)
            .adjusted(upgrade * 2, "Permanent D${current.value} to D${next.value} upgrade")
            .adjusted(thresholdLoss, "Current-round Buy threshold impact")
            .adjusted((context.progress.cultivationRoundsRemaining ?: 0).coerceAtMost(5) * 2, "Future rounds benefit from upgrade")
    }
}
