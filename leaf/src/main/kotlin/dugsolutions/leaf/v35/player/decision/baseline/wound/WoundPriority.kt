package dugsolutions.leaf.v35.player.decision.baseline.wound

import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.wound.WoundChoice

/** Prefer sacrificing the card with the smallest current Human Baseline loss value. */
object WoundPriority {
    fun score(
        context: DecisionContext,
        choice: WoundChoice,
        cardScorers: HumanBaselineCardScorerRegistry = HumanBaselineCardScorerRegistry()
    ): PriorityScore {
        val view = context.self.board.creature.firstOrNull { it.id == choice.card.id }
            ?: return PriorityScore(0)
        val loss = cardScorers.forPlant(view).lossValue(context, view)
        var score = PriorityScore(120 - loss)
        if (choice is WoundChoice.Snip) {
            score = score.adjusted(-20, "Snip is a permanent loss")
        } else {
            score = score.adjusted(5, "Flip is temporary")
        }
        return score
    }
}
