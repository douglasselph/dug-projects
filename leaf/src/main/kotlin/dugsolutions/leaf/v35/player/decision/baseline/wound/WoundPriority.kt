package dugsolutions.leaf.v35.player.decision.baseline.wound

import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.wound.WoundChoice

/**
 * Provisional non-card loss valuation used until Step 7 supplies per-card
 * acquire/play/loss values. Lower-cost/lower-VP Plants are preferred victims.
 */
object WoundPriority {
    fun score(context: DecisionContext, choice: WoundChoice): PriorityScore {
        val view = context.self.board.creature.firstOrNull { it.id == choice.card.id }
        val cost = view?.cost ?: choice.card.card.cost
        val vp = when (val rule = view?.scoringRule ?: choice.card.card.scoringRule) {
            is PlantScoringRule.Fixed -> rule.points
            PlantScoringRule.PerButterfly -> context.self.board.butterflies.size
            PlantScoringRule.PerGraftedVine -> context.self.board.creature.count {
                it.type == dugsolutions.leaf.v35.plant.domain.PlantType.VINE
            }
        }
        val permanentPenalty = if (choice is WoundChoice.Snip) 20 + vp * 8 else 0
        return PriorityScore(base = 100)
            .adjusted(-cost * 3, "Prefer losing a less expensive Plant")
            .adjusted(-permanentPenalty, if (choice is WoundChoice.Snip) "Avoid permanent Snip/VP loss" else "Flip is temporary")
    }
}
