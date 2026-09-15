package dugsolutions.leaf.v35.player.decision.baseline.cultivation

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.CardPhase
import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import dugsolutions.leaf.v35.player.decision.baseline.cultivation.resource.*
import dugsolutions.leaf.v35.player.decision.baseline.influence.BaselineInfluenceRegistry
import dugsolutions.leaf.v35.player.decision.baseline.scoring.BaselineScoreEngine
import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionCandidate
import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionTag
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.cultivation.*
import dugsolutions.leaf.v35.player.decision.mechanical.cultivation.MechanicalCultivationStrategy
import dugsolutions.leaf.v35.player.decision.support.SupportAction

class HumanBaselineCultivationStrategy(
    private val delegate: CultivationStrategy = MechanicalCultivationStrategy(),
    internal val scoreEngine: BaselineScoreEngine = BaselineScoreEngine(),
    internal val cardScorers: HumanBaselineCardScorerRegistry = HumanBaselineCardScorerRegistry(),
    internal val influenceRegistry: BaselineInfluenceRegistry = BaselineInfluenceRegistry(cardScorers)
) : CultivationStrategy {
    override fun chooseAction(request: ChooseCultivationActionRequest): CultivationAction {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseAction(request)
        return scoreEngine.chooseValue(
            context = request.context,
            candidates = request.legalChoices.map { choice ->
                DecisionCandidate(
                    choice = choice,
                    score = score(request, choice),
                    tags = tags(request, choice)
                )
            },
            influenceRegistry = influenceRegistry
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

    private fun tags(
        request: ChooseCultivationActionRequest,
        choice: CultivationAction
    ): Set<DecisionTag> =
        when (choice) {
            CultivationAction.Done -> emptySet()
            is CultivationAction.Support -> supportTags(choice.action)
            is CultivationAction.Main -> when (choice.action) {
                CultivationMainAction.Draw -> emptySet()
                is CultivationMainAction.ActivatePlant -> emptySet()
                CultivationMainAction.RoundEffect1 -> roundEffectTags(request.roundCard.firstEffect.effect)
                CultivationMainAction.RoundEffect2 -> roundEffectTags(request.roundCard.secondEffect.effect)
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

    private fun roundEffectTags(effect: GameEffect): Set<DecisionTag> =
        when (effect) {
            GameEffect.GAIN_WATER_TOKEN -> WaterPriority.tags
            GameEffect.MULCH_DIE_FROM_HAND -> setOf(DecisionTag.ACQUIRE_MULCH)
            else -> emptySet()
        }

    private fun supportTags(action: SupportAction): Set<DecisionTag> =
        when (action) {
            is SupportAction.PlayWisp -> setOf(DecisionTag.PLAY_WISP)
            is SupportAction.UseWaterReroll -> setOf(DecisionTag.SPEND_WATER)
            SupportAction.UseWaterRefresh -> setOf(
                DecisionTag.SPEND_WATER,
                DecisionTag.REFRESH_CREATURE
            )
            is SupportAction.UseMulch -> setOf(DecisionTag.SPEND_MULCH)
            is SupportAction.UseWormFlip -> setOf(
                DecisionTag.SPEND_WORM,
                DecisionTag.REFRESH_CREATURE
            )
            is SupportAction.UseButterfly -> emptySet()
        }
}
