package dugsolutions.leaf.v35.player.decision.baseline.cultivation

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.CardPhase
import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import dugsolutions.leaf.v35.player.decision.baseline.cultivation.resource.*
import dugsolutions.leaf.v35.player.decision.baseline.scoring.BaselineScoreEngine
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.baseline.scoring.ScoredChoice
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.cultivation.*
import dugsolutions.leaf.v35.player.decision.mechanical.cultivation.MechanicalCultivationStrategy

class HumanBaselineCultivationStrategy(
    private val delegate: CultivationStrategy = MechanicalCultivationStrategy(),
    internal val scoreEngine: BaselineScoreEngine = BaselineScoreEngine(),
    internal val cardScorers: HumanBaselineCardScorerRegistry = HumanBaselineCardScorerRegistry()
) : CultivationStrategy {
    override fun chooseAction(request: ChooseCultivationActionRequest): CultivationAction {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseAction(request)
        return scoreEngine.chooseValue(
            request.legalChoices.map { choice -> ScoredChoice(choice, score(request, choice)) }
        )
    }

    private fun score(request: ChooseCultivationActionRequest, choice: CultivationAction): PriorityScore =
        when (choice) {
            CultivationAction.Done -> PriorityScore(if (request.mainActionsRemaining == 0) 55 else 0)
            is CultivationAction.Support -> PriorityScore(30)
            is CultivationAction.Main -> when (val action = choice.action) {
                CultivationMainAction.Draw -> DrawPriority.score(request.context)
                is CultivationMainAction.ActivatePlant ->
                    cardScorers.forPlant(action.card.card).playScore(
                        context = request.context,
                        phase = CardPhase.CULTIVATION,
                        cardName = action.card.card.name
                    )
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
