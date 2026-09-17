package dugsolutions.leaf.v35.player.decision.baseline.buy

import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.buy.BuyItem
import dugsolutions.leaf.v35.player.decision.context.DecisionContext

/**
 * Optional composable adjustment applied after ordinary Human Baseline purchase
 * scoring and before the final candidate is compared with other legal buys.
 *
 * Core Human Baseline uses [NONE]. Simulation layers can provide modifiers for
 * research behaviors (for example a Creature plan) without teaching the core
 * baseline about those experimental goals.
 */
fun interface PurchaseScoreModifier {
    fun modify(
        context: DecisionContext,
        item: BuyItem,
        score: PriorityScore
    ): PriorityScore

    companion object {
        val NONE: PurchaseScoreModifier = PurchaseScoreModifier { _, _, score -> score }
    }
}
