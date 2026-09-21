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

/**
 * Human Baseline policy for one decision opportunity during Cultivation Build.
 *
 * The rules engine owns legality and repeatedly supplies the currently legal
 * choices. This strategy only ranks those choices. Main Actions are scored as
 * Draw, Plant activation, or one of the two Round Effects; Support Actions and
 * Done compete in the same candidate set. Shared [BaselineScoreEngine] logic
 * then applies card influences and uses strategy RNG only to break equal scores.
 *
 * The current implementation is still under Milestone-2 designer review. Its
 * detailed A1 architecture inventory, legal-choice table, current scoring entry
 * points, tests, and known review questions are documented in:
 *
 * `doc/HUMAN_BASELINE_CULTIVATION.md`
 *
 * That document describes the current implementation; it is not yet the final
 * certified Cultivation behavior contract.
 */
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
