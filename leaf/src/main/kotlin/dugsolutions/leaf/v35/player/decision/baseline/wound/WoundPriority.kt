package dugsolutions.leaf.v35.player.decision.baseline.wound

import dugsolutions.leaf.v35.player.decision.baseline.battle.BattleEnabledPlantAnalyzer
import dugsolutions.leaf.v35.player.decision.baseline.card.PlantPreservationEvaluator
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.wound.WoundChoice

/** Prefer sacrificing the legal Plant whose relevant current value is smallest. */
object WoundPriority {
    fun score(
        context: DecisionContext,
        choice: WoundChoice,
        battleEnabledPlantAnalyzer: BattleEnabledPlantAnalyzer,
        plantPreservationEvaluator: PlantPreservationEvaluator
    ): PriorityScore {
        val view = context.self.board.creature.firstOrNull { it.id == choice.card.id }
            ?: return PriorityScore(0)

        return when (choice) {
            is WoundChoice.Flip -> {
                val contribution = battleEnabledPlantAnalyzer(context, view).priority.total
                PriorityScore(0)
                    .adjusted(-contribution, "Preserve stronger immediate Battle Plant contribution")
            }

            is WoundChoice.Snip -> {
                val preservation = plantPreservationEvaluator(context, view)
                PriorityScore(0)
                    .adjusted(-preservation, "Preserve greater permanent Plant value")
            }
        }
    }
}
