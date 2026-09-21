package dugsolutions.leaf.v35.player.decision.baseline.cultivation

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
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
 * choices. This strategy ranks those choices on one common [PriorityScore]
 * scale; [BaselineScoreEngine] applies semantic card influences and uses
 * strategy RNG only to break genuine ties.
 *
 * Target design, at a high level:
 *
 * - Main Actions compare Draw, Plant activation, and the two Round Effects by
 *   their actual ordinary-human benefit in the current state.
 * - Support Actions should ultimately be scored from the specific benefit of
 *   the offered Wisp/reroll/refresh/Mulch/Worm/Butterfly rather than sharing a
 *   generic Support score.
 * - After both Main Actions, `Done` is a benchmark: useful Supports may beat it,
 *   while weak or wasteful Supports should be saved and lose to it.
 * - When an action's value depends on a target (for example Compost, Mulch, or
 *   Sunlight), the top-level scorer and later Effect Choice must use compatible
 *   target valuation so the chosen target realizes the value that justified
 *   choosing the action.
 * - Shared development, reserve, purchase-threshold, card-scoring, and
 *   influence heuristics should be used only where they correspond to simple,
 *   explainable ordinary-human judgment rather than deep optimization.
 *
 * B2 wiring now makes the threshold-sensitive Round Effects use the injected
 * policy's normal purchasing power (so protected Critters are not treated as
 * ordinary Buy power). Compost also receives the policy's modest permanent
 * dice-development nudge. Draw deliberately does not receive that long-term
 * nudge because drawing an existing die does not increase total dice-pool power.
 *
 * Current implementation details and A1/A2 findings are documented in:
 *
 * `doc/HUMAN_BASELINE_CULTIVATION.md`
 *
 * The forward-looking target architecture, open design questions, test plan,
 * and remaining certification sequence are documented in:
 *
 * `doc/HUMAN_BASELINE_CULTIVATION_PLAN.md`
 *
 * Cross-cutting tuning knobs and their overridable policy layer are documented in:
 *
 * `doc/HUMAN_BASELINE_POLICY.md`
 *
 * Cultivation is still under Milestone-2 implementation and verification. The
 * A3/A4 behavior direction is approved, but the area is not certified until the
 * remaining B/C checkpoints and full regression are complete.
 */
class HumanBaselineCultivationStrategy(
    private val delegate: CultivationStrategy = MechanicalCultivationStrategy(),
    internal val scoreEngine: BaselineScoreEngine = BaselineScoreEngine(),
    internal val cardScorers: HumanBaselineCardScorerRegistry = HumanBaselineCardScorerRegistry(),
    internal val influenceRegistry: BaselineInfluenceRegistry = BaselineInfluenceRegistry(cardScorers),
    internal val policy: HumanBaselinePolicy = HumanBaselinePolicy()
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
            CultivationAction.Done -> PriorityScore(
                if (request.mainActionsRemaining == 0) policy.cultivationDoneScore(request.context) else 0
            )
            is CultivationAction.Support -> PriorityScore(30)
            is CultivationAction.Main -> when (val action = choice.action) {
                CultivationMainAction.Draw -> DrawPriority.score(request.context)
                is CultivationMainAction.ActivatePlant ->
                    cardScorers.forPlant(action.card.card).playScore(
                        context = request.context,
                        phase = CardPhase.CULTIVATION,
                        cardName = action.card.card.name,
                        normalPurchasingPower = policy.normalPurchasingPower(request.context)
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
            GameEffect.UPGRADE_DIE_FROM_HAND -> CompostPriority.score(
                context = context,
                normalPurchasingPower = policy.normalPurchasingPower(context),
                developmentBonus = policy.cultivationDiceDevelopmentBonus(context)
            )
            GameEffect.MULCH_DIE_FROM_HAND -> MulchPriority.score(
                context = context,
                normalPurchasingPower = policy.normalPurchasingPower(context)
            )
            GameEffect.GAIN_WATER_TOKEN -> WaterPriority.score(context)
            GameEffect.RAISE_DIE_PLUS_3 -> SunlightPriority.score(
                context = context,
                normalPurchasingPower = policy.normalPurchasingPower(context)
            )
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
