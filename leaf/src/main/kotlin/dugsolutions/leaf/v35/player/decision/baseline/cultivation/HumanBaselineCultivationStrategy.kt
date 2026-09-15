package dugsolutions.leaf.v35.player.decision.baseline.cultivation

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.cultivation.resource.*
import dugsolutions.leaf.v35.player.decision.baseline.scoring.BaselineScoreEngine
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.baseline.scoring.ScoredChoice
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.cultivation.*
import dugsolutions.leaf.v35.player.decision.mechanical.cultivation.MechanicalCultivationStrategy

class HumanBaselineCultivationStrategy(
    private val delegate: CultivationStrategy = MechanicalCultivationStrategy(),
    internal val scoreEngine: BaselineScoreEngine = BaselineScoreEngine()
) : CultivationStrategy {
    override fun chooseAction(request: ChooseCultivationActionRequest): CultivationAction {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseAction(request)
        val choices = request.legalChoices.map { choice ->
            ScoredChoice(choice, score(request, choice))
        }
        return scoreEngine.chooseValue(choices)
    }

    private fun score(request: ChooseCultivationActionRequest, choice: CultivationAction): PriorityScore =
        when (choice) {
            CultivationAction.Done -> PriorityScore(if (request.mainActionsRemaining == 0) 55 else 0)
            is CultivationAction.Support -> PriorityScore(30) // generic support is intentionally conservative in Cultivation
            is CultivationAction.Main -> when (choice.action) {
                CultivationMainAction.Draw -> DrawPriority.score(request.context)
                is CultivationMainAction.ActivatePlant -> PriorityScore(50) // Step 7 replaces this placeholder with card scoring
                CultivationMainAction.RoundEffect1 -> scoreRoundEffect(request.roundCard.firstEffect.effect, request.context)
                CultivationMainAction.RoundEffect2 -> scoreRoundEffect(request.roundCard.secondEffect.effect, request.context)
            }
        }

    private fun scoreRoundEffect(effect: GameEffect, context: DecisionContext): PriorityScore =
        when (effect) {
            GameEffect.UPGRADE_DIE_FROM_HAND -> CompostPriority.score(context)
            GameEffect.MULCH_DIE_FROM_HAND -> MulchPriority.score(context)
            GameEffect.GAIN_WATER_TOKEN -> WaterPriority.score(context)
            GameEffect.RAISE_DIE_PLUS_3 -> SunlightPriority.score(context)
            else -> PriorityScore(45)
        }
}
