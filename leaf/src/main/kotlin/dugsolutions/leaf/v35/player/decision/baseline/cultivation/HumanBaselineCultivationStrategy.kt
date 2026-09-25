package dugsolutions.leaf.v35.player.decision.baseline.cultivation

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
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
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import dugsolutions.leaf.v35.player.decision.support.SupportAction

/**
 * Human Baseline policy for one decision opportunity during Cultivation Build.
 *
 * The rules engine owns legality and repeatedly supplies the currently legal
 * choices. This strategy ranks those choices on one common [PriorityScore]
 * scale; [BaselineScoreEngine] applies semantic card influences. Strategy RNG
 * breaks genuine ties and models the explicitly probabilistic Compost tendency.
 *
 * Target design, at a high level:
 *
 * - Main Actions compare Draw, Plant activation, and the two Round Effects by
 *   their actual ordinary-human benefit in the current state.
 * - Support Actions are scored from the specific benefit of the offered
 *   Wisp/reroll/refresh/Mulch/Worm/Butterfly, with soft reserve penalties for
 *   consumable resources.
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
 * B2 wiring makes threshold-sensitive Main Actions use the injected policy's
 * normal purchasing power (so protected Critters are not treated as ordinary
 * Buy power). Compost receives the policy's modest permanent dice-development
 * nudge. B3 routes Plant activation through [PlantActivationPriority], keeping
 * card-local valuation in the card scorer while applying the same cross-cutting
 * policy to Plants that unambiguously improve permanent dice-pool strength.
 * Draw deliberately does not receive that long-term nudge because drawing an
 * existing die does not increase total dice-pool power.
 *
 * B6 aligns downstream Effect choices with the value that justified the
 * top-level action: Compost, Mulch, and Sunlight share target scorers with the
 * Effect strategy; Cultivation die-targeting card effects use the same
 * reserve-aware purchasing-power model; and Petal To Die 4 shares branch
 * valuation between activation scoring and branch selection.
 *
 * Current implementation details and A1/A2 findings are documented in:
 *
 * `doc/HUMAN_BASELINE_CULTIVATION.md`
 *
 * The design rationale, approved target architecture, and completed certification
 * sequence are documented in:
 *
 * `doc/HUMAN_BASELINE_CULTIVATION_PLAN.md`
 *
 * Cross-cutting tuning knobs and their overridable policy layer are documented in:
 *
 * `doc/HUMAN_BASELINE_POLICY.md`
 *
 * The executable ordinary-human behavior contract is kept in:
 *
 * `src/test/kotlin/dugsolutions/leaf/v35/player/decision/baseline/cultivation/HumanBaselineCultivationStrategyTest.kt`
 *
 * Milestone-2 Cultivation is CERTIFIED. The approved behavior contract, focused
 * verification, target/branch alignment, designer review, and full unit +
 * integration + simulation regression all agree for this decision area.
 */
class HumanBaselineCultivationStrategy(
    private val delegate: CultivationStrategy = MechanicalCultivationStrategy(),
    internal val scoreEngine: BaselineScoreEngine = BaselineScoreEngine(),
    internal val cardScorers: HumanBaselineCardScorerRegistry = HumanBaselineCardScorerRegistry(),
    internal val influenceRegistry: BaselineInfluenceRegistry = BaselineInfluenceRegistry(cardScorers),
    internal val policy: HumanBaselinePolicy = HumanBaselinePolicy(),
    private val strategyRandomizer: StrategyRandomizer = StrategyRandomizer.create()
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
            is CultivationAction.Support -> CultivationSupportPriority.score(
                context = request.context,
                action = choice.action,
                cardScorers = cardScorers,
                policy = policy
            )
            is CultivationAction.Main -> when (val action = choice.action) {
                CultivationMainAction.Draw -> DrawPriority.score(request.context)
                is CultivationMainAction.ActivatePlant ->
                    PlantActivationPriority.score(
                        context = request.context,
                        card = action.card,
                        cardScorers = cardScorers,
                        policy = policy
                    )
                CultivationMainAction.RoundEffect1 -> scoreRoundEffect(
                    effect = request.roundCard.firstEffect.effect,
                    context = request.context,
                    mainActionsRemaining = request.mainActionsRemaining
                )
                CultivationMainAction.RoundEffect2 -> scoreRoundEffect(
                    effect = request.roundCard.secondEffect.effect,
                    context = request.context,
                    mainActionsRemaining = request.mainActionsRemaining
                )
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

    private fun scoreRoundEffect(
        effect: GameEffect,
        context: DecisionContext,
        mainActionsRemaining: Int
    ): PriorityScore =
        when (effect) {
            GameEffect.UPGRADE_DIE_FROM_HAND -> scoreCompost(
                context = context,
                mainActionsRemaining = mainActionsRemaining
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

    private fun scoreCompost(
        context: DecisionContext,
        mainActionsRemaining: Int
    ): PriorityScore {
        val normalPurchasingPower = policy.normalPurchasingPower(context)
        val score = CompostPriority.score(
            context = context,
            normalPurchasingPower = normalPurchasingPower,
            developmentBonus = policy.cultivationDiceDevelopmentBonus(context)
        )
        val percentage = CompostPriority.usePercentage(
            context = context,
            normalPurchasingPower = normalPurchasingPower,
            mainActionsRemaining = mainActionsRemaining
        )
        if (percentage <= 0) return score

        val accepted = strategyRandomizer.nextInt(100) < percentage
        return score.adjusted(
            amount = if (accepted) 0 else -100,
            reason = "Compost tendency ${if (accepted) "accepted" else "declined"} ($percentage%)"
        )
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
