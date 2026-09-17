package dugsolutions.leaf.simulation.v35.strategy.planned

import dugsolutions.leaf.v35.player.decision.baseline.buy.PurchaseScoreModifier
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.buy.BuyItem
import dugsolutions.leaf.v35.player.decision.context.DecisionContext

/**
 * Adds the simple Planned Baseline purchase preference from the Codex plan.
 *
 * Human Baseline still supplies the ordinary purchase score. This modifier
 * only nudges targeted Plant cards while their requested copy count remains
 * unmet: +40 for the first desired copy, +30 for later desired copies.
 */
class CardFocusModifier(
    private val plan: CreaturePlan,
    private val firstCopyBonus: Int = FIRST_COPY_BONUS,
    private val additionalCopyBonus: Int = ADDITIONAL_COPY_BONUS
) : PurchaseScoreModifier {

    override fun modify(
        context: DecisionContext,
        item: BuyItem,
        score: PriorityScore
    ): PriorityScore {
        if (item !is BuyItem.Plant) return score

        val target = TargetCard(item.card.name)
        val desiredCopies = plan.targetCount(target)
        if (desiredCopies <= 0) return score

        val ownedCopies = context.self.board.creature.count { it.name == item.card.name }
        if (ownedCopies >= desiredCopies) return score

        val bonus = if (ownedCopies == 0) firstCopyBonus else additionalCopyBonus
        val remainingAfterPurchase = desiredCopies - (ownedCopies + 1)
        val copyDescription = if (ownedCopies == 0) "first" else "additional"
        val remainingText = if (remainingAfterPurchase > 0) {
            "; ${remainingAfterPurchase} more planned after this purchase"
        } else {
            "; plan target reached by this purchase"
        }

        return score.adjusted(
            amount = bonus,
            reason = "Creature plan wants $copyDescription copy of ${item.card.title} " +
                "(${ownedCopies}/${desiredCopies} owned$remainingText)"
        )
    }

    companion object {
        const val FIRST_COPY_BONUS: Int = 40
        const val ADDITIONAL_COPY_BONUS: Int = 30
    }
}
